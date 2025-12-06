package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.DailyLogDto;
import com.emiraslan.memento.entity.DailyLog;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.DailyLogType;
import com.emiraslan.memento.repository.DailyLogRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private DailyLogRepository dailyLogRepository;
    private UserRepository userRepository;

    // gets a specific day's daily logs, such as today
    public List<DailyLogDto> getLogsByDate(Integer patientId, LocalDate date){
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return dailyLogRepository.findByPatient_UserIdAndCreatedAtBetween(patientId, startOfDay, endOfDay)
                .stream()
                .map(MapperUtil::toDailyLogDto)
                .collect(Collectors.toList());
    }

    // brings last x days' reports. For example, if given 7, it will return this week's reports
    public List<DailyLogDto> getRecentLogs(Integer patientId, Integer daysBack) {
        LocalDate today = LocalDate.now();

        // end time is today 23:59
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);

        // start time is (today - daysBack)'s 00:00
        LocalDateTime startDateTime = today.minusDays(daysBack).atStartOfDay();

        return dailyLogRepository.findByPatient_UserIdAndCreatedAtBetween(patientId, startDateTime, endDateTime)
                .stream()
                .map(MapperUtil::toDailyLogDto)
                .collect(Collectors.toList());
    }

    // adds a new log (water or food)
    @Transactional
    public DailyLogDto createLog(DailyLogDto dto){
        User patient = userRepository.findById(dto.getPatientUserId())
                .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND: " + dto.getPatientUserId()));

        DailyLog log = MapperUtil.toDailyLogEntity(dto, patient);
        return MapperUtil.toDailyLogDto(dailyLogRepository.save(log));
    }

    public void deleteLog(Integer logId) {
        if (!dailyLogRepository.existsById(logId)) {
            throw new EntityNotFoundException("DAILY_LOG_NOT_FOUND: " + logId);
        }
        dailyLogRepository.deleteById(logId);
    }

    // calculates how much water the patient drank today
    public Integer getTodayTotalWaterIntake(Integer patientId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        List<DailyLog> waterLogs = dailyLogRepository.findByPatient_UserIdAndDailyLogTypeAndCreatedAtBetween(
                patientId, DailyLogType.WATER, start, end
        );

        return waterLogs.stream()
                .mapToInt(log -> log.getQuantityMl() != null ? log.getQuantityMl() : 0)
                .sum();
    }
}
