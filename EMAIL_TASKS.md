# NB-Ecom-Notifications-API: email plan and task list

**Goal (owner, 1 Nov 2026):** this repo sends the email for every successful event for a member or a provider: sign-up, profile and password changes, onboarding, sessions, payments, invoices and the rest.

**Last checked against the code:** 1 Nov 2026, Care API branch `feature/provider-onboarding-merge`. Line numbers will drift; method names are the reference.

**Status of this plan:** the phases are proposed, and work starts only when Phase 0's start gate is met. Nothing below overrides the main `TASKS.md`, which stays the tracker for all other work.

## How agents use this file

- **Status lives here** for the `M` stories below. Main `TASKS.md` F7 points to this file. When an `M` story finishes, also tick the F7 story it maps to.
- **Who:** `MAILS` = this repo's agent, `API` = Care API agent, `PAY` = Money API agent, `UI` = UI agent, `PO` = the owner.
- **Status:** `TODO`, `DOING`, `REVIEW`, `BLOCKED`, `DONE`, as in main `TASKS.md`. Claim a story by setting `DOING` with your branch. Re-read this file right before saving, and edit only stories on your side.
- **Never invent an answer to a decision.** If a story needs one that's open, leave it `BLOCKED`.
- **Don't build or start anything in `NB-Ecomm-API` from another session.** The owner's running site uses the API on port 8080 from that folder.

## Rules for every email

1. **A failed email never breaks the action.** Send after the database commit, never inside the transaction.
2. **No health details** in subject, body, logs or event data. No test results, conditions, pregnancy stage or protocol steps. Say "your care session", then link to the signed-in page.
3. **No repeats.** Each email has an event ID, and a repeat of that ID is ignored.
4. **Logs hold IDs only**, never addresses or message text.
5. **Times are shown in IST.** Check for UTC server clocks.
6. **Secrets stay out of chat and git.** `RESEND_API_KEY` and the private service token go in Vercel settings only.
7. **Mails owns the wording.** Callers send the event type and a few safe values, not subject lines or finished text.
8. **Each repo keeps its own data.** Mails doesn't read Care's tables except the two it will take over, under the database decision D-M2.

## How an email gets sent

```text
Care or Money API                         Mails API                        Resend
action + send row ──> POST /internal/v1/emails ──> check event ID ──> template ──> send
  (same commit)  │     (private token, ~3s timeout)      │
                 └ not sent: scheduler retries the row   └ write to the send log
```

