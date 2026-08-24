package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.GoalRequestDto;
import com.emiraslan.memento.dto.response.ActiveGoalsResponseDto;
import com.emiraslan.memento.dto.response.GoalResponseDto;
import com.emiraslan.memento.entity.Goal;
import com.emiraslan.memento.entity.GoalLog;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.GoalType;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.goal.GoalRepository;
import com.emiraslan.memento.repository.goal.GoalStreakProjection;
import com.emiraslan.memento.repository.goal.GoalWithTodayLogProjection;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    // Logs aren't included here, they will be included on demand with another endpoint
    public List<GoalResponseDto> getDeactivatedGoals(Integer patientId) {

        List<Goal> goals = goalRepository.findByPatient_UserIdAndIsActiveFalse(patientId);

        return goals.stream()
                .map(MapperUtil::toGoalResponseDto)
                .toList();
    }

    // Brings active goals with today's logs included
    public List<ActiveGoalsResponseDto> getActiveGoalsByPatient(Integer patientId) {

        LocalDate today = LocalDate.now();

        // Joining Goal and GoalLog in a single query
        List<GoalWithTodayLogProjection> results = goalRepository.findActiveGoalsWithTodayLog(patientId, today);

        List<GoalStreakProjection> streaks = goalRepository.findActiveGoalStreaks(patientId);
        Map<Integer, Integer> streakMap = streaks.stream()
                .collect(Collectors.toMap(GoalStreakProjection::getGoalId, GoalStreakProjection::getStreak));

        return results.stream().map(result -> {

            Goal goal = result.getGoal();
            GoalLog log = result.getTodayGoalLog();

            GoalResponseDto goalDto = MapperUtil.toGoalResponseDto(goal);
            ActiveGoalsResponseDto.TodayGoalLogDto todayLogDto;

            // inject the logs into GoalResponseDto
            if (log != null) {
                // if logs exist, give the values from DB
                todayLogDto = new ActiveGoalsResponseDto.TodayGoalLogDto(
                        log.getGoalLogId(),
                        log.getStatus(),
                        log.getProgressValue() != null ? log.getProgressValue() : 0.0,
                        log.getLogTargetValue(),
                        log.getLogUnit()
                );
            } else {
                // if no logs for today
                todayLogDto = null;
            }

            // 0 streak in case there are no logs
            Integer currentStreak = streakMap.getOrDefault(result.getGoal().getGoalId(), 0);

            return new ActiveGoalsResponseDto(goalDto, todayLogDto, currentStreak);

        }).toList();
    }

    @Transactional
    public GoalResponseDto createGoal(GoalRequestDto dto, User creator) {

        User patient;

        // if the creator is patient, patientId in dto is set automatically
        if (creator.getRole() == UserRole.PATIENT) {
            patient = creator;
            dto.setPatientUserId(creator.getUserId());
        }
        // if the creator is a relative
        else {
            patient = userRepository.findById(dto.getPatientUserId())
                    .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND: " + dto.getPatientUserId()));
        }

        // if the GoalType is a unique one (not OTHER)
        if (dto.getGoalType() != GoalType.OTHER) {
            // check if the DB already has a goal of this type
            Optional<Goal> existingGoalOpt = goalRepository.findByPatient_UserIdAndGoalType(
                    patient.getUserId(), dto.getGoalType());

            if (existingGoalOpt.isPresent()) {
                Goal existingGoal = existingGoalOpt.get();

                if (Boolean.TRUE.equals(existingGoal.getIsActive())) {
                    throw new IllegalStateException("UNIQUE_GOAL_TYPE_ALREADY_ACTIVE");
                }
                // update its values and reactivate the goal if it was deactivated
                else {
                    existingGoal.setIsActive(true);
                    existingGoal.setTitle(dto.getTitle());
                    existingGoal.setCurrentTargetValue(dto.getCurrentTargetValue());
                    existingGoal.setUnit(dto.getUnit());

                    return MapperUtil.toGoalResponseDto(goalRepository.save(existingGoal));
                }
            }
        }

        // If the GoalType is OTHER, or a record doesn't exist, create the goal
        Goal goal = MapperUtil.toGoalEntity(dto, patient, creator);

        return MapperUtil.toGoalResponseDto(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponseDto updateGoal(Integer goalId, GoalRequestDto dto) {

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("GOAL_NOT_FOUND"));

        if (dto.getGoalType() != goal.getGoalType()) {
            throw new IllegalStateException("GOAL_TYPE_CANNOT_BE_CHANGED");
        }

        goal.setTitle(dto.getTitle());
        goal.setCurrentTargetValue(dto.getCurrentTargetValue());
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