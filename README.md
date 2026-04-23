# tsg-weather-app
Application which uses netty and java21+, to provide simple 3 GET rest api answers. Supplied by redis to make them deliver despite of some downtime.

build and compile:
`docker compose -f docker/docker-compose.yml --force-rebuild --build`

to test the endpoint:
`curl http://localhost:8080/api/dashboard`
