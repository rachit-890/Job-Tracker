# JobTrackr 🚀

JobTrackr is a production-grade, full-stack **job application tracking platform**. It empowers users to manage their entire job search pipeline — from logging applications, taking notes, and setting weekly goals to analyzing application trends via an AI-powered insights dashboard — all in a single robust application.

---

## 🌟 Key Features

### 1. 📊 Comprehensive Application Pipeline
- **Kanban Board:** Drag-and-drop your applications through various stages (Applied → Screening → Interview → Offer/Rejected).
- **Application Notes:** Maintain a timeline of interactions, follow-ups, and interview notes per application.
- **Goal Tracking:** Set weekly application targets and monitor your progress automatically.
- **Bulk CSV Import/Export & PDF Export:** Easily import your existing pipeline or generate PDF/CSV reports.

### 2. 🧠 AI-Powered Intelligence (Gemini Integration)
- **Resume Versioning & Embeddings:** Store multiple versions of your resume. The system uses **Google Gemini AI** to generate vector embeddings.
- **Smart Gap Analysis:** Matches your resume embeddings against the Job Description using Cosine Similarity to generate a "Match Score (0-100%)" and provides intelligent gap analysis.
- **Tag Extraction:** Automatically extracts key skills and technologies from job descriptions.

### 3. ✉️ Automated Email Parsing & Ingestion
- Configurable webhook to parse and classify incoming emails (e.g., Rejection, Interview Invite).
- Matches the email to existing applications in the database.
- Idempotent processing ensures duplicate emails don't corrupt your data.

### 4. 📈 Advanced Analytics & Sharing
- **Real-Time Dashboards:** Displays Sankey diagrams of your application flow, day-of-the-week heatmaps, and timeline trends.
- **Public Dashboard Sharing:** Generate a time-limited, secure public link to showcase your job search analytics (completely anonymized) with mentors or peers.

### 5. ⚡ Event-Driven Architecture
- Powered by **Apache Kafka** for robust asynchronous event processing (AI scoring, email notifications, weekly digests, reminder delivery).
- Includes an **Admin DLT (Dead Letter Topic) Dashboard** to replay failed events directly from the UI.
- Implements the **Transactional Outbox Pattern** and **ShedLock** for safe, distributed processing and guaranteed delivery without duplicates.

---

## 🛠️ Technology Stack

### Backend
- **Framework:** Spring Boot 3.5.3 (Java 21)
- **Database:** PostgreSQL 16 (Flyway Migrations, Spring Data JPA, Hibernate)
- **Message Broker:** Apache Kafka 4.1 (Event-Driven Processing)
- **Cache:** Redis 7 (Spring Data Redis)
- **AI Integration:** LangChain4j 1.5 + Google Gemini API
- **Security:** Spring Security + JWT authentication
- **Other:** Micrometer + Prometheus (Observability), iTextPDF (Export), Spring Mail (SMTP)

### Frontend
- **Framework:** React 18 + Vite
- **Routing:** React Router v6
- **State Management:** TanStack React Query v5
- **Styling:** Tailwind CSS 3.4
- **Charts:** Recharts (Sankey, Pie, Bar, Heatmaps)

---

## 🏗️ Architecture Overview

The system is built as a Single Page Application (SPA) interacting with a RESTful Spring Boot backend. 

- **Frontend & API:** The React frontend authenticates via stateless JWT tokens.
- **Kafka Event Bus:** As operations occur (e.g., `application.created`, `status.changed`, `resume.scored`), events are safely persisted via an Outbox table and published to Kafka topics.
- **Consumers:** Specialized consumers (AI Consumer, Analytics Consumer, Notification Consumer) pick up these events for background processing, ensuring the API remains highly responsive.
- **Observability:** Prometheus metrics (`/actuator/prometheus`) are exposed, capturing counters and timers for all core operations (including AI request latencies and scheduler jobs).

---

## 🚀 Getting Started

### Prerequisites
- **Docker & Docker Compose** (for running PostgreSQL, Redis, and Kafka locally)
- **Java 21** & **Maven**
- **Node.js 20+** & **npm**
- A Google Gemini API Key

### 1. Local Infrastructure Setup
Start the required infrastructure (PostgreSQL, Kafka, Redis) using Docker Compose:
```bash
docker-compose up -d
```

### 2. Backend Setup
Set your Gemini API Key in your environment:
```bash
export GEMINI_API_KEY="your-api-key-here"
```

Compile and run the Spring Boot application:
```bash
mvn clean compile
mvn spring-boot:run
```
*(The backend runs on `http://localhost:8080`)*

### 3. Frontend Setup
Navigate to the frontend directory, install dependencies, and start the development server:
```bash
cd frontend
npm install
npm run dev
```
*(The frontend runs on `http://localhost:3000`)*

---

## 🧪 Testing

The project comes with a comprehensive **Testcontainers Integration Test Suite**. These tests spin up real instances of PostgreSQL and Kafka to verify end-to-end functionality (including consumer idempotency, DLT routing, and status transition flows).

Run the tests via Maven:
```bash
mvn verify
```

---

## 📂 Project Structure

- **`src/main/java/.../`**: Backend Spring Boot source code.
  - `config/`: Security, Kafka, Redis, and Gemini configurations.
  - `controller/`: REST APIs.
  - `consumer/`: Kafka Event Consumers.
  - `service/`: Core business logic, AI orchestration, Analytics generation.
  - `domain/ & entity/`: JPA Entities and POJOs.
  - `scheduler/`: Scheduled Cron jobs (e.g., Weekly Digests, Stale Application flagging).
- **`src/main/resources/db/migration/`**: Flyway SQL schema definitions.
- **`frontend/`**: The complete React SPA.

---

## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License
This project is licensed under the MIT License.
