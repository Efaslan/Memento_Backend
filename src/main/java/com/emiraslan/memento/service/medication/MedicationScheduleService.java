package com.emiraslan.memento.service.medication;

import com.emiraslan.memento.dto.request.MedicationScheduleRequestDto;
import com.emiraslan.memento.dto.response.medication.ActiveSchedulesResponseDto;
import com.emiraslan.memento.dto.response.medication.DeactivatedSchedulesResponseDto;
import com.emiraslan.memento.dto.response.medication.MedicationScheduleResponseDto;
import com.emiraslan.memento.dto.response.medication.MedicationStatsResponseDto;
import com.emiraslan.memento.entity.medication.MedicationLog;
import com.emiraslan.memento.entity.medication.MedicationSchedule;
import com.emiraslan.memento.entity.medication.MedicationScheduleTime;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.repository.medication.MedicationLogRepository;
import com.emiraslan.memento.repository.medication.MedicationScheduleRepository;
import com.emiraslan.memento.repository.medication.MedicationScheduleTimeRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.service.notification.NotificationService;
import com.emiraslan.memento.util.MapperUtil;
import com.emiraslan.memento.repository.medication.ScheduleStatsProjection;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleService {

    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationScheduleTimeRepository timeRepository;
    private final UserRepository userRepository;
    private final MedicationLogRepository logRepository;
    private final NotificationService notificationService;

    public ActiveSchedulesResponseDto getActiveSchedules(Integer patientId, boolean includeStatistics) {

        // 1. Get all active schedules
        List<MedicationSchedule> activeSchedules = scheduleRepository.findByPatient_UserIdAndIsActiveTrue(patientId);

        if (activeSchedules.isEmpty()){
            return new ActiveSchedulesResponseDto(Collections.emptyList(), null);
        }

        List<Integer> scheduleIds = activeSchedules.stream()
                .map(MedicationSchedule::getScheduleId)
                .toList();

        // 2. Add all times to the schedules and map them into DTOs
        List<MedicationScheduleResponseDto> dtos = buildBaseDtos(activeSchedules, scheduleIds);

        // 3. Add today's logs to the schedules
        attachTodayLogs(dtos, scheduleIds);

        // 4. Add medication consumption statistics on demand
        MedicationStatsResponseDto statsDto = null;
        if (includeStatistics) {
            statsDto = buildMedicationStatsDto(patientId, true);
        }

        return new ActiveSchedulesResponseDto(dtos, statsDto);
    }

    private List<MedicationScheduleResponseDto> buildBaseDtos(List<MedicationSchedule> schedules, List<Integer> scheduleIds) {
        List<MedicationScheduleTime> scheduleTimes = timeRepository.findBySchedule_ScheduleIdIn(scheduleIds);

        Map<Integer, List<MedicationScheduleTime>> timesMap = scheduleTimes.stream()
                .collect(Collectors.groupingBy(time -> time.getSchedule().getScheduleId()));

        return schedules.stream().map(schedule -> {
            List<MedicationScheduleTime> times = timesMap.getOrDefault(schedule.getScheduleId(), Collections.emptyList()); // group times and schedules together
            MedicationScheduleResponseDto dto = MapperUtil.toMedicationScheduleResponseDto(schedule, times); // map them into our DTO

            dto.setTodayLogs(Collections.emptyList()); // Empty list for schedules without logs

            return dto;
        }).collect(Collectors.toList()); // Mutable list
    }

    private void attachTodayLogs(List<MedicationScheduleResponseDto> dtos, List<Integer> scheduleIds) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<MedicationLog> logs = logRepository.findTodayLogsByScheduleIds(scheduleIds, startOfDay, endOfDay);

        Map<Integer, List<MedicationLog>> logsMap = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getScheduleTime().getSchedule().getScheduleId()));

        // Match logs with their schedules
        for (MedicationScheduleResponseDto dto : dtos) {
            List<MedicationLog> scheduleLogs = logsMap.getOrDefault(dto.getScheduleId(), Collections.emptyList());

            // Add the logs to MedicationScheduleResponseDto's todayLogs field
            dto.setTodayLogs(scheduleLogs.stream().map(MapperUtil::toMedicationLogResponseDto).toList());
        }
    }

    private MedicationStatsResponseDto buildMedicationStatsDto(Integer patientId, Boolean isActive) {

        ScheduleStatsProjection stats = logRepository.getOverallStatisticsByPatient(patientId, isActive);

        if (stats == null || stats.getTotalLogs() == 0) {
            return null;
        }

        // add percentages to DTO
        return MedicationStatsResponseDto.builder()
                .takenPercentage((int) Math.round((double) stats.getTakenCount() / stats.getTotalLogs() * 100))
                .delayedPercentage((int) Math.round((double) stats.getDelayedCount() / stats.getTotalLogs() * 100))
                .skippedPercentage((int) Math.round((double) stats.getSkippedCount() / stats.getTotalLogs() * 100))
                .totalPastLogs(stats.getTotalLogs())
                .build();
    }

    // brings all past schedules of a patient
    public DeactivatedSchedulesResponseDto getAllPastSchedulesByPatient(Integer patientId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "endDate"));
        Page<MedicationSchedule> schedulePage = scheduleRepository.findByPatient_UserIdAndIsActiveFalse(patientId, pageable);

        if (schedulePage.isEmpty()) {
            return new DeactivatedSchedulesResponseDto(Page.empty(pageable), null);
        }

        List<MedicationSchedule> schedules = schedulePage.getContent();
        List<Integer> scheduleIds = schedules.stream().map(MedicationSchedule::getScheduleId).toList();

        // We don't add today's logs in contrast to active schedules
        List<MedicationScheduleResponseDto> dtos = buildBaseDtos(schedules, scheduleIds);
        Page<MedicationScheduleResponseDto> pageResult = new PageImpl<>(dtos, pageable, schedulePage.getTotalElements());
        MedicationStatsResponseDto statsDto = buildMedicationStatsDto(patientId, false);


        return new DeactivatedSchedulesResponseDto(pageResult, statsDto);
    }

    @Transactional
    public MedicationScheduleResponseDto createSchedule(MedicationScheduleRequestDto dto, User doctor) {
        User patient = userRepository.findById(dto.getPatientUserId())
                .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND: " + dto.getPatientUserId()));

        MedicationSchedule schedule = MapperUtil.toMedicationScheduleEntity(dto, patient, doctor);
        MedicationSchedule savedSchedule = scheduleRepository.save(schedule);

        saveScheduleTimes(savedSchedule, dto);

        List<MedicationScheduleTime> savedTimes = timeRepository.findBySchedule_ScheduleId(savedSchedule.getScheduleId());
        return MapperUtil.toMedicationScheduleResponseDto(savedSchedule, savedTimes);
    }

    // special update method. The doctor cannot edit parts of a schedule if the patient has taken the medicine according to that schedule before.
    @Transactional
    public MedicationScheduleResponseDto updateSchedule(Integer scheduleId, MedicationScheduleRequestDto dto) {
        MedicationSchedule existing = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND: " + scheduleId));

        // checking if the patient has taken the medicine from a specific schedule
        boolean hasLogs = logRepository.existsByScheduleTime_Schedule_ScheduleId(scheduleId);

        if (hasLogs) {
            // if there are logs, only non-critical fields can be updated
            // if the doctor tries to edit a medication's name, dosage, or times, there will be an error
            // (comparing current values to the incoming DTO values)
            if (!existing.getMedicationName().equals(dto.getMedicationName()) ||
                    !existing.getDosage().equals(dto.getDosage()) ||
                    !existing.getIsPrn().equals(dto.getIsPrn())) {

                throw new IllegalStateException("Medication's name, dosage, and type cannot be changed if the patient has already consumed a dose. This is in order to protect the patient's medical history. Please deactivate the schedule and create a new one.");
            }

            // times also cannot be changed because logs are directly related to the times
            if (dto.getTimes() != null && !dto.getTimes().isEmpty()) {
                throw new IllegalStateException("Medication times cannot be changed if the patient has already consumed a dose. This is in order to protect the patient's medical history. Please deactivate the schedule and create a new one.");
            }

            // update permitted fields, except the start date
            existing.setNotes(dto.getNotes());
            existing.setEndDate(dto.getEndDate());

        } else {
            // if there are no medication logs (patient hasn't taken the medicine) doctor can change anything

            existing.setMedicationName(dto.getMedicationName());
            existing.setDosage(dto.getDosage());
            existing.setNotes(dto.getNotes());
            existing.setStartDate(dto.getStartDate());
            existing.setEndDate(dto.getEndDate());
            existing.setIsPrn(dto.getIsPrn());

            // delete and recreate the times
            if (dto.getTimes() != null) {
                List<MedicationScheduleTime> oldTimes = timeRepository.findBySchedule_ScheduleId(scheduleId);
                timeRepository.deleteAll(oldTimes);
                timeRepository.flush();
                saveScheduleTimes(existing, dto);
            }
        }

        MedicationSchedule updatedSchedule = scheduleRepository.save(existing);
        List<MedicationScheduleTime> currentTimes = timeRepository.findBySchedule_ScheduleId(scheduleId);
        return MapperUtil.toMedicationScheduleResponseDto(updatedSchedule, currentTimes);
    }

    // saving medication times
    private void saveScheduleTimes(MedicationSchedule schedule, MedicationScheduleRequestDto dto) {
        if (Boolean.TRUE.equals(dto.getIsPrn())) {
            if (dto.getTimes() != null && !dto.getTimes().isEmpty()) {
                throw new IllegalArgumentException("PRN medications cannot have time information.");
            }

            MedicationScheduleTime time = MedicationScheduleTime.builder()
                    .schedule(schedule)
                    .scheduledTime(null)
                    .build();
            timeRepository.save(time);

        }
        // if not prn medication:
        else {
            // they must have time information
            if (dto.getTimes() == null || dto.getTimes().isEmpty()) {
                throw new IllegalArgumentException("You must enter at least 1 time(HH:mm) for medication schedules.");
            }

            for (LocalTime timeVal : dto.getTimes()) {
                MedicationScheduleTime time = MedicationScheduleTime.builder()
                        .schedule(schedule)
                        .scheduledTime(timeVal)
                        .build();
                timeRepository.save(time);
            }
        }
    }

    // manual deactivation of a schedule, in case the doctor wants to end it earlier than planned
    @Transactional
    public void deactivateSchedule(Integer scheduleId) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND: " + scheduleId));

        schedule.setIsActive(false);
        scheduleRepository.save(schedule);
    }

    // cron job method, each night 00:05
    @Transactional
    public int autoDeactivateExpiredSchedules() {
        return scheduleRepository.deactivateExpiredSchedules(LocalDate.now());
    }

    // we can't use <= time for medications because time only holds LocalTime and =<
    // would send notifications for past medications as well
    @Transactional
    public int processMedications(LocalTime now) {
        List<MedicationScheduleTime> currentTimes = timeRepository.findBySchedule_IsActiveTrueAndScheduledTime(now);

        int notificationCounter = 0;
        for (MedicationScheduleTime time : currentTimes) {
            String title = "İlaç Vakti!";
            String body = time.getSchedule().getMedicationName() + " ilacından " + time.getSchedule().getDosage() + " alınız.";

            notificationService.sendNotificationToUser(time.getSchedule().getPatient().getUserId(), title, body);
            notificationCounter++;
        }
        return notificationCounter;
    }
}