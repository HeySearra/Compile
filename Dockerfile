FROM openjdk:11
WORKDIR /app/
COPY ./* ./
RUN javac src/App.java -encode utf-8