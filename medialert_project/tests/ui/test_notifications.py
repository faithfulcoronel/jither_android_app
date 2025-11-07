from datetime import datetime
from types import SimpleNamespace

from ui import notifications
from ui.notifications import NotificationManager


class DummyDose:
    dose_id = "dose-1"

    def effective_due_time(self):
        return datetime.now()


class DummyMedication:
    medication_id = "med-1"
    name = "Pain Reliever"
    dosage = "10mg"
    instructions = ""


def test_show_notification_without_tk_triggers_snooze(monkeypatch):
    manager = NotificationManager(default_snooze_minutes=5)
    monkeypatch.setattr(notifications, "tk", None)

    calls = []

    manager.show_notification(
        dose=DummyDose(),
        medication=DummyMedication(),
        on_taken=lambda: calls.append("taken"),
        on_snooze=lambda minutes: calls.append(("snoozed", minutes)),
        on_missed=lambda: calls.append("missed"),
    )

    assert calls == [("snoozed", 5)]


def test_handle_action_invokes_callback_and_closes():
    manager = NotificationManager()

    destroyed = []
    root = SimpleNamespace(destroy=lambda: destroyed.append(True))

    calls = []
    manager._handle_action(
        root=root,
        callback=lambda: calls.append("done"),
        action="taken",
        dose=DummyDose(),
        medication=DummyMedication(),
    )

    assert calls == ["done"]
    assert destroyed == [True]
