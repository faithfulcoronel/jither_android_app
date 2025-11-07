"""Persistence helpers for the reminder service."""
from __future__ import annotations

import json
import threading
from pathlib import Path
from typing import Dict, Iterable, List, Optional

from . import models


class ReminderStorage:
    """Persist reminder data to a local JSON document."""

    def __init__(self, path: Path):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._lock = threading.RLock()
        if not self.path.exists():
            self._write_state(self._initial_state())

    def _initial_state(self) -> Dict[str, List[Dict]]:
        return {
            "medications": [],
            "upcoming_doses": [],
            "history": [],
        }

    def _load_state(self) -> Dict[str, List[Dict]]:
        with self._lock:
            with self.path.open("r", encoding="utf-8") as handle:
                return json.load(handle)

    def _write_state(self, state: Dict[str, List[Dict]]) -> None:
        with self._lock:
            with self.path.open("w", encoding="utf-8") as handle:
                json.dump(state, handle, indent=2)

    # Medication helpers -------------------------------------------------

    def load_medications(self) -> List[models.Medication]:
        state = self._load_state()
        return models.deserialize_medications(state.get("medications", []))

    def save_medications(self, medications: Iterable[models.Medication]) -> None:
        state = self._load_state()
        state["medications"] = models.serialize_collection(medications)
        self._write_state(state)

    def upsert_medication(self, medication: models.Medication) -> None:
        medications = {item.medication_id: item for item in self.load_medications()}
        medications[medication.medication_id] = medication
        self.save_medications(medications.values())

    def get_medication(self, medication_id: str) -> Optional[models.Medication]:
        return next(
            (item for item in self.load_medications() if item.medication_id == medication_id),
            None,
        )

    # Upcoming dose helpers ---------------------------------------------

    def load_upcoming_doses(self) -> List[models.UpcomingDose]:
        state = self._load_state()
        return models.deserialize_upcoming_doses(state.get("upcoming_doses", []))

    def save_upcoming_doses(self, doses: Iterable[models.UpcomingDose]) -> None:
        state = self._load_state()
        state["upcoming_doses"] = models.serialize_collection(doses)
        self._write_state(state)

    def upsert_upcoming_dose(self, dose: models.UpcomingDose) -> None:
        doses = {item.dose_id: item for item in self.load_upcoming_doses()}
        doses[dose.dose_id] = dose
        self.save_upcoming_doses(doses.values())

    def remove_upcoming_dose(self, dose_id: str) -> None:
        doses = [item for item in self.load_upcoming_doses() if item.dose_id != dose_id]
        self.save_upcoming_doses(doses)

    def get_upcoming_dose(self, dose_id: str) -> Optional[models.UpcomingDose]:
        return next(
            (item for item in self.load_upcoming_doses() if item.dose_id == dose_id),
            None,
        )

    # History helpers ----------------------------------------------------

    def load_history(self) -> List[models.DoseHistoryEntry]:
        state = self._load_state()
        return models.deserialize_history(state.get("history", []))

    def append_history(self, entry: models.DoseHistoryEntry) -> None:
        state = self._load_state()
        history = state.get("history", [])
        history.append(entry.to_dict())
        state["history"] = history
        self._write_state(state)

    # Utilities ----------------------------------------------------------

    def reset(self) -> None:
        """Reset storage to an empty state."""
        self._write_state(self._initial_state())


__all__ = ["ReminderStorage"]
