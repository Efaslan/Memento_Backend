package com.emiraslan.memento.repository.user;

import com.emiraslan.memento.entity.user.PatientRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRelationshipRepository extends JpaRepository<PatientRelationship, Integer> {

    @Query("""
        SELECT r FROM PatientRelationship r
        JOIN FETCH r.patient
        JOIN FETCH r.caregiver
        WHERE (r.patient.userId = :userId OR r.caregiver.userId = :userId)
          AND r.isActive = true
    """)
    List<PatientRelationship> findAllActiveRelationshipsByUserId(@Param("userId") Integer userId);

    // Brings all primary contacts
    List<PatientRelationship> findByPatient_UserIdAndIsPrimaryContactTrueAndIsActiveTrue(Integer patientId);

    // checks if a relationship already exists
    Optional<PatientRelationship> findByPatient_UserIdAndCaregiver_UserId(Integer patientId, Integer caregiverId);

    // checks if a user is primary contact and active. Used for alert acknowledgements
    boolean existsByPatient_UserIdAndCaregiver_UserIdAndIsPrimaryContactTrueAndIsActiveTrue(Integer patientId, Integer caregiverId);

    List<PatientRelationship> findAllByIsActiveTrue();
}