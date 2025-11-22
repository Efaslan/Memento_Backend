package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Integer> {
    // Since the userId is the same, there isn't a need for extra methods.
}