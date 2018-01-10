
FROM openjdk:8-jre-alpine
EXPOSE  8463
ADD /target/accounts-1.00.00.jar server.jar
CMD ["/bin/sh","-c","java -Dlight-4j-config-dir=/config -Dlogback.configurationFile=/config/logback.xml -jar /server.jar"]
