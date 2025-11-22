package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.SavedLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedLocationRepository extends JpaRepository<SavedLocation, Integer> {

    // All saved locations of the patient
    List<SavedLocation> findByPatient_UserId(Integer patientId);

    // Chatbot: "KIZIM konumunu bul" ignore case (kızım = KIZIM = Kızım)
    Optional<SavedLocation> findByPatient_UserIdAndLocationNameContainingIgnoreCase(Integer patientId, String locationName);
}