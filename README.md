# lps-oppfolgingsplan-mottak
Application exposing an external API used by LPS-system (lønn -og personalsystem) to publish
"oppfølgingsplaner" for internal record keeping in NAV (jounalføring).

## 🚀 Initial setup
- Install and configure the [Detect IDEA plugin](https://plugins.jetbrains.com/plugin/10761-detekt) for live code analysis
- Install the [Kotest IDEA plugin](https://plugins.jetbrains.com/plugin/14080-kotest) to enable test-runs in IDEA
- Set [target JVM version](https://www.jetbrains.com/help/idea/compiler-kotlin-compiler.html#kotlin-compiler-jvm-settings) to 19

## 🔎 Code analysis
This project uses [Detect](https://detekt.dev/) for static code analysis, as part of the gradle build process.

## 🧪 Testing
| Testing framework  | Mocking library   |
|--------------------|-------------------|
| https://kotest.io/ | https://mockk.io/ |