- **Now:** the caller saves a send row in the same transaction as the action, then calls Mails after commit. A crash can't lose the email, because the row already exists. Care must confirm this in M2.1 and M2.2.
- **No waiting on Mails:** after commit, the row ID goes to a small background worker and the member's request returns at once. Calling Mails from the after-commit step itself would hold one of the 5 database connections while waiting (Care API session's reading of Spring; prove it when building).
- **Sleeping containers:** Mails takes 20-40 seconds to wake (Care API session's figure), so the worker gives up after about 3 seconds and the scheduler sends the row. The same happens if the worker's queue is full or Vercel freezes the container. The first email after idle can therefore arrive up to one scheduler run late, about 15 minutes under D18. This makes D18 required from Phase 2, not only for reminders.
- **Later:** the same request comes from the F4-S4 event system. Templates and the send log don't change.
- **Recipient:** either a `userId` (Mails asks Care for the address at send time and keeps no copy) or a raw address (for careers and public forms).
- **Timed work:** Vercel containers sleep, so `@Scheduled` can't be relied on (BUG-2). Retries and reminders need the external scheduler from D18.

## Event catalogue

**Type:**
- `T` = transactional, always sent because the person's own action or account triggered it
- `R` = reminder, sent only if the person's email preference is on (off by default today)

**State of the trigger in the code:**
- `Ready` = the code exists and no email is sent
- `Sends` = an email already goes through `EmailSender`, and must be moved to Mails
- `Missing` = the feature behind the event isn't built yet

### Member

| Event | Type | Where it happens | State | Phase |
|---|---|---|---|---|
| Email verification | T | Firebase, from the UI (`firebase-auth.service.ts`) | Firebase sends it; stays there | None |
| Password reset link | T | Firebase, from the UI | Firebase sends it; stays there | None |
| Welcome, on sign-up | T | `AuthController.syncFirebaseUser`, when `isNewUser` | Ready | 3 |
| Profile updated | T | `UserProfileService.applyProfileUpdate` (used by `PUT /users/me` and `PATCH /me`; confirm in M3.2) | Ready | 3 |
| Password changed | T | Firebase only; Care is never told | Missing, see D-M4 | 3 |
| Email address changed, notice to the old address | T | `AuthController.syncFirebaseUser` overwrites the email without comparing. A phone sign-in's token can carry no email, so send only when both old and new are present | Ready | 3 |
| Account deletion requested, cancelled, status changed | T | `AccountDataService.requestDeletion`, `cancelDeletion`, `updateDeletionStatus` | Ready | 3 |
| Onboarding document signed | T | `OnboardingService.signDocument` calls `sendTransactionalEmail`, which is a NoOp | Ready | 4 |
| Onboarding complete | T | `OnboardingService.complete` | Ready | 4 |
| Session booked, rescheduled, cancelled | T | `BookingService.createBooking`, `rescheduleBooking`, `cancelBooking` | Ready | 4 |
| Session completed, no-show | T | `BookingService.completeSession`, the no-show method (status set at line 211) | Ready | 4 |
| Session cancelled because the provider was paused | T | `ProviderLifecycleService` line 181 | Sends | 2 |
| Session reminder, next-step reminder | R | `ReminderGenerationJob` | NoOp today; BUG-16 and BUG-2 first | 5 |
| Payment succeeded | T | `PaymentService.handleSucceeded` | Ready; mock payments only until F8 | 6 |
| Payment failed | T | `PaymentService.handleFailed` | Ready; mock payments only until F8 | 6 |
| Refund issued | T | `RefundService.createRefund` | Ready; has its own safety gap, see F5-S4 | 6 |
| Invoice issued | T | No invoice code exists yet (F5-S3) | Missing; needs D5 | 6 |
| Credit note issued | T | No code yet (F5-S4) | Missing | 6 |
| Product order placed, paid, shipped | T | No product ordering exists (F14). `OrderService.createOrder` is plan checkout only, and an unpaid plan order isn't a success, so it sends nothing | Missing | 6 |
| Referral emails | T | `ReferralService` | Sends | 2 |
| Careers, feedback and idea form replies | T | `CareersService`, `FeedbackService`, `IdeaService` | Sends | 2 |

Not emailed, on purpose: health profile changes, health records, children's details and journey content. An email that one of these changed reveals health activity. If the owner wants "a record was shared with you", it needs generic wording and D-M1 sign-off.

### Provider

| Event | Type | Where it happens | State | Phase |
|---|---|---|---|---|
| Application received | T | `ApplicationService.submit` | Sends | 2 |
| Application approved, needs a change, declined | T | `ApplicationReviewService` lines 230, 255, 270 | Sends | 2 |
| Bank details problem | T | `ApplicationReviewService.verifyBank` | Sends | 2 |
| Profile change decided | T | `ProfileChangeService.decide` | Sends | 2 |
| Profile live, paused for lapsed licence | T | `ProviderLifecycleService` lines 95, 129 | Sends | 2 |
| Application withdrawn, profile restored after renewal | T | `ApplicationService.withdraw`, `ProviderLifecycleService.restoreAfterLicenceRenewal` | Ready | 5 |
| New booking, cancellation, reschedule | T | `BookingService` | Ready | 5 |
| Upcoming session reminder | R | New, from Care's reminder job | Missing | 5 |
| Payout created or paid | T | `PayoutService.createPayoutForBooking`; paid comes from Money (F8-S5) | Created is Ready; paid is Missing | 6 |
| Provider statement issued | T | Money API (F5-S7). A statement from NartureBelle, not an invoice | Missing | 6 |
| Profile updated by the provider | T | `ProfileChangeService.request` | Ready | 5 |

### Employee

Outside the goal above, which covers members and providers. Listed because the owner decided on payslips (D45). Not in the first release.

| Event | Type | Where it happens | State | Phase |
|---|---|---|---|---|
| Payslip issued, with no salary figures in the email | T | Money API (F5-S8) | Missing | 6 |

### Internal, to the NartureBelle team

| Event | State | Phase |
|---|---|---|
| New careers application with résumé | Sends; see D-M3 | 2 |
| New feedback, idea or referral | Sends | 2 |
| Emails that keep failing | Missing | 7 |

## Decisions needed

| # | Question | Recommendation | Owner's answer |
|---|---|---|---|
| D-M1 | **"Every event" and privacy.** Should every email above go out as listed, with reminders staying opt-in and health-related changes never emailed? | Yes. Security and account emails can't be turned off; reminders can | Yes (owner, 1 Nov 2026) |
| D-M2 | **Database login.** Should Mails get its own login, limited to its own schema and the two tables it takes over? | Yes. Otherwise nothing stops it reading Care's data | Yes (owner, 1 Nov 2026). Owner still has to create the login in Supabase |
| D-M3 | **Résumé email.** Send an admin download link instead of the attachment? A 4 MB résumé becomes about 5.3 MB in base64, over Vercel's 4.5 MB limit | Yes, a link behind admin sign-in, with downloads audited. This changes what you receive | Yes, a link (owner, 1 Nov 2026) |
| D-M4 | **Password changed email.** Firebase handles passwords and doesn't tell our API, and the UI has only the reset link, with no change-password screen built or planned (confirmed by the UI agent). A screen would be a new story. Which way should we detect it? | Care checks Firebase's "tokens valid after" time at sign-in and emails when it moved. Sent at the next sign-in, not at the moment of change, so if someone else changed the password and never signs in, the real owner hears nothing. That makes it a weak security notice, but it's the simplest option. M3.4 must prove it works before it's relied on | Yes, labelled as a weaker notice (owner, 1 Nov 2026) |
| D-M5 | **Profile updated email.** Should every save send one? Several saves in a row would mean several emails | Send one per 15 minutes at most, naming no changed values | Yes (owner, 1 Nov 2026) |
| D-M6 | **Reminder wording in `SERVICE_ARCHITECTURE.md`.** Correct it so Care keeps deciding what's due, including consent, while Mails stores and sends? | Yes. Both API sessions agree | Yes (owner, 1 Nov 2026) |
| D-M7 | **Start date** for the move, given the open Care API branches | The owner picks, after those branches merge | |
| D18 | Scheduler for retries and reminders (main `TASKS.md`). Needed from Phase 2, because the first email after idle depends on it | As written there: an external scheduler about every 15 minutes | |
| D5 | Invoice format, confirmed by the accountant (main `TASKS.md`) | Blocks invoices only | |
| D10 | SMS and WhatsApp provider (main `TASKS.md`) | Blocks Phase 8 only | |

## Phases

Each phase ends with something the owner can check. Order matters: nothing sends real email until Phase 1 is proven, and no reminder emails go out until BUG-16 is fixed.

### Phase 0: Ready to start

**Start gate for Phases 1 to 5, all required:** open Care API branches merged, D-M7 answered, and the Care API session confirms the code is ready to copy. M0.1 to M0.4 can go ahead without it.

**Update, 1 Nov 2026:** the owner told the Mails session to start the move now. Built and waiting for the Care API session's check, on `feature/notifications-move` here and `feature/move-notifications-to-mails` in Care (worktree `C:SERSVIJAYAPPDATAocaltemp
bcm`), local only. nothing merges until that check passes and the ui has switched. care still sends finished email text, so m2.3 and m2.4 still owe the templates. the owner still has to create the d-m2 login.
bcm`, from `7030e29`). nothing merges until the care api session has checked it and the ui has switched. decisions d-m2, d-m3 and d-m6 are still open: no restricted login exists, the résumé stays an attachment, and care still sends finished email text (m2.3 and m2.4 still owe the templates).

