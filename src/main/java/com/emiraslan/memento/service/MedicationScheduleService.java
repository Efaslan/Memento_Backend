package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.MedicationScheduleDto;
import com.emiraslan.memento.entity.MedicationSchedule;
import com.emiraslan.memento.entity.MedicationScheduleTime;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.MedicationLogRepository;
import com.emiraslan.memento.repository.MedicationScheduleRepository;
import com.emiraslan.memento.repository.MedicationScheduleTimeRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleService {

    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationScheduleTimeRepository timeRepository;
    private final UserRepository userRepository;
    private final MedicationLogRepository logRepository;

    // helper method: Entity -> DTO conversion (with times)
    private MedicationScheduleDto convertToDtoWithTimes(MedicationSchedule schedule) {
        List<MedicationScheduleTime> times = timeRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
        return MapperUtil.toMedicationScheduleDto(schedule, times);
    }

    // brings active medication schedules and times
    public List<MedicationScheduleDto> getActiveSchedulesByPatient(Integer patientId) {
        return scheduleRepository.findByPatient_UserIdAndIsActiveTrue(patientId).stream()
                .map(this::convertToDtoWithTimes)
                .collect(Collectors.toList());
    }

    // brings all schedules (including deactivated)
    public List<MedicationScheduleDto> getAllSchedulesByPatient(Integer patientId) {
        return scheduleRepository.findByPatient_UserId(patientId).stream()
                .map(this::convertToDtoWithTimes)
                .collect(Collectors.toList());
    }

    // brings all active PRN schedules
    public List<MedicationScheduleDto> getPrnSchedulesByPatient(Integer patientId) {
        return scheduleRepository.findByPatient_UserIdAndIsActiveTrueAndIsPrnTrue(patientId).stream()
                .map(this::convertToDtoWithTimes)
                .collect(Collectors.toList());
    }

    // brings all schedules prescribed by a specific doctor
    public List<MedicationScheduleDto> getSchedulesByDoctor(Integer doctorId) {
        return scheduleRepository.findByDoctor_UserId(doctorId).stream()
                .map(this::convertToDtoWithTimes)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicationScheduleDto createSchedule(MedicationScheduleDto dto) {
        User patient = userRepository.findById(dto.getPatientUserId())
                .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND: " + dto.getPatientUserId()));

        User doctor = null;
        if (dto.getDoctorUserId() != null) {
            doctor = userRepository.findById(dto.getDoctorUserId())
                    .orElseThrow(() -> new EntityNotFoundException("USER_DOCTOR_NOT_FOUND: " + dto.getDoctorUserId()));
        }

        MedicationSchedule schedule = MapperUtil.toMedicationScheduleEntity(dto, patient, doctor);
        MedicationSchedule savedSchedule = scheduleRepository.save(schedule);

        saveScheduleTimes(savedSchedule, dto);

        List<MedicationScheduleTime> savedTimes = timeRepository.findBySchedule_ScheduleId(savedSchedule.getScheduleId());
        return MapperUtil.toMedicationScheduleDto(savedSchedule, savedTimes);
    }

    // special update method. The doctor cannot edit parts of a schedule if the patient has taken the medicine according to that schedule before.
    @Transactional
    public MedicationScheduleDto updateSchedule(Integer scheduleId, MedicationScheduleDto dto) {
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

                throw new IllegalStateException("Medication's name, dosage, and type cannot be changed in order to protect the patient's medical history. Please deactivate the schedule and create a new one.");
            }

            // times also cannot be changed because logs are directly related to the times
            if (dto.getTimes() != null && !dto.getTimes().isEmpty()) {
                throw new IllegalStateException("Medication times cannot be changed in order to protect the patient's medical history.");
            }

            // update permitted fields, except the start date
            existing.setNotes(dto.getNotes());
            existing.setEndDate(dto.getEndDate());
            existing.setIsActive(dto.getIsActive());

        } else {
            // if there are no medication logs (patient hasn't taken the medicine) doctor can change anything

            existing.setMedicationName(dto.getMedicationName());
            existing.setDosage(dto.getDosage());
            existing.setNotes(dto.getNotes());
            existing.setStartDate(dto.getStartDate());
            existing.setEndDate(dto.getEndDate());
            existing.setIsPrn(dto.getIsPrn());
            existing.setIsActive(dto.getIsActive());

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
        return MapperUtil.toMedicationScheduleDto(updatedSchedule, currentTimes);
    }

    // saving medication times
    private void saveScheduleTimes(MedicationSchedule schedule, MedicationScheduleDto dto) {
        if (Boolean.TRUE.equals(dto.getIsPrn())) {
            // if isPrn = true, time will be null
            MedicationScheduleTime time = MedicationScheduleTime.builder()
                    .schedule(schedule)
                    .scheduledTime(null)
                    .build();
            timeRepository.save(time);
        } else {
            // create an entry for each time sent by the frontend
            if (dto.getTimes() != null && !dto.getTimes().isEmpty()) {
                for (LocalTime timeVal : dto.getTimes()) {
                    MedicationScheduleTime time = MedicationScheduleTime.builder()
                            .schedule(schedule)
                            .scheduledTime(timeVal)
                            .build();
                    timeRepository.save(time);
                }
            }
        }
    }

    // manual deactivation of a schedule, in case the doctor wants to end it earlier than planned
    public void deactivateSchedule(Integer scheduleId) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND: " + scheduleId));

        schedule.setIsActive(false);
        scheduleRepository.save(schedule);
    }

    // automatic deactivation of expired medication schedules
    // operates each day at 00:05
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void autoDeactivateExpiredSchedules() {
        log.info("Scheduled Task started: Checking for expired medication schedules...");

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
}