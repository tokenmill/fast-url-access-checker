FROM tokenmill/clojure:graalvm-ce-19.0.0-tools-deps-1.10.0.442 as builder

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY deps.edn /usr/src/app/
RUN clojure -R:native-image
COPY . /usr/src/app
RUN clojure -A:native-image
RUN cp $JAVA_HOME/jre/lib/amd64/libsunec.so .
RUN cp target/app url-checker
