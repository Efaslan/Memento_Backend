package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.DailyLogRequestDto;
import com.emiraslan.memento.dto.response.DailyLogResponseDto;
import com.emiraslan.memento.dto.response.DailyLogWithWaterResponseDto;
import com.emiraslan.memento.entity.DailyLog;
import com.emiraslan.memento.entity.GoalLog;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.repository.DailyLogRepository;
import com.emiraslan.memento.repository.goal.GoalLogRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final AiService aiService;
    private final GoalLogRepository goalLogRepository;

    // brings last x days' reports. For example, if given 7, it will return this week's reports. This method will also return WATER consumption data if the user had a WATER GoalType in those days
    public List<DailyLogWithWaterResponseDto> getRecentLogs(Integer patientId, Integer daysBack) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(daysBack);

        // Pull the DailyLogs between the dates
        List<DailyLog> dailyLogs = dailyLogRepository.findByPatient_UserIdAndCreatedAtBetween(
                patientId, startDate, endDate);

        if (dailyLogs.isEmpty()) {
            return Collections.emptyList();
        }

        // Pull the WATER GoalTypes between the dates
        List<GoalLog> waterLogs = goalLogRepository.findWaterLogsByDateRange(
                patientId, startDate, endDate);

        // Map the WATER GoalLogs with their dates
        Map<LocalDate, GoalLog> waterLogsMap = waterLogs.stream()
                .collect(Collectors.toMap(GoalLog::getCreatedAt, log -> log));

        // Combine both DailyLogs and Waters
        return dailyLogs.stream().map(dailyLog -> {

            DailyLogResponseDto dailyLogDto = MapperUtil.toDailyLogResponseDto(dailyLog);

            GoalLog waterLog = waterLogsMap.get(dailyLog.getCreatedAt());

            DailyLogWithWaterResponseDto.WaterSummaryDto waterDto = null;
            if (waterLog != null) {
                waterDto = DailyLogWithWaterResponseDto.WaterSummaryDto.builder()
                        .progressValue(waterLog.getProgressValue())
                        .logTargetValue(waterLog.getLogTargetValue())
                        .logUnit(waterLog.getLogUnit())
                        .build();
            }

            return new DailyLogWithWaterResponseDto(dailyLogDto, waterDto);

        }).toList();
    }

    // adds or update a new log
    @Transactional
    public DailyLogResponseDto upsertTodayLog(DailyLogRequestDto dto, User patient) {

        LocalDate today = LocalDate.now();

        // check if today's log exists
        Optional<DailyLog> existingLogOpt = dailyLogRepository
                .findFirstByPatient_UserIdAndCreatedAt(patient.getUserId(), today);


        String incomingDescription = dto.getDescription();
        String finalDescription = incomingDescription; // assigning the incoming as final at first, we will check if the DB already has the same description to not waste AI credits
        String warningMessage = null;
        boolean shouldCallAi = false;

        // the description can't be null, but it can be empty string so we're checking with trim
        if (!dto.getIsManualEditing() && !incomingDescription.trim().isEmpty()) {
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
