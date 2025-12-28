package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.SavedLocationDto;
import com.emiraslan.memento.entity.SavedLocation;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.SavedLocationRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedLocationService {

    private final SavedLocationRepository locationRepository;

    public List<SavedLocationDto> getLocationsByPatient(Integer patientId) {
        return locationRepository.findByPatient_UserId(patientId)
                .stream()
                .map(MapperUtil::toSavedLocationDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SavedLocationDto createLocation(SavedLocationDto dto, User patient) {
        SavedLocation location = MapperUtil.toSavedLocationEntity(dto, patient); // object with nulls

        SavedLocation savedLocation = locationRepository.save(location); // obj after it receives an id from the db

        return MapperUtil.toSavedLocationDto(savedLocation);
    }

    @Transactional
    public SavedLocationDto updateLocation(Integer locationId, SavedLocationDto dto) {

        SavedLocation existingLocation = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("LOCATION_NOT_FOUND: " + locationId));

        // update fields
        existingLocation.setLocationName(dto.getLocationName());
        existingLocation.setLatitude(dto.getLatitude());
        existingLocation.setLongitude(dto.getLongitude());
        existingLocation.setAddressDetails(dto.getAddressDetails());

        SavedLocation updatedLocation = locationRepository.save(existingLocation);

        return MapperUtil.toSavedLocationDto(updatedLocation);
    }

    public void deleteLocation(Integer locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new EntityNotFoundException("LOCATION_NOT_FOUND: " + locationId);
        }
        locationRepository.deleteById(locationId);
    }
}