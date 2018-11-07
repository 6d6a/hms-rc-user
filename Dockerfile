FROM docker-registry.intr/base/javabox:master

ENV XMS 512M
ENV XMX 2048M
ENV XMN 384M
ENV DEBUG ""
#ENV DEBUG "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6006"

COPY ./build/libs/*rc-user*jar /

ENTRYPOINT [ "/entrypoint.sh" ]
