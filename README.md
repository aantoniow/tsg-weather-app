# tsg-weather-app
Recruitment rest api application, which uses netty and java21+


to make it work
`docker compose -f docker/docker-compose.yml --force-rebuild --build`

a następnie by przetestować
`curl http://localhost:8080/api/dashboard`