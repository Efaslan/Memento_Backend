package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.GeneralReminderRequestDto;
import com.emiraslan.memento.dto.response.GeneralReminderResponseDto;
import com.emiraslan.memento.entity.GeneralReminder;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.RecurrenceRule;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.GeneralReminderRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralReminderService {

    private final GeneralReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // brings all active reminders
    public List<GeneralReminderResponseDto> getAllRemindersByPatient(Integer patientId) {
        return reminderRepository.findByPatient_UserIdOrderByReminderTimeAsc(patientId)
                .stream()
                .map(MapperUtil::toGeneralReminderResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GeneralReminderResponseDto createReminder(GeneralReminderRequestDto dto, User creator) {
        User patient;

        // if the creator is patient, patientId in dto is set automatically
        if(creator.getRole() == UserRole.PATIENT){
            patient = creator;
            dto.setPatientUserId(creator.getUserId());
        }
        // if the creator is a doctor or relative
        else {
            patient = userRepository.findById(dto.getPatientUserId())
                    .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND: " + dto.getPatientUserId()));
        }

        GeneralReminder reminder = MapperUtil.toGeneralReminderEntity(dto, patient, creator);
        return MapperUtil.toGeneralReminderResponseDto(reminderRepository.save(reminder));
    }

    @Transactional
    public GeneralReminderResponseDto updateReminder(Integer reminderId, GeneralReminderRequestDto dto) {
        GeneralReminder existingReminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND: " + reminderId));

        // Full Update / PUT. Mobile will send all fields filled, no need for null checks
        // Patient/Creator stays the same
        existingReminder.setTitle(dto.getTitle());
        existingReminder.setReminderTime(dto.getReminderTime());
        existingReminder.setIsRecurring(dto.getIsRecurring());
        existingReminder.setRecurrenceRule(dto.getRecurrenceRule());

        return MapperUtil.toGeneralReminderResponseDto(reminderRepository.save(existingReminder));
    }

    public void deleteReminder(Integer reminderId) {
        if (!reminderRepository.existsById(reminderId)) {
            throw new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND: " + reminderId);
        }
        reminderRepository.deleteById(reminderId);
    }

    // we find general reminders with <= currentDateTime, in case the server went offline during a reminder's time.
    // because we set the next reminder time or complete the reminder after notification, we don't send multiple notifications
    // for a single reminder
    @Transactional
    public void processGeneralReminders(LocalDateTime now) {
        List<GeneralReminder> dueReminders = reminderRepository.findDueRemindersWithPatient(now);

        List<GeneralReminder> toUpdate = new ArrayList<>();
        List<GeneralReminder> toDelete = new ArrayList<>();

        for (GeneralReminder reminder : dueReminders) {
            notificationService.sendNotificationToUser(reminder.getPatient().getUserId(), "Memento", reminder.getTitle());

            // for isRecurring = true reminders
            if (Boolean.TRUE.equals(reminder.getIsRecurring()) && reminder.getRecurrenceRule() != null) {
                reminder.setReminderTime(calculateNextReminderTime(reminder.getReminderTime(), reminder.getRecurrenceRule()));
                toUpdate.add(reminder);
            } else { // delete the reminder if its not recurring
                toDelete.add(reminder);
            }
        }
        // batch write
        if (!toDelete.isEmpty()) reminderRepository.deleteAllInBatch(toDelete);
        if (!toUpdate.isEmpty()) reminderRepository.saveAll(toUpdate);
    }

    private LocalDateTime calculateNextReminderTime(LocalDateTime currentReminderTime, RecurrenceRule recurrenceRule){
        return switch(recurrenceRule){
            case DAILY -> currentReminderTime.plusDays(1);
            case WEEKLY -> currentReminderTime.plusWeeks(1);
            case MONTHLY -> currentReminderTime.plusMonths(1);
            case YEARLY -> currentReminderTime.plusYears(1);
        };
    }
}