# FDR Portal

A role-based web application for managing Fixed Deposit Receipts (FDRs) across their lifecycle—from institutional registration and document submission to verification, renewal, release requests, maturity monitoring, notifications, reporting, and audit history.

> **Project scope:** This repository provides workflow and evidence tracking. It does **not** connect to bank systems for automatic/independent FDR confirmation, and it does not execute bank transfers or refunds.

## Contents

- [Overview](#overview)
- [Key features](#key-features)
- [Roles and responsibilities](#roles-and-responsibilities)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Run locally](#run-locally)
- [Configuration](#configuration)
- [FDR lifecycle](#fdr-lifecycle)
- [API overview](#api-overview)
- [Tests and production build](#tests-and-production-build)
- [Security notes](#security-notes)
- [Known limitations](#known-limitations)

## Overview

The FDR Portal centralizes FDR records and supporting documents so authorized users can track instrument status, verification decisions, renewal/release requests, maturity dates, and related notifications. Role-specific workspaces and server-side authorization separate submission, verification, and approval responsibilities.

## Key features

- Institution and user management.
- FDR registration with required-field, amount, date, and duplicate-record validation.
- Upload and retrieval of FDR supporting documents.
- Verification outcomes and review remarks recorded against FDR records.
- Separate renewal and release/refund request workflows.
- Role-separated verification and approval queues, enforced by the backend as well as the UI.
- Maturity monitoring and scheduled reminders (default thresholds: 30, 15, 7, and 1 day before maturity).
- Notifications, summary reports, CSV exports, and audit-event history.
- JWT-based authentication, password hashing, request validation, and structured API errors.
- PostgreSQL persistence and Flyway migrations.

## Roles and responsibilities

The application uses role-based access. Exact navigation depends on the signed-in role.

| Role | Main responsibility |
| --- | --- |
| `ADMIN` | Administrative setup, institution management, and user provisioning. |
| Institution user | Register FDRs, upload supporting documents, and initiate eligible renewal/release requests. |
| `VERIFICATION_OFFICIAL` | Review FDR records/documents and record verification outcomes. This role does not handle renewal/release approvals. |
| `APPROVING_AUTHORITY` | Review and approve/reject renewal and release requests. This role does not perform general FDR verification. |

Authorization is enforced by Spring Security and backend service rules; hiding a menu item in the frontend is not the security boundary.

## Technology stack

- **Frontend:** React 18, Vite, React Router, Tailwind CSS, Axios, Recharts, Lucide.
- **Backend:** Java 21, Spring Boot, Spring Web MVC, Spring Security, Spring Data JPA, Bean Validation.
- **Authentication:** JWT (HMAC-SHA256) and BCrypt password hashing.
- **Database:** PostgreSQL 17.
- **Schema migrations:** Flyway.
- **Local database:** Docker Compose.
- **CI:** GitHub Actions runs backend tests and builds the frontend.

## Architecture

```text
Browser
  └── React + Vite (frontend/)
        └── /api proxy ──> Spring Boot REST API (fdr-portal/, port 8080)
                              ├── Spring Security / JWT
                              ├── Services and workflow validation
                              ├── Spring Data JPA + Flyway
                              ├── PostgreSQL (port 5432)
                              └── Private FDR document storage (./var/fdr-documents)
```

The Compose file starts PostgreSQL only. Run the Spring Boot backend and Vite frontend in separate terminal sessions during local development.

## Prerequisites

Install the following:

- Git
- Docker Desktop or Docker Engine with the Compose plugin
- JDK 21
- Node.js 20 or newer and npm
- OpenSSL (for generating a local JWT secret)

## Run locally

### 1. Clone the repository

```bash
git clone https://github.com/atharva9282/FDR-Portal.git
cd FDR-Portal
```

### 2. Start PostgreSQL

```bash
cd fdr-portal

# Set a local development password. Use the same value for the backend.
export DB_PASSWORD='change-this-local-db-password'

docker compose up -d postgres
```

The Compose configuration creates database `fdr_portal` and database user `fdr_app`, and publishes PostgreSQL on port `5432`.

### 3. Configure and start the backend

Run these commands in the same terminal session where you will start Spring Boot. Replace the example administrator credentials with your own local values.

```bash
export DB_PASSWORD='change-this-local-db-password'
export JWT_SECRET="$(openssl rand -base64 32)"
export BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export BOOTSTRAP_ADMIN_PASSWORD='change-this-admin-password'

./mvnw spring-boot:run
```

The backend starts at **http://localhost:8080**. The database schema is managed by the configured Flyway migrations and JPA configuration. The bootstrap administrator is optional; set both bootstrap variables if you want the application to initialize an administrator account.

Keep the backend terminal running. If you open a new terminal to run other commands, set any required environment variables again where applicable.

### 4. Start the frontend

Open a second terminal from the repository root:

```bash
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite proxies requests from `/api` to `http://localhost:8080` by default.

### 5. Stop the local services

Stop Spring Boot and Vite with `Ctrl+C` in their respective terminals. Stop PostgreSQL with:

```bash
cd fdr-portal
docker compose down
```

The named Docker volume preserves database data. To delete the local database data as well, use `docker compose down -v` (destructive).

## Configuration

Configure secrets through environment variables; do not commit real credentials or secrets.

| Variable | Purpose | Default / requirement |
| --- | --- | --- |
| `DB_PASSWORD` | PostgreSQL password used by Compose and Spring Boot | Required; use the same local value for both. |
| `JWT_SECRET` | Base64-encoded HMAC signing key | Required; must decode to at least 32 bytes. Generate one with `openssl rand -base64 32`. |
| `BOOTSTRAP_ADMIN_EMAIL` | Email for the initial administrator | Optional; set together with the password to bootstrap an admin. |
| `BOOTSTRAP_ADMIN_PASSWORD` | Password for the initial administrator | Optional; use a strong local value and never commit it. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins allowed by the backend | Defaults to `http://localhost:5173`. Use explicit origins, not wildcards. |
| `MATURITY_ALERT_CRON` | Cron expression for the maturity scan | Defaults to daily at 8:00 AM Asia/Kolkata. |
| `SHOW_SQL` / `FORMAT_SQL` | Enable SQL logging / formatting | Defaults to `false`. |
| `VITE_API_BASE_URL` | Frontend API base URL at build time | Optional; defaults to `/api`. |

Additional backend settings are in [application.properties](fdr-portal/src/main/resources/application.properties), and frontend-specific notes are in [frontend/README.md](frontend/README.md).

## FDR lifecycle

1. **Register:** An institution submits the FDR reference, bank details, amount, issue date, and maturity date.
2. **Upload:** The institution attaches the supporting FDR document.
3. **Verify:** A Verification Official reviews the record and document and records an outcome and remarks.
4. **Renew:** The institution submits a renewal request and replacement FDR details/documents. Approval of the renewal request does not replace verification of the replacement FDR.
5. **Release:** The institution submits an eligible release request. The Approving Authority reviews it, moves it through the review workflow, and approves or rejects it. An approved request must then have completion evidence/reference recorded before the FDR is marked released.
6. **Monitor and report:** Users with access can view maturity-related information, notifications, reports, and audit history.

Actual state transitions depend on the backend's workflow rules. A document upload or an official review outcome is not, by itself, proof of independent confirmation from the issuing bank.

## API overview

The REST API is served by Spring Boot on port `8080`. Main endpoint groups include:

| Area | Base path |
| --- | --- |
| Authentication and user provisioning | `/api/auth` |
| Institutions | `/api/institutions` |
| FDR records | `/api/fdrs` |
| FDR documents | `/api/fdrs/{fdrId}/documents` |
| Verification reviews | `/api/verification` (see controller routes) |
| Renewal requests | `/api/fdrs/{fdrId}/renewal-requests` |
| Release requests | `/api/fdrs/{fdrId}/release-requests` |
| Release approval queue | `/api/release-requests` |
| Monitoring | `/api/monitoring` |
| Reports and CSV export | `/api/reports` |
| Audit events | `/api/audit-events` |

For exact routes, request/response DTOs, and authorization rules, see the controllers under `fdr-portal/src/main/java/com/aicte/fdrportal/controller`.

## Tests and production build

### Backend tests

From `fdr-portal/`, configure `DB_PASSWORD` and `JWT_SECRET`, ensure the test database is available as required, then run:

```bash
./mvnw --batch-mode clean test
```

### Frontend production build

From `frontend/`:

```bash
npm ci
npm run build
```

The production frontend bundle is written to `frontend/dist/`.

GitHub Actions runs backend tests against PostgreSQL and builds the frontend on pushes to `main` and on pull requests.

## Security notes

- Never commit database passwords, JWT secrets, real user credentials, private FDR documents, or production data.
- Use strong unique secrets for each environment.
- Restrict CORS to known frontend origins.
- Uploaded FDR documents are stored outside the frontend's public directory by default.
- Use demo data for presentations and testing.
- Before production deployment, review TLS termination, backups, retention policies, logging, access control, secret management, document access, and operational monitoring.

## Known limitations

- **No live bank integration:** The application records verification outcomes and evidence references; it does not independently contact a bank or validate an FDR against bank systems.
- **No fund transfer:** Release approval and completion tracking are workflow records; the portal does not execute a bank transfer or refund.
- **Prototype deployment:** Local setup instructions are intended for development. Production deployment requires environment-specific hardening, monitoring, backup, and operational controls.

---

Developed as a prototype for streamlining FDR registration, verification, renewal, release tracking, and monitoring.
