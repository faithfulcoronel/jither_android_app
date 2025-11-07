"""Domain models for the medication reminder service."""
from __future__ import annotations

from dataclasses import dataclass, field, asdict
from datetime import datetime, time, timedelta
from typing import Any, Dict, Iterable, List, Optional
from uuid import uuid4

ISO_FORMAT = "%Y-%m-%dT%H:%M:%S.%fZ"


def _to_iso(dt: Optional[datetime]) -> Optional[str]:
    if dt is None:
        return None
    if dt.tzinfo is not None:
        dt = dt.astimezone(tz=None).replace(tzinfo=None)
    return dt.strftime(ISO_FORMAT)


def _from_iso(value: Optional[str]) -> Optional[datetime]:
    if value is None:
        return None
    try:
        return datetime.strptime(value, ISO_FORMAT)
    except ValueError:
        # Support datetimes stored without microseconds
        return datetime.fromisoformat(value.replace("Z", ""))


def _time_to_str(value: time) -> str:
    return value.strftime("%H:%M")


def _time_from_str(value: str) -> time:
    return datetime.strptime(value, "%H:%M").time()


@dataclass
class DoseSchedule:
    """Defines how a medication repeats over time."""

    medication_id: str
    start_time: datetime
    repeat_interval: Optional[timedelta] = None
    times_of_day: Optional[List[time]] = None

    def next_due(self, after: datetime) -> Optional[datetime]:
        """Return the next scheduled time after the provided datetime."""
        if self.times_of_day:
            # Handle daily specific times.
            sorted_times = sorted(self.times_of_day)
            search_date = after.date()
            # Search forward for up to two weeks which is ample for daily schedules.
            for _ in range(14):
                for scheduled_time in sorted_times:
                    candidate = datetime.combine(search_date, scheduled_time)
                    if candidate < self.start_time:
                        continue
                    if candidate > after:
                        return candidate
                search_date = search_date + timedelta(days=1)
            return None

        if not self.repeat_interval:
            # One-off schedule
            return self.start_time if self.start_time > after else None

        if after < self.start_time:
            return self.start_time

        elapsed = after - self.start_time
        intervals = int(elapsed / self.repeat_interval) + 1
        return self.start_time + self.repeat_interval * intervals

    def to_dict(self) -> Dict[str, Any]:
        payload: Dict[str, Any] = {
            "medication_id": self.medication_id,
            "start_time": _to_iso(self.start_time),
            "repeat_interval_minutes": (
                int(self.repeat_interval.total_seconds() // 60)
                if self.repeat_interval
                else None
            ),
            "times_of_day": [_time_to_str(t) for t in self.times_of_day]
            if self.times_of_day
            else None,
        }
        return payload

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "DoseSchedule":
        repeat_minutes = data.get("repeat_interval_minutes")
        repeat_interval = (
            timedelta(minutes=repeat_minutes) if repeat_minutes is not None else None
        )
        times_raw = data.get("times_of_day") or []
        times = [_time_from_str(item) for item in times_raw] or None
        return cls(
            medication_id=data["medication_id"],
            start_time=_from_iso(data["start_time"]),
            repeat_interval=repeat_interval,
            times_of_day=times,
        )


@dataclass
class Medication:
    """Metadata for a medication being tracked."""

    medication_id: str
    name: str
    dosage: str
    instructions: str = ""
    schedule: Optional[DoseSchedule] = None
    tags: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        payload = asdict(self)
        if self.schedule:
            payload["schedule"] = self.schedule.to_dict()
        else:
            payload["schedule"] = None
        return payload

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "Medication":
        schedule_payload = data.get("schedule")
        schedule = DoseSchedule.from_dict(schedule_payload) if schedule_payload else None
        return cls(
            medication_id=data["medication_id"],
            name=data["name"],
            dosage=data.get("dosage", ""),
            instructions=data.get("instructions", ""),
            schedule=schedule,
            tags=list(data.get("tags", [])),
        )


@dataclass
class UpcomingDose:
    """Represents a scheduled dose that is pending or recently actioned."""

    dose_id: str
    medication_id: str
    scheduled_time: datetime
    status: str = "pending"
    snoozed_until: Optional[datetime] = None
    notified: bool = False
    taken_at: Optional[datetime] = None

    def effective_due_time(self) -> datetime:
        return self.snoozed_until or self.scheduled_time

    def to_dict(self) -> Dict[str, Any]:
        return {
            "dose_id": self.dose_id,
            "medication_id": self.medication_id,
            "scheduled_time": _to_iso(self.scheduled_time),
            "status": self.status,
            "snoozed_until": _to_iso(self.snoozed_until),
            "notified": self.notified,
            "taken_at": _to_iso(self.taken_at),
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "UpcomingDose":
        return cls(
            dose_id=data["dose_id"],
            medication_id=data["medication_id"],
            scheduled_time=_from_iso(data["scheduled_time"]),
            status=data.get("status", "pending"),
            snoozed_until=_from_iso(data.get("snoozed_until")),
            notified=data.get("notified", False),
            taken_at=_from_iso(data.get("taken_at")),
        )

    @staticmethod
    def create(medication_id: str, scheduled_time: datetime) -> "UpcomingDose":
        return UpcomingDose(
            dose_id=str(uuid4()),
            medication_id=medication_id,
            scheduled_time=scheduled_time,
        )


@dataclass
class DoseHistoryEntry:
    """Historical log of doses that have been taken, missed, or snoozed."""

    dose_id: str
    medication_id: str
    scheduled_time: datetime
    timestamp: Optional[datetime] = None
    status: str
    acted_at: datetime
    notes: str = ""

    def to_dict(self) -> Dict[str, Any]:
        return {
            "dose_id": self.dose_id,
            "medication_id": self.medication_id,
            "scheduled_time": _to_iso(self.scheduled_time),
            "timestamp": _to_iso(self.timestamp),
            "status": self.status,
            "acted_at": _to_iso(self.acted_at),
            "notes": self.notes,
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "DoseHistoryEntry":
        timestamp = _from_iso(data.get("timestamp"))
        if timestamp is None:
            timestamp = _from_iso(data.get("acted_at"))
        return cls(
            dose_id=data["dose_id"],
            medication_id=data["medication_id"],
            scheduled_time=_from_iso(data["scheduled_time"]),
            timestamp=timestamp,
            status=data["status"],
            acted_at=_from_iso(data["acted_at"]),
            notes=data.get("notes", ""),
        )


def serialize_collection(items: Iterable[Any]) -> List[Dict[str, Any]]:
    return [item.to_dict() for item in items]


def deserialize_medications(payload: Iterable[Dict[str, Any]]) -> List[Medication]:
    return [Medication.from_dict(item) for item in payload]


def deserialize_upcoming_doses(payload: Iterable[Dict[str, Any]]) -> List[UpcomingDose]:
    return [UpcomingDose.from_dict(item) for item in payload]


def deserialize_history(payload: Iterable[Dict[str, Any]]) -> List[DoseHistoryEntry]:
    return [DoseHistoryEntry.from_dict(item) for item in payload]


__all__ = [
    "DoseSchedule",
    "Medication",
    "UpcomingDose",
    "DoseHistoryEntry",
    "serialize_collection",
    "deserialize_medications",
    "deserialize_upcoming_doses",
    "deserialize_history",
]
