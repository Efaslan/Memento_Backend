# Memento: AI-Assisted Elderly Care & Daily Monitoring System (Backend)

> **⚠️ Status:** This project is currently under development as part of my **Bachelor's Thesis**.

Memento is an android app designed to enhance the safety and well-being of elderly individuals. It processes real-time data from mobile phones to detect falls and manages medication schedules, navigation, and emergency alerts for caregivers.

---

## Quick Start

You can run this project immediately without installing MSSQL. The project is configured to use an **In-Memory H2 Database** with pre-populated mock data.

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/Efaslan/Memento_Backend]
    ```
2.  **Open in IntelliJ IDEA (or Eclipse)** and run `MementoApplication.java`.
3.  **Explore the API:**
    For the Swagger UI, visit: `http://localhost:8080/swagger-ui/index.html`.
4.  **Explore the database:**
    For the H2 In-Memory Database, visit: `http://localhost:8080/h2-console`.

### Test Credentials

The system starts with a populated database. You can use these credentials in the Swagger UI to get a JWT Token and test endpoints:

| Role | Email | Password
| :--- | :--- | :---
| **Patient** | `demo.patient@test.com` | `123456`
| **Doctor** | `demo.doctor@test.com` | `123456`
| **Relative** | `demo.son@test.com` | `123456`

---

## Tech Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3
* **Security:** Spring Security & JWT Authentication
* **Database:** MSSQL Server (Dev) / H2 In-Memory (Test)
* **Architecture:** N-Tier Layered Architecture (Controller-Service-Repository)
* **Documentation:** Open API (Swagger UI)
* **Build Tool:** Maven

---

## Key Features (In-progress)

* **Action-Oriented AI Support:** Backend infrastructure supporting the mobile AI assistant; processes natural language intents (e.g., *"I took my medicine"*) to trigger database actions automatically.
* **Medication Management:** Schedules reminders for medicines and tracks intake compliance.
* **Daily Logs:** Tracks daily hydration (`WATER`) and nutrition (`FOOD`) for future reference.
* **Role-Based Access Control (RBAC):** A unified ecosystem connecting **Patients**, **Relatives**, and **Doctors** with distinct permissions.
* **Notification System:** Push notifications for emergencies and reminders.

---

### Database Schema (ERD)

The system uses a normalized relational database model designed to handle complex relationships between patients, caregivers, and medical logs.

<img width="796" height="871" alt="erd" src="https://github.com/user-attachments/assets/102eddd7-6ff0-48ff-9a4a-8b3b484a9daf" />

---

## Authors & Contact

This project is a collaborative thesis work.

**Emir Faik Aslan** (Backend Developer)
* LinkedIn: [https://www.linkedin.com/in/emir-faik-aslan]
* Email: [efaslan11@gmail.com]

**Yusuf Kaya** (Mobile & AI Developer)
* *Responsible for Flutter Frontend and NLU Integration.*
* LinkedIn: [https://www.linkedin.com/in/yusuf-kaya-07a237283]
