# FDR Sentinel frontend

Responsive React/Vite UI for the FDR management backend in `../fdr-portal`.

## Run locally

From `frontend/`:

```sh
npm ci
npm run dev
```

Vite serves the application at `http://localhost:5173` and proxies `/api` to `http://localhost:8080`. Start the Spring Boot service and its configured PostgreSQL database separately. The frontend does not include seeded users or credentials; sign in with an account provisioned by the backend.

Use `npm run build` to create the production bundle in `dist/`.

## API configuration

- `VITE_API_BASE_URL` (build-time, optional): API base URL. It defaults to `/api`, which is appropriate when the frontend and backend share an origin or a reverse proxy forwards `/api` to Spring Boot.
- For a separately hosted API, set `VITE_API_BASE_URL` to the backend API base URL, for example `https://api.example.test/api`, at frontend build time.
- Configure the backend's `CORS_ALLOWED_ORIGINS` with the exact frontend origin(s), comma-separated. The default is `http://localhost:5173`; wildcard origins are rejected.

## Reports and audit trail

The Reports & audit screen uses live backend endpoints:

- `GET /api/reports/summary?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/reports/fdrs.csv?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/audit-events?page=0&size=25`

Only administrators and regulation officials can access the reports/audit workspace. Text cells in FDR CSV exports are quoted and neutralized when they begin with a spreadsheet formula prefix. Audit query dates, text filters, and page size are bounded.

## Security and prototype limits

- Verification outcomes are recorded through the review API. There is no independent bank verification integration; recording an external confirmation reference is not itself proof that the bank was contacted.
- JWT access tokens are retained in browser local storage to match the existing backend token contract. This prototype does not implement refresh tokens or an HttpOnly cookie session.
- Workflow collections are assembled from existing FDR list and per-FDR history endpoints, with bounded request concurrency. A dedicated server-side workflow queue would scale better for a large production dataset.
- Frontend role checks shape navigation and forms only. Spring Security and backend services remain responsible for authorization and workflow transitions.

## Backend configuration notes

See `../fdr-portal/src/main/resources/application.properties` for defaults. Set secrets through environment variables rather than committing them. In particular, configure `DB_PASSWORD`, a Base64-encoded `JWT_SECRET` with at least 32 decoded bytes, and initial administrator credentials where needed. The scheduler runs daily at 8 AM Asia/Kolkata by default; `MATURITY_ALERT_CRON` can override that schedule for testing.
