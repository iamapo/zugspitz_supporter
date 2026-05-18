# Zugspitz Supporter

Zugspitz Supporter is a Kotlin Multiplatform app for race-day crews supporting a runner during the Zugspitz Ultratrail. The app is designed as a fast, glanceable coordination tool that helps supporters answer a simple question:

When should we be at the next VP, and what should we prepare there?

## What the app does

- Lets supporters choose an expected finish range and race start time.
- Projects likely arrival times for the course checkpoints and VPs.
- Shows the current VP in a focused card view with race context.
- Allows quick check-ins with actual arrival times.
- Recalculates future VP estimates after each check-in.
- Provides a dense VP list view for scanning the whole race plan.

## Main screens

### Setup

Supporters define the expected finish duration and start time. The app uses that input to calculate projected arrival windows.

### VP Card

The main race-day screen. It highlights a selected VP and shows timing, distance, elevation, and context for the next section.

### Check-in

Supporters can record the actual arrival time at a VP with minimal friction. Future predictions shift based on the delta between planned and actual timing.

### VP List

A full checkpoint overview with completed and upcoming VPs in one place.

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform
- Android target with Jetpack Compose tooling
- iOS target via Kotlin/Native framework output

## Project structure

- `composeApp/` shared UI, Android app setup, and business logic
- `iosApp/` iOS entrypoint
- `design/` mockups and product/design notes

## Build targets

Current Gradle configuration includes:

- Android
- iOS (`iosX64`, `iosArm64`, `iosSimulatorArm64`)

## Getting started

### Prerequisites

- JDK 17
- Android Studio with Kotlin Multiplatform support
- Xcode, if you want to run the iOS target

### Run on Android

```bash
./gradlew :composeApp:assembleDebug
```

Open the project in Android Studio to run the Android app or use Compose previews.

### Configure Supabase live sharing

Debug live sharing now uses Supabase instead of Firebase.

Add these values to `local.properties` for Android builds:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY
```

For iOS debug builds, either set `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`
as environment variables in the Xcode scheme, or fill the matching generated
Info.plist keys in the Xcode project.

Apply the schema and RLS policies from `supabase/live_sharing.sql` to your
Supabase project before using live sharing.

### Run on iOS

Open the `iosApp` project in Xcode after syncing the Gradle project and building the shared framework as needed from the Kotlin Multiplatform setup.

## Design direction

The UI is intended to feel calm, readable, and practical in outdoor race conditions, including poor light and high-pressure support moments. The product is optimized for supporters rather than runners.

## Status

This repository currently contains the first app structure, core screens, shared race projection logic, and design exploration assets.
