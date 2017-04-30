FROM docker-registry.intr/user/javabox
ADD ./"${jarBuildName}" app.jar
RUN sh -c 'touch /app.jar'
RUN adduser -u 32150 -D rc-user
USER rc-user
ENTRYPOINT [ ${entryPoint} ]
