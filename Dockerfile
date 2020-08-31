FROM openjdk:8-jre-slim

ENV XMS 512M
ENV XMX 2048M
ENV XMN 384M
ENV DEBUG ""
#ENV DEBUG "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6006"
ENV TZ Europe/Moscow

RUN apt-get update \
 && apt-get install -y curl jq putty-tools \
 && apt-get autoremove -y && apt-get clean && rm -rf /var/cache/apt/archives/* /var/lib/apt/lists/* /usr/share/doc/* /usr/share/man/* \
 && curl -s -o /tmp/root.crt http://archive.intr/Majordomo_LLC_Root_CA.crt \
 && keytool -trustcacerts -keystore /usr/local/openjdk-8/lib/security/cacerts -storepass changeit -alias Root -import -file /tmp/root.crt -noprompt

COPY ./build/libs/*rc-user*jar /

COPY healthcheck.sh /healthcheck.sh
ONBUILD HEALTHCHECK --interval=10s --timeout=10s --retries=3 CMD /healthcheck.sh

ENTRYPOINT [ "/bin/bash", "-c" ,"exec java -Xms${XMS} -Xmx${XMX} -Xmn${XMN} ${DEBUG} -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -jar /*.jar" ]
