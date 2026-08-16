package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.GoalLogRequestDto;
import com.emiraslan.memento.dto.response.GoalLogResponseDto;
import com.emiraslan.memento.entity.Goal;
import com.emiraslan.memento.entity.GoalLog;
import com.emiraslan.memento.enums.GoalStatus;
import com.emiraslan.memento.repository.GoalLogRepository;
import com.emiraslan.memento.repository.GoalRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoalLogService {

    private final GoalLogRepository goalLogRepository;
    private final GoalRepository goalRepository;

    @Transactional
    public GoalLogResponseDto upsertTodayGoalLog(Integer goalId, GoalLogRequestDto dto) {

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        LocalDate today = LocalDate.now();

        // checking if there's a log for today
        Optional<GoalLog> existingLogOpt = goalLogRepository.findByGoal_GoalIdAndCreatedAt(goalId, today);

        GoalLog log;

        if (existingLogOpt.isPresent()) {
            // Update if log exists
            log = existingLogOpt.get();
            log.setProgressValue(dto.getProgressValue());
        } else {
            // Insert if it doesn't
            log = MapperUtil.toGoalLogEntity(dto, goal);
        }

        if(log.getProgressValue() == 0){
            log.setStatus(GoalStatus.NOT_DONE);
        } else if (log.getProgressValue() < goal.getCurrentTargetValue()) {
            log.setStatus(GoalStatus.PARTIAL);
        } else {
            log.setStatus(GoalStatus.COMPLETED);
        }

        return MapperUtil.toGoalLogResponseDto(goalLogRepository.save(log));
    }

    // Brings a specific goal's logs
    public List<GoalLogResponseDto> getLogsByGoal(Integer goalId, Integer daysBack) {

        // Same process as the medication logs
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(daysBack);

        List<GoalLog> logs = goalLogRepository.findLogsByGoalIdAndDateRange(goalId, start, end);

        return logs.stream()
                .map(MapperUtil::toGoalLogResponseDto)
                .toList();
    }
}
