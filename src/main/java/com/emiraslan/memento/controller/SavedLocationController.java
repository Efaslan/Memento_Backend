package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.SavedLocationDto;
import com.emiraslan.memento.service.SavedLocationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "04 - Saved Locations")
public class SavedLocationController {

    private final SavedLocationService locationService;

     // brings all locations of a patient
     // Endpoint: GET /api/v1/locations/patient/{patientId}
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SavedLocationDto>> getLocations(@PathVariable Integer patientId) {
        return ResponseEntity.ok(locationService.getLocationsByPatient(patientId));
    }

     // Endpoint: POST /api/v1/locations
     // Body: { "patientUserId": 5, "locationName": "Evim", "latitude": 40.123, ... }
    @PostMapping
    public ResponseEntity<SavedLocationDto> createLocation(@RequestBody SavedLocationDto dto) {
        return ResponseEntity.ok(locationService.createLocation(dto));
    }

     // Endpoint: PUT /api/v1/locations/{locationId}
    @PutMapping("/{locationId}")
    public ResponseEntity<SavedLocationDto> updateLocation(
            @PathVariable Integer locationId,
            @RequestBody SavedLocationDto dto
    ) {
        return ResponseEntity.ok(locationService.updateLocation(locationId, dto));
    }

     // Endpoint: DELETE /api/v1/locations/{locationId}
    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Integer locationId) {
        locationService.deleteLocation(locationId);
        // returns 204 (No Content), if successful
        return ResponseEntity.noContent().build();
    }
}