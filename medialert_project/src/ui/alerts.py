"""Alert dialog helpers for due medication reminders."""
from __future__ import annotations

import logging
import threading
from typing import Callable

try:
    import tkinter as tk
    from tkinter import ttk
except Exception:  # pragma: no cover - Tk is optional in some environments
    tk = None  # type: ignore
    ttk = None  # type: ignore

LOGGER = logging.getLogger(__name__)


class AlertDialogManager:
    """Render a modal alert dialog for due doses."""

    def __init__(self, default_snooze_minutes: int = 10) -> None:
        self.default_snooze_minutes = default_snooze_minutes
        self._lock = threading.RLock()

    def show_alert(
        self,
        dose,
        medication,
        on_taken: Callable[[], None],
        on_snooze: Callable[[int], None],
        on_skip: Callable[[], None],
    ) -> None:
        """Display the alert dialog asynchronously."""

        LOGGER.info(
            "Alert for dose %s (%s) at %s",
            dose.dose_id,
            medication.name,
            dose.effective_due_time(),
        )

        if tk is None or ttk is None:
            LOGGER.warning("Tkinter not available, defaulting to snooze")
            on_snooze(self.default_snooze_minutes)
            return

        threading.Thread(
            target=self._render_dialog,
            args=(dose, medication, on_taken, on_snooze, on_skip),
            name=f"AlertDialog-{dose.dose_id}",
            daemon=True,
        ).start()

    def _render_dialog(
        self,
        dose,
        medication,
        on_taken: Callable[[], None],
        on_snooze: Callable[[int], None],
        on_skip: Callable[[], None],
    ) -> None:
        with self._lock:
            root = tk.Tk()  # type: ignore[call-arg]
            root.title("Medication Alert")
            root.geometry("360x220")
            root.resizable(False, False)
            try:
                root.attributes("-topmost", True)
                root.after(200, lambda: root.attributes("-topmost", False))
            except Exception:  # pragma: no cover - attributes may not be supported
                LOGGER.debug("Unable to set topmost attribute for alert dialog")
            root.grab_set()
            root.focus_force()
            root.protocol(
                "WM_DELETE_WINDOW",
                lambda: self._handle_action(root, on_skip, "skipped", dose, medication),
            )

            frame = ttk.Frame(root, padding=12)
            frame.pack(fill=tk.BOTH, expand=True)

            info_label = ttk.Label(
                frame,
                text=(
                    f"Medication: {medication.name}\n"
                    f"Dosage: {medication.dosage}\n"
                    f"Due: {dose.effective_due_time():%Y-%m-%d %H:%M}"
                ),
                justify=tk.LEFT,
            )
            info_label.pack(anchor=tk.W)

            instructions = getattr(medication, "instructions", "").strip()
            if instructions:
                instructions_label = ttk.Label(
                    frame,
                    text=f"Instructions: {instructions}",
                    wraplength=320,
                    justify=tk.LEFT,
                )
                instructions_label.pack(anchor=tk.W, pady=(8, 0))

            snooze_frame = ttk.Frame(frame)
            snooze_frame.pack(anchor=tk.W, pady=(12, 0))

            ttk.Label(snooze_frame, text="Snooze for (minutes)").grid(row=0, column=0, sticky=tk.W)
            snooze_var = tk.StringVar(value=str(self.default_snooze_minutes))
            snooze_entry = ttk.Spinbox(
                snooze_frame,
                from_=1,
                to=240,
                width=5,
                textvariable=snooze_var,
            )
            snooze_entry.grid(row=0, column=1, padx=(8, 0))

            button_frame = ttk.Frame(frame)
            button_frame.pack(fill=tk.X, pady=(18, 0))

            taken_button = ttk.Button(
                button_frame,
                text="Taken",
                command=lambda: self._handle_action(root, on_taken, "taken", dose, medication),
            )
            taken_button.pack(side=tk.LEFT, expand=True, padx=4)

            snooze_button = ttk.Button(
                button_frame,
                text="Snooze",
                command=lambda: self._handle_snooze(
                    root,
                    snooze_var.get(),
                    on_snooze,
                    dose,
                    medication,
                ),
            )
            snooze_button.pack(side=tk.LEFT, expand=True, padx=4)

            skip_button = ttk.Button(
                button_frame,
                text="Skip",
                command=lambda: self._handle_action(root, on_skip, "skipped", dose, medication),
            )
            skip_button.pack(side=tk.LEFT, expand=True, padx=4)

            root.mainloop()

    def _handle_snooze(
        self,
        root,
        value: str,
        callback: Callable[[int], None],
        dose,
        medication,
    ) -> None:
        try:
            minutes = int(value)
        except (TypeError, ValueError):
            minutes = self.default_snooze_minutes
        self._handle_action(
            root,
            lambda: callback(max(minutes, 1)),
            "snoozed",
            dose,
            medication,
        )

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
                "User marked dose %s for %s as %s",
                dose.dose_id,
                medication.name,
                action,
            )
        finally:
            root.destroy()


__all__ = ["AlertDialogManager"]
