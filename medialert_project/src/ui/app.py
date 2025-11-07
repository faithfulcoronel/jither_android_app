"""Tkinter application shell for managing medication reminders."""
from __future__ import annotations

from datetime import datetime, time as time_cls
from typing import Any, Dict, Iterable, List, Mapping, Optional

import tkinter as tk
from tkinter import filedialog, messagebox, ttk

from .history import HistoryMetrics, calculate_metrics, filter_history_entries
from ..features import MedicationLabelScanner, OCRFailure


def _coerce_datetime(value: Any) -> Optional[datetime]:
    if isinstance(value, datetime):
        return value
    if isinstance(value, str) and value:
        try:
            return datetime.fromisoformat(value)
        except ValueError:
            try:
                return datetime.strptime(value, "%Y-%m-%d %H:%M")
            except ValueError:
                return None
    return None


def _coerce_time(value: Any) -> Optional[time_cls]:
    if isinstance(value, time_cls):
        return value
    if isinstance(value, str) and value:
        try:
            return datetime.strptime(value, "%H:%M").time()
        except ValueError:
            return None
    return None


def _from_mapping_or_attr(payload: Any, key: str, default: Any = None) -> Any:
    if isinstance(payload, Mapping):
        return payload.get(key, default)
    return getattr(payload, key, default)


class MedicationFormFrame(ttk.LabelFrame):
    """Reusable form widget for capturing medication details."""

    def __init__(self, master: tk.Misc, *, title: str = "Medication") -> None:
        super().__init__(master, text=title)
        self._medication_id: Optional[str] = None

        self.name_var = tk.StringVar()
        self.dosage_var = tk.StringVar()
        self.frequency_var = tk.StringVar()
        self.times_var = tk.StringVar()

        self._build()

    def _build(self) -> None:
        for index in range(4):
            self.columnconfigure(index, weight=1)

        name_label = ttk.Label(self, text="Name")
        name_label.grid(row=0, column=0, sticky="w", padx=5, pady=(10, 2))
        name_entry = ttk.Entry(self, textvariable=self.name_var)
        name_entry.grid(row=0, column=1, columnspan=3, sticky="ew", padx=5, pady=(10, 2))

        dosage_label = ttk.Label(self, text="Dosage")
        dosage_label.grid(row=1, column=0, sticky="w", padx=5, pady=2)
        dosage_entry = ttk.Entry(self, textvariable=self.dosage_var)
        dosage_entry.grid(row=1, column=1, sticky="ew", padx=5, pady=2)

        frequency_label = ttk.Label(self, text="Frequency (minutes)")
        frequency_label.grid(row=1, column=2, sticky="w", padx=5, pady=2)
        frequency_entry = ttk.Entry(self, textvariable=self.frequency_var)
        frequency_entry.grid(row=1, column=3, sticky="ew", padx=5, pady=2)

        times_label = ttk.Label(self, text="Times (HH:MM, comma separated)")
        times_label.grid(row=2, column=0, columnspan=2, sticky="w", padx=5, pady=2)
        times_entry = ttk.Entry(self, textvariable=self.times_var)
        times_entry.grid(row=2, column=2, columnspan=2, sticky="ew", padx=5, pady=2)

        notes_label = ttk.Label(self, text="Notes")
        notes_label.grid(row=3, column=0, sticky="nw", padx=5, pady=(10, 2))

        self.notes_widget = tk.Text(self, height=6, wrap="word")
        self.notes_widget.grid(row=3, column=1, columnspan=3, sticky="nsew", padx=5, pady=(10, 2))

        self.rowconfigure(3, weight=1)

    def get_form_data(self) -> Dict[str, Any]:
        notes = self.notes_widget.get("1.0", tk.END).strip()
        times = [chunk.strip() for chunk in self.times_var.get().split(",") if chunk.strip()]
        frequency_raw = self.frequency_var.get().strip()
        frequency: Any
        if frequency_raw:
            try:
                frequency = int(frequency_raw)
            except ValueError:
                frequency = frequency_raw
        else:
            frequency = None
        payload = {
            "medication_id": self._medication_id,
            "name": self.name_var.get().strip(),
            "dosage": self.dosage_var.get().strip(),
            "frequency": frequency,
            "times": times,
            "notes": notes,
        }
        return payload

    def set_form_data(self, payload: Mapping[str, Any]) -> None:
        self._medication_id = payload.get("medication_id")
        self.name_var.set(payload.get("name", ""))
        self.dosage_var.set(payload.get("dosage", ""))
        frequency = payload.get("frequency", "")
        self.frequency_var.set(str(frequency) if frequency is not None else "")
        times_value = payload.get("times")
        if isinstance(times_value, str):
            times_text = times_value
        elif isinstance(times_value, Iterable):
            formatted: List[str] = []
            for item in times_value:
                parsed = _coerce_time(item)
                if parsed is not None:
                    formatted.append(parsed.strftime("%H:%M"))
                elif item:
                    formatted.append(str(item))
            times_text = ", ".join(formatted)
        else:
            times_text = ""
        self.times_var.set(times_text)
        notes = payload.get("notes", "")
        self.notes_widget.delete("1.0", tk.END)
        if notes:
            self.notes_widget.insert("1.0", notes)

    def reset(self) -> None:
        self._medication_id = None
        self.name_var.set("")
        self.dosage_var.set("")
        self.frequency_var.set("")
        self.times_var.set("")
        self.notes_widget.delete("1.0", tk.END)

    @property
    def current_medication_id(self) -> Optional[str]:
        return self._medication_id


