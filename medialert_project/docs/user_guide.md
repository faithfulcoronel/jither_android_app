# MediaLert User Guide

This guide walks you through installing, configuring, and troubleshooting the MediaLert medication reminder assistant.

## Installation

### Prerequisites
- Python 3.9 or newer
- A virtual environment manager such as `venv` or `conda`
- Optional: Android Studio if you intend to work with the Android client contained in the repository

### Steps
1. Clone the repository and navigate to the `medialert_project` directory:
   ```bash
   git clone <repository-url>
   cd medialert_project
   ```
2. Create and activate a virtual environment:
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # On Windows use: .venv\Scripts\activate
   ```
3. Install Python dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Install optional development tooling (testing and linting):
   ```bash
   pip install -r requirements-dev.txt
   ```
   If you prefer manual installation, install `pytest`, `pytest-tkinter`, `black`, and `flake8` individually.

## Usage

### Running the desktop reminder assistant
1. Ensure your Python environment is active and the dependencies are installed.
2. Launch the reminder application:
   ```bash
   python main.py
   ```
3. Follow the prompts to scan prescriptions, review analytics, and receive reminder notifications. Tkinter is used for rendering reminder dialogs; if Tkinter is unavailable the application will automatically snooze reminders using the default snooze duration.

### Running automated checks
- **Unit and integration tests**:
  ```bash
  pytest
  ```
  This executes scheduler, storage, and notification smoke tests.
- **UI smoke tests**: Tkinter dialogs are validated with lightweight tests that run alongside the unit suite. To run manual checklist smoke tests, verify that notification dialogs render, buttons trigger the expected actions, and snooze timing matches configuration values.
- **Static analysis and formatting**:
  ```bash
  flake8
  black --check .
  ```
  Use `black .` to automatically format the codebase.

## Troubleshooting

### Tkinter is unavailable
If Tkinter cannot be imported (common on headless Linux servers), MediaLert falls back to automatically snoozing reminders. Install the `python3-tk` system package to enable dialogs, or continue using the automatic snooze fallback.

### Tests cannot locate project modules
Ensure you have activated the virtual environment and installed dependencies. Running the test suite from the project root ensures the tests can locate the `src/` directory. If you run tests from another directory, set `PYTHONPATH` to include the absolute path to `src`.

### Notifications are not being scheduled
Confirm that medications have a valid schedule with a future start time. Use the analytics history to verify that doses are being logged. If issues persist, delete the storage file in your user data directory to reset reminder state.

### Linting reports formatting errors
Run `black .` to auto-format the project, then rerun `flake8` to ensure the code meets style requirements. Configure your editor to apply Black formatting on save for the `src/` and `tests/` directories.

### Need to reset all reminder data
Use the CLI helper to reset the reminder storage:
```bash
python -c "from reminders.storage import ReminderStorage; ReminderStorage('path/to/storage.json').reset()"
```
Replace `path/to/storage.json` with the path configured in your environment.
