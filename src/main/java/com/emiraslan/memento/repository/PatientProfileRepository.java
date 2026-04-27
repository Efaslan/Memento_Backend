package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Integer> {
    // brings all profiles from the list of user ids, used for doctor web
    List<PatientProfile> findByPatient_UserIdIn(List<Integer> userIds);
}