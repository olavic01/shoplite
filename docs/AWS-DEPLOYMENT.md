# Deploying ShopLite on AWS (3 servers)

## 1. Network
One VPC, one public subnet is enough for practice. All three servers sit in it and talk over **private IPs**.
Ubuntu 22.04 or 24.04, with an Elastic IP on each so addresses survive stop/start.

| Server | Size | Disk | Runs |
|---|---|---|---|
| shoplite-app | t3.large (8 GB) | 30 GB | `deploy/` stack |
| shoplite-jenkins | t3.medium (4 GB) | 30 GB | `jenkins/` stack |
| shoplite-obs | t3.medium (4 GB) | 30 GB | `observability/` stack |

## 2. Security groups (least privilege)
Create three groups: `sg-app`, `sg-jenkins`, `sg-obs`. "My IP" means your own public IP /32.

**sg-app inbound**
| Port | From | Why |
|---|---|---|
| 22 | My IP, `sg-jenkins` | SSH and deploys |
| 80 | 0.0.0.0/0 (or My IP) | the website |
| 8081-8086, 8088, 9100 | `sg-obs` | Prometheus scraping (actuator, cAdvisor, node exporter) |

**sg-jenkins inbound**: 22 and 8080 from My IP.
**sg-obs inbound**: 22 from My IP; 3000 (Grafana), 9090 (Prometheus), 9093 (Alertmanager) from My IP; 3100 (Loki) from `sg-app`.

Why is this safe? The actuator ports are only reachable from the observability server, and Loki only accepts logs from the app server.

## 3. Install Docker on each server
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu && newgrp docker
docker compose version
```

## 4. Observability server (do this first)
```bash
git clone <your-repo> shoplite && cd shoplite/observability
cp .env.example .env     # APP_HOST = app server PRIVATE IP, set a Grafana password
docker compose up -d
```
Open `http://<obs-public-ip>:3000` (user `admin`). The "ShopLite overview" dashboard is already loaded. Panels stay empty until the app runs.

## 5. App server
```bash
git clone <your-repo> shoplite && cd shoplite/deploy
cp .env.example .env     # set POSTGRES_PASSWORD, JWT_SECRET, LOKI_HOST = obs server PRIVATE IP
docker compose up -d --build
docker compose ps        # wait until everything is Up / healthy (first build takes several minutes)
```
Open `http://<app-public-ip>`. In Grafana, check **Status > Targets** in Prometheus (`:9090/targets`): all should be UP.

## 6. Jenkins server
```bash
git clone <your-repo> shoplite && cd shoplite/jenkins
docker compose up -d --build
docker exec jenkins-jenkins-1 cat /var/jenkins_home/secrets/initialAdminPassword
```
Open `http://<jenkins-public-ip>:8080`, finish the wizard with the suggested plugins, then add the **SSH Agent** plugin.

1. **Credentials**
   - `app-server-ssh`: SSH username with private key (user `ubuntu`). Put the matching public key in `~/.ssh/authorized_keys` on the app server.
   - `shoplite-env`: Secret file containing the same content as `deploy/.env`.
2. **Global variable**: Manage Jenkins > System > Environment variables: `APP_HOST` = app server private IP.
3. **Job**: New Item > Multibranch Pipeline > add your Git repo. It finds the `Jenkinsfile`.

Branch behaviour: feature branches and PRs build and test only; `develop` deploys automatically; `main` waits for your approval.
(With one app server, both deploy to the same place. When you want separate dev and prod, add a fourth server and a second `APP_HOST`.)

## 7. Git flow to practise
1. `git checkout -b feature/add-search develop`, commit, push, open a PR into `develop`.
2. Watch Jenkins build the PR. Merge. Watch `develop` deploy.
3. Open a PR `develop` -> `main`, merge, approve the deploy in Jenkins.
4. Turn on branch protection for `develop` and `main` (PR required, build must pass).

## 8. Alerts
`observability/alertmanager.yml` sends to a placeholder webhook. Create a free URL at webhook.site, paste it in, then `docker compose restart alertmanager` and trigger an alert (drill 1 below).

## 9. Break-things drills
| # | Do this | Look here |
|---|---|---|
| 1 | `docker stop shoplite-order-service-1` | Alert `ServiceDown` after ~1 min; UI shows order errors; Loki logs from the gateway |
| 2 | `docker stop shoplite-kafka-1`, place orders, then start it again | Order errors, then Kafka consumer lag panel catching up |
| 3 | Set `SLOW_MS=800` in `.env`, `docker compose up -d product-service`, flush cache with `docker exec shoplite-redis-1 redis-cli flushall` | p95 latency panel and `SlowRequestsP95` alert; cache hit/miss panel |
| 4 | Change `POSTGRES_PASSWORD` in `.env` only, recreate `user-service` | Auth failure in logs (like your Oracle connection issue). Find it in Loki: `{service="user-service"} \|= "password"` |
| 5 | Wrong password 20 times on the login form | `LoginFailureSpike` alert, failed-logins panel |
| 6 | Order 101 of one product (stock is 100) | `inventory_rejected_total` and the warning in logs |
| 7 | `docker run --rm -it --network host busybox sh -c "dd if=/dev/zero of=/tmp/x bs=1M count=2000"` style disk fill, or `stress` for CPU | Node exporter panels, `HighCpu` / `DiskAlmostFull` |
| 8 | Merge a bad commit to `develop` | Pipeline fails; practise rollback with `IMAGE_TAG=build-<old number> docker compose up -d` |

Useful Loki queries (Grafana > Explore > Loki):
- `{service="order-service"}` all logs for one service
- `{level="ERROR"}` all errors
- `{service="api-gateway"} |= "Rejected"` rejected requests

## 10. Cost control
Stop the three instances when you finish for the day (EBS keeps your data; Elastic IPs cost a little while unattached to a running instance). Use an AWS Budget alert so a forgotten instance does not surprise you.
