package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.GeneralReminderRequestDto;
import com.emiraslan.memento.dto.response.GeneralReminderResponseDto;
import com.emiraslan.memento.entity.DeviceToken;
import com.emiraslan.memento.entity.GeneralReminder;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.RecurrenceRule;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.DeviceTokenRepository;
import com.emiraslan.memento.repository.GeneralReminderRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralReminderService {

    private final GeneralReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;

    // brings all active reminders
    public List<GeneralReminderResponseDto> getAllOngoingRemindersByPatient(Integer patientId) {
        return reminderRepository.findByPatient_UserIdAndIsCompletedFalseOrderByReminderTimeAsc(patientId)
                .stream()
                .map(MapperUtil::toGeneralReminderResponseDto)
                .collect(Collectors.toList());
    }

    // brings all past reminders
    public List<GeneralReminderResponseDto> getCompletedRemindersByPatient(Integer patientId) {
        return reminderRepository.findByPatient_UserIdAndIsCompletedTrueOrderByReminderTimeAsc(patientId)
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

    @Transactional
    public GeneralReminderResponseDto markAsCompleted(Integer reminderId) {
        GeneralReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND: " + reminderId));

        if (Boolean.TRUE.equals(reminder.getIsCompleted())) {
            // if its already completed, return it as it is
            return MapperUtil.toGeneralReminderResponseDto(reminder);
        }

        reminder.setIsCompleted(true);

        return MapperUtil.toGeneralReminderResponseDto(reminderRepository.save(reminder));
    }

    public void deleteReminder(Integer reminderId) {
        if (!reminderRepository.existsById(reminderId)) {
            throw new EntityNotFoundException("GENERAL_REMINDER_NOT_FOUND: " + reminderId);
        }
        reminderRepository.deleteById(reminderId);
    }

    // Checks every minute for general reminders that are due
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void notifyGeneralReminders(){
        LocalDateTime now = LocalDateTime.now();

        // bring incomplete and due reminders
        List<GeneralReminder> dueReminders = reminderRepository.findByReminderTimeLessThanEqualAndIsCompletedFalse(now);
        log.info("Notify General Reminders CRON Began:");

        for(GeneralReminder reminder : dueReminders){

            List<DeviceToken> deviceTokens = deviceTokenRepository.findByUser_UserId(reminder.getPatient().getUserId());
            log.info("{} devices found.", deviceTokens.size());

            int counter = 0;
            for(DeviceToken token : deviceTokens){
                if(token.getFcmToken() != null && !token.getFcmToken().isEmpty()){
                    fcmService.sendNotificationToToken(token.getFcmToken(), "Memento Hatırlatıcı", reminder.getTitle());
                    counter++;
                }
            }
            int failedNotifications = deviceTokens.size() - counter;
            log.info("{} notifications sent. {} failed.", counter, failedNotifications);

            // if the reminder is reoccurring, calculate next reminder's time
            if(Boolean.TRUE.equals(reminder.getIsRecurring()) && reminder.getRecurrenceRule() != null){
                LocalDateTime nextReminderTime = calculateNextReminderTime(reminder.getReminderTime(), reminder.getRecurrenceRule());
                reminder.setReminderTime(nextReminderTime);
            } else{ // If it is not reoccurring, set it as completed
                reminder.setIsCompleted(true);
            }
            reminderRepository.save(reminder);
        }
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