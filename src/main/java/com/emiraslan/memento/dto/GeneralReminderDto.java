package com.emiraslan.memento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralReminderDto {
    private Integer reminderId;
    private Integer patientUserId;
    private Integer creatorUserId;
    private String creatorName; // For showing the creator's name
    private String title;
    private LocalDateTime reminderTime;
    private Boolean isRecurring;
    private String recurrenceRule;
    private Boolean isCompleted;
}