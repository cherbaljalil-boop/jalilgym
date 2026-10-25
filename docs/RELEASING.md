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

1. Bump `versionCode` (always) and `versionName` in `app/build.gradle.kts`, and
   the same in `wear/build.gradle.kts`. The watch's code stays 1,000,000 above the
   phone's so the two never collide on Play.
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
   food search (airplane mode). With a paired watch, start a workout on the
   phone and log a few sets from the watch, including during a rest timer.

## 4. Upload

Upload the `.aab` to the internal testing track first, then promote. The watch
app (`wear-release.aab`) goes into the same Play app: Release → Form factors →
add Wear OS, then upload it to that form factor's track. It is signed with the
same key, which the Data Layer requires. Play also needs Wear OS screenshots and
a Wear OS review opt-in. Keep the
`mapping.txt` from the same CI run (it's in the artifact) so Play can
de-obfuscate crash reports.

Store listing text, graphics, the privacy policy and Data safety answers are in
[`docs/play-store/`](play-store/). The privacy policy needs a public URL. The
simplest option is GitHub Pages, or the file's GitHub link.
