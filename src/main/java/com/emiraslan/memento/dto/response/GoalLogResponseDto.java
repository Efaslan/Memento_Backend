package com.emiraslan.memento.dto.response;

import com.emiraslan.memento.enums.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalLogResponseDto {
    private Integer goalLogId;
    private GoalStatus status;
    private Double progressValue;
    private Double logTargetValue;
    private String logUnit;
    private LocalDate createdAt;
}
