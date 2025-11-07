import threading
from datetime import datetime, timedelta

from reminders.models import DoseSchedule, Medication, UpcomingDose
from reminders.scheduler import ReminderScheduler
from reminders.storage import ReminderStorage


def _make_medication(start_time: datetime, repeat_hours: int = 8) -> Medication:
    schedule = DoseSchedule(
        medication_id="med-1",
        start_time=start_time,
        repeat_interval=timedelta(hours=repeat_hours),
    )
    return Medication(
        medication_id="med-1",
        name="Pain Reliever",
        dosage="10mg",
        schedule=schedule,
    )


def test_ensure_next_dose_creates_initial_entry(tmp_path):
    storage = ReminderStorage(tmp_path / "storage.json")
    medication = _make_medication(datetime.now() + timedelta(minutes=10))
    scheduler = ReminderScheduler(storage)

    scheduler.add_medication(medication)

    doses = storage.load_upcoming_doses()
    assert len(doses) == 1
    assert doses[0].medication_id == medication.medication_id


def test_mark_taken_records_history_and_schedules_follow_up(tmp_path):
    storage = ReminderStorage(tmp_path / "storage.json")
    start = datetime.now() - timedelta(hours=1)
    medication = _make_medication(start)
    storage.upsert_medication(medication)

    due_time = datetime.now() - timedelta(minutes=5)
    dose = UpcomingDose.create(medication.medication_id, due_time)
    storage.upsert_upcoming_dose(dose)

    scheduler = ReminderScheduler(storage)
    scheduler.mark_taken(dose.dose_id)

    assert storage.get_upcoming_dose(dose.dose_id) is None
    remaining = storage.load_upcoming_doses()
    assert len(remaining) == 1
    assert remaining[0].scheduled_time > due_time

    history = storage.load_history()
    assert history
    assert history[-1].status == "taken"


def test_scheduler_triggers_due_handler(tmp_path):
    storage = ReminderStorage(tmp_path / "storage.json")
    start = datetime.now() - timedelta(hours=2)
    medication = _make_medication(start)
    storage.upsert_medication(medication)

    due_time = datetime.now() - timedelta(minutes=1)
    dose = UpcomingDose.create(medication.medication_id, due_time)
    storage.upsert_upcoming_dose(dose)

    triggered = threading.Event()
    calls = []

    def handler(d, med, *_callbacks):
        calls.append((d.dose_id, med.medication_id))
        triggered.set()

    scheduler = ReminderScheduler(storage, due_handler=handler, poll_interval=0.01)
    scheduler.start()
    try:
        assert triggered.wait(timeout=1), "Scheduler did not trigger due handler"
    finally:
        scheduler.stop()

    assert calls == [(dose.dose_id, medication.medication_id)]
