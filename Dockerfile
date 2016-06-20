FROM maven:3-jdk-8

RUN apt-get update && apt-get install -y git-all

RUN mkdir -p /app && useradd -d /home heroku
USER heroku

RUN mkdir -p /app
ENV HOME /app
WORKDIR /app
