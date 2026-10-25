# Setting up cloud sync (Supabase)

Cloud sync is optional. Builds without a Supabase project configured work
exactly as before and simply hide the Cloud sync section in Profile.

## 1. Create the project

1. Create a project at supabase.com. Pick the region closest to your users.
2. **SQL editor** → paste [`schema.sql`](schema.sql) → Run.

## 2. Turn on sign-in with an email code

The app signs people in with a 6-digit code sent by email (no password, no
link to open).

1. **Authentication → Sign In / Providers → Email**: enabled. "Confirm email"
   can stay on.
2. **Authentication → Emails → Magic Link** template: the body must contain
   `{{ .Token }}` (the code) rather than `{{ .ConfirmationURL }}`. Do the same
   for **Confirm signup**, which is what a first-time user receives. Example:

   ```html
   <h2>Your Jalilgym code</h2>
   <p>Enter this code in the app: <strong>{{ .Token }}</strong></p>
   <p>It expires in one hour. If you didn't ask for it, ignore this email.</p>
   ```
3. **Authentication → Emails → SMTP settings**: set up your own SMTP sender
   before release. Supabase's built-in sender is rate-limited and meant for
   testing.

## 3. Give the build the project's address and key

From **Project Settings → API Keys**, copy the project URL and the
**publishable** key (`sb_publishable_…`). Never use the secret key in the app.

Locally, add them to `~/.gradle/gradle.properties` (not the repository):

```properties
jalilgym.supabaseUrl=https://YOUR-PROJECT.supabase.co
jalilgym.supabaseKey=sb_publishable_...
```

In CI, set the repository secrets `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`;
the release job passes them to Gradle. The publishable key is designed to ship in
apps: Row Level Security limits each person to their own rows.

## What is stored

One table, `sync_rows`: for each row of the app's database that changed, its
table name, primary key, the row as JSON, a deleted flag and when it changed.
Account-wide settings (onboarding answers, units, body profile, water goal,
reminders) are one more row, with table name `_settings`. Progress photos and
per-phone settings (Health Connect access) are not synced. Deleting the account in the app
(Profile → Cloud sync → Delete account) removes the user and all their rows.
