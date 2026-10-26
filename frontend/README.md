# FDR Sentinel frontend

Responsive React/Vite prototype for the FDR management backend in `../fdr-portal`.

## Run locally

```sh
npm install
npm run dev
```

Vite serves the application at `http://localhost:5173` and proxies `/api` to `http://localhost:8080`. Start the Spring Boot service and its configured PostgreSQL database separately. The frontend does not include seeded users or credentials; sign in with an account provisioned by the backend.

Use `npm run build` to create the production bundle in `dist/`.

## Backend gaps and prototype limits

- The backend currently has no reporting or audit-event endpoints. The Reports & audit page identifies the missing API and the response information needed; it does not display sample results.
- Verification outcomes are recorded through the available review API. No independent bank verification service is integrated, so the interface does not claim that a bank record has been verified externally.
- JWT access tokens are retained in browser local storage to match the existing backend token contract. This prototype does not implement refresh tokens or an HttpOnly cookie session.
- Workflow collections are assembled from the existing paginated FDR list and per-FDR history endpoints, with bounded request concurrency. A dedicated server-side workflow queue endpoint would scale better for a large production dataset.
- Frontend role checks only shape navigation and forms. Spring Security and the backend services remain responsible for access control and workflow transitions.
