FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /build
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && rm -rf /var/lib/apt/lists/*
ARG SBT_VERSION=1.12.5
RUN curl -fsSL "https://repo.maven.apache.org/maven2/org/scala-sbt/sbt-launch/${SBT_VERSION}/sbt-launch-${SBT_VERSION}.jar" -o /opt/sbt-launch.jar
COPY . .
RUN java -Xmx1536m -Dsbt.supershell=false -jar /opt/sbt-launch.jar test stage

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
RUN groupadd --system flicklytics && useradd --system --gid flicklytics flicklytics
COPY --from=build --chown=flicklytics:flicklytics /build/target/universal/stage/ /app/
USER flicklytics
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:ActiveProcessorCount=2 -XX:+UseSerialGC"
EXPOSE 10000
CMD ["sh", "-c", "exec bin/play_web_application -Dhttp.address=0.0.0.0 -Dhttp.port=${PORT:-10000} -Dpidfile.path=/tmp/flicklytics.pid"]
