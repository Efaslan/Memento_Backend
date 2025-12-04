package com.emiraslan.memento.enums;

public enum MedicationStatus {
    TAKEN, // +- 30 minutes
    LATE_DOSE, // 30-120 minutes
    SKIPPED // after 2-hours
}
