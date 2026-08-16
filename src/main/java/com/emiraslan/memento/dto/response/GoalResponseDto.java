package com.emiraslan.memento.dto.response;

import com.emiraslan.memento.enums.GoalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponseDto {
    private Integer goalId;
    private GoalType goalType;
    private String title;
    private Double currentTargetValue;
    private String unit;
    private Boolean isActive;
    private LocalDateTime createdAt;
}