| ID | Task | Who | Status | Needs | Done when |
|---|---|---|---|---|---|
| M0.1 | Tidy this repo: add `bin/` to `.gitignore` and delete the Java crash logs (`hs_err_*`, `replay_*`, already ignored) | MAILS | REVIEW | | `git status` shows only source files |
| M0.2 | Create `develop` from `main` and add `vercel.json`, with the D43 rule in the README (F0-S3 step 2) | MAILS | TODO | F0-S2 | Rule is on `develop` and `main` |
| M0.3 | Answer the decisions above | PO | TODO | | Answers filled in |
| M0.4 | Set up Resend and DNS (F7-S1). Check the free plan's daily limit against expected volume. Sender `no-reply@mail.narturebelle.com`; replies to `support-care@`, team notices to `hello@` (owner, 11 Nov 2026). Steps in the README | PO | TODO | | Domain verified in Resend |
| M0.5 | Vercel project for Mails, on `develop` and `main` | PO | BLOCKED | BUG-27, account paused until 30 Nov | Mails answers on Vercel |

### Phase 1: Mails can send (F7-S2 part 1)

Done when: a test request to Mails delivers a real email, and repeating it sends nothing.

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M1.1 | Add database, security and validation to the service. Own `mails` schema with its own Flyway history for new tables: schemas and default schema set to `mails` only, `baselineOnMigrate` false, never touching `public`, validating only mapped tables. The D-M2 login has create rights in `mails` alone. Count its connections against the shared limit. Care stays the only migrator of `reminders` and `notification_preferences` | MAILS | REVIEW | Gate, D-M2 |
| M1.2 | Copy `email/*` from Care: `EmailSender`, `EmailMessage`, `ResendEmailSender`, `LoggingEmailSender` | MAILS | REVIEW | Gate |
| M1.3 | `POST /internal/v1/emails`: event ID, type, recipient, safe values. Private token, input checks, 503 if a dependency is down | MAILS | REVIEW | M1.1 |
| M1.4 | Send log: event ID (unique), type, status, attempts, time. No address and no message text | MAILS | REVIEW | M1.1 |
| M1.5 | Template system with one branded layout, text and HTML, IST time format, and a test that fails on banned health words | MAILS | TODO | M1.3 |
| M1.6 | Internal address lookup in Care, by user ID, behind the private token | API | REVIEW | Gate |
| M1.7 | Tests: repeat ignored, Resend down, bad token, logs hold no address | MAILS | REVIEW | M1.3 |