class ReminderApp:
    """Main application shell wrapping the Tkinter UI."""

    def __init__(self, controller: Any) -> None:
        self.controller = controller
        self.root = tk.Tk()
        self.root.title("Medication Reminder")
        self.root.geometry("1200x800")
        self.root.minsize(960, 640)

        self.scanner = MedicationLabelScanner()
        self._medication_cache: Dict[str, Mapping[str, Any]] = {}

        self._build_layout()
        self.refresh_medications()
        self.refresh_schedule()
        self.refresh_history()

    # Layout helpers -------------------------------------------------
    def _build_layout(self) -> None:
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)

        container = ttk.Frame(self.root, padding=10)
        container.grid(row=0, column=0, sticky="nsew")
        container.columnconfigure(0, weight=1)
        container.columnconfigure(1, weight=3)
        container.rowconfigure(0, weight=1)
        container.rowconfigure(1, weight=1)

        self._build_form_section(container)
        self._build_schedule_section(container)
        self._build_history_section(container)

    def _build_form_section(self, container: ttk.Frame) -> None:
        form_holder = ttk.Frame(container)
        form_holder.grid(row=0, column=0, rowspan=2, sticky="nsew", padx=(0, 12))
        form_holder.rowconfigure(0, weight=1)

        self.form = MedicationFormFrame(form_holder)
        self.form.grid(row=0, column=0, sticky="nsew")

        button_frame = ttk.Frame(form_holder)
        button_frame.grid(row=1, column=0, sticky="ew", pady=(10, 0))
        for index in range(4):
            button_frame.columnconfigure(index, weight=1)

        self.scan_button = ttk.Button(
            button_frame, text="Scan Label", command=self.on_scan_label
        )
        self.scan_button.grid(row=0, column=0, sticky="ew", padx=5)

        self.add_button = ttk.Button(button_frame, text="Add", command=self.on_add_medication)
        self.add_button.grid(row=0, column=1, sticky="ew", padx=5)

        self.edit_button = ttk.Button(button_frame, text="Edit", command=self.on_edit_medication)
        self.edit_button.grid(row=0, column=2, sticky="ew", padx=5)

        self.delete_button = ttk.Button(button_frame, text="Delete", command=self.on_delete_medication)
        self.delete_button.grid(row=0, column=3, sticky="ew", padx=5)

    def _build_schedule_section(self, container: ttk.Frame) -> None:
        schedule_frame = ttk.LabelFrame(container, text="Upcoming Schedule")
        schedule_frame.grid(row=0, column=1, sticky="nsew")
        schedule_frame.columnconfigure(0, weight=1)
        schedule_frame.rowconfigure(2, weight=1)

        filter_frame = ttk.Frame(schedule_frame)
        filter_frame.grid(row=0, column=0, sticky="ew", padx=8, pady=(8, 0))
        filter_frame.columnconfigure(1, weight=1)
        filter_frame.columnconfigure(3, weight=1)

        search_label = ttk.Label(filter_frame, text="Filter")
        search_label.grid(row=0, column=0, sticky="w", padx=(0, 6))
        self.schedule_filter_var = tk.StringVar()
        search_entry = ttk.Entry(filter_frame, textvariable=self.schedule_filter_var)
        search_entry.grid(row=0, column=1, sticky="ew", padx=(0, 12))

        status_label = ttk.Label(filter_frame, text="Status")
        status_label.grid(row=0, column=2, sticky="w", padx=(0, 6))
        self.schedule_status_var = tk.StringVar(value="All")
        self.status_filter = ttk.Combobox(
            filter_frame,
            textvariable=self.schedule_status_var,
            values=["All", "pending", "taken", "missed", "snoozed"],
            state="readonly",
        )
        self.status_filter.grid(row=0, column=3, sticky="ew")
        self.status_filter.bind("<<ComboboxSelected>>", lambda event: self.refresh_schedule())

        apply_button = ttk.Button(filter_frame, text="Apply", command=self.refresh_schedule)
        apply_button.grid(row=0, column=4, padx=(12, 0))

        button_frame = ttk.Frame(schedule_frame)
        button_frame.grid(row=1, column=0, sticky="ew", padx=8, pady=6)
        button_frame.columnconfigure(0, weight=1)
        button_frame.columnconfigure(1, weight=1)

        self.mark_taken_button = ttk.Button(
            button_frame, text="Mark Taken", command=self.on_mark_taken
        )
        self.mark_taken_button.grid(row=0, column=0, sticky="ew", padx=5)

        self.mark_missed_button = ttk.Button(
            button_frame, text="Mark Missed", command=self.on_mark_missed
        )
        self.mark_missed_button.grid(row=0, column=1, sticky="ew", padx=5)

        tree_container = ttk.Frame(schedule_frame)
        tree_container.grid(row=2, column=0, sticky="nsew", padx=8, pady=(0, 8))
        tree_container.columnconfigure(0, weight=1)
        tree_container.rowconfigure(0, weight=1)

        columns = ("medication", "due", "status", "medication_id")
        self.schedule_tree = ttk.Treeview(tree_container, columns=columns, show="headings")
        self.schedule_tree["displaycolumns"] = ("medication", "due", "status")
        self.schedule_tree.heading("medication", text="Medication")
        self.schedule_tree.heading("due", text="Due")
        self.schedule_tree.heading("status", text="Status")
        self.schedule_tree.column("medication", width=200, anchor=tk.W)
        self.schedule_tree.column("due", width=180, anchor=tk.W)
        self.schedule_tree.column("status", width=100, anchor=tk.CENTER)
        self.schedule_tree.column("medication_id", width=0, stretch=False)
        self.schedule_tree.grid(row=0, column=0, sticky="nsew")
        self.schedule_tree.bind("<<TreeviewSelect>>", self.on_schedule_select)

        scrollbar = ttk.Scrollbar(tree_container, orient=tk.VERTICAL, command=self.schedule_tree.yview)
        scrollbar.grid(row=0, column=1, sticky="ns")
        self.schedule_tree.configure(yscrollcommand=scrollbar.set)

    def _build_history_section(self, container: ttk.Frame) -> None:
        history_frame = ttk.LabelFrame(container, text="Dose History")
        history_frame.grid(row=1, column=1, sticky="nsew", pady=(12, 0))
        history_frame.columnconfigure(0, weight=1)
        history_frame.rowconfigure(2, weight=1)

        filter_frame = ttk.Frame(history_frame)
        filter_frame.grid(row=0, column=0, sticky="ew", padx=8, pady=(8, 0))
        filter_frame.columnconfigure(1, weight=1)
        filter_frame.columnconfigure(3, weight=1)

        start_label = ttk.Label(filter_frame, text="Start (YYYY-MM-DD)")
        start_label.grid(row=0, column=0, sticky="w", padx=(0, 6))
        self.history_start_var = tk.StringVar()
        start_entry = ttk.Entry(filter_frame, textvariable=self.history_start_var)
        start_entry.grid(row=0, column=1, sticky="ew", padx=(0, 12))

        end_label = ttk.Label(filter_frame, text="End (YYYY-MM-DD)")
        end_label.grid(row=0, column=2, sticky="w", padx=(0, 6))
        self.history_end_var = tk.StringVar()
        end_entry = ttk.Entry(filter_frame, textvariable=self.history_end_var)
        end_entry.grid(row=0, column=3, sticky="ew", padx=(0, 12))

        history_apply = ttk.Button(filter_frame, text="Apply", command=self.refresh_history)
        history_apply.grid(row=0, column=4)

        summary_frame = ttk.Frame(history_frame)
        summary_frame.grid(row=1, column=0, sticky="ew", padx=8, pady=(6, 0))
        summary_frame.columnconfigure(0, weight=1)

        self.history_summary_var = tk.StringVar(value="No history entries to display.")
        summary_label = ttk.Label(summary_frame, textvariable=self.history_summary_var)
        summary_label.grid(row=0, column=0, sticky="w")

        tree_container = ttk.Frame(history_frame)
        tree_container.grid(row=2, column=0, sticky="nsew", padx=8, pady=(6, 8))
        tree_container.columnconfigure(0, weight=1)
        tree_container.rowconfigure(0, weight=1)

        history_columns = ("medication", "scheduled", "status", "acted", "notes")
        self.history_tree = ttk.Treeview(tree_container, columns=history_columns, show="headings")
        self.history_tree.heading("medication", text="Medication")
        self.history_tree.heading("scheduled", text="Scheduled")
        self.history_tree.heading("status", text="Status")
        self.history_tree.heading("acted", text="Acted At")
        self.history_tree.heading("notes", text="Notes")
        self.history_tree.column("medication", width=200, anchor=tk.W)
        self.history_tree.column("scheduled", width=180, anchor=tk.W)
        self.history_tree.column("status", width=100, anchor=tk.CENTER)
        self.history_tree.column("acted", width=180, anchor=tk.W)
        self.history_tree.column("notes", width=260, anchor=tk.W)
        self.history_tree.grid(row=0, column=0, sticky="nsew")

        history_scroll = ttk.Scrollbar(
            tree_container, orient=tk.VERTICAL, command=self.history_tree.yview
        )
        history_scroll.grid(row=0, column=1, sticky="ns")
        self.history_tree.configure(yscrollcommand=history_scroll.set)

        missed_frame = ttk.LabelFrame(history_frame, text="Missed Doses by Medication")
        missed_frame.grid(row=3, column=0, sticky="ew", padx=8, pady=(0, 8))
        missed_frame.columnconfigure(0, weight=1)

        missed_columns = ("medication", "count")
        self.history_missed_tree = ttk.Treeview(
            missed_frame,
            columns=missed_columns,
            show="headings",
            height=4,
        )
        self.history_missed_tree.heading("medication", text="Medication")
        self.history_missed_tree.heading("count", text="Missed")
        self.history_missed_tree.column("medication", anchor=tk.W, width=240)
        self.history_missed_tree.column("count", anchor=tk.CENTER, width=80)
        self.history_missed_tree.grid(row=0, column=0, sticky="ew")

    # Data refresh ---------------------------------------------------
    def _medication_payload(self, item: Any) -> Optional[Dict[str, Any]]:
        medication_id = _from_mapping_or_attr(item, "medication_id")
        if not medication_id:
            return None
        notes = _from_mapping_or_attr(item, "notes")
        if not notes:
            notes = _from_mapping_or_attr(item, "instructions", "")
        payload: Dict[str, Any] = {
            "medication_id": medication_id,
            "name": _from_mapping_or_attr(item, "name", ""),
            "dosage": _from_mapping_or_attr(item, "dosage", ""),
            "notes": notes or "",
            "frequency": None,
            "times": [],
        }
        schedule = _from_mapping_or_attr(item, "schedule")
        if schedule is not None:
            frequency_value = _from_mapping_or_attr(schedule, "repeat_interval")
            if frequency_value is None:
                frequency_value = _from_mapping_or_attr(schedule, "repeat_interval_minutes")
            if frequency_value is not None:
                try:
                    payload["frequency"] = int(frequency_value.total_seconds() // 60)
                except AttributeError:
                    payload["frequency"] = frequency_value
            times_of_day = _from_mapping_or_attr(schedule, "times_of_day")
            if times_of_day:
                payload["times"] = list(times_of_day)
        return payload

    def refresh_medications(self) -> None:
        loader = getattr(self.controller, "list_medications", None)
        medications: Iterable[Any]
        if callable(loader):
            medications = loader()
        else:
            medications = []

        cache: Dict[str, Mapping[str, Any]] = {}
        for item in medications:
            payload = self._medication_payload(item)
            if not payload:
                continue
            cache[payload["medication_id"]] = payload
        self._medication_cache = cache

    def refresh_schedule(self) -> None:
        loader = getattr(self.controller, "list_upcoming_doses", None)
        filter_text = self.schedule_filter_var.get().strip()
        status_filter = self.schedule_status_var.get()
        status_value = status_filter if status_filter and status_filter != "All" else None

        doses: Iterable[Any]
        if callable(loader):
            params: Dict[str, Any] = {}
            if filter_text:
                params["filter_text"] = filter_text
            if status_value:
                params["status"] = status_value
            doses = loader(**params)
        else:
            doses = []

        for child in self.schedule_tree.get_children():
            self.schedule_tree.delete(child)

        for index, dose in enumerate(doses):
            dose_id = _from_mapping_or_attr(dose, "dose_id", f"dose-{index}")
            medication = _from_mapping_or_attr(dose, "medication_name")
            medication_id = _from_mapping_or_attr(dose, "medication_id", "")
            if not medication:
                medication = medication_id
            due = _from_mapping_or_attr(dose, "effective_due_time")
            if callable(due):
                due = due()
            if due is None:
                due = _from_mapping_or_attr(dose, "scheduled_time")
            due_dt = _coerce_datetime(due)
            due_display = due_dt.strftime("%Y-%m-%d %H:%M") if due_dt else str(due or "")
            status = _from_mapping_or_attr(dose, "status", "")

            iid = dose_id or f"dose-{index}"
            self.schedule_tree.insert(
                "",
                tk.END,
                iid=iid,
                values=(medication, due_display, status, medication_id),
            )

    def refresh_history(self) -> None:
        loader = getattr(self.controller, "list_history", None)
        start_raw = self.history_start_var.get().strip()
        end_raw = self.history_end_var.get().strip()

        start_dt: Optional[datetime] = None
        end_dt: Optional[datetime] = None
        if start_raw:
            try:
                start_dt = datetime.strptime(start_raw, "%Y-%m-%d")
            except ValueError:
                self._show_warning("Invalid start date format. Use YYYY-MM-DD.")
        if end_raw:
            try:
                end_dt = datetime.strptime(end_raw, "%Y-%m-%d")
            except ValueError:
                self._show_warning("Invalid end date format. Use YYYY-MM-DD.")

        history: Iterable[Any]
        if callable(loader):
            params = {}
            if start_dt:
                params["start"] = start_dt
            if end_dt:
                params["end"] = end_dt
            history = loader(**params)
        else:
            history = []

        history_list = list(history)
        filter_result = filter_history_entries(history_list, start_dt, end_dt)
        metrics = calculate_metrics(filter_result)
        filtered_history = filter_result.entries

        self._update_history_summary(metrics)
        self._update_missed_by_medication(metrics)

        for child in self.history_tree.get_children():
            self.history_tree.delete(child)

        for index, entry in enumerate(filtered_history):
            medication = _from_mapping_or_attr(entry, "medication_name")
            if not medication:
                medication = _from_mapping_or_attr(entry, "medication_id", "")
            scheduled = _from_mapping_or_attr(entry, "scheduled_time")
            scheduled_dt = _coerce_datetime(scheduled)
            scheduled_display = (
                scheduled_dt.strftime("%Y-%m-%d %H:%M") if scheduled_dt else str(scheduled or "")
            )
            status = _from_mapping_or_attr(entry, "status", "")
            acted = _from_mapping_or_attr(entry, "acted_at")
            acted_dt = _coerce_datetime(acted)
            acted_display = acted_dt.strftime("%Y-%m-%d %H:%M") if acted_dt else str(acted or "")
            notes = _from_mapping_or_attr(entry, "notes", "")

            iid = f"history-{index}"
            self.history_tree.insert(
                "",
                tk.END,
                iid=iid,
                values=(medication, scheduled_display, status, acted_display, notes),
            )

    def _update_history_summary(self, metrics: HistoryMetrics) -> None:
        if metrics.total == 0:
            self.history_summary_var.set("No history entries for the selected range.")
            return

        summary_text = (
            "Total: {total} | Taken: {taken} | Missed: {missed} | Snoozed: {snoozed} | "
            "Adherence: {adherence:.1f}%"
        ).format(
            total=metrics.total,
            taken=metrics.taken,
            missed=metrics.missed,
            snoozed=metrics.snoozed,
            adherence=metrics.adherence_percent,
        )
        self.history_summary_var.set(summary_text)

    def _update_missed_by_medication(self, metrics: HistoryMetrics) -> None:
        if not hasattr(self, "history_missed_tree"):
            return

        for child in self.history_missed_tree.get_children():
            self.history_missed_tree.delete(child)

        if not metrics.missed_by_medication:
            return

        for index, (medication, count) in enumerate(metrics.missed_by_medication.items()):
            iid = f"missed-{index}"
            self.history_missed_tree.insert("", tk.END, iid=iid, values=(medication, count))

    # Event handlers -------------------------------------------------
    def on_scan_label(self) -> None:
        use_camera = messagebox.askyesno(
            "Scan Medication Label",
            "Would you like to capture the label with your camera?\n\n"
            "Select 'No' to choose an existing image file instead.",
        )

        try:
            if use_camera:
                result = self.scanner.capture_from_camera()
            else:
                path = filedialog.askopenfilename(
                    title="Select medication label image",
                    filetypes=[
                        ("Image files", "*.png *.jpg *.jpeg *.bmp *.tif *.tiff"),
                        ("All files", "*.*"),
                    ],
                )
                if not path:
                    return
                result = self.scanner.scan_from_path(path)
        except OCRFailure as exc:
            self._show_warning(str(exc))
            return

        recognized_text = result.text.strip()
        if not recognized_text:
            self._show_warning(
                "No text was detected in the label image. Please try again with a clearer photo."
            )
            return

        current = self.form.get_form_data()
        updated = dict(current)
        changed_fields: List[str] = []

        if result.medication_name:
            updated["name"] = result.medication_name
            changed_fields.append(f"Name → {result.medication_name}")
        if result.dosage:
            updated["dosage"] = result.dosage
            changed_fields.append(f"Dosage → {result.dosage}")

        self.form.set_form_data(updated)

        if changed_fields:
            self._show_info(
                "Label scanned successfully. Review the detected values before saving:\n\n"
                + "\n".join(changed_fields)
            )
        else:
            preview = recognized_text if len(recognized_text) < 300 else recognized_text[:297] + "..."
            self._show_warning(
                "The label text was read but the medication name or dosage could not be detected.\n"
                "Please review the recognized text and fill the details manually:\n\n"
                + preview
            )

    def on_add_medication(self) -> None:
        action = getattr(self.controller, "add_medication", None)
        if not callable(action):
            self._show_warning("Add medication action is not available.")
            return
        payload = self.form.get_form_data()
        action(payload)
        self.form.reset()
        self.refresh_medications()
        self.refresh_schedule()

    def on_edit_medication(self) -> None:
        action = getattr(self.controller, "edit_medication", None)
        if not callable(action):
            self._show_warning("Edit medication action is not available.")
            return
        payload = self.form.get_form_data()
        medication_id = payload.get("medication_id")
        if not medication_id:
            self._show_warning("Select a medication to edit.")
            return
        action(medication_id, payload)
        self.refresh_medications()
        self.refresh_schedule()

    def on_delete_medication(self) -> None:
        action = getattr(self.controller, "delete_medication", None)
        if not callable(action):
            self._show_warning("Delete medication action is not available.")
            return
        medication_id = self.form.current_medication_id
        if not medication_id:
            self._show_warning("Select a medication to delete.")
            return
        if not messagebox.askyesno("Confirm", "Delete this medication?"):
            return
        action(medication_id)
        self.form.reset()
        self.refresh_medications()
        self.refresh_schedule()

    def on_mark_taken(self) -> None:
        action = getattr(self.controller, "mark_dose_taken", None)
        if not callable(action):
            self._show_warning("Mark taken action is not available.")
            return
        dose_id = self._selected_dose_id()
        if not dose_id:
            return
        action(dose_id)
        self.refresh_schedule()
        self.refresh_history()

    def on_mark_missed(self) -> None:
        action = getattr(self.controller, "mark_dose_missed", None)
        if not callable(action):
            self._show_warning("Mark missed action is not available.")
            return
        dose_id = self._selected_dose_id()
        if not dose_id:
            return
        action(dose_id)
        self.refresh_schedule()
        self.refresh_history()

    def on_schedule_select(self, _event: Any) -> None:
        selection = self.schedule_tree.selection()
        if not selection:
            return
        dose_id = selection[0]
        medication_id = self.schedule_tree.set(dose_id, "medication_id")
        getter = getattr(self.controller, "get_medication", None)
        if medication_id and callable(getter):
            data = getter(medication_id)
            if isinstance(data, Mapping):
                self._medication_cache[medication_id] = data
                self.form.set_form_data(data)
                return
            converted = self._medication_payload(data)
            if converted:
                self._medication_cache[converted["medication_id"]] = converted
                self.form.set_form_data(converted)
                return
        if medication_id and medication_id in self._medication_cache:
            self.form.set_form_data(self._medication_cache[medication_id])
            return
        values = self.schedule_tree.item(dose_id).get("values", [])
        if values:
            medication_name = values[0]
            for payload in self._medication_cache.values():
                if payload.get("name") == medication_name:
                    self.form.set_form_data(payload)
                    break

    def _selected_dose_id(self) -> Optional[str]:
        selection = self.schedule_tree.selection()
        if not selection:
            self._show_warning("Select a dose from the schedule.")
            return None
        return selection[0]

    def _show_warning(self, message: str) -> None:
        messagebox.showwarning("Medication Reminder", message)

    def _show_info(self, message: str) -> None:
        messagebox.showinfo("Medication Reminder", message)

    # Public API -----------------------------------------------------
    def run(self) -> None:
        self.root.mainloop()


__all__ = ["ReminderApp", "MedicationFormFrame"]
