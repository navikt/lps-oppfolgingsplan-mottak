FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:55d6597badfa521bc2cf6ea3383632118d109c37e3c827d4dbf365923b6d9b37
COPY build/libs/*-all.jar app.jar
ENV JDK_JAVA_OPTIONS='-Dlogback.configurationFile=logback.xml'
ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