### Phase 2: Move today's emails without losing any (F7-S2 part 2)

Done when: every email in the catalogue marked `Sends` arrives through Mails with the same meaning, and turning Mails off doesn't break any action.

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M2.1 | Care's mail client: after commit, pass the row ID to a small bounded background worker and return at once. The worker calls Mails with about a 3-second timeout and never throws. Mails' event-ID check stays the real guard against double sends | API | REVIEW | Phase 1 |
| M2.2 | Care's small send-row table, written in the same transaction as the action, and a secured retry endpoint for the scheduler, which claims rows with `FOR UPDATE SKIP LOCKED` (test on Postgres, not H2). The scheduler calls Care only, and Care wakes Mails only when rows are waiting. Actions with no transaction today need one around the action and the row, with nothing external inside it: `RefundService.createRefund`, `FeedbackService.submit`, `IdeaService.submit`, `AuthController.syncFirebaseUser`; check each other caller as it's wired. Raw-address rows hold the address only until sent, then clear it, and are covered by retention and deletion work. Replaces `ApplicationNotifier`'s in-memory queue, which drops mail when full or when the container sleeps | API | REVIEW | M2.1, D18 |
| M2.3 | Templates and caller changes for provider emails (`ApplicationNotifier`) | MAILS, API | TODO | M2.1 |
| M2.4 | Careers, feedback, idea and referral emails, sent to raw addresses | MAILS, API | TODO | M2.1 |
| M2.5 | Résumé: use the admin link, or keep the attachment in Care until the answer | MAILS, API | REVIEW | D-M3 |
| M2.6 | Make transactional emails really send. Done by sending them through the event-ID path instead of replacing `NoOpEmailChannel`, which only callers without an event ID still reach | API, MAILS | REVIEW (`feature/resend-email` here, `feature/transactional-email-event-id` in Care) | M2.1 |
| M2.7 | Set up the real key on stage, and send one of each type to a test inbox | MAILS, PO | TODO; how stage avoids emailing real people is still open | M0.4, M2.3, M2.4 |
| M2.8 | Resend ready for production: event ID as Resend's idempotency key, default reply-to, one branded HTML layout around the text, Resend's message ID in the send log, its error name in logs | MAILS | REVIEW (`feature/resend-email`) | M1.2 |

