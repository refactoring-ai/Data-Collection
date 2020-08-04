FROM openjdk:11
VOLUME /Data-Collection
VOLUME /storage
WORKDIR /Data-Collection

ENTRYPOINT ["sh", "run-cloud.sh"]
CMD []