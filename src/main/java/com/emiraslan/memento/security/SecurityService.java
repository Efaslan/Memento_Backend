package com.emiraslan.memento.security;

import com.emiraslan.memento.dto.GeneralReminderDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.RelationshipType;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.GeneralReminderRepository;
import com.emiraslan.memento.repository.PatientRelationshipRepository;
import com.emiraslan.memento.repository.SavedLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service("guard")
@RequiredArgsConstructor
public class SecurityService {

    private final PatientRelationshipRepository relationshipRepository;
    private final SavedLocationRepository locationRepository;
    private final GeneralReminderRepository reminderRepository;

    // --- helper method ----
    private boolean hasActiveRelationship(Integer patientId, Integer caregiverId){
        return relationshipRepository.findByPatient_UserIdAndCaregiver_UserId(patientId, caregiverId)
                .map(rel -> Boolean.TRUE.equals(rel.getIsActive()))
                .orElse(false);
    }

    // ========================================================================
    // RELATIONSHIP SECURITY
    // ========================================================================

    public boolean canUpdateRelationship(Integer relationshipId, User user) {
        return relationshipRepository.findById(relationshipId)
                .map(rel -> {
                    boolean isPatient = rel.getPatient().getUserId().equals(user.getUserId());
                    boolean isCaregiver = rel.getCaregiver().getUserId().equals(user.getUserId());

                    if (!isPatient && !isCaregiver) {
                        throw new AccessDeniedException("YOU_ARE_NOT_PART_OF_THIS_RELATIONSHIP");
                    }

                    // only doctor users can update type.doctor relationships
                    if (rel.getRelationshipType() == RelationshipType.DOCTOR && !isCaregiver) {
                        throw new AccessDeniedException("ONLY_DOCTORS_CAN_UPDATE_DOCTOR_RELATIONSHIPS");
                    }

                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));
    }

    // ========================================================================
    // PROFILE SECURITY
    // ========================================================================

    public boolean canViewPatientProfile(Integer patientId, User user) {
        if (!hasActiveRelationship(patientId, user.getUserId())) {
            throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
        } // returns the same 403 in ALL cases except 200
        return true;
    }

    // ========================================================================
    // SAVED LOCATION SECURITY
    // ========================================================================

    public boolean isLocationOwner(Integer locationId, User user){
        return locationRepository.findById(locationId)
                .map(location -> {
                    if (!location.getPatient().getUserId().equals(user.getUserId())){
                        throw new AccessDeniedException("NOT_LOCATION_OWNER");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("LOCATION_NOT_FOUND"));
    }

    // ========================================================================
    // GENERAL REMINDER SECURITY
    // ========================================================================

    public boolean canAccessPatientReminders(Integer patientId, User user) {
        if (!hasActiveRelationship(patientId, user.getUserId())) {
            throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
        }
        return true;
    }

    public boolean canCreateReminder(GeneralReminderDto dto, User user) {
        // patients do not need to include id in their request. It is automatically set in service
        if (user.getRole() == UserRole.PATIENT) {
            return true;
        }

        // this will throw 400 error if doctors or relatives did not include patient's id. Does not block patients because of method order
        if (dto.getPatientUserId() == null) {
            throw new IllegalArgumentException("PATIENT_ID_REQUIRED");
        }

        if (!hasActiveRelationship(dto.getPatientUserId(), user.getUserId())) {
            throw new AccessDeniedException("NO_ACTIVE_RELATIONSHIP_WITH_PATIENT");
        }
        return true;
    }

    // only the patient, or the creator, can update/delete the reminder
    public boolean canModifyReminder(Integer reminderId, User user) {
        return reminderRepository.findById(reminderId)
                .map(reminder -> {
                    boolean isPatient = reminder.getPatient().getUserId().equals(user.getUserId());
                    boolean isCreator = reminder.getCreator() != null && reminder.getCreator().getUserId().equals(user.getUserId());

                    if (!isPatient && !isCreator) {
                        throw new AccessDeniedException("YOU_CAN_ONLY_MODIFY_YOUR_OWN_OR_CREATED_REMINDERS");
                    }
                    return true;
                })
                .orElseThrow(() -> new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND"));
    }


}
