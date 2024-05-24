# LPS follow-up plan API: Consumer guide

This API allows for the submission of a [follow-up plan (oppfølgingsplan)](https://www.nav.no/arbeidsgiver/oppfolgingsplan) to NAV and/or the general practitioner on behalf of an employer.
This is a <i>Delegated
API</i>, which means that you as the API consumer are acting on behalf of another company/end-user. The API is secured
with Maskinporten. NAV does not authorize the API consumer directly. Authorization to submit a plan is given to you as an API consumer by the end-user in Altinn.

## 🎯 Target audience

This README is primarily intended for "Lønns- og personalsystemer" (Payroll and HR Systems) that wants to integrate with NAV for submitting follow-up plans.

## 🚀 Getting started

### 1. The employer must delegate rights to act on their behalf

To submit a follow-up plan on behalf of an employer, the employer must delegate rights to the API (scope) to you as an
LPS system. This is done
in Altinn. Please refer to
the [Altinn documentation](https://altinn.github.io/docs/utviklingsguider/api-delegering/tilgangsstyrer/). Please note
that the employer can find our API by searching for <i>"Oppfølgingsplan"</i> in Altinn, in the menu for <i>"Tilgang til
programmeringsgrensesnitt - API"</i>.

### 2. Configure a maskinporten-client

In order to use the API, you need to have a Maskinporten client configured. Please refer to
the [Maskinporten documentation](https://docs.digdir.no/docs/Maskinporten/maskinporten_guide_apikonsument).
Please take note that this is a so called <i>Delegated API</i>, which means that you are acting on behalf of an
employer. This must be configured in your client according to the Maskinporten documentation.

### 3. Retrieve a Maskinporten token on behalf of the employer

To retrieve a Maskinporten token on behalf of the employer, you need to send a POST request to the Maskinporten token
endpoint. Please refer to
the [Maskinporten documentation](https://docs.digdir.no/docs/Maskinporten/maskinporten_summary.html) for more
information.
<br>
- Scope to be used when requesting token: `nav:oppfolgingsplan/lps.write`

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

## 🧪 Testing
Please refer to [NAV's guide for testing delegable API's](https://github.com/navikt/nav-ekstern-api-dok/blob/main/api-dok/teste-delegerbart-api.md)

## 🎬 Demo

We have created a demo-app, which acts as an example of how you can implement the API. Here you will find relevant
input-fields with headers and descriptions for our API. When you fill out the demo-form, it will finally retrieve a
maskinporten-token on behalf of a random test-user, and submit this to our API on our test server. On the last page you
will be able to download a PDF, which looks similar to the PDF generated in production. <br>

Please note that the demo app only showcases the fields relevant for the API. This means that you are not limited to
these fields (and functionality) in your own application! We encourage you to customize the form to fit your own needs.
For example could `sykmeldingsgrad` be a relevant field for leaders, however NAV does not need this information, and it
is therefore not included in the API. <br>

- [Link to demo app](https://demo.ekstern.dev.nav.no/oppfolgingsplan-lps)
- [Link to demo repository](https://github.com/navikt/oppfolgingsplan-lps-demo)

## ✉️ Contact

Please write an email to **team-esyfo@nav.no** if you have questions about the API.
Questions about maskinporten or Altinn must be directed to Digdir/Altinn.

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