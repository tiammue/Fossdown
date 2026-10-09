# Fossdroid

Free and open-source countdown and progress tracker for Android.

Track deadlines, habits, and milestones with day/week/hour remaining labels, customizable progress styles (bar, dots, circle, segments), and a Home screen widget.

## Features

- Countdown, count-up, and timer events
- Display units: days, weeks (optional 1-decimal), hours, days+hours, percent
- Show up to three remaining lines on the card and widget
- Progress styles: bar, dots, circle, segments
- Themes, accent colors, and Home screen widgets
- In-app **Check for updates** via [GitHub Releases](https://github.com/tiammue/Fossdroid/releases)

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Requires JDK 17+.

### Version overrides

```bash
./gradlew assembleRelease -PversionName=1.2.0 -PversionCode=10200
```

## Releases

Push a version tag to publish a GitHub Release with an APK attachment:

```bash
git tag v1.1.0
git push origin v1.1.0
```

GitHub Actions (`.github/workflows/release.yml`) builds the release APK and creates the release automatically. CI on `main` runs unit tests and a debug build (`.github/workflows/ci.yml`).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
