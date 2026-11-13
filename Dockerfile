# Build image with: 
#   `docker build -t pasda .`
# Run container with:
#   `docker run -it pasda /bin/sh`
# Check the example pair with:
#   `docker run --rm pasda ./CheckPair.sh bank/loan/NEq`
# Run benchmarks with:
#   `./RunningBenchmarks.sh`
FROM --platform=amd64 ubuntu:22.04

RUN apt-get update -y && apt-get install -y \
    openjdk-8-jdk \
    git \
    jq

COPY docker/sqlean/sqlean-ubuntu /usr/bin/sqlite3
RUN chmod +x /usr/bin/sqlite3

RUN mkdir -p /home/pasda

COPY benchmarks /home/pasda/benchmarks
COPY examples /home/pasda/benchmarks
COPY Implementation /home/pasda/implementation

RUN mkdir -p /home/pasda/implementation/analysis/results
RUN chmod +x /home/pasda/implementation/RunningBenchmarks.sh /home/pasda/implementation/CheckPair.sh /home/pasda/implementation/gradlew

WORKDIR /home/pasda/implementation

# RunningBenchmarks.sh and CheckPair.sh build the JAR files when they are missing.
# Building them here lets a container start its analysis right away.
RUN ./gradlew -PmainClass=Runner.Runner shadowJar \
    && ./gradlew -PmainClass=differencing.DifferencingRunner shadowJar
