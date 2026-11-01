# NB-Ecom-Mails-API

Sends NartureBelle's email and owns reminders and notification preferences. The plan and task status are in [EMAIL_TASKS.md](EMAIL_TASKS.md).

## Run locally

Needs the same database settings as the Care API, and the Care API running. Never point a local run at production.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:DB_URL = '...'; $env:DB_USERNAME = '...'; $env:DB_PASSWORD = '...'
$env:INTERNAL_API_TOKEN = '...'   # the same value in the Care API
$env:CARE_API_URL = 'http://localhost:8080'
.\gradlew bootRun
```

The first start creates a `mails` schema in that database. Check it at http://localhost:8082/api/v1/hello, which returns "Hello Emails".

Without `RESEND_API_KEY`, nothing is delivered and sends are only logged.

Tests use an in-memory database: `.\gradlew test`.

## Endpoints

| Who calls it | Endpoint | What it does |
|---|---|---|
| UI, signed in | `GET /api/v1/me/reminders`, `POST /api/v1/me/reminders/{id}/dismiss` | Reminders; same paths and JSON as the Care API had |
| UI, signed in | `GET /api/v1/me/notification-preferences`, `PUT /api/v1/me/notification-preferences/{channel}` | Channel preferences; same paths and JSON |
| Care API, private token | `POST /internal/v1/emails` | Sends one email, at most once per event ID |
| Care API, private token | `POST /internal/v1/notifications/transactional` | Transactional channel, still a NoOp |
| Care API, private token | `POST /internal/v1/reminders` | Creates due reminders; repeats are ignored |
| Care API, private token | `GET /internal/v1/users/{id}/export` | The user's data, for their export |

Private calls send the token in the `X-Internal-Token` header.
