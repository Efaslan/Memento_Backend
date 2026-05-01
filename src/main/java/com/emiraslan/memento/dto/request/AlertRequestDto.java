package com.emiraslan.memento.dto.request;

import com.emiraslan.memento.enums.AlertType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequestDto {

    @NotNull(message = "ALERT_TYPE_REQUIRED")
    private AlertType alertType;

    @Digits(integer = 3, fraction = 6, message = "INVALID_LATITUDE_FORMAT")
    private BigDecimal latitude;

    @Digits(integer = 3, fraction = 6, message = "INVALID_LONGITUDE_FORMAT")
    private BigDecimal longitude;
}
