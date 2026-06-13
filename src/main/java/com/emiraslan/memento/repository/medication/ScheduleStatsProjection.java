package com.emiraslan.memento.repository.medication;

public interface ScheduleStatsProjection {
    Integer getScheduleId();
    Long getTakenCount();
    Long getDelayedCount();
    Long getSkippedCount();
}
