"""Entry point that wires the reminder scheduler, storage and UI together."""
from __future__ import annotations

import logging
from datetime import datetime, timedelta, time as time_cls
from pathlib import Path
from typing import List

from src.reminders.models import DoseSchedule, Medication
from src.reminders.scheduler import ReminderScheduler
from src.reminders.storage import ReminderStorage
from src.ui.notifications import NotificationManager

LOGGER = logging.getLogger(__name__)


def configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )


def build_scheduler(storage: ReminderStorage) -> ReminderScheduler:
    notification_manager = NotificationManager()

    def handle_due(dose, medication, on_taken, on_snooze, on_missed):
        notification_manager.show_notification(dose, medication, on_taken, on_snooze, on_missed)

    scheduler = ReminderScheduler(storage, due_handler=handle_due)
    return scheduler


def ensure_pending_doses(scheduler: ReminderScheduler, storage: ReminderStorage) -> None:
    for medication in storage.load_medications():
        scheduler.ensure_next_dose(medication)


def prompt(text: str) -> str:
    try:
        return input(text)
    except EOFError:  # pragma: no cover - CLI convenience
        return ""


def parse_times(times_str: str) -> List[time_cls]:
    values = []
    for chunk in times_str.split(","):
        chunk = chunk.strip()
        if not chunk:
            continue
        values.append(datetime.strptime(chunk, "%H:%M").time())
    return values


def add_medication_interactively(storage: ReminderStorage, scheduler: ReminderScheduler) -> None:
    name = prompt("Medication name: ").strip()
    if not name:
        print("Name is required")
        return
    dosage = prompt("Dosage (e.g. 10mg): ").strip()
    instructions = prompt("Instructions: ").strip()
    start_raw = prompt("First dose time (YYYY-MM-DD HH:MM): ").strip()
    try:
        start_time = datetime.strptime(start_raw, "%Y-%m-%d %H:%M")
    except ValueError:
        print("Invalid start time format")
        return

    repeat_raw = prompt("Repeat every N minutes (blank for none): ").strip()
    times_raw = prompt("Specific times of day (HH:MM, comma separated, optional): ").strip()

    repeat_interval = timedelta(minutes=int(repeat_raw)) if repeat_raw else None
    times_of_day = parse_times(times_raw) if times_raw else None

    medication = Medication(
        medication_id=name.lower().replace(" ", "-"),
        name=name,
        dosage=dosage,
        instructions=instructions,
        schedule=DoseSchedule(
            medication_id=name.lower().replace(" ", "-"),
            start_time=start_time,
            repeat_interval=repeat_interval,
            times_of_day=times_of_day,
        ),
    )

    storage.upsert_medication(medication)
    scheduler.ensure_next_dose(medication)
    print(f"Added medication {name}")


def list_medications(storage: ReminderStorage) -> None:
    medications = storage.load_medications()
    if not medications:
        print("No medications configured")
        return
    for med in medications:
        print(f"- {med.name} ({med.dosage})")
        if med.schedule:
            interval = (
                f"every {int(med.schedule.repeat_interval.total_seconds() // 60)} minutes"
                if med.schedule.repeat_interval
                else "one-time"
            )
            if med.schedule.times_of_day:
                times = ", ".join(
                    schedule_time.strftime("%H:%M") for schedule_time in med.schedule.times_of_day
                )
                interval += f" at {times}"
            print(f"  Schedule: {interval}, starting {med.schedule.start_time:%Y-%m-%d %H:%M}")


def list_upcoming_doses(storage: ReminderStorage) -> None:
    doses = storage.load_upcoming_doses()
    if not doses:
        print("No upcoming doses scheduled")
        return
    for dose in doses:
        print(
            f"- {dose.dose_id} for {dose.medication_id} at {dose.scheduled_time:%Y-%m-%d %H:%M}"
            f" (status: {dose.status})"
        )


def list_history(storage: ReminderStorage) -> None:
    history = storage.load_history()
    if not history:
        print("No history available")
        return
    for entry in history[-10:]:
        print(
            f"- {entry.medication_id} dose scheduled {entry.scheduled_time:%Y-%m-%d %H:%M}"
            f" marked {entry.status} at {entry.acted_at:%Y-%m-%d %H:%M}"
        )


def run_cli(storage: ReminderStorage, scheduler: ReminderScheduler) -> None:
    print("Medication reminder service running. Commands: list, doses, history, add, quit")
    while True:
        command = prompt("> ").strip().lower()
        if command in {"quit", "exit"}:
            break
        if command == "list":
            list_medications(storage)
        elif command == "doses":
            list_upcoming_doses(storage)
        elif command == "history":
            list_history(storage)
        elif command == "add":
            add_medication_interactively(storage, scheduler)
        elif command == "":
            continue
        else:
            print("Unknown command")


def main() -> None:
    configure_logging()
    storage = ReminderStorage(Path("data/reminders.json"))
    scheduler = build_scheduler(storage)
    ensure_pending_doses(scheduler, storage)
    scheduler.start()
    try:
        run_cli(storage, scheduler)
    finally:
        scheduler.stop()


if __name__ == "__main__":
    main()
