"""Scheduling engine for medication reminders."""
from __future__ import annotations

import logging
import threading
from datetime import datetime, timedelta
from typing import Callable, Optional

from .models import DoseHistoryEntry, Medication, UpcomingDose
from .storage import ReminderStorage

LOGGER = logging.getLogger(__name__)


DueHandler = Callable[[UpcomingDose, Medication, Callable[[], None], Callable[[int], None], Callable[[], None]], None]


class ReminderScheduler:
    """Service that polls for pending doses and triggers notifications."""

    def __init__(
        self,
        storage: ReminderStorage,
        due_handler: Optional[DueHandler] = None,
        poll_interval: int = 60,
    ) -> None:
        self.storage = storage
        self.due_handler = due_handler
        self.poll_interval = poll_interval
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None
        self._lock = threading.RLock()

    # Lifecycle ----------------------------------------------------------

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._run, name="ReminderScheduler", daemon=True)
        self._thread.start()
        LOGGER.info("Reminder scheduler started")

    def stop(self) -> None:
        self._stop_event.set()
        if self._thread:
            self._thread.join()
            LOGGER.info("Reminder scheduler stopped")

    # Scheduling operations ----------------------------------------------

    def add_medication(self, medication: Medication) -> None:
        """Persist a medication and ensure an upcoming dose exists."""
        self.storage.upsert_medication(medication)
        self.ensure_next_dose(medication)

    def ensure_next_dose(self, medication: Medication) -> None:
        if not medication.schedule:
            return
        doses = [
            dose
            for dose in self.storage.load_upcoming_doses()
            if dose.medication_id == medication.medication_id and dose.status == "pending"
        ]
        if doses:
            return
        next_due = medication.schedule.next_due(after=datetime.now())
        if next_due is None:
            return
        dose = UpcomingDose.create(medication.medication_id, next_due)
        self.storage.upsert_upcoming_dose(dose)
        LOGGER.debug("Created initial dose %s for medication %s", dose.dose_id, medication.name)

    def snooze(self, dose_id: str, minutes: int) -> None:
        with self._lock:
            dose = self.storage.get_upcoming_dose(dose_id)
            if not dose:
                return
            dose.snoozed_until = datetime.now() + timedelta(minutes=minutes)
            dose.notified = False
            self.storage.upsert_upcoming_dose(dose)
            LOGGER.info("Snoozed dose %s for %s minutes", dose_id, minutes)

    def mark_taken(self, dose_id: str) -> None:
        with self._lock:
            dose = self.storage.get_upcoming_dose(dose_id)
            if not dose:
                return
            dose.status = "taken"
            dose.taken_at = datetime.now()
            self.storage.upsert_upcoming_dose(dose)
            history = DoseHistoryEntry(
                dose_id=dose.dose_id,
                medication_id=dose.medication_id,
                scheduled_time=dose.scheduled_time,
                status="taken",
                acted_at=dose.taken_at,
            )
            self.storage.append_history(history)
            self.storage.remove_upcoming_dose(dose.dose_id)
            LOGGER.info("Marked dose %s as taken", dose_id)
            self._schedule_follow_up(dose)

    def mark_missed(self, dose_id: str) -> None:
        with self._lock:
            dose = self.storage.get_upcoming_dose(dose_id)
            if not dose:
                return
            dose.status = "missed"
            self.storage.upsert_upcoming_dose(dose)
            history = DoseHistoryEntry(
                dose_id=dose.dose_id,
                medication_id=dose.medication_id,
                scheduled_time=dose.scheduled_time,
                status="missed",
                acted_at=datetime.now(),
            )
            self.storage.append_history(history)
            self.storage.remove_upcoming_dose(dose.dose_id)
            LOGGER.info("Marked dose %s as missed", dose_id)
            self._schedule_follow_up(dose)

    # Internal helpers ---------------------------------------------------

    def _schedule_follow_up(self, dose: UpcomingDose) -> None:
        medication = self.storage.get_medication(dose.medication_id)
        if not medication or not medication.schedule:
            return
        next_due = medication.schedule.next_due(after=dose.scheduled_time)
        if next_due is None:
            return
        new_dose = UpcomingDose.create(dose.medication_id, next_due)
        self.storage.upsert_upcoming_dose(new_dose)
        LOGGER.debug(
            "Scheduled follow up dose %s for medication %s at %s",
            new_dose.dose_id,
            medication.name,
            next_due,
        )

    def _emit_due(self, dose: UpcomingDose, medication: Medication) -> None:
        if not self.due_handler:
            return

        def _taken() -> None:
            self.mark_taken(dose.dose_id)

        def _snooze(minutes: int) -> None:
            self.snooze(dose.dose_id, minutes)

        def _missed() -> None:
            self.mark_missed(dose.dose_id)

        self.due_handler(dose, medication, _taken, _snooze, _missed)

    def _run(self) -> None:
        while not self._stop_event.is_set():
            now = datetime.now()
            medications = {m.medication_id: m for m in self.storage.load_medications()}
            for dose in self.storage.load_upcoming_doses():
                medication = medications.get(dose.medication_id)
                if not medication:
                    continue
                if dose.status != "pending":
                    continue
                due_time = dose.effective_due_time()
                if due_time <= now and not dose.notified:
                    LOGGER.debug(
                        "Dose %s for medication %s is due at %s",
                        dose.dose_id,
                        medication.name,
                        due_time,
                    )
                    dose.notified = True
                    self.storage.upsert_upcoming_dose(dose)
                    self._emit_due(dose, medication)
            self._stop_event.wait(self.poll_interval)


__all__ = ["ReminderScheduler", "DueHandler"]
