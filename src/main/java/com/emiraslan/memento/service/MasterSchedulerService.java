package com.emiraslan.memento.service;

import com.emiraslan.memento.service.medication.MedicationLogService;
import com.emiraslan.memento.service.medication.MedicationScheduleService;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterSchedulerService {

    private final GeneralReminderService reminderService;
    private final MedicationScheduleService medicationScheduleService;
    private final MedicationLogService medicationLogService;
    private final UserDeviceService userDeviceService;

    // cron that works each minute to find due general reminders and medications and sends out notifications
    @Scheduled(cron = "0 * * * * *")
    public void masterNotificationCron() {
        LocalDateTime currentDateTime = LocalDateTime.now(); // for general reminders
        LocalTime currentTime = currentDateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES); // for medications

        log.info("CRON [Notifications] began: ");

        reminderService.processGeneralReminders(currentDateTime);
        medicationScheduleService.processMedications(currentTime);
    }

    // checking for missed medications every hour and logs them as skipped if not taken within 2 hours
    @Scheduled(cron = "0 0 * * * *")
    public void masterSkippedMedicationCron() {
        log.info("CRON [Skipped Medications]: Checking for medications missed by >2 hours");
        medicationLogService.markMissedMedicationsAsSkipped();
    }

    // works at 00:05 each night
    @Scheduled(cron = "0 5 0 * * *")
    public void endOfDayCron() {
        log.info("CRON [End of day] began:");
        // Finds expired medication schedules and deactivates them
        medicationScheduleService.autoDeactivateExpiredSchedules();
        // Deletes expired refresh tokens
        userDeviceService.deleteExpiredRefreshTokens();
    }
}
