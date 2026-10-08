# ShopLite

A small order system built to practise DevOps: Docker, Docker Compose, Jenkins CI/CD, Kafka, Redis, Prometheus, Grafana, Loki and Git PR flow.

```
Browser -> nginx (frontend) -> api-gateway -> user / product / order services
                                                  order-service --Kafka--> inventory-service
                                                                      \--> notification-service
Postgres (4 databases) | Redis (product cache) | Kafka + Zookeeper
```

| Part | Tech | Host port (app server) |
|---|---|---|
| frontend + nginx | Angular 19 | 80 |
| api-gateway (JWT check, routing) | Spring Cloud Gateway | 8081 |
| user-service (register, login, JWT) | Spring Boot, Postgres | 8082 |
| product-service (catalog, Redis cache) | Spring Boot, Postgres, Redis | 8083 |
| order-service (places orders, publishes events) | Spring Boot, Postgres, Kafka | 8084 |
| inventory-service (reduces stock from events) | Spring Boot, Postgres, Kafka | 8085 |
| notification-service (order confirmations) | Spring Boot, Kafka | 8086 |

Every service exposes `/actuator/health` and `/actuator/prometheus`, and logs JSON to stdout.

## Run locally (one machine)
```bash
cd deploy
cp .env.example .env        # edit the passwords
docker compose up -d --build
```
Open http://localhost, create an account, and place an order. Watch the "Messages" panel: the confirmation arrives through Kafka a moment after you order.
The agents at the bottom of the compose file (promtail etc.) only matter once you have an observability server; set `LOKI_HOST` to any IP for now.

## Frontend dev mode
```bash
cd frontend && npm ci && npm start     # http://localhost:4200, proxies /api to localhost:8081
```

## Repo layout
```
services/        6 Spring Boot modules (Maven multi-module) + shared Dockerfile
frontend/        Angular app + nginx
deploy/          app-server compose, Postgres init, Promtail config
observability/   observability-server compose, Prometheus, alert rules, Alertmanager, Loki, Grafana dashboard
jenkins/         Jenkins-server compose + image with Maven and Docker CLI
Jenkinsfile      the pipeline
docs/            AWS deployment guide and practice drills
```
