package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.GeneralReminderDto;
import com.emiraslan.memento.entity.GeneralReminder;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.GeneralReminderRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeneralReminderService {

    private final GeneralReminderRepository reminderRepository;
    private final UserRepository userRepository;

    // brings all active reminders
    public List<GeneralReminderDto> getAllOngoingRemindersByPatient(Integer patientId) {
        return reminderRepository.findByPatient_UserIdAndIsCompletedFalseOrderByReminderTimeAsc(patientId)
                .stream()
                .map(MapperUtil::toGeneralReminderDto)
                .collect(Collectors.toList());
    }

    // brings all past reminders
    public List<GeneralReminderDto> getCompletedRemindersByPatient(Integer patientId) {
        return reminderRepository.findByPatient_UserIdAndIsCompletedTrueOrderByReminderTimeAsc(patientId)
                .stream()
                .map(MapperUtil::toGeneralReminderDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GeneralReminderDto createReminder(GeneralReminderDto dto) {

        User patient = userRepository.findById(dto.getPatientUserId())
                .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND: " + dto.getPatientUserId()));

        // find creator if not null
        User creator = null;
        if (dto.getCreatorUserId() != null) {
            creator = userRepository.findById(dto.getCreatorUserId())
                    .orElseThrow(() -> new EntityNotFoundException("CREATOR_USER_NOT_FOUND: " + dto.getCreatorUserId()));
        }

        GeneralReminder reminder = MapperUtil.toGeneralReminderEntity(dto, patient, creator);

        return MapperUtil.toGeneralReminderDto(reminderRepository.save(reminder));
    }

    @Transactional
    public GeneralReminderDto updateReminder(Integer reminderId, GeneralReminderDto dto) {
        GeneralReminder existingReminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND: " + reminderId));

        // Full Update / PUT. Mobile will send all fields filled, no need for null checks
        // Patient/Creator stays the same
        existingReminder.setTitle(dto.getTitle());
        existingReminder.setReminderTime(dto.getReminderTime());
        existingReminder.setIsRecurring(dto.getIsRecurring());
        existingReminder.setRecurrenceRule(dto.getRecurrenceRule());
        existingReminder.setIsCompleted(dto.getIsCompleted());

        return MapperUtil.toGeneralReminderDto(reminderRepository.save(existingReminder));
    }

    @Transactional
    public GeneralReminderDto markAsCompleted(Integer reminderId) {
        GeneralReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND: " + reminderId));

        if (Boolean.TRUE.equals(reminder.getIsCompleted())) {
            // if its already completed, return it as it is
            return MapperUtil.toGeneralReminderDto(reminder);
        }

        reminder.setIsCompleted(true);

        return MapperUtil.toGeneralReminderDto(reminderRepository.save(reminder));
    }

    public void deleteReminder(Integer reminderId) {
        if (!reminderRepository.existsById(reminderId)) {
            throw new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND: " + reminderId);
        }
        reminderRepository.deleteById(reminderId);
    }
}