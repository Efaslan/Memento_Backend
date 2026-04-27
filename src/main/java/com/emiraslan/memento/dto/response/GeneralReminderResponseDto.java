package com.emiraslan.memento.dto.response;

import com.emiraslan.memento.enums.RecurrenceRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralReminderResponseDto {
    private Integer reminderId;
    private Integer patientUserId;
    private Integer creatorUserId;
    private String creatorName;
    private String title;
    private LocalDateTime reminderTime;
    private Boolean isRecurring;
    private RecurrenceRule recurrenceRule;
    private Boolean isCompleted;
}