### Phase 3: Account emails

Done when: signing up, editing a profile, changing an email address and asking for deletion each send the right email once.

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M3.1 | Welcome email, for new users only, not on later sign-ins | MAILS, API | TODO | Phase 2 |
| M3.2 | Profile updated, on both routes, limited per D-M5, naming no values | MAILS, API | BLOCKED | D-M5 |
| M3.3 | Email address changed: compare before saving, and send the notice to the old address | MAILS, API | TODO | Phase 2 |
| M3.4 | Password changed: prove the detection on stage, then build it. Needs the last-seen time stored by a Care migration, a Firebase user lookup in `/auth/firebase` only, no email on the first sign-in after release, and password accounts only, because an admin token revoke also moves that time | API, MAILS | BLOCKED | D-M4 |
| M3.5 | Account deletion emails. Run the user-deletion handling here too (remove reminders, preferences and the send log's user link) | MAILS, API | TODO | Phase 2 |
| M3.6 | Match Firebase's verification and reset emails to our brand and domain, so all mail looks like it's from us | PO, UI | TODO | M0.4 |

### Phase 4: Member onboarding and session emails (F7-S3, F7-S4)

Done when: a test member gets each email once, with IST times and no health details, and a rolled-back booking sends nothing.

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M4.1 | Onboarding: document signed, with a secure link to the document, and onboarding complete. `signDocument` calls `sendTransactionalEmail` inside its transaction today, so move that after commit | MAILS, API | DOING: both emails now really send (Care `feature/transactional-email-event-id`; `MailQueue` already sends after commit). The secure document link is still open | Phase 2 |
| M4.2 | UI: replace the "coming soon" wording about emails | UI | TODO | M4.1 |
| M4.3 | Session booked, rescheduled, cancelled | MAILS, API | TODO | Phase 2 |
| M4.4 | Session completed, no-show | MAILS, API | TODO | M4.3 |
| M4.5 | Tests: no email on rollback, no repeat on retry, IST time on a UTC clock | API | TODO | M4.3 |

### Phase 5: Reminders, preferences and provider emails

Done when: reminders follow preferences, a dismissed reminder stays dismissed, and a provider hears about their bookings.

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M5.1 | Fix BUG-16 (dismissed reminders come back). Must be done before any reminder email | API, MAILS | REVIEW | Gate |
| M5.2 | Move reminder rows, preferences, their endpoints and models to Mails. UI points to the new address | MAILS, UI | REVIEW | D-M6, M1.1 |
| M5.3 | Sign-in on Mails: Care resolves Firebase UID to user ID and roles, with a short cache. A Care outage gives 503, never 401 | API, MAILS | REVIEW | M5.2 |
| M5.4 | Care's job decides what's due and checks consent, then sends a small batched, repeat-safe request. Mails stores and sends | API, MAILS | REVIEW | D18, M5.2 |
| M5.5 | Data export includes reminders and preferences through `/internal/users/{id}/export` | MAILS, API | REVIEW | M5.2 |
| M5.6 | Provider: new booking, reschedule, cancellation, upcoming session | MAILS, API | TODO | Phase 2 |
| M5.7 | Provider: application withdrawn, profile restored, profile update requested | MAILS, API | TODO | Phase 2 |

### Phase 6: Money emails (F7-S5, F7-S6)

Done when: each is proven on stage with mock payments, and again with real ones after F8.

Payment templates can be built now on mock payments. Real-money sending waits for F8, invoices for F5-S3 and D5.

**Invoices and credit notes (Money API review, 1 Nov 2026):** Money saves the send request in the same transaction as the document, then calls Mails from that row, so they don't use the after-commit call. The event ID comes from the document, such as `invoice-issued:<invoiceId>`, so a replay can't send twice. Settle the exact format in M1.3.

**Decided by the owner, as relayed by the Money API agent:** Care and later Providers work out provider earnings. Money creates the provider document and runs the gateways.

**Also decided (D44, D45 in main `TASKS.md`):** the provider document is a statement from NartureBelle, not an invoice. Employees get payslips only, not in the first release.

**Still open:** one statement per payout or per month (D46, accountant).

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M6.1 | Payment succeeded and failed. A replayed webhook sends nothing. `handleSucceeded` runs inside the webhook transaction, so send after commit, as its referral step does. On `feature/pricing-and-validations`, a payment on a closed order grants nothing, so it must not send a success email | MAILS, API | TODO | Phase 2 |
| M6.2 | Refund issued | MAILS, API | TODO | M6.1 |
| M6.3 | Product order placed and paid | MAILS, API | BLOCKED | M6.1, F14 |
| M6.4 | Move the caller side to the Money API when it takes over payments | PAY | BLOCKED | Money move |
| M6.5 | Invoice issued: secure sign-in link to download, or PDF attached per D5. An attachment would need a new field in M1.3 and must fit Vercel's 4.5 MB limit; the link fits M1.3 as written | MAILS, PAY | BLOCKED | F5-S3, F5-S5, D5 |
| M6.6 | Credit note issued | MAILS, PAY | BLOCKED | F5-S4 |
| M6.7 | Payout created and paid, for providers. "Created": caller is Care, later Providers (`PayoutService`). "Paid": caller is Money, once real transfers are built there | MAILS, API, PAY | BLOCKED | "Created": real payouts. "Paid": F8-S5 |
| M6.8 | Order shipped | MAILS, API | BLOCKED | F14-S1 |
| M6.9 | Provider statement issued, separate from M6.7's payout notice. Timing follows D46 | MAILS, PAY | BLOCKED | F5-S7, D46 |
| M6.10 | Payslip issued: no salary figures in the email, only a sign-in link. Not in the first release | MAILS, PAY | BLOCKED | F5-S8 |

### Phase 7: Run it safely

Done when: the owner can see what was sent and what failed, without needing a developer.

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M7.1 | Admin page or endpoint: sent and failed emails, with a resend button | MAILS, UI | TODO | Phase 2, D9 |
| M7.2 | Alert the team when an email keeps failing | MAILS | TODO | M7.1 |
| M7.3 | Handle Resend's bounce and spam-complaint reports, and stop sending to bad addresses | MAILS | TODO | Phase 2 |
| M7.4 | Move to the F4-S4 event system when it exists, and remove the direct calls | MAILS, API, PAY | BLOCKED | F4-S4 |
| M7.5 | Remove the moved code from Care. Update `SERVICE_ARCHITECTURE.md`, `UI_INTEGRATION_GUIDE.md` and `TASKS.md` | API | TODO | Phases 2 and 5 |

### Phase 8: SMS and WhatsApp, later (F7-S7)

| ID | Task | Who | Status | Needs |
|---|---|---|---|---|
| M8.1 | Replace `NoOpSmsChannel` and `NoOpWhatsAppChannel`, including templates and DLT registration where required | MAILS | BLOCKED | D10 |
| M8.2 | UI: remove the "not connected yet" labels | UI | BLOCKED | M8.1 |

## Change log

| Date | Change |
|---|---|
| 2026-11-01 | First version, from the owner's goal and a check of the code |
| 2026-11-01 | Care API session's check: product orders corrected, plus notes on sign-in email, transactions and password detection |
| 2026-11-01 | Send row saved with the action, about 3-second call timeout, D18 required from Phase 2, `mails` schema conditions, weak-notice warning on D-M4 |
| 2026-11-01 | Care API session's review: background worker instead of an after-commit call, `SKIP LOCKED`, callers without transactions, clearing raw addresses |
| 2026-11-01 | Owner's D44 and D45, relayed by the Money API agent: added M6.9 and M6.10, split M6.7's needs |
| 2026-11-01 | Owner ordered the move to start; stories in progress marked `DOING` |
| 2026-11-01 | Move built and tested; stories set to `REVIEW`. Résumé emails link to the admin screen (D-M3) |
| 2026-11-01 | The repo is now `NB-Ecom-Notifications-API` on GitHub. Java packages and the local folder keep their old names |
| 2026-11-11 | Owner chose Resend from `mail.narturebelle.com`, with only the `hello@` and `support-care@` mailboxes. M2.6 and the new M2.8 built and in review. Domain verified in Resend; a local test email arrived at `hello@` |
| 2026-11-11 | Local test setup (`scripts/local`, in-memory database, `EMAIL_REDIRECT_TO`), and Care's onboarding-complete welcome email (M4.1) |
