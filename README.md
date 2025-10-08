# 🚀 Distributed Rate Limiting System using Redis & Spring Cloud Gateway

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Redis](https://img.shields.io/badge/Redis-7.x-red)
![Bucket4j](https://img.shields.io/badge/Bucket4j-8.x-yellow)
![License Service](https://img.shields.io/badge/Service-License%20Service-orange)
![Status](https://img.shields.io/badge/Status-Active-success)

---

## 🧠 Overview

This project implements a **distributed rate-limiting system** using:

- **Spring Cloud Gateway** for API routing  
- **Bucket4j** for rate-limiting logic  
- **Redis** as a shared distributed cache  
- **License Service** as an example backend microservice  

The system ensures **fair and consistent request throttling** across multiple gateway instances using a centralized Redis backend.

---

## 🏗️ System Architecture

Below is the high-level architecture of the system (you can replace this with your own `draw.io` diagram):

![System Architecture](./docs/system-architecture.png)

**Components:**
1. **API Gateway** – Intercepts incoming requests and applies distributed rate limiting.
2. **Redis** – Stores token bucket states to synchronize rate limits across gateways.
3. **License Service** – A backend microservice used to demonstrate rate limiting.
4. **Clients** – Send API requests through the gateway.

---

## ⚙️ Key Features

✅ Distributed rate limiting using **Bucket4j + Redis**  
✅ Works across multiple **API Gateway** instances  
✅ Supports both **per-user** and **per-route** rate-limiting rules  
✅ Fully **reactive** (non-blocking I/O) setup  
✅ Ready for **containerized deployment** with Docker Compose  

---

## 🧩 Tech Stack

| Layer | Technology |
|-------|-------------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Gateway | Spring Cloud Gateway |
| Rate Limiting | Bucket4j |
| Cache / Storage | Redis 7.x |
| Build Tool | Gradle / Maven |
| Container | Docker & Docker Compose |

---

## 🐳 Docker Setup

You can run the entire environment using **Docker Compose**.

```yaml
version: "3.8"

services:
  redis:
    image: redis:7
    container_name: leads-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    networks:
      - monitoring-net

networks:
  monitoring-net:
    driver: bridge
