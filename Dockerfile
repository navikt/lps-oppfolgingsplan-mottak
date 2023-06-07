FROM navikt/java:19
LABEL org.opencontainers.image.source=https://github.com/navikt/lps-oppfolgingsplan-backend
COPY build/libs/*.jar app.jar
