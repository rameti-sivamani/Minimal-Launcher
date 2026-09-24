# Play Store Release Checklist

Everything needed to take Minimalist Launcher from this repo to a Google Play production release.

## 1. One-time decisions (cannot be changed after the first upload)

- [ ] **Application ID** — `app/build.gradle.kts` → `applicationId`. It is currently `com.minimalist.launcher`, which is generic and may already be taken on Play. Pick a unique one (e.g. `com.yourname.minimal`) *before* the first upload. The Java package / `namespace` can stay as is.
- [ ] **App name** — `app_name` in `res/values/strings.xml`. Check Play for existing apps with the same name.

## 2. Signing

1. Create an upload key once and keep it safe (back up the file **and** passwords):
   ```bash
   keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Copy `keystore.properties.example` to `keystore.properties` (git-ignored) and fill it in.
3. In Play Console, enable **Play App Signing** (default for new apps). Google holds the app signing key; you only sign uploads.

Without `keystore.properties` the release build is **unsigned** (never debug-signed) and Play will reject it. That is intentional.

## 3. Build

```bash
./gradlew testDebugUnitTest lintRelease
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
# R8 mapping: app/build/outputs/mapping/release/mapping.txt (upload it to Play for readable crash reports)
```

Before every release, bump `versionCode` (must increase) and `versionName` in `app/build.gradle.kts`.

Room schemas are exported to `app/schemas/`. **Commit that folder.** When the database changes, bump the version in `AppDatabase` and add a `Migration`. There is intentionally no destructive fallback, so users never lose their focus modes.

## 4. Play Console setup

- [ ] **Privacy policy URL** — host `PRIVACY_POLICY.md` publicly (GitHub Pages or a public repo link) and paste the URL in Play Console **and** in `privacy_policy_url` in `strings.xml`. The URL must be publicly reachable without a GitHub login, and the policy needs a real contact email filled in.
- [ ] **Data safety form**:
  - Data collected: **None** (all processing is on-device; the app has no INTERNET permission).
  - Data shared: **None**.
  - "App activity → App interactions / Other actions" is *accessed* for on-device screen time but not collected, so it doesn't need declaring as collected.
- [ ] **Permissions declaration**:
  - `PACKAGE_USAGE_STATS` — used for on-device screen time. The app shows a prominent in-app disclosure (onboarding and Settings) before sending users to grant it.
  - `QUERY_ALL_PACKAGES` is **not** used. Package visibility uses a `<queries>` block for launchable apps.
- [ ] **Content rating** questionnaire (expected: Everyone).
- [ ] **Target audience**: 13+ (not designed for children).
- [ ] **App category**: Personalization.
- [ ] **Ads**: No ads.

## 5. Store listing assets

- [ ] App icon 512×512 PNG (export from `ic_launcher_foreground.xml` on a black background).
- [ ] Feature graphic 1024×500.
- [ ] 4–8 phone screenshots: home screen, app list with search, settings, focus modes, launch warning dialog.
- [ ] Short description (≤80 chars), e.g. *"A calm, text-only home screen that helps you use your phone with intention."*
- [ ] Full description covering screen time, launch reminders, focus modes, hidden apps, and themes.

## 6. Testing track (required for new personal developer accounts)

New personal accounts must run a **closed test with at least 12 testers for 14 continuous days** before applying for production access. Check the current requirement in Play Console.

- [ ] Create a closed testing track and upload the AAB.
- [ ] Recruit testers (friends, communities such as r/androidapps or r/digitalminimalism).
- [ ] Fix crashes reported in **Android vitals** during the test.

## 7. Pre-launch manual test pass

Run on at least Android 10, one mid-range Samsung device, and Android 15/16:

- [ ] First launch: onboarding → usage access disclosure → set as default home.
- [ ] Home: clock follows the 12/24h system setting, date, screen time (with and without usage access).
- [ ] Swipe right/up opens the app list, swipe left opens the camera, tap the clock opens the app list.
- [ ] Search; Enter opens the top result.
- [ ] Long-press menus: favorite add/remove/reorder, app info, uninstall, hide/unhide.
- [ ] Launch warning after N launches (threshold setting), including from favorites.
- [ ] Every setting (theme, font size, icons, day of week, quick info, screen time, sort order).
- [ ] Focus modes: Deep Work, custom mode with app picker, edit, activate/deactivate, delete.
- [ ] Install and uninstall an app while the app list is open.
- [ ] Rotate the device and switch light/dark system theme with "Auto".
