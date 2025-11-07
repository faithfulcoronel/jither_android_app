"""Utilities for recording reminder analytics and history."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from .models import DoseHistoryEntry, UpcomingDose
from .storage import ReminderStorage


@dataclass
class AlertAction:
    """Represents an action taken in response to an alert."""

    dose: UpcomingDose
    status: str
    notes: str = ""
    acted_at: Optional[datetime] = None


class AlertActionLogger:
    """Persist alert actions to history for analytics."""

    def __init__(self, storage: ReminderStorage) -> None:
        self.storage = storage

    def log(self, action: AlertAction) -> None:
        """Write an alert action entry to history."""

        acted_at = action.acted_at or datetime.now()
        entry = DoseHistoryEntry(
            dose_id=action.dose.dose_id,
            medication_id=action.dose.medication_id,
            scheduled_time=action.dose.scheduled_time,
            status=action.status,
            acted_at=acted_at,
            notes=action.notes,
        )
        self.storage.append_history(entry)

    def log_action(
        self,
        dose: UpcomingDose,
        status: str,
        *,
        notes: str = "",
        acted_at: Optional[datetime] = None,
    ) -> None:
        """Convenience wrapper to create and log an action."""

        self.log(AlertAction(dose=dose, status=status, notes=notes, acted_at=acted_at))


__all__ = ["AlertAction", "AlertActionLogger"]
