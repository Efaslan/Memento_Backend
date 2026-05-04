package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.SavedLocationRequestDto;
import com.emiraslan.memento.dto.response.SavedLocationResponseDto;
import com.emiraslan.memento.entity.SavedLocation;
import com.emiraslan.memento.entity.user.User;
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

    public List<SavedLocationResponseDto> getLocationsByPatient(Integer patientId) {
        return locationRepository.findByPatient_UserId(patientId)
                .stream()
                .map(MapperUtil::toSavedLocationResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SavedLocationResponseDto createLocation(SavedLocationRequestDto dto, User patient) {
        SavedLocation location = MapperUtil.toSavedLocationEntity(dto, patient); // object with nulls

        SavedLocation savedLocation = locationRepository.save(location); // obj after it receives an id from the db

        return MapperUtil.toSavedLocationResponseDto(savedLocation);
    }

    @Transactional
    public SavedLocationResponseDto updateLocation(Integer locationId, SavedLocationRequestDto dto) {
        SavedLocation existingLocation = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("LOCATION_NOT_FOUND: " + locationId));

        // update fields
        existingLocation.setLocationName(dto.getLocationName());
        existingLocation.setLatitude(dto.getLatitude());
        existingLocation.setLongitude(dto.getLongitude());
        existingLocation.setAddressDetails(dto.getAddressDetails());

        SavedLocation updatedLocation = locationRepository.save(existingLocation);

        return MapperUtil.toSavedLocationResponseDto(updatedLocation);
    }

    public void deleteLocation(Integer locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new EntityNotFoundException("LOCATION_NOT_FOUND: " + locationId);
        }
        locationRepository.deleteById(locationId);
    }
}