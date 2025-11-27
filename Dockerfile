FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21
COPY build/libs/*-all.jar app.jar
ENV JDK_JAVA_OPTIONS='-Dlogback.configurationFile=logback.xml'
ENV TZ="Europe/Oslo"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
