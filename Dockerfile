FROM gcr.io/distroless/java21
COPY build/libs/*-all.jar app.jar
ENV JDK_JAVA_OPTIONS='-Dlogback.configurationFile=logback.xml'
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
