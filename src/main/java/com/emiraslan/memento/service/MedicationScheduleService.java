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
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicationScheduleService {

    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationScheduleTimeRepository timeRepository;
    private final UserRepository userRepository;
    private final MedicationLogRepository logRepository;

    // brings active medications and times
    public List<MedicationScheduleDto> getActiveSchedulesByPatient(Integer patientId) {
        return scheduleRepository.findByPatient_UserIdAndIsActiveTrue(patientId).stream()
                .map(schedule -> {
                    List<MedicationScheduleTime> times = timeRepository.findBySchedule_ScheduleId(schedule.getScheduleId());
                    return MapperUtil.toMedicationScheduleDto(schedule, times);
                })
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

    public void deactivateSchedule(Integer scheduleId) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("SCHEDULE_NOT_FOUND: " + scheduleId));

        schedule.setIsActive(false);
        scheduleRepository.save(schedule);
    }
}