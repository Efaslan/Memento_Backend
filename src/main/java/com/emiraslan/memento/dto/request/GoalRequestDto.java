package com.emiraslan.memento.dto.request;

import com.emiraslan.memento.enums.GoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequestDto {

    @NotNull(message = "PATIENT_ID_REQUIRED")
    private Integer patientUserId;

    @NotNull(message = "GOAL_TYPE_CANNOT_BE_NULL")
    private GoalType goalType;

    @NotBlank(message = "TITLE_CANNOT_BE_EMPTY")
    @Size(max = 100, message = "TITLE_TOO_LONG")
    private String title;

    @NotNull(message = "CURRENT_TARGET_VALUE_CANNOT_BE_NULL")
    @Positive(message = "TARGET_VALUE_MUST_BE_POSITIVE")
    private Double currentTargetValue;

    @NotBlank(message = "UNIT_CANT_BE_EMPTY")
    @Size(max = 20, message = "UNIT_TOO_LONG")
    private String unit;
}