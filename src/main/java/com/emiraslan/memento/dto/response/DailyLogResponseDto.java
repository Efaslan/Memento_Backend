package com.emiraslan.memento.dto.response;

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
    private String description;
    private Integer quantityMl;
    private LocalDateTime createdAt;

    // message to frontend in case the Groq API couldn't format user speech, and their raw Speech-To-Text description was saved to DB.
    private String warningMessage;
}