package com.emiraslan.memento.dto.response;

import com.emiraslan.memento.enums.DailyLogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLogResponseDto {
    private Integer dailyLogId;
    private Integer patientUserId;
    private DailyLogType dailyLogType;
    private String description;
    private Integer quantityMl;
    private LocalDateTime createdAt;
}