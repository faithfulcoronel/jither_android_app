"""SQLite-backed storage layer for MediaLert alerts."""
from __future__ import annotations

from dataclasses import dataclass, replace
from pathlib import Path
import sqlite3
from typing import Iterable, List, Optional


@dataclass
class Alert:
    """Represents a single alert record stored in the database."""

    title: str
    message: str
    severity: str = "info"
    id: Optional[int] = None


class SQLiteStorage:
    """A tiny SQLite helper for persisting :class:`Alert` objects.

    The storage is intentionally lightweight so it can be used by a CLI, a UI
    prototype, or even a background service.  The constructor ensures the
    backing database exists and that the schema is created if necessary.
    """

    def __init__(self, db_path: str | Path) -> None:
        self._db_path = Path(db_path)
        if not self._db_path.parent.exists():
            self._db_path.parent.mkdir(parents=True, exist_ok=True)
        self._initialise()

    def _initialise(self) -> None:
        with self._connect() as connection:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
            )

    def _connect(self) -> sqlite3.Connection:
        return sqlite3.connect(self._db_path)

    def add_alert(self, alert: Alert) -> Alert:
        """Persist *alert* and return a new instance with the generated id."""

        with self._connect() as connection:
            cursor = connection.execute(
                """
                INSERT INTO alerts (title, message, severity)
                VALUES (?, ?, ?)
                """,
                (alert.title, alert.message, alert.severity),
            )
            connection.commit()
        return replace(alert, id=cursor.lastrowid)

    def list_alerts(self) -> List[Alert]:
        """Return all alerts ordered by creation time descending."""

        with self._connect() as connection:
            rows = connection.execute(
                """
                SELECT id, title, message, severity
                FROM alerts
                ORDER BY created_at DESC
                """
            ).fetchall()
        return [Alert(id=row[0], title=row[1], message=row[2], severity=row[3]) for row in rows]

    def delete_alerts(self, alert_ids: Iterable[int]) -> None:
        """Delete alerts matching *alert_ids*."""

        ids = list(alert_ids)
        if not ids:
            return
        with self._connect() as connection:
            connection.executemany("DELETE FROM alerts WHERE id = ?", ((alert_id,) for alert_id in ids))
            connection.commit()

    def clear(self) -> None:
        """Remove all alert records."""

        with self._connect() as connection:
            connection.execute("DELETE FROM alerts")
            connection.commit()


__all__ = ["Alert", "SQLiteStorage"]
