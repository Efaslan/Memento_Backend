package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.DailyLogRequestDto;
import com.emiraslan.memento.dto.response.DailyLogResponseDto;
import com.emiraslan.memento.entity.DailyLog;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.DailyLogType;
import com.emiraslan.memento.repository.DailyLogRepository;
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

    private final DailyLogRepository dailyLogRepository;

    // brings last x days' reports. For example, if given 7, it will return this week's reports. 0 returns today
    public List<DailyLogResponseDto> getRecentLogs(Integer patientId, Integer daysBack) {
        LocalDate today = LocalDate.now();

        // end time is today 23:59
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);

        // start time is (today - daysBack)'s 00:00
        LocalDateTime startDateTime = today.minusDays(daysBack).atStartOfDay();

        return dailyLogRepository.findByPatient_UserIdAndCreatedAtBetween(patientId, startDateTime, endDateTime)
                .stream()
                .map(MapperUtil::toDailyLogResponseDto)
                .collect(Collectors.toList());
    }

    // adds a new log (water or food)
    @Transactional
    public DailyLogResponseDto createLog(DailyLogRequestDto dto, User patient){

        DailyLog log = MapperUtil.toDailyLogEntity(dto, patient);
        return MapperUtil.toDailyLogResponseDto(dailyLogRepository.save(log));
    }

    @Transactional
    public DailyLogResponseDto updateLog(Integer logId, DailyLogRequestDto dto) {
        DailyLog existingLog = dailyLogRepository.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("DAILY_LOG_NOT_FOUND: " + logId));

        existingLog.setDescription(dto.getDescription());
        existingLog.setQuantityMl(dto.getQuantityMl());
        existingLog.setDailyLogType(dto.getDailyLogType());

        return MapperUtil.toDailyLogResponseDto(dailyLogRepository.save(existingLog));
    }

    public void deleteLog(Integer logId) {
        dailyLogRepository.deleteById(logId);
    }

    // calculates how much water the patient drank today todo, suya yemege ayri ayri loglar var gibi backend'de, 1 gun icin su ve yemege 1 log olsun. LOG_TYPE direkt gitsin
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
