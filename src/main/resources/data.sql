-- USERS (IDs: 1=Patient, 2=Doctor, 3=Relative1, 4=Relative2)
-- Password for all: 123456
INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES (1, 'demo.patient@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Ahmet', 'Yilmaz', '05551112233', 'PATIENT', CURRENT_TIMESTAMP);

INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES (2, 'demo.doctor@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Zeynep', 'Kaya', '05554445566', 'DOCTOR', CURRENT_TIMESTAMP);

INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES (3, 'demo.son@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Mehmet', 'Yilmaz', '05557778899', 'RELATIVE', CURRENT_TIMESTAMP);

INSERT INTO users (user_id, email, password_hash, first_name, last_name, phone_number, role, created_at)
VALUES (4, 'demo.daughter@test.com', '$2a$10$2pa//C6VUsTQo1kd1djt7OEFqg5DZ7eoZvy7qlbStFDeWk823cmL6', 'Ayse', 'Demir', '05550001122', 'RELATIVE', CURRENT_TIMESTAMP);

-- PROFILES
INSERT INTO patient_profiles (patient_user_id, date_of_birth, height_cm, weight_kg, blood_type, emergency_notes)
VALUES (1, '1950-05-20', 175, 80, 'A_POSITIVE', 'Allergic to Penicillin. Hypertension.');

INSERT INTO doctor_profiles (doctor_user_id, specialization, hospital_name, title)
VALUES (2, 'Geriatrics', 'City Research Hospital', 'Prof. Dr.');

-- RELATIONSHIPS
-- Patient (1) <-> Son (3)
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (1, 3, 'SON', true, true);

-- Patient (1) <-> Daughter (4)
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (1, 4, 'DAUGHTER', false, true);

-- Patient (1) <-> Doctor (2)
INSERT INTO patient_relationships (patient_user_id, caregiver_user_id, relationship_type, is_primary_contact, is_active)
VALUES (1, 2, 'DOCTOR', false, true);

-- MEDICATIONS (IDs: 10, 20, 30)
INSERT INTO medication_schedules (schedule_id, patient_user_id, doctor_user_id, medication_name, dosage, notes, start_date, end_date, is_prn, is_active)
VALUES (10, 1, 2, 'Coraspin', '100 mg', 'Take after food.', '2024-01-01', '2025-12-31', false, true);

INSERT INTO medication_schedules (schedule_id, patient_user_id, doctor_user_id, medication_name, dosage, notes, start_date, end_date, is_prn, is_active)
VALUES (20, 1, 2, 'Lantus Insulin', '1 Unit', 'Before meals.', '2024-01-01', '2025-12-31', false, true);

INSERT INTO medication_schedules (schedule_id, patient_user_id, doctor_user_id, medication_name, dosage, notes, start_date, end_date, is_prn, is_active)
VALUES (30, 1, 2, 'Parol', '500 mg', 'If pain occurs.', '2024-01-01', '2025-12-31', true, true);

-- SCHEDULE TIMES
-- Coraspin (10) -> 09:00
INSERT INTO medication_schedule_times (time_id, schedule_id, scheduled_time)
VALUES (100, 10, '09:00:00');

-- Insulin (20) -> 08:00 & 20:00
INSERT INTO medication_schedule_times (time_id, schedule_id, scheduled_time) VALUES (201, 20, '08:00:00');
INSERT INTO medication_schedule_times (time_id, schedule_id, scheduled_time) VALUES (202, 20, '20:00:00');

-- Parol (30) -> No fixed time
INSERT INTO medication_schedule_times (time_id, schedule_id, scheduled_time)
VALUES (301, 30, NULL);

-- MEDICATION LOGS
INSERT INTO medication_logs (medication_log_id, schedule_time_id, patient_user_id, taken_at, status)
VALUES (100, 201, 1, CURRENT_TIMESTAMP, 'TAKEN');

-- DAILY LOGS
INSERT INTO daily_logs (patient_user_id, log_type, quantity_ml, created_at)
VALUES (1, 'WATER', 200, CURRENT_TIMESTAMP);

INSERT INTO daily_logs (patient_user_id, log_type, description, created_at)
VALUES (1, 'FOOD', 'Lentil soup and salad', CURRENT_TIMESTAMP);

-- SAVED LOCATIONS
INSERT INTO saved_locations (patient_user_id, location_name, latitude, longitude, address_details)
VALUES (1, 'Home', 40.765432, 29.987654, 'Downtown District No:5');

INSERT INTO saved_locations (patient_user_id, location_name, latitude, longitude, address_details)
VALUES (1, 'Summer House', 41.008237, 28.978358, 'Coastal Road No:3');

-- REMINDERS (Using H2 date functions)
INSERT INTO general_reminders (patient_user_id, creator_user_id, title, reminder_time, is_recurring, is_completed)
VALUES (1, 3, 'Grandson Birthday', DATEADD('DAY', 2, CURRENT_DATE), false, false);

INSERT INTO general_reminders (patient_user_id, creator_user_id, title, reminder_time, is_recurring, is_completed)
VALUES (1, 2, 'Cardiology Appointment', DATEADD('DAY', 7, CURRENT_DATE), false, false);