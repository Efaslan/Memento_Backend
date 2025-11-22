package com.emiraslan.memento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "SavedLocations")
public class SavedLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Integer locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patient;

    @Column(name = "location_name", length = 100)
    private String locationName;

    // Precise Location Data (Google Maps standard: 9,6)
    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "address_details", columnDefinition = "NVARCHAR(MAX)")
    private String addressDetails;
}
