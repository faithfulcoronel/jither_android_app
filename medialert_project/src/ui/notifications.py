"""Simple Tkinter-based notification dialogs for medication reminders."""
from __future__ import annotations

import logging
import threading
from typing import Callable

try:
    import tkinter as tk
except Exception:  # pragma: no cover - Tk is optional in some environments
    tk = None  # type: ignore

LOGGER = logging.getLogger(__name__)


class NotificationManager:
    """Render reminder notifications and route user actions."""

    def __init__(self, default_snooze_minutes: int = 10) -> None:
        self.default_snooze_minutes = default_snooze_minutes
        self._lock = threading.RLock()

    def show_notification(
        self,
        dose,
        medication,
        on_taken: Callable[[], None],
        on_snooze: Callable[[int], None],
        on_missed: Callable[[], None],
    ) -> None:
        """Display a reminder dialog and trigger callbacks based on the user response."""

        LOGGER.info(
            "Dose %s for %s is due at %s",
            dose.dose_id,
            medication.name,
            dose.effective_due_time(),
        )

        if tk is None:
            LOGGER.warning("Tkinter not available, defaulting to automatic snooze")
            on_snooze(self.default_snooze_minutes)
            return

        threading.Thread(
            target=self._render_dialog,
            args=(dose, medication, on_taken, on_snooze, on_missed),
            daemon=True,
            name=f"Notification-{dose.dose_id}",
        ).start()

    def _render_dialog(
        self,
        dose,
        medication,
        on_taken: Callable[[], None],
        on_snooze: Callable[[int], None],
        on_missed: Callable[[], None],
    ) -> None:
        with self._lock:
            root = tk.Tk()  # type: ignore[call-arg]
            root.title("Medication Reminder")
            root.geometry("320x160")
            root.resizable(False, False)

            label = tk.Label(
                root,
                text=(
                    f"{medication.name}\n"
                    f"Dosage: {medication.dosage}\n"
                    f"Due: {dose.effective_due_time():%Y-%m-%d %H:%M}"
                ),
                justify=tk.LEFT,
                padx=10,
                pady=10,
            )
            label.pack(fill=tk.BOTH, expand=True)

            button_frame = tk.Frame(root)
            button_frame.pack(fill=tk.X, pady=5)

            taken_button = tk.Button(
                button_frame,
                text="Taken",
                command=lambda: self._handle_action(root, on_taken, "taken", dose, medication),
            )
            taken_button.pack(side=tk.LEFT, padx=5)

            snooze_button = tk.Button(
                button_frame,
                text=f"Snooze {self.default_snooze_minutes}m",
                command=lambda: self._handle_action(
                    root,
                    lambda: on_snooze(self.default_snooze_minutes),
                    "snoozed",
                    dose,
                    medication,
                ),
            )
            snooze_button.pack(side=tk.LEFT, padx=5)

            missed_button = tk.Button(
                button_frame,
                text="Missed",
                command=lambda: self._handle_action(root, on_missed, "missed", dose, medication),
            )
            missed_button.pack(side=tk.LEFT, padx=5)

            instructions = medication.instructions.strip()
            if instructions:
                instructions_label = tk.Label(
                    root,
                    text=f"Instructions: {instructions}",
                    wraplength=300,
                    justify=tk.LEFT,
                    padx=10,
                )
                instructions_label.pack(fill=tk.X, pady=(0, 10))

            root.mainloop()

    def _handle_action(
        self,
        root,
        callback: Callable[[], None],
        action: str,
        dose,
        medication,
    ) -> None:
        try:
            callback()
            LOGGER.info(
                "User marked dose %s for %s as %s", dose.dose_id, medication.name, action
            )
        finally:
            root.destroy()


__all__ = ["NotificationManager"]
