# MathWake Android

MathWake is a native Android alarm app written in Java. It forces you to solve a math problem before an alarm can be dismissed.

## What’s included

- Persistent alarm list stored with `SharedPreferences`
- Exact alarm scheduling with `AlarmManager.setAlarmClock`
- Boot/package-replaced rescheduling
- Foreground ringing service with default Android alarm sound + vibration
- Full-screen ring activity
- Math-gated dismissal flow
- Add/edit alarms
- Toggle enable/disable
- Long-press delete from the alarm list
- Native test alarm scheduling
- Java-only Android source, with no Flutter, Kotlin, or Jetpack Compose layer

## Project structure

```
app/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/mathwake/android/
    │   ├── MainActivity.java
    │   ├── alarm/
    │   ├── data/
    │   ├── math/
    │   ├── model/
    │   └── ui/
    └── res/
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Open and run

1. Open this folder in Android Studio as a standalone Android project.
2. Use JDK 17 for Gradle.
3. Run the app on an Android device.
4. Grant notification permission and exact alarm permission when prompted.

## Important device notes

- Exact alarms may still require system approval on Android 12+.
- Some OEMs need battery optimization disabled for fully reliable alarm behavior.
- The test button schedules a real native alarm for a few seconds later.
