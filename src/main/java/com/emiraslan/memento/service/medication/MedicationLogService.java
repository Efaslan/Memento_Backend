package com.emiraslan.memento.service.medication;

import com.emiraslan.memento.dto.response.MedicationLogResponseDto;
import com.emiraslan.memento.dto.response.MedicationLogSummaryResponseDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public MedicationLogSummaryResponseDto getRecentLogsSummary(Integer patientId, Integer daysBack, Integer page, Integer size) {

        LocalDate today = LocalDate.now();
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);
        LocalDateTime startDateTime = today.minusDays(daysBack).atStartOfDay();

        // latest logs are on top
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "takenAt"));

        // pull the log statistics of all logs between the given dates
        MedicationLogSummaryResponseDto.StatsProjection stats = logRepository.getStatistics(patientId, startDateTime, endDateTime);

        // pull the logs with pagination
        Page<MedicationLog> logPage = logRepository.findByPatient_UserIdAndTakenAtBetween(
                patientId, startDateTime, endDateTime, pageable
        );

        // maps the logs into dto pages
        Page<MedicationLogResponseDto> dtoPage = logPage.map(MapperUtil::toMedicationLogResponseDto);

        return MedicationLogSummaryResponseDto.builder()
                .takenCount(stats.getTakenCount())
                .delayedCount(stats.getDelayedCount())
                .skippedCount(stats.getSkippedCount())
                .logs(dtoPage)
                .build();
    }

    // creates a new log
    @Transactional
    public MedicationLogResponseDto logMedicationTaken(User patient, Integer scheduleTimeId) {
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
    public void markMissedMedicationsAsSkipped() {
        log.info("Scheduled Task started: Checking for skipped medication...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalTime thresholdTime = now.toLocalTime().minusHours(2); // 2 hours before now()

        // bring all unlogged and 2 hours past medications of today, together with schedules and patient data
        List<MedicationScheduleTime> unloggedOverdueTimes = timeRepository
                .findOverdueTimesWithoutLogsToday(thresholdTime, startOfDay, now);

        if (unloggedOverdueTimes.isEmpty()) {
            log.info("No unlogged overdue medications found.");
            return;
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
        log.info("Automatically Skipped {} medications.", logsToSave.size());
    }
}