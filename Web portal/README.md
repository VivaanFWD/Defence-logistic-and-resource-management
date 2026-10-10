# Defence Logistics — Main Web Microservice

Spring Boot MVC application for the Defence Logistics web interface. It preserves the supplied Thymeleaf pages and integrates with the four existing domain services through direct HTTP calls. It does not define a domain database or duplicate their JPA entities/repositories.

## Technology

- Java 21
- Spring Boot 4.0.8
- Spring MVC + Thymeleaf
- Bootstrap 5.3.8 + Bootstrap Icons
- Executable JAR packaging

## Service topology

| Service | Default base URL | Responsibility in this web application |
|---|---|---|
| `Resource_Service` | `http://localhost:8081` | User/personnel and vehicle/resource reads and writes |
| `Logistics_Service` | `http://localhost:8082` | Requests, approval/process actions and allocation |
| `Inventory_Service` | `http://localhost:8083` | Inventory and equipment data |
| `Maintainance_Service` | `http://localhost:8084` | Maintenance data used by dashboard counts |
| Main Web Microservice | `http://localhost:8080` | HTML rendering, MVC routing and service-to-service HTTP calls |

Configure service URLs with environment variables `RESOURCE_SERVICE_URL`, `LOGISTICS_SERVICE_URL`, `INVENTORY_SERVICE_URL`, and `MAINTENANCE_SERVICE_URL`.

## API contract assumptions to verify

The archive contained no API definitions for the four domain services. The UI adapter assumes these REST endpoints; compare them with your actual controllers and adjust `AdminController` if necessary:

- Resource service: `GET /api/users`, `POST /api/users`, `GET /api/personnel`, `GET /api/vehicles`, `POST /api/vehicles`, `PUT /api/vehicles/{id}`, `GET /api/vehicles/available`, `GET /api/users/{id}`, `PUT /api/users/{id}`, `POST /api/users/register`
- Logistics service: `GET /api/requests`, `GET /api/requests/my`, `GET /api/requests/{id}`, `POST /api/requests`, `POST /api/requests/{id}/approve`, `POST /api/requests/{id}/reject`, `POST /api/requests/{id}/allocate`, `GET /api/reports`
- Inventory service: `GET /api/inventory`, `GET /api/inventory/{id}`, `POST /api/inventory`, `PUT /api/inventory/{id}`, `GET /api/equipment`
- Maintenance service: `GET /api/maintenance`

`MicroserviceClient` logs downstream errors and returns an empty result for read calls, so the main web application can still render if a service is offline. Writes display a failure message rather than pretending that data was saved.

## Authentication and role-based UI

The web application has its own persistent H2 database for authentication accounts only. It does not store vehicles, inventory, maintenance records, or logistics requests. Passwords are hashed with BCrypt. Spring Security protects controller routes, and the shared Thymeleaf sidebar hides links that do not match the signed-in account role. Hiding links is only a usability feature; server-side route rules enforce access.

- Public registration always creates a `QUARTERMASTER` account; visitors cannot grant themselves privileged roles.
- `ADMIN` and `LOGISTIC_OFFICER` accounts are provisioned by an administrator. The initial administrator is created from environment variables if all required values are supplied.
- Administrators can manage web authentication accounts at `/admin/accounts`, including roles and enabled/disabled status.
- The four domain services remain responsible for enforcing record-level authorization and ownership on their own APIs. The web app's route authorization does not replace API security.

### First administrator (PowerShell)

Set these values before starting the application. Choose your own strong password; do not commit it to source control.

```powershell
$env:DEFENCE_ADMIN_USERNAME = "your-admin-username"
$env:DEFENCE_ADMIN_PASSWORD = "use-a-long-unique-password"
$env:DEFENCE_ADMIN_EMAIL = "admin@example.com"
$env:DEFENCE_ADMIN_FULL_NAME = "System Administrator"
.\mvnw.cmd spring-boot:run
```

Authentication data is stored locally in `./data/defence-logistics-auth`. You can override the database URL with `AUTH_DATABASE_URL`.

## Run locally

Requirements: JDK 21 and internet access on the first Maven Wrapper run so Maven can download dependencies.

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Build the executable JAR:

```powershell
.\mvnw.cmd clean package
java -jar target\defence-logistics-web-1.0.0.jar
```

The web UI runs on port `8080`; start the four domain services on ports `8081`–`8084` first for live data. Without those services the HTML app still starts, but service-backed tables will be empty and writes will report a downstream failure. If their route names differ, update the documented paths in `AdminController`.

## Pages retained from the uploaded project

Home, login, registration, admin dashboard, vehicle management, inventory/equipment, logistics dashboard, requester dashboard, create request, request details, allocation, request tracking, reports and user management. Additional pages include the role matrix, authentication account management, profile and vehicle/inventory/user forms. Templates are grouped into `auth`, `admin`, `logistics`, `requester`, `shared`, and `fragments` directories.
