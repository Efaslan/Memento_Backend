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

        log.info("CRON [Notifications] began: Sending notifications for General Reminders and Medications.");

        int generalNotifCount = reminderService.processGeneralReminders(currentDateTime);
        int medicationNotifCount = medicationScheduleService.processMedications(currentTime);

        log.info("CRON [Notifications] ended: GeneralReminders: {}, Medications: {} notifications sent.", generalNotifCount, medicationNotifCount);
    }

    // checking for missed medications every hour and logs them as skipped if not taken within 2 hours
    @Scheduled(cron = "0 0 * * * *")
    public void masterSkippedMedicationCron() {
        log.info("CRON [Skipped Medications] began: Checking for medications missed by >2 hours.");

        int missedMedicationCounter = medicationLogService.markMissedMedicationsAsSkipped();

        log.info("CRON [Skipped Medications] ended: {} medications were logged as SKIPPED.", missedMedicationCounter);
    }

    // works at 00:05 each night
    @Scheduled(cron = "0 5 0 * * *")
    public void endOfDayCron() {
        log.info("CRON [End of day] began:");
        // Finds expired medication schedules and deactivates them
        int deactivatedSchedules = medicationScheduleService.autoDeactivateExpiredSchedules();
        // Deletes expired refresh tokens
        int expiredRefreshTokens = userDeviceService.deleteExpiredRefreshTokens();

        log.info("CRON [End of day] ended: Deactivated {} schedules, and deleted {} expired Refresh Tokens.", deactivatedSchedules, expiredRefreshTokens);
    }
}
