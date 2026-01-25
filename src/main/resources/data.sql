-- USERS (IDs: 1=Patient, 2=Doctor, 3=Relative1, 4=Relative2)
-- Password for all: 123456
INSERT INTO users (email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES ('demo.patient@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Ahmet', 'Yilmaz', '05551112233', 'PATIENT', CURRENT_TIMESTAMP);

INSERT INTO users (email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES ('demo.doctor@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Zeynep', 'Kaya', '05554445566', 'DOCTOR', CURRENT_TIMESTAMP);

INSERT INTO users (email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES ('demo.son@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Mehmet', 'Yilmaz', '05557778899', 'RELATIVE', CURRENT_TIMESTAMP);

INSERT INTO users (email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES ('demo.daughter@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Ayse', 'Demir', '05550001122', 'RELATIVE', CURRENT_TIMESTAMP);

-- PROFILES, getting user id's from emails
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

-- RELATIONSHIPS
-- Patient (1) <-> Son (3)
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.son@test.com'),
    'SON', true, true
);

-- Patient (1) <-> Daughter (4)
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.daughter@test.com'),
    'DAUGHTER', false, true
);

-- Patient (1) <-> Doctor (2)
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'DOCTOR', false, true
);

-- MEDICATIONS
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

-- Parol
INSERT INTO medication_schedules (patient_user_id, doctor_user_id, medication_name, dosage, notes, start_date, end_date, is_prn, is_active)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'Parol', '500 mg', 'If pain occurs.', '2024-01-01', '2025-12-31', true, true
);

-- SCHEDULE TIMES
-- Coraspin (09:00)
INSERT INTO medication_schedule_times (schedule_id, scheduled_time)
VALUES (
    (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Coraspin' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    '09:00:00'
);

-- Insulin (08:00)
INSERT INTO medication_schedule_times (schedule_id, scheduled_time)
VALUES (
    (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Lantus Insulin' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    '08:00:00'
);

-- Insulin (20:00)
INSERT INTO medication_schedule_times (schedule_id, scheduled_time)
VALUES (
    (SELECT schedule_id FROM medication_schedules WHERE medication_name = 'Lantus Insulin' AND patient_user_id = (SELECT user_id FROM users WHERE email = 'demo.patient@test.com')),
    '20:00:00'
);

-- Parol (NULL - PRN ilaç olduğu için)
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

-- DAILY LOGS
INSERT INTO daily_logs (patient_user_id, log_type, quantity_ml, created_at)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    'WATER', 200, CURRENT_TIMESTAMP
);

INSERT INTO daily_logs (patient_user_id, log_type, description, created_at)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    'FOOD', 'Lentil soup and salad', CURRENT_TIMESTAMP
);

-- SAVED LOCATIONS
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

-- REMINDERS (Using H2 date functions)
INSERT INTO general_reminders (patient_user_id, creator_user_id, title, reminder_time, is_recurring, is_completed)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.son@test.com'),
    'Grandson Birthday', DATEADD('DAY', 2, CURRENT_DATE), false, false
);

-- Cardiology Appointment (Created by Doctor)
INSERT INTO general_reminders (patient_user_id, creator_user_id, title, reminder_time, is_recurring, is_completed)
VALUES (
    (SELECT user_id FROM users WHERE email = 'demo.patient@test.com'),
    (SELECT user_id FROM users WHERE email = 'demo.doctor@test.com'),
    'Cardiology Appointment', DATEADD('DAY', 7, CURRENT_DATE), false, false
);