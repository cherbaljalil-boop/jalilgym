# Data safety form answers

How to answer Play Console → Policy → App content → Data safety, based on what
the code does (checked for version 1.0.0). Review before submitting: Play's
definitions change, and the developer account owner is responsible for them.

## Data collection and security

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **Yes** | Food search terms and barcodes are sent to Open Food Facts. |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | HTTPS only; cleartext traffic is disabled (`network_security_config.xml`). |
| Do you provide a way for users to request that their data is deleted? | **Yes** | All data is on the device: delete entries in the app, clear storage or uninstall. |

## Data types

Declare only this one:

| Data type | Collected | Shared | Optional | Purpose | Notes |
|---|---|---|---|---|---|
| App activity → **Other user-generated content** (food search terms and barcode numbers) | Yes | Yes (Open Food Facts) | Yes: searching online and scanning are optional; the built-in foods work without them | App functionality | Not linked to the user; no identifiers are sent; not used for tracking or ads |

Everything else stays on the device and is **not collected** under Play's
definition (data that never leaves the device doesn't need to be declared):
health and fitness data (workouts, nutrition, weight, measurements), photos,
personal info from setup (sex, birth date, height), and settings.

The app has no account, no advertising ID usage, no analytics and no
crash-reporting SDKs.

## Related declarations

- **Foreground service (specialUse):** the workout service keeps the rest timer
  and workout controls running on the lock screen while a workout is active.
  The manifest declares the subtype (`feature/workout/src/main/AndroidManifest.xml`).
  In the Play Console declaration, explain: "Shows the active workout's rest
  timer and set controls in a notification so users can log sets and hear the
  rest-timer alert with the screen off. It runs only between starting and
  finishing a workout." Attach a short screen recording of the notification.
- **Camera permission:** barcode scanning only, requested when the scanner opens.
- **Health apps declaration:** the app is a fitness and nutrition tracker, not a
  medical device. It shows "Not medical advice" with calorie targets, and its
  targets have safety floors.
- **Content rating questionnaire:** no violence, sexual content, profanity,
  gambling, user-to-user communication or location sharing. Expected rating:
  Everyone / PEGI 3.
- **Target audience:** 13 and over (nutrition targets aren't meant for children).
- **Ads:** the app contains no ads.
