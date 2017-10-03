FROM tomee:8-jre-7.0.3-plus
MAINTAINER Sébastien Mosser (mosser@i3s.unice.fr)

# Build with : docker build -t petitroll/holidays .
# Publish with: docker push petitroll/holidays

WORKDIR /usr/local/tomee/

RUN apt-get  update \
      && apt-get --no-install-recommends install -y openjdk-8-jdk \
      && rm -rf /var/lib/apt/lists/*


RUN wget https://github.com/flowable/flowable-engine/releases/download/flowable-6.1.2/flowable-6.1.2.zip\
      && unzip flowable-6.1.2.zip \
      && cp ./flowable-6.1.2/wars/flowable-rest.war ./webapps/.

RUN cd ./webapps \
    && mkdir flowable-rest \
    && cd flowable-rest \
    && jar xf ../flowable-rest.war \
    && cd .. \
    && rm -rf ./flowable-rest.war \
    && cd ..

COPY ./target/flowable-demo-1.0-SNAPSHOT.jar ./webapps/flowable-rest/WEB-INF/lib/custom-classes.jar

HEALTHCHECK --interval=5s CMD curl --fail http://localhost:8080/ || exit 1

EXPOSE 8080