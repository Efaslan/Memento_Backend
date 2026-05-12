-- ==========================================
-- USERS (IDs: 1=Patient, 2=Doctor, 3=Relative1, 4=Relative2)
-- Password for all: 1234567Ab+
-- ==========================================
INSERT INTO users (email, password_hash, first_name, last_name, phone_number, gender, role, is_email_verified, created_at)
VALUES ('demo.patient@test.com', '$2a$10$y4AUDJTufaJLc.cLE2JzI.jrdCzY62ZY0H.DsAboQFx8kGuJh6vmW', 'Ahmet', 'Yilmaz', '05551112233', 'MALE', 'PATIENT', true, CURRENT_TIMESTAMP);

INSERT INTO users (email, password_hash, first_name, last_name, phone_number, gender, role, is_email_verified, created_at)
VALUES ('demo.doctor@test.com', '$2a$10$y4AUDJTufaJLc.cLE2JzI.jrdCzY62ZY0H.DsAboQFx8kGuJh6vmW', 'Zeynep', 'Kaya', '05554445566', 'FEMALE', 'DOCTOR', true, CURRENT_TIMESTAMP);

INSERT INTO users (email, password_hash, first_name, last_name, phone_number, gender, role, is_email_verified, created_at)
VALUES ('demo.son@test.com', '$2a$10$y4AUDJTufaJLc.cLE2JzI.jrdCzY62ZY0H.DsAboQFx8kGuJh6vmW', 'Mehmet', 'Yilmaz', '05557778899', 'MALE', 'RELATIVE', true, CURRENT_TIMESTAMP);

INSERT INTO users (email, password_hash, first_name, last_name, phone_number, gender, role, is_email_verified, created_at)
VALUES ('demo.daughter@test.com', '$2a$10$y4AUDJTufaJLc.cLE2JzI.jrdCzY62ZY0H.DsAboQFx8kGuJh6vmW', 'Ayse', 'Demir', '05550001122', 'FEMALE', 'RELATIVE', true, CURRENT_TIMESTAMP);

-- ==========================================
-- PROFILES
-- ==========================================
INSERT INTO patient_profiles (patient_user_id, date_of_birth, height_cm, weight_kg, blood_type, emergency_notes)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    '1950-05-20', 175, 80, 'A_POSITIVE', 'Allergic to Penicillin. Hypertension.'
);

INSERT INTO doctor_profiles (doctor_user_id, specialization, hospital_name, title)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'Geriatrics', 'City Research Hospital', 'Prof. Dr.'
);

-- ==========================================
-- RELATIONSHIPS
-- ==========================================
-- Patient <-> Son
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.son@test.com'),
    'SON', true, true
);

-- Patient <-> Daughter
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.daughter@test.com'),
    'DAUGHTER', false, true
);

-- Patient <-> Doctor
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'DOCTOR', false, true
);

-- ==========================================
-- MEDICATIONS & SCHEDULES
-- ==========================================
-- Coraspin
INSERT INTO medication_schedules (patient_user_id, doctor_user_id, medication_name, dosage, notes, start_date, end_date, is_prn, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'Coraspin', '100 mg', 'Take after food.', '2024-01-01', '2025-12-31', false, true
);

-- Lantus Insulin
INSERT INTO medication_schedules (patient_user_id, doctor_user_id, medication_name, dosage, notes, start_date, end_date, is_prn, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'Lantus Insulin', '1 Unit', 'Before meals.', '2024-01-01', '2025-12-31', false, true
);

-- Parol (PRN - As needed)
INSERT INTO medication_schedules (patient_user_id, doctor_user_id, medication_name, dosage, notes, start_date, end_date, is_prn, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'Parol', '500 mg', 'If pain occurs.', '2024-01-01', '2025-12-31', true, true
);

