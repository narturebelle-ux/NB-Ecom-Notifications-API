# NB-Ecom-Notifications-API

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

Without `RESEND_API_KEY`, nothing is delivered and sends are only logged. In production, startup fails without it.

In prod and stage, startup also fails without `INTERNAL_API_TOKEN` and `CARE_API_URL`.

Tests use an in-memory database: `.\gradlew test`.

## Deploy

Deploys to Vercel as a container from the root `Dockerfile`. `vercel.json` allows only `develop` and `main` to deploy (D43).

Set these on the Vercel project. Never put their values in chat or git.

| Setting | Required | What it is |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `stage` on `develop`, `prod` on `main`. Without it, the prod and stage startup checks don't run, and Firebase falls back to dev bypass |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Yes | Supabase database. Use the Mails login from D-M2 once the owner has created it |
| `INTERNAL_API_TOKEN` | Yes | Private token, the same value as on the Care API |
| `CARE_API_URL` | Yes | Address of the Care API |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | Yes | Verifies sign-in tokens |
| `RESEND_API_KEY` | Prod | Sends real email. Stage stays log-only without it, and the Care API then erases each sent row's content |
| `EMAIL_FROM` | No | Sender address. Has a default |

The Care API needs `MAILS_API_URL` set to this service's address.

**Order:** the Care API's migrations V31 and V32 must reach the database first, because this service checks `reminders.dedupe_key` at startup. The first start also needs rights to create the `mails` schema.

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
