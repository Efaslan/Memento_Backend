package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.PatientRelationship;
import com.emiraslan.memento.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRelationshipRepository extends JpaRepository<PatientRelationship, Integer> {

    // All deactivated relationships of a patient
    List<PatientRelationship> findByPatient_UserIdAndIsActiveFalse(Integer patientId);

    // All active relationships of a patient
    List<PatientRelationship> findByPatient_UserIdAndIsActiveTrue(Integer patientId);

    List<PatientRelationship> findByCaregiver_UserIdAndIsActiveTrue(Integer caregiverId);

    List<PatientRelationship> findByCaregiver_UserIdAndIsActiveFalse(Integer caregiverId);

    // Filter a specific role from the relationships. Example: show only relatives by excluding DOCTOR
    List<PatientRelationship> findByPatient_UserIdAndRelationshipTypeNotAndIsActiveTrue(Integer patientId, RelationshipType type);

    // Brings all primary contacts
    List<PatientRelationship> findByPatient_UserIdAndIsPrimaryContactTrueAndIsActiveTrue(Integer patientId);

    // checks if a relationship already exists
    Optional<PatientRelationship> findByPatient_UserIdAndCaregiver_UserId(Integer patientId, Integer caregiverId);

    // For Chatbot to find people with a specific role ("Doktorum kim?", "Oğlum kim?")
    // kizimin telefon numarasi ne, doktorumun adi ne? gibi sorular icin bu metot yazildi. Gerek olmazsa sileriz ileride. Lokasyon icin sadece SavedLocation kullaniliyor, bu degil.
    List<PatientRelationship> findByPatient_UserIdAndRelationshipTypeAndIsActiveTrue(Integer patientId, RelationshipType type);
}