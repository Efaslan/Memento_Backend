package com.emiraslan.memento.enums;

public enum AlertStatus {
    PENDING, // waiting for 30 seconds to confirm the fall
    SENT, // notifications sent to all relatives
    ACKNOWLEDGED, // a relative is handling the situation
    CANCELLED // false alarm, patient cancelled within 30 seconds
}
