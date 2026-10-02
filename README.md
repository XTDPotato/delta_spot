# Delta Spot

Delta Spot is a standalone NeoForge 1.21.1 tactical marker mod extracted from
Xero Delta. Middle-click places a marker, a rapid second click at the same
position upgrades it to an enemy marker, and holding middle-click opens the
radial marker wheel.

## Compatibility

- Xero Delta is optional. When installed, Delta Spot respects Xero Delta's
  downed/carry interaction lock and uses its team integration where available.
- FTB Teams is optional. Markers are shared only with members of the same team;
without a team, markers remain visible to their owner.

## Installation

Install Minecraft 1.21.1 with NeoForge 21.1.233 or newer. Download
`delta_spot-0.1.0.jar` from this repository's Releases and place it in the `mods`
directory on both the client and server.

## Building

Use JDK 21 and set `JAVA_HOME` to its installation directory.

On Windows:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 build
```

On Linux or macOS:

```sh
./gradlew --no-daemon --max-workers=1 build
```

The built JAR is written to `build/libs/`.

## License

Copyright (c) 2026 xtdpotato. Released under the MIT License.
