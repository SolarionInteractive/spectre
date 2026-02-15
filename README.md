# Spectre

A server-side anticheat for Hytale, built to combat exploits and keep servers fair.

## Requirements

- Java 25
- Hytale Server

## Local Development

Build then run the local Hytale server with Spectre loaded:

```shell
clear && ./gradlew shadowJar && ./start_server.sh
```

Windows PowerShell:

```shell
Clear-Host; .\gradlew.bat shadowJar; if ($LASTEXITCODE -eq 0) { .\start_server.bat }
```

## Installation

1. Build the plugin: `./gradlew shadowJar`
2. Copy the JAR from `build/libs/` to your server's mods folder
3. Restart the server

## Contributing

We welcome contributions! Everything helps whether it be docs, code, or comments!

- [Submit a Pull Request](.github/pull_request_template.md)
- [Report a Bug](.github/ISSUE_TEMPLATE/bug_report.md)
- [Request a Feature](.github/ISSUE_TEMPLATE/feature_request.md)

## License

GNU Affero General Public License v3.0 — see [LICENSE](LICENSE) for details.