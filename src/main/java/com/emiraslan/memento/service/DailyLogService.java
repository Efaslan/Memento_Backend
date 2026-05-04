package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.DailyLogRequestDto;
import com.emiraslan.memento.dto.response.DailyLogResponseDto;
import com.emiraslan.memento.entity.DailyLog;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.repository.DailyLogRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
                .toList();
    }

    // adds or update a new log
    @Transactional
    public DailyLogResponseDto upsertTodayLog(DailyLogRequestDto dto, User patient) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        // check if today's log exists
        Optional<DailyLog> existingLogOpt = dailyLogRepository
                .findTopByPatient_UserIdAndCreatedAtBetween(patient.getUserId(), start, end);

        DailyLog log;

        if (existingLogOpt.isPresent()) {
            // if it exists, update it
            log = existingLogOpt.get();

            log.setDescription(dto.getDescription());
            log.setQuantityMl(dto.getQuantityMl());

        } else {
            // create log if not
            log = MapperUtil.toDailyLogEntity(dto, patient);
            dailyLogRepository.save(log);
        }

        return MapperUtil.toDailyLogResponseDto(dailyLogRepository.save(log));
    }

    public void deleteLog(Integer logId) {
        dailyLogRepository.deleteById(logId);
    }
}
