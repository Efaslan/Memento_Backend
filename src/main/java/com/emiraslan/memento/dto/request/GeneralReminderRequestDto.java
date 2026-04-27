package com.emiraslan.memento.dto.request;

import com.emiraslan.memento.enums.RecurrenceRule;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralReminderRequestDto {

    @NotNull(message = "PATIENT_ID_REQUIRED")
    private Integer patientUserId;

    @NotBlank(message = "TITLE_REQUIRED")
    @Size(max = 100, message = "TITLE_TOO_LONG")
    private String title;

    @NotNull(message = "REMINDER_TIME_REQUIRED")
    @Future(message = "REMINDER_TIME_MUST_BE_IN_FUTURE")
    private LocalDateTime reminderTime;

    @NotNull(message = "IS_RECURRING_REQUIRED")
    private Boolean isRecurring;

    // nullable in case isRecurring = false
    private RecurrenceRule recurrenceRule;
}
