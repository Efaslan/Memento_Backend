package com.emiraslan.memento.repository.relationship;

import com.emiraslan.memento.entity.relationship.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Integer> {

    // All relationships
    List<Relationship> findByPatient_UserId(Integer patientId);

    List<Relationship> findByCaregiver_UserId(Integer caregiverId);

    // Brings all primary contacts
    List<Relationship> findByPatient_UserIdAndIsPrimaryContactTrue(Integer patientId);

    // checks if a relationship already exists
    Optional<Relationship> findByPatient_UserIdAndCaregiver_UserId(Integer patientId, Integer caregiverId);

    // checks if a user is primary contact and active. Used for alert acknowledgements
    boolean existsByPatient_UserIdAndCaregiver_UserIdAndIsPrimaryContactTrue(Integer patientId, Integer caregiverId);
}