-- SCHEDULE TIMES
INSERT INTO medication_schedule_times (schedule_id, scheduled_time)
VALUES (
    (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Coraspin' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    '09:00:00'
);

INSERT INTO medication_schedule_times (schedule_id, scheduled_time)
VALUES (
    (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Lantus Insulin' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    '08:00:00'
);

INSERT INTO medication_schedule_times (schedule_id, scheduled_time)
VALUES (
    (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Lantus Insulin' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    '20:00:00'
);

INSERT INTO medication_schedule_times (schedule_id, scheduled_time)
VALUES (
    (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Parol' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    NULL
);

-- MEDICATION LOGS
INSERT INTO medication_logs (schedule_time_id, patient_user_id, taken_at, status)
VALUES (
    (SELECT time_id FROM medication_schedule_times
     WHERE scheduled_time = '08:00:00'
     AND schedule_id = (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Lantus Insulin' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'))),
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    CURRENT_TIMESTAMP,
    'TAKEN'
);

-- ==========================================
-- DAILY LOGS & LOCATIONS
-- ==========================================
INSERT INTO daily_logs (patient_user_id, description, quantity_ml, created_at)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'), 'Lentil soup and salad', 200, CURRENT_TIMESTAMP
);

INSERT INTO saved_locations (patient_user_id, location_name, latitude, longitude, address_details)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    'Home', 40.765432, 29.987654, 'Downtown District No:5'
);

INSERT INTO saved_locations (patient_user_id, location_name, latitude, longitude, address_details)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    'Summer House', 41.008237, 28.978358, 'Coastal Road No:3'
);

-- ==========================================
-- GENERAL REMINDERS (PostgreSQL INTERVAL usage)
-- ==========================================
INSERT INTO general_reminders (patient_user_id, creator_user_id, title, reminder_time, is_recurring)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.son@test.com'),
    'Grandson Birthday', CURRENT_TIMESTAMP + INTERVAL '2 days', false
);

INSERT INTO general_reminders (patient_user_id, creator_user_id, title, reminder_time, is_recurring)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'Cardiology Appointment', CURRENT_TIMESTAMP + INTERVAL '7 days', false
);

-- ==========================================
-- NEW TABLES: DEVICES, REFRESH TOKENS, NOTIFICATION TOKENS
-- ==========================================

-- 1. USER DEVICES
INSERT INTO user_devices (user_id, device_model, os_version, public_key, biometric_enabled, last_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    'Samsung Galaxy S23', 'Android 14', 'mock_public_key_patient_123', true, CURRENT_TIMESTAMP
);

INSERT INTO user_devices (user_id, device_model, os_version, public_key, biometric_enabled, last_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'iPhone 15 Pro', 'iOS 17', 'mock_public_key_doctor_456', true, CURRENT_TIMESTAMP
);

-- 2. REFRESH TOKENS
-- Setting expiry date to 30 days in the future using Postgres INTERVAL
INSERT INTO refresh_tokens (device_id, refresh_token, expiry_date)
VALUES (
    (SELECT device_id FROM user_devices WHERE user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    'mock_refresh_token_patient_abc123',
    CURRENT_TIMESTAMP + INTERVAL '14 days'
);

INSERT INTO refresh_tokens (device_id, refresh_token, expiry_date)
VALUES (
    (SELECT device_id FROM user_devices WHERE user_id = (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com')),
    'mock_refresh_token_doctor_def456',
    CURRENT_TIMESTAMP + INTERVAL '14 days'
);

-- 3. NOTIFICATION TOKENS (FCM)
INSERT INTO notification_tokens (device_id, fcm_token, last_updated)
VALUES (
    (SELECT device_id FROM user_devices WHERE user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    'mock_fcm_token_patient_xyz',
    CURRENT_TIMESTAMP
);

INSERT INTO notification_tokens (device_id, fcm_token, last_updated)
VALUES (
    (SELECT device_id FROM user_devices WHERE user_id = (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com')),
    'mock_fcm_token_doctor_qwe',
    CURRENT_TIMESTAMP
);