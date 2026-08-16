package com.emiraslan.memento.repository.medication;

public interface ScheduleStatsProjection {
    Long getTotalLogs();
    Long getTakenCount();
    Long getDelayedCount();
    Long getSkippedCount();
}
