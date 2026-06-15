package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalLogRequestDto {

    @NotNull(message = "PROGRESS_VALUE_CANNOT_BE_NULL")
    @PositiveOrZero(message = "PROGRESS_VALUE_MUST_BE_POSITIVE_OR_ZERO")
    private Double progressValue;
}
