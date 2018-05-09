FROM openjdk:8 AS BUILD_IMAGE

RUN wget -q https://github.com/vmk/rite/archive/master.zip
RUN unzip master.zip
RUN cd rite-master && ./gradlew build
RUN cd rite-master && ./gradlew installDist


FROM evidence/newsreaderdutch

COPY --from=BUILD_IMAGE /rite-master/build/install/rite /rite

ENTRYPOINT [ "/rite/bin/rite" ]
