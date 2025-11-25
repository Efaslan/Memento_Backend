package com.emiraslan.memento.dto;

import com.emiraslan.memento.enums.AlertStatus;
import com.emiraslan.memento.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private Integer alertId;
    private Integer patientUserId;
    private AlertType alertType;
    private LocalDateTime alertTimestamp;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private AlertStatus status;
    private String details;
}