# Heroku Replay

This application funnels production log drains into Kafka, and then replays the requests represented in those logs
against a staging application.

## Usage

Locally with Docker, run:

```
$ docker-compose run shell
...
heroku@03248675cfea:~$ mvn clean package
```

In other terminal run:

```
$ docker-compose up web
```

Again in the `shell` session run cURL to test it:

```
heroku@03248675cfea:~$ cat << EOF > logs.txt
> 242 <158>1 2016-06-20T21:56:57.107495+00:00 host heroku router - at=info method=GET path="/" host=demodayex.herokuapp.com request_id=1850b395-c7aa-485c-aa04-7d0894b5f276 fwd="68.32.161.89" dyno=web.1 connect=0ms service=6ms status=200 bytes=1548
> EOF
heroku@03248675cfea:~$ curl -d @logs.txt web:8080/logs
```

## Deploying

To deploy on Heroku run:

```
$ heroku create mylogdrain
$ heroku addons:create heroku-kafka
$ heroku plugins:install heroku-kafka
$ heroku kafka:wait
$ heroku config:set REPLAY_HOST="http:://example.com"
$ git push heroku master
$ heroku ps:scale replay=1
$ heroku drains:add https://user:pass@mylogdrain.herokuapp.com/logs -a myapp
```