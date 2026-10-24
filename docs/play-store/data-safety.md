# Data safety form answers

How to answer Play Console → Policy → App content → Data safety, based on what
the code does (checked for version 1.1.0). Review before submitting: Play's
definitions change, and the developer account owner is responsible for them.

## Data collection and security

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** | Food search terms and barcodes are sent to Open Food Facts; optional cloud sync uploads the account email and app data. |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | HTTPS only; cleartext traffic is disabled (`network_security_config.xml`). |
| Do you provide a way for users to request that their data is deleted? | **Yes** | Profile → Cloud sync → Delete account deletes the account and its synced data. Everything else is on the device: delete entries, clear storage or uninstall. |
| Can users create an account? | **Yes, optional** | Email + one-time code. Add the account-deletion link (a web page or the in-app path) in Play Console → App content → Data deletion. |

## Data types

Declare these:

| Data type | Collected | Shared | Optional | Purpose | Notes |
|---|---|---|---|---|---|
| App activity → **Other user-generated content** (food search terms and barcode numbers) | Yes | Yes (Open Food Facts) | Yes: searching online and scanning are optional; the built-in foods work without them | App functionality | Not linked to the user; no identifiers are sent; not used for tracking or ads |
| Personal info → **Email address** | Yes | No | Yes (cloud sync only) | App functionality, Account management | Used only to sign in; Supabase is a service provider, not sharing |
| Health and fitness → **Fitness info** (workouts, body weight, measurements) | Yes | No | Yes (cloud sync only) | App functionality | Linked to the account; encrypted in transit and at rest |
| Health and fitness → **Health info** (food diary, nutrition targets, water) | Yes | No | Yes (cloud sync only) | App functionality | Linked to the account; encrypted in transit and at rest |
| App activity → **Other user-generated content** (custom exercises, recipes, grocery list) | Yes | No | Yes (cloud sync only) | App functionality | Linked to the account |

Processing by Supabase on our behalf counts as a service provider, so these
are "collected", not "shared".

Without cloud sync, everything else stays on the device and is **not collected** under Play's
definition (data that never leaves the device doesn't need to be declared):
health and fitness data (workouts, nutrition, weight, measurements), photos,
personal info from setup (sex, birth date, height), and settings. Optional
Health Connect sync reads and writes health data only between the app and
Health Connect on the same device. Nothing is transmitted off the device, so
it isn't collected either. Export files are written only where the user
chooses through the system file picker.

Apart from the optional cloud sync account, the app has no account, no
advertising ID usage, no analytics and no crash-reporting SDKs.

## Related declarations

- **Foreground service (specialUse):** the workout service keeps the rest timer
  and workout controls running on the lock screen while a workout is active.
  The manifest declares the subtype (`feature/workout/src/main/AndroidManifest.xml`).
  In the Play Console declaration, explain: "Shows the active workout's rest
  timer and set controls in a notification so users can log sets and hear the
  rest-timer alert with the screen off. It runs only between starting and
  finishing a workout." Attach a short screen recording of the notification.
- **Health Connect permissions declaration:** declare each permission in Play
  Console → App content → Health apps, with these justifications:
  - `WRITE_EXERCISE`: "Writes the user's finished strength-training workouts
    to Health Connect so other fitness apps can see them."
  - `READ_WEIGHT` and `WRITE_WEIGHT`: "Shares the body weights logged in
    Jalilgym and imports weigh-ins from scale apps into the weight trend."
  - `WRITE_NUTRITION`: "Writes the calories and macronutrients of each meal
    logged in the food diary."
  - `READ_STEPS`: "Shows today's step count on the Today dashboard."
  The privacy policy has a Health Connect section. The app shows a rationale
  screen (`HealthPermissionsActivity`) linked from Health Connect. Health
  data isn't read in the background.
- **Camera permission:** barcode scanning only, requested when the scanner opens.
- **Health apps declaration:** the app is a fitness and nutrition tracker, not a
  medical device. It shows "Not medical advice" with calorie targets, and its
  targets have safety floors.
- **Content rating questionnaire:** no violence, sexual content, profanity,
  gambling, user-to-user communication or location sharing. Expected rating:
  Everyone / PEGI 3.
- **Target audience:** 13 and over (nutrition targets aren't meant for children).
- **Ads:** the app contains no ads.
