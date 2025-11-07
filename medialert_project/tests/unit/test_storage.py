from datetime import datetime, timedelta

from reminders.models import DoseSchedule, Medication, UpcomingDose
from reminders.storage import ReminderStorage


def _make_medication(start_time: datetime) -> Medication:
    schedule = DoseSchedule(
        medication_id="med-1",
        start_time=start_time,
        repeat_interval=timedelta(hours=8),
    )
    return Medication(
        medication_id="med-1",
        name="Pain Reliever",
        dosage="10mg",
        instructions="Take with water",
        schedule=schedule,
    )


def test_storage_persists_entities(tmp_path):
    storage = ReminderStorage(tmp_path / "storage.json")
    medication = _make_medication(datetime.now())
    storage.upsert_medication(medication)

    loaded_med = storage.get_medication(medication.medication_id)
    assert loaded_med is not None
    assert loaded_med.name == medication.name

    dose = UpcomingDose.create(
        medication_id=medication.medication_id,
        scheduled_time=datetime.now() + timedelta(minutes=5),
    )
    storage.upsert_upcoming_dose(dose)

    loaded_dose = storage.get_upcoming_dose(dose.dose_id)
    assert loaded_dose is not None
    assert loaded_dose.medication_id == medication.medication_id

    storage.remove_upcoming_dose(dose.dose_id)
    assert storage.get_upcoming_dose(dose.dose_id) is None

    storage.reset()
    assert storage.load_medications() == []
    assert storage.load_upcoming_doses() == []
    assert storage.load_history() == []
