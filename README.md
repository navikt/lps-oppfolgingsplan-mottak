# lps-oppfolgingsplan-mottak

## General information
<hr>

Maskinporten Delegated API for:

- Submitting a follow-up plan to NAV on behalf of an employer.
- Checking the sending status after sharing the follow-up plan with NAV and/or a general practitioner.

The endpoint for submitting a follow-up plan will return a unique uuid. This uuid can be used later to check the sending status (sent or not sent).

#### Interchangeable terms and abbreviations in the project

+ "oppfølgingsplan", "followup plan", "lps plan" means same in terms of this project
+ "general practitioner", "fastlege"  means same in terms of this project
+ LPS: "Lønn- og personalsystem", external software operating on behalf of employer (for example sending followup plan
  to NAV)

### Demo

When the follow-up plan is shared with NAV and/or a general practitioner 
(e.g., when `sendPlanToNav` and/or `sendPlanToGeneralPractitioner`  <br> 
are set to true in the submit request), the API will generate a PDF file to be sent to NAV and/or the general practitioner. <br>

Please check our [demo solution](https://demo.ekstern.dev.nav.no/oppfolgingsplan-lps) to see how the resulting PDF will
appear (actual styling may differ). <br>

When the follow-up plan is shared with NAV, the resulting PDF is accessible to the employee on sick leave at
https://person.nav.no/dokumentarkiv/tema/OPP (login required).


## Technical
<hr>

### 🚀 Initial setup

- Install and configure the [Detect IDEA plugin](https://plugins.jetbrains.com/plugin/10761-detekt) for live code
  analysis
- Install the [Kotest IDEA plugin](https://plugins.jetbrains.com/plugin/14080-kotest) to enable test-runs in IDEA
- Set [target JVM version](https://www.jetbrains.com/help/idea/compiler-kotlin-compiler.html#kotlin-compiler-jvm-settings) to 19

### 🤖 Maskinporten

You will need to configure [Maskinporten](https://docs.digdir.no/docs/Maskinporten/maskinporten_summary.html) 
in order to be able to operate on behalf of employer. <br>

### 🗺️ Documentation and useful links

| Description          | Url                                                                |
|----------------------|--------------------------------------------------------------------|
| Testing library      | https://kotest.io/                                                 |
| Mocking library      | https://mockk.io/                                                  |
| Static code analysis | https://detekt.dev/                                                |
| Maskinporten         | https://docs.digdir.no/docs/Maskinporten/maskinporten_summary.html |
| Swagger test         | https://lps-oppfolgingsplan-mottak.ekstern.dev.nav.no/swagger      |
| Demo application     | https://demo.ekstern.dev.nav.no/oppfolgingsplan-lps                |

## Contact

Please write an email to **team-esyfo@nav.no** if you have questions about an API.
