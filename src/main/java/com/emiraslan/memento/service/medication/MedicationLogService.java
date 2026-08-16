package com.emiraslan.memento.service.medication;

import com.emiraslan.memento.dto.response.medication.MedicationLogResponseDto;
import com.emiraslan.memento.entity.medication.MedicationLog;
import com.emiraslan.memento.entity.medication.MedicationScheduleTime;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.MedicationStatus;
import com.emiraslan.memento.repository.medication.MedicationLogRepository;
import com.emiraslan.memento.repository.medication.MedicationScheduleTimeRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationLogService {

    private final MedicationLogRepository logRepository;
    private final MedicationScheduleTimeRepository timeRepository;

    // timespan for "TAKEN" status
    private static final int ON_TIME_TOLERANCE_MINUTES = 30;

    public List<MedicationLogResponseDto> getLogsBySchedule(Integer scheduleId, Integer daysBack) {

        // calculate dates from Today - daysBack
        LocalDate today = LocalDate.now();
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);
        LocalDateTime startDateTime = today.minusDays(daysBack).atStartOfDay();

        List<MedicationLog> logs = logRepository.findLogsByScheduleIdAndDateRange(scheduleId, startDateTime, endDateTime);

        return logs.stream()
                .map(MapperUtil::toMedicationLogResponseDto)
                .toList();
    }

    // creates a new log
    @Transactional
    public MedicationLogResponseDto logMedicationTaken(User patient, Integer scheduleTimeId) {

        LocalDateTime now = LocalDateTime.now();
        MedicationScheduleTime scheduleTime = timeRepository.findById(scheduleTimeId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_TIME_NOT_FOUND"));

        boolean isPrnSchedule = scheduleTime.getScheduledTime() == null; // only PRN schedules have null times

        if (!isPrnSchedule){
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX);

            boolean alreadyLoggedToday = logRepository.existsByScheduleTime_TimeIdAndTakenAtBetween(
                    scheduleTimeId, startOfDay, endOfDay
            );
            if (alreadyLoggedToday) {
                throw new IllegalStateException("MEDICATION_ALREADY_LOGGED_TODAY");
            }
        }

        // get the status
        MedicationStatus status = determineStatus(scheduleTime.getScheduledTime(), now);

        // build and save the log
        MedicationLog log = MedicationLog.builder()
                .scheduleTime(scheduleTime)
                .patient(patient)
                .takenAt(now)
                .status(status)
                .build();

        return MapperUtil.toMedicationLogResponseDto(logRepository.save(log));
    }

    // helper method to calculate what the status of a log should be
    private MedicationStatus determineStatus(LocalTime scheduledTime, LocalDateTime takenDateTime) {

        // PRN medication will not have a time, and so there will be no delay status
        if (scheduledTime == null) {
            return MedicationStatus.TAKEN;
        }

        LocalTime takenTime = takenDateTime.toLocalTime();

        // take abs value of the time difference between the schedule and when the patient actually took the medicine
        long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(scheduledTime, takenTime));
        // if the difference is +- 30 minutes, the medicine is considered taken on time
        if (diffMinutes <= ON_TIME_TOLERANCE_MINUTES) {
            return MedicationStatus.TAKEN;
        } else { // it will count as a late dose if consumed within 30-120 minute range
            return MedicationStatus.LATE_DOSE;
        }
    }

    // cron job works every hour to check schedule times and automatically log old ones as SKIPPED if 2 hours past
    @Transactional
    public int markMissedMedicationsAsSkipped() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalTime thresholdTime = now.toLocalTime().minusHours(2); // 2 hours before now()

        // bring all unlogged and 2 hours past medications of today, together with schedules and patient data
        List<MedicationScheduleTime> unloggedOverdueTimes = timeRepository
                .findOverdueTimesWithoutLogsToday(thresholdTime, startOfDay, now);

        if (unloggedOverdueTimes.isEmpty()) {
            return 0;
        }

        // all unlogged medication will be saved as SKIPPED
        List<MedicationLog> logsToSave = new ArrayList<>();

        for (MedicationScheduleTime time : unloggedOverdueTimes) {
            MedicationLog skippedLog = MedicationLog.builder()
                    .scheduleTime(time)
                    .patient(time.getSchedule().getPatient())
                    .takenAt(now)
                    .status(MedicationStatus.SKIPPED)
                    .build();
            logsToSave.add(skippedLog);
        }
        logRepository.saveAll(logsToSave); // batch save the list
        return logsToSave.size();
    }
}