# NB-Ecom-Notifications-API

Sends NartureBelle's email and owns reminders and notification preferences. The plan and task status are in [EMAIL_TASKS.md](EMAIL_TASKS.md).

## Test email locally (no database setup)

Runs this service alone on http://localhost:8082 with an in-memory database (the `local` profile), so it never touches stage or production. The database starts empty on every run.

1. Copy `scripts\local\env.local.example` to `.env.local` in the repo root (gitignored). Put a Resend key with sending access in `RESEND_API_KEY`, or leave it empty to only log. Keep `EMAIL_REDIRECT_TO` set: the local website signs everyone in as `dev@localhost`, so every local email goes to that inbox instead.
2. Start the service: `.\scripts\local\run-local.ps1` and leave that window open. Stop it with Ctrl+C.
3. In a second window: `.\scripts\local\send-test-email.ps1 -To you@example.com`. It sends one email, then repeats it: the first should be `SENT` and the repeat `DUPLICATE`, and only one email should arrive.

Emails to a `userId` need the Care API too, for the address: run it on port 8080, or set `CARE_API_URL` in `.env.local`. Everything counts against Resend's 100 emails a day.

## Run locally against a real database

Needs the same database settings as the Care API, and the Care API running. Never point a local run at production, and not at the shared stage database from an unmerged branch: a migration it applies there stops the deployed stage service from starting.

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
| `RESEND_API_KEY` | Prod | Sends real email. Stage stays log-only without it, and the Care API then erases each sent row's content. Use a separate key per environment, with sending access only |
| `EMAIL_FROM` | No | Sender. Default `NartureBelle <no-reply@mail.narturebelle.com>`; the domain must be verified in Resend |
| `EMAIL_REPLY_TO` | No | Where replies go when the caller names no one. Default `support-care@narturebelle.com` |
| `EMAIL_REDIRECT_TO` | No | Local or stage only: every email goes to this one inbox, its subject marked `[Test]`. Ignored in prod |

The Care API needs `MAILS_API_URL` set to this service's address.

### Resend and DNS

Email goes out through Resend from the subdomain `mail.narturebelle.com` (owner, 11 Nov 2026). The domain's DNS and the `hello@` and `support-care@` mailboxes are at Hostinger; Resend's records don't change them.

1. In Resend, add the domain `mail.narturebelle.com`.
2. In Hostinger, Domains, DNS records, add exactly what Resend shows, copying each value in full. In November 2026 that was a TXT (DKIM) on `resend._domainkey.mail` and two CNAMEs, on `send.mail` and `rsend.mail`, with no MX. Names are relative to `narturebelle.com`. Don't edit the root MX or SPF records, which Hostinger mail needs. Leave Resend's "Enable receiving" off: replies go to the Hostinger mailboxes.
3. Wait for Resend to show the domain as verified, then create the API keys.

Each email is HTML in one branded layout, plus the plain text the caller sent. The event ID goes to Resend as its `Idempotency-Key`, so a retry after a lost reply can't send twice within Resend's 24 hours. Resend's ID for each email is kept in `email_log.provider_message_id`.

Resend's free plan allows 100 emails a day and 3,000 a month. Past either limit Resend refuses with 429, this service answers 502, and the Care API keeps the email and retries it.

**Order:** the Care API's migrations V31 and V32 must reach the database first, because this service checks `reminders.dedupe_key` at startup. The first start also needs rights to create the `mails` schema.

## Endpoints

| Who calls it | Endpoint | What it does |
|---|---|---|
| UI, signed in | `GET /api/v1/me/reminders`, `POST /api/v1/me/reminders/{id}/dismiss` | Reminders; same paths and JSON as the Care API had |
| UI, signed in | `GET /api/v1/me/notification-preferences`, `PUT /api/v1/me/notification-preferences/{channel}` | Channel preferences; same paths and JSON |
| Care API, private token | `POST /internal/v1/emails` | Sends one email, at most once per event ID |
| Care API, private token | `POST /internal/v1/notifications/transactional` | Sends a transactional email, at most once per `eventId`. Without an `eventId` it stays the old no-op |
| Care API, private token | `POST /internal/v1/reminders` | Creates due reminders; repeats are ignored |
| Care API, private token | `GET /internal/v1/users/{id}/export` | The user's data, for their export |

Private calls send the token in the `X-Internal-Token` header.
