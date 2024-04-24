# LPS follow-up plan API: Consumer guide

This API allows for the submission of a follow-up plan to NAV on behalf of an employer. This API is a <i>Delegated
API</i>, which means that you as the API consumer is acting on behalf of another company/end-user. The API is secured
with Maskinporten.

When a follow-up plan is submitted, the API will generate a PDF file to be shared with NAV and/or the general
practitioner, depending on the input from `sendPlanToNav` and `sendPlanToGeneralPractitioner`. When `sendPlanToNav` is set
to true, the resulting PDF is accessible to the employee on sick leave at
https://person.nav.no/dokumentarkiv/tema/OPP (login required). If it is set to false, the follow-up plan will not be
shared with NAV, and the PDF will not be added to the NAV website. When `sendPlanToGeneralPractitioner` is set to true,
the resulting PDF will be sent to the general practitioner.

## 🚀 Getting started

### 1. Configure a maskinporten-client

In order to use the API, you need to have a Maskinporten client configured. Please refer to
the [Maskinporten documentation](https://docs.digdir.no/docs/Maskinporten/maskinporten_summary.html)
Please take not that this is a so called <i>"Delegated API"</i>, which means that you are acting on behalf of an
employer. You
can find technical documentation on how to configure the delegation-part
here: [Maskinporten delegation](https://docs.digdir.no/docs/Maskinporten/maskinporten_guide_apikonsument#bruke-delegering-som-leverand%C3%B8r)

### 2. The employer must delegate rights to act on their behalf

To submit a follow-up plan on behalf of an employer, the employer must delegate rights to the API (scope). This is done
in Altinn. Please refer to
the [Altinn documentation](https://altinn.github.io/docs/utviklingsguider/api-delegering/tilgangsstyrer/). Please note
that the employer can find our API by searching for <i>"Oppfølgingsplan"</i> in Altinn, in the menu for <i>"Tilgang til
programmeringsgrensesnitt - API"</i>.

### 3. Retrieve a Maskinporten token on behalf of the employer

To retrieve a Maskinporten token on behalf of the employer, you need to send a POST request to the Maskinporten token
endpoint. Please refer to
the [Maskinporten documentation](https://docs.digdir.no/docs/Maskinporten/maskinporten_summary.html) for more
information.

### 4. Submit a follow-up plan

To submit a follow-up plan, you need to send a POST request with the required payload. The API will return a unique
uuid, which can be used later to check the sending status. Please refer to
the [Swagger documentation](https://lps-oppfolgingsplan-mottak.ekstern.dev.nav.no/swagger) for more information. Please
note that you will need to provide a valid Maskinporten token in the Authorization header.
<br>

- Test API: `https://lps-oppfolgingsplan-mottak.ekstern.dev.nav.no/api/v1/followupplan`
- Production API: `https://lps-oppfolgingsplan-mottak.nav.no/api/v1/followupplan`

### 5. Check the sending status (optional)

To check the sending status of a follow-up plan, you need to send a GET request with the uuid you received when
submitting the follow-up plan. Please refer to
the [Swagger documentation](https://lps-oppfolgingsplan-mottak.ekstern.dev.nav.no/swagger) for more information. Please
note that you will need to provide a valid Maskinporten token in the Authorization header.
<br>

- Test API: `https://lps-oppfolgingsplan-mottak.ekstern.dev.nav.no/api/v1/followupplan/{uuid}/sendingstatus`
- Production API: `https://lps-oppfolgingsplan-mottak.nav.no/api/v1/followupplan/{uuid}/sendingstatus`

## 🧪 Testing guide

We have a step-by-step guide on how to test a delegated API (in our test environment), providing a bit more detail than
the steps above. Please refer to the [Testing Guide](./TestingGuide.pptx)

## 🎬 Demo

We have created a demo-app, which acts as an example of how you can implement the API. Here you will find relevant
input-fields with headers and descriptions for our API. When you fill out the demo-form, it will finally retrieve a
maskinporten-token on behalf of a random test-user, and submit this to our API on our test server. On the last page you
will be able to download a PDF, which looks similar to the PDF generated in production. <br>

Please note that the demo app only showcases the fields relevant for the API. This means that you are not limited to these fields (and functionality) in your own application! We encourage you to customize the form to fit your own needs. For example could `sykmeldingsgrad` be a relevant field for leaders, however NAV does not need this information, and it is therefore not included in the API. <br>

- [Link to demo app](https://demo.ekstern.dev.nav.no/oppfolgingsplan-lps)
- [Link to demo repository](https://github.com/navikt/oppfolgingsplan-lps-demo)

## 🔄 Interchangeable terms and abbreviations in the project

+ "oppfølgingsplan", "followup plan", "lps plan" means the same in terms of this project
+ "general practitioner", "fastlege"  means the same in terms of this project
+ LPS: "Lønn- og personalsystem", external software operating on behalf of employer (for example sending followup plan
  to NAV)

## ✉️ Contact

Please write an email to **team-esyfo@nav.no** if you have questions about the API.
Please note that questions about maskinporten or Altinn must be directed to Digdir/Altinn.

<br>
<details>
<summary><b>🛠️🛠️ For NAV Developers 🛠️🛠️</b></summary>

## Technical

<hr>

### 🚀 Initial setup

- Install and configure the [Detect IDEA plugin](https://plugins.jetbrains.com/plugin/10761-detekt) for live code
  analysis
- Install the [Kotest IDEA plugin](https://plugins.jetbrains.com/plugin/14080-kotest) to enable test-runs in IDEA
-

Set [target JVM version](https://www.jetbrains.com/help/idea/compiler-kotlin-compiler.html#kotlin-compiler-jvm-settings)
to 19

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

</details>