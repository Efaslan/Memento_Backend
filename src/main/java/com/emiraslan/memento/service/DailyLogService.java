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
    private final AiService aiService;

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


        String incomingDescription = dto.getDescription();
        String finalDescription = incomingDescription; // assigning the incoming as final at first, we will check if the DB already has the same description to not waste AI credits
        String warningMessage = null;
        boolean shouldCallAi = false;

        if (incomingDescription != null && !incomingDescription.trim().isEmpty()) {
            if (existingLogOpt.isEmpty()) {
                // If the DB's description is empty, we'll do AI formatting for sure
                shouldCallAi = true;
            } else {
                // If the DB has a description, check if it's the same with the incoming one
                String dbDescription = existingLogOpt.get().getDescription();
                if (!incomingDescription.equals(dbDescription)) {
                    // If different, we'll format it again
                    shouldCallAi = true;
                }
            }
        }

        if (shouldCallAi) {
            try {
                finalDescription = aiService.formatDailyLog(incomingDescription, patient);
            } catch (Exception e) {
                warningMessage = e.getMessage();
                // If there's an error, the finalDescription will be equal to the incomingDescription, thanks to the first assignment
            }
        }

        DailyLog logEntity;

        if (existingLogOpt.isPresent()) {
            // if it exists, update it
            logEntity = existingLogOpt.get();

            logEntity.setDescription(finalDescription);
            logEntity.setQuantityMl(dto.getQuantityMl());

        } else {
            // create log if not
            dto.setDescription(finalDescription);
            logEntity = MapperUtil.toDailyLogEntity(dto, patient);
        }

        DailyLogResponseDto responseDto = MapperUtil.toDailyLogResponseDto(dailyLogRepository.save(logEntity));

        if (warningMessage != null) {
            responseDto.setWarningMessage(warningMessage);
        }

        return responseDto;
    }

    public void deleteLog(Integer logId) {
        dailyLogRepository.deleteById(logId);
    }
}
