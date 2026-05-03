package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.MedicationScheduleRequestDto;
import com.emiraslan.memento.dto.response.MedicationScheduleResponseDto;
import com.emiraslan.memento.entity.MedicationSchedule;
import com.emiraslan.memento.entity.MedicationScheduleTime;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.*;
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
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    private Function<MedicationSchedule, MedicationScheduleResponseDto> buildScheduleMapper(List<MedicationSchedule> schedules){

        // pile the schedule ids into a list
        List<Integer> scheduleIds = schedules.stream()
                .map(MedicationSchedule::getScheduleId)
                .toList();
        // pull all times from the schedule id list
        List<MedicationScheduleTime> scheduleTimes = timeRepository.findBySchedule_ScheduleIdIn(scheduleIds);

        // group the times by their schedule ids
        Map<Integer, List<MedicationScheduleTime>> scheduleIdsWithTimes = scheduleTimes.stream()
                .collect(Collectors.groupingBy(time -> time.getSchedule().getScheduleId()));

        // create the DTOs with schedules and their times
        return schedule -> {
            List<MedicationScheduleTime> timesForThisSchedule = scheduleIdsWithTimes.getOrDefault(schedule.getScheduleId(), Collections.emptyList());
            return MapperUtil.toMedicationScheduleResponseDto(schedule, timesForThisSchedule);
        };
    }

    // brings active medication schedules and times
    public List<MedicationScheduleResponseDto> getActiveSchedulesByPatient(Integer patientId) {
        List<MedicationSchedule> activeSchedules = scheduleRepository.findByPatient_UserIdAndIsActiveTrue(patientId);

        if (activeSchedules.isEmpty()){
            return Collections.emptyList();
        }

        return activeSchedules.stream()
                .map(buildScheduleMapper(activeSchedules))
                .toList();
    }

    // brings all past schedules of a patient
    public Page<MedicationScheduleResponseDto> getAllPastSchedulesByPatient(Integer patientId, int page, int size) {

        // sorting to show the latest expired schedules on top
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "endDate"));

        Page<MedicationSchedule> schedulePage = scheduleRepository.findByPatient_UserIdAndIsActiveFalse(patientId, pageable);

        if (schedulePage.isEmpty()) {
            return schedulePage.map(schedule -> MapperUtil.toMedicationScheduleResponseDto(schedule, Collections.emptyList()));
        }

        return schedulePage.map(buildScheduleMapper(schedulePage.getContent()));
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
    public void autoDeactivateExpiredSchedules() {
        LocalDate today = LocalDate.now();

        // find all schedules with an endDate before today
        List<MedicationSchedule> expiredSchedules = scheduleRepository.findByIsActiveTrueAndEndDateBefore(today);

        if (!expiredSchedules.isEmpty()) {
            for (MedicationSchedule schedule : expiredSchedules) {
                schedule.setIsActive(false); // deactivate all found expired schedules
            }
            scheduleRepository.saveAll(expiredSchedules);

            log.info("Deactivated {} expired medication schedules.", expiredSchedules.size());
        } else {
            log.info("No expired medication schedule was found.");
        }
    }

    // we can't use <= time for medications because time only holds LocalTime and =<
    // would send notifications for past medications as well
    @Transactional
    public void processMedications(LocalTime now) {
        List<MedicationScheduleTime> currentTimes = timeRepository.findBySchedule_IsActiveTrueAndScheduledTime(now);

        for (MedicationScheduleTime time : currentTimes) {
            String title = "İlaç Vakti!";
            String body = time.getSchedule().getMedicationName() + " ilacından " + time.getSchedule().getDosage() + " alınız.";

            notificationService.sendNotificationToUser(time.getSchedule().getPatient().getUserId(), title, body);
        }
    }
}