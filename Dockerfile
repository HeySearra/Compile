FROM openjdk:11
WORKDIR /app/
COPY ./* ./
RUN javac App.java -encoding utf-8