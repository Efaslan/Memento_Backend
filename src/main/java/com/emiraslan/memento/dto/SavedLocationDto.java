package com.emiraslan.memento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedLocationDto {
    private Integer locationId;
    private Integer patientUserId;
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressDetails;
}