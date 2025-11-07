"""Utilities for presenting dose history data in the UI."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Any, Dict, Iterable, List, Mapping, Optional


@dataclass
class HistoryFilterResult:
    """Container for filtered history entries and status counts."""

    entries: List[Any]
    status_counts: Dict[str, int]


@dataclass
class HistoryMetrics:
    """Aggregated metrics calculated from a filtered history result."""

    total: int
    taken: int
    missed: int
    snoozed: int
    adherence_percent: float
    missed_by_medication: Dict[str, int]
    status_counts: Dict[str, int]


def _coerce_datetime(value: Any) -> Optional[datetime]:
    if isinstance(value, datetime):
        return value
    if isinstance(value, str) and value:
        for fmt in ("%Y-%m-%dT%H:%M:%S", "%Y-%m-%dT%H:%M:%S.%f", "%Y-%m-%d %H:%M"):
            try:
                return datetime.strptime(value, fmt)
            except ValueError:
                continue
        try:
            return datetime.fromisoformat(value)
        except ValueError:
            return None
    return None


def _value_from_entry(entry: Any, key: str) -> Any:
    if isinstance(entry, Mapping):
        return entry.get(key)
    return getattr(entry, key, None)


def filter_history_entries(
    entries: Iterable[Any],
    start: Optional[datetime] = None,
    end: Optional[datetime] = None,
) -> HistoryFilterResult:
    """Filter entries by timestamp/acted_at and provide status counts."""

    items = list(entries)
    filtered: List[Any] = []
    status_counts: Dict[str, int] = {}

    end_bound = end
    inclusive_upper = True
    if end is not None and end.time() == datetime.min.time():
        end_bound = end + timedelta(days=1)
        inclusive_upper = False

    for entry in items:
        timestamp = _value_from_entry(entry, "timestamp")
        acted_at = _value_from_entry(entry, "acted_at")
        fallback = _value_from_entry(entry, "scheduled_time")

        effective_time = (
            _coerce_datetime(timestamp)
            or _coerce_datetime(acted_at)
            or _coerce_datetime(fallback)
        )

        if start is not None and effective_time is not None and effective_time < start:
            continue
        if end_bound is not None and effective_time is not None:
            if inclusive_upper:
                if effective_time > end_bound:
                    continue
            else:
                if effective_time >= end_bound:
                    continue

        filtered.append(entry)
        status_value = (_value_from_entry(entry, "status") or "").strip().lower()
        if not status_value:
            status_value = "unknown"
        status_counts[status_value] = status_counts.get(status_value, 0) + 1

    return HistoryFilterResult(entries=filtered, status_counts=status_counts)


def calculate_metrics(result: HistoryFilterResult) -> HistoryMetrics:
    """Calculate adherence and missed-dose summaries from the filter result."""

    total = len(result.entries)
    taken = result.status_counts.get("taken", 0)
    missed = result.status_counts.get("missed", 0)
    snoozed = result.status_counts.get("snoozed", 0)

    denominator = taken + missed
    adherence = (taken / denominator * 100.0) if denominator else 0.0

    missed_by_medication: Dict[str, int] = {}
    for entry in result.entries:
        status_value = (_value_from_entry(entry, "status") or "").strip().lower()
        if status_value != "missed":
            continue
        medication = (
            _value_from_entry(entry, "medication_name")
            or _value_from_entry(entry, "medication")
            or _value_from_entry(entry, "medication_id")
            or "Unknown"
        )
        medication_key = str(medication)
        missed_by_medication[medication_key] = missed_by_medication.get(medication_key, 0) + 1

    missed_by_medication = dict(
        sorted(
            missed_by_medication.items(),
            key=lambda item: (-item[1], item[0].lower()),
        )
    )

    return HistoryMetrics(
        total=total,
        taken=taken,
        missed=missed,
        snoozed=snoozed,
        adherence_percent=adherence,
        missed_by_medication=missed_by_medication,
        status_counts=dict(result.status_counts),
    )


__all__ = [
    "HistoryFilterResult",
    "HistoryMetrics",
    "filter_history_entries",
    "calculate_metrics",
]
