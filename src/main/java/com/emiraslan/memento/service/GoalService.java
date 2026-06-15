package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.GoalRequestDto;
import com.emiraslan.memento.dto.response.GoalResponseDto;
import com.emiraslan.memento.entity.Goal;
import com.emiraslan.memento.entity.GoalLog;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.GoalStatus;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.GoalRepository;
import com.emiraslan.memento.repository.GoalWithTodayLogProjection;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    // Logs aren't included here, they will be included on demand with another endpoint
    public List<GoalResponseDto> getGoalsByActiveStatus(Integer patientId, Boolean isActive) {

        List<Goal> goals = goalRepository.findByPatient_UserIdAndIsActive(patientId, isActive);

        return goals.stream()
                .map(MapperUtil::toGoalResponseDto)
                .toList();
    }

    // Brings active goals with today's logs included
    public List<GoalResponseDto> getActiveGoalsByPatient(Integer patientId) {

        LocalDate today = LocalDate.now();

        // Joining Goal and GoalLog in a single query
        List<GoalWithTodayLogProjection> results = goalRepository.findActiveGoalsWithTodayLog(patientId, today);

        return results.stream().map(result -> {

            Goal goal = result.getGoal();
            GoalLog log = result.getTodayGoalLog();

            GoalResponseDto dto = MapperUtil.toGoalResponseDto(goal);

            // inject the logs into GoalResponseDto
            if (log != null) {
                // if logs exist, give the values from DB
                dto.setTodayGoalLog(new GoalResponseDto.TodayGoalLogDto(
                        log.getGoalLogId(),
                        log.getStatus(),
                        log.getProgressValue() != null ? log.getProgressValue() : 0.0
                ));
            } else {
                // if no logs for today
                dto.setTodayGoalLog(new GoalResponseDto.TodayGoalLogDto(
                        null, // ID null, there isn't a log for that log today
                        GoalStatus.NOT_DONE,
                        0.0
                ));
            }

            return dto;

        }).toList();
    }

    @Transactional
    public GoalResponseDto createGoal(GoalRequestDto dto, User creator) {

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

        Goal goal = MapperUtil.toGoalEntity(dto, patient, creator);

        return MapperUtil.toGoalResponseDto(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponseDto updateGoal(Integer goalId, GoalRequestDto dto) {

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        goal.setGoalType(dto.getGoalType());
        goal.setTitle(dto.getTitle());
        goal.setTargetValue(dto.getTargetValue());
        goal.setUnit(dto.getUnit());

        return MapperUtil.toGoalResponseDto(goalRepository.save(goal));
    }

    @Transactional
    public void deactivateGoal(Integer goalId) {

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        if (!goal.getIsActive()) {
            throw new IllegalStateException("GOAL_ALREADY_DEACTIVATED");
        }

        goal.setIsActive(false);
        goalRepository.save(goal);
    }
}