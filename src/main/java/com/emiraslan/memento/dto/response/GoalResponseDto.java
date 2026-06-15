package com.emiraslan.memento.dto.response;

import com.emiraslan.memento.enums.GoalStatus;
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
    private Double targetValue;
    private String unit;
    private Boolean isActive;
    private LocalDateTime createdAt;

    private TodayGoalLogDto todayGoalLog;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TodayGoalLogDto {
        private Integer goalLogId; // today's logs doesn't exist if this is null
        private GoalStatus status;
        private Double progressValue;
    }
}