package com.emiraslan.memento.dto.response;

import com.emiraslan.memento.enums.AlertStatus;
import com.emiraslan.memento.enums.AlertType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class AlertResponseDto { // alert dto'nun aynisi

    private Integer alertId;
    private Integer patientUserId;

    @NotNull(message = "ALERT_TYPE_REQUIRED")
    private AlertType alertType;

    private LocalDateTime alertTimestamp;

    // Location data is included to provide relatives with the data
    @Digits(integer = 3, fraction = 6, message = "INVALID_LATITUDE_FORMAT")
    private BigDecimal latitude;
    @Digits(integer = 3, fraction = 6, message = "INVALID_LONGITUDE_FORMAT")
    private BigDecimal longitude;

    private AlertStatus status;
    @Size(max = 100, message = "DETAILS_TOO_LONG")
    private String details;

    // alert acknowledged by:
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer acknowledgedByUserId;

    @Size(max = 100, message = "ACKNOWLEDGED_BY_NAME_TOO_LONG")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String acknowledgedByName; // for display
}
