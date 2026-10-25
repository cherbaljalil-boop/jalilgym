# Releasing Jalilgym

## 1. Create the upload key (once)

Keep the keystore and its passwords out of the repository, in a password
manager and an offline backup. Losing it means asking Google to reset the upload
key.

```sh
keytool -genkeypair -v -keystore jalilgym-upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Enroll in **Play App Signing** when creating the app in Play Console. Google
keeps the app signing key; you sign uploads with this upload key.

## 2. Give CI the key

In GitHub → repository → Settings → Secrets and variables → Actions, add:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 jalilgym-upload.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | the keystore password |
| `RELEASE_KEY_ALIAS` | `upload` |
| `RELEASE_KEY_PASSWORD` | the key password |

Until these exist, the **Signed release build (R8)** CI job signs with a
throwaway key (artifact `jalilgym-release-ci-test-key`) to prove the minified,
signed build works. Those builds can't be uploaded to Play. With the secrets set,
the artifact is `jalilgym-release-upload-key` and contains the `.aab` to upload.

To build locally, export the same values and run:

```sh
export JALILGYM_KEYSTORE_FILE=/path/to/jalilgym-upload.jks
export JALILGYM_KEYSTORE_PASSWORD=… JALILGYM_KEY_ALIAS=upload JALILGYM_KEY_PASSWORD=…
./gradlew :app:bundleRelease
```

## 3. Before each release

1. Bump `versionCode` (always) and `versionName` in `app/build.gradle.kts`.
2. Run the **Baseline Profile** workflow (Actions → Baseline Profile → Run
   workflow). It regenerates `app/src/release/generated/baselineProfiles/` on
   an emulator, commits it, and reports cold-start times with and without it.
3. Make sure CI is green: ktlint, unit and Robolectric UI tests, Android lint,
   Room schema check, and the signed release build.
4. If the database schema changed, add a migration and a test in
   `core/database/src/test/.../MigrationTest.kt`. Destructive migrations are
   never enabled.
5. Install the release APK from the CI artifact on a real phone and smoke-test:
   onboarding, a full workout with the screen locked, barcode scanning, offline
   food search (airplane mode).

## First release: Health Connect and cloud sync off

`gradle.properties` decides what release builds ship:

```properties
jalilgym.healthConnectInRelease=false
jalilgym.cloudSyncInRelease=false
```

With both off, the release APK/AAB has no health permissions, no Health Connect
screens and no Cloud sync section. CI fails the release job if a health
permission slips in. Debug builds keep both features for testing. For the Play
forms this means:

- **Health apps declaration:** none needed; there are no health permissions.
- **Data safety:** declare only the Open Food Facts row (see
  `docs/play-store/data-safety.md`, "First release").
- **Accounts:** the app has none, so there's no account-deletion requirement.

To ship either feature in a later update, set its property to `true`. For cloud
sync, also set up Supabase (`docs/supabase/SETUP.md`) and the repository
secrets. Then fill in the matching Play declarations.

## 4. Upload

Upload the `.aab` to the internal testing track first, then promote. Keep the
`mapping.txt` from the same CI run (it's in the artifact) so Play can
de-obfuscate crash reports.

Store listing text, graphics, the privacy policy and Data safety answers are in
[`docs/play-store/`](play-store/). The privacy policy needs a public URL. The
simplest option is GitHub Pages, or the file's GitHub link.
