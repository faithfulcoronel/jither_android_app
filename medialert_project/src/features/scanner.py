"""Utilities for capturing medication labels and extracting text with OCR."""
from __future__ import annotations

from dataclasses import dataclass
import re
from typing import Optional, Sequence

import cv2  # type: ignore
import numpy as np
import pytesseract
from pytesseract import TesseractError, TesseractNotFoundError


class OCRFailure(RuntimeError):
    """Raised when the scanner is unable to capture or process an image."""


@dataclass(slots=True)
class OCRResult:
    """Container for the OCR text and extracted medication attributes."""

    text: str
    medication_name: Optional[str]
    dosage: Optional[str]


class MedicationLabelScanner:
    """Capture an image of a medication label and extract structured data."""

    _DOSAGE_PATTERN = re.compile(
        r"(\d+(?:\.\d+)?\s?(?:mg|mcg|g|kg|ml|l|iu|units?|tablets?|tabs?|"
        r"capsules?|caps?|pills?|drops?))",
        re.IGNORECASE,
    )

    def capture_from_camera(self, camera_index: int = 0) -> OCRResult:
        """Capture a single frame from the given camera and run OCR on it."""

        image = self._capture_image(camera_index)
        return self.scan_image(image)

    def scan_from_path(self, path: str) -> OCRResult:
        """Load an image from ``path`` and run OCR on the resulting frame."""

        image = self._load_image(path)
        return self.scan_image(image)

    def scan_image(self, image: np.ndarray) -> OCRResult:
        """Run OCR on the provided ``image`` and parse medication details."""

        text = self._extract_text(image)
        if not text.strip():
            raise OCRFailure(
                "No readable text was detected. Try retaking the photo in better lighting."
            )
        medication_name, dosage = self._parse_medication_details(text)
        return OCRResult(text=text.strip(), medication_name=medication_name, dosage=dosage)

    # Internal helpers -------------------------------------------------
    def _capture_image(self, camera_index: int) -> np.ndarray:
        camera = cv2.VideoCapture(camera_index)
        if not camera.isOpened():
            raise OCRFailure(
                "Unable to access the camera. Ensure it is connected and not in use."
            )
        try:
            success, frame = camera.read()
            if not success or frame is None:
                raise OCRFailure("Failed to capture a photo from the camera.")
            return frame
        finally:
            camera.release()

    def _load_image(self, path: str) -> np.ndarray:
        image = cv2.imread(path)
        if image is None:
            raise OCRFailure(f"Could not read an image from '{path}'.")
        return image

    def _extract_text(self, image: np.ndarray) -> str:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        blurred = cv2.medianBlur(gray, 3)
        _, threshold = cv2.threshold(
            blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU
        )
        try:
            text = pytesseract.image_to_string(threshold)
        except TesseractNotFoundError as exc:  # pragma: no cover - environment specific
            raise OCRFailure(
                "Tesseract OCR engine is not installed. Install it to enable scanning."
            ) from exc
        except TesseractError as exc:  # pragma: no cover - passthrough from engine
            raise OCRFailure("An unexpected OCR error occurred. Please try again.") from exc
        return text

    def _parse_medication_details(self, text: str) -> tuple[Optional[str], Optional[str]]:
        lines = [line.strip() for line in text.splitlines() if line.strip()]
        dosage = self._find_dosage(lines)
        medication_name = self._find_medication_name(lines, dosage)
        return medication_name, dosage

    def _find_dosage(self, lines: Sequence[str]) -> Optional[str]:
        for line in lines:
            match = self._DOSAGE_PATTERN.search(line)
            if match:
                return match.group(0).strip()
        return None

    def _find_medication_name(
        self, lines: Sequence[str], dosage: Optional[str]
    ) -> Optional[str]:
        if dosage:
            dosage_lower = dosage.lower()
            for line in lines:
                lower_line = line.lower()
                idx = lower_line.find(dosage_lower)
                if idx != -1:
                    candidate = line[:idx].strip(" -,:\n\t")
                    if candidate:
                        return candidate
        for line in lines:
            if any(char.isalpha() for char in line) and not line.isdigit():
                return line
        return None


__all__ = ["MedicationLabelScanner", "OCRResult", "OCRFailure"]
