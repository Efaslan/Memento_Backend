package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.MedicationLogDto;
import com.emiraslan.memento.entity.MedicationLog;
import com.emiraslan.memento.entity.MedicationScheduleTime;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.MedicationStatus;
import com.emiraslan.memento.repository.MedicationLogRepository;
import com.emiraslan.memento.repository.MedicationScheduleRepository;
import com.emiraslan.memento.repository.MedicationScheduleTimeRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationLogService {

    private final MedicationLogRepository logRepository;
    private final MedicationScheduleTimeRepository timeRepository;
    private final MedicationScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    // timespan for "TAKEN" status
    private static final int ON_TIME_TOLERANCE_MINUTES = 30;

    // brings patient logs of a specific date
    public List<MedicationLogDto> getLogsByDate(Integer patientId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return logRepository.findByPatient_UserIdAndTakenAtBetween(patientId, startOfDay, endOfDay)
                .stream()
                .map(MapperUtil::toMedicationLogDto)
                .collect(Collectors.toList());
    }

    // creates a new log
    @Transactional
    public MedicationLogDto logMedicationTaken(Integer patientId, Integer scheduleTimeId) {
        // find user and the time
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND"));

        MedicationScheduleTime scheduleTime = timeRepository.findById(scheduleTimeId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_TIME_NOT_FOUND"));

        // get the status
        LocalDateTime now = LocalDateTime.now();
        MedicationStatus status = determineStatus(scheduleTime.getScheduledTime(), now);

        // build and save the log
        MedicationLog log = MedicationLog.builder()
                .scheduleTime(scheduleTime)
                .patient(patient)
                .takenAt(now)
                .status(status)
                .build();

        return MapperUtil.toMedicationLogDto(logRepository.save(log));
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

    // Scheduled Task operating to check schedule times and automatically log old ones as SKIPPED if the next medication time of the schedule has come
    @Scheduled(cron = "0 0 * * * *") // each hour at any date
    @Transactional
    public void markMissedMedicationsAsSkipped() {
        log.info("Scheduled Task started: Checking for skipped medication...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        // Get all times and their ids
        List<MedicationScheduleTime> allTimes = timeRepository.findAll();

        for (MedicationScheduleTime time : allTimes) {
            // skipping PRN medication, they don't have times
            if (time.getScheduledTime() == null) continue;

            // getting the exact timestamp of when the medication should be taken (today + time)
            LocalDateTime scheduledDateTime = LocalDateTime.of(LocalDate.now(), time.getScheduledTime());

            // if it has been more than 2 hours since the timestamp
            if (scheduledDateTime.isBefore(now.minusHours(2))) { // note: we do not need schedule data here because we are simply doing a 2-hour time check

                // check if there is a log of that specific timeId in log repository
                boolean exists = logRepository.existsByScheduleTime_TimeIdAndTakenAtBetween(
                        time.getTimeId(), startOfDay, now
                );
                // if there isn't a log, automatically create one and set its status to SKIPPED
                if (!exists) {
                    MedicationLog skippedLog = MedicationLog.builder()
                            .scheduleTime(time)
                            .patient(time.getSchedule().getPatient()) // finding user through time -> schedule -> patient relation
                            .takenAt(now)
                            .status(MedicationStatus.SKIPPED)
                            .build();

                    // save the new medicationLog and print it to logger
                    logRepository.save(skippedLog);
                    log.info("Medication Automatically Skipped: UserID={}, Medication={}",
                            skippedLog.getPatient().getUserId(), time.getSchedule().getMedicationName());
                }
            }
        }
    }
}