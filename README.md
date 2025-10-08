# 🚀 Distributed Rate Limiting System using Redis & Spring Cloud Gateway

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Redis](https://img.shields.io/badge/Redis-7.x-red)
![Kafka](https://img.shields.io/badge/Kafka-3.x-orange)
![Bucket4j](https://img.shields.io/badge/Bucket4j-8.x-yellow)
![License Service](https://img.shields.io/badge/Service-License%20Service-orange)
![Status](https://img.shields.io/badge/Status-Active-success)

---

## 🧠 Overview

This project implements a **distributed rate-limiting system** using:

- **Spring Cloud Gateway** for API routing  
- **Bucket4j** for rate-limiting logic  
- **Redis** as a shared distributed cache
- **Kafka** for reliable, real-time propagation of configuration changes  
- **License Service** as an example backend microservice  

The system ensures **fair and consistent request throttling** across multiple gateway instances using a centralized Redis backend.
Client-specific rate limit rules are defined per `clientId` and per endpoint. The **License Service** (Admin) is the authoritative configuration writer — it persists config to the database and publishes change events to Kafka. Gateways consume those events and update their local cache and Bucket4j buckets accordingly.

---

## 🏗️ System Architecture

Below is the high-level architecture of the system (you can replace this with your own `draw.io` diagram):

![System Architecture](./docs/system-architecture.png)

**Components:**
1. **API Gateway** – Intercepts incoming requests and applies distributed rate limiting.
2. **Redis** – Stores token bucket states to synchronize rate limits across gateways.
3. **Kafka** — topic(s) for configuration change events (durable, ordered propagation). 
4. **License Service** – A backend microservice used to demonstrate rate limiting.
5. **Clients** – Send API requests through the gateway.

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

---

## ⚙️ Why Kafka + DB + Redis?

- **DB**: durable source of truth, audit, versioning.  
- **Kafka**: reliable and replayable propagation of config changes to many consumers (gateways).  
- **Redis**: low-latency token state for global/consistent rate limits across gateway instances.

This hybrid approach gives you durability, low latency, and scalability.

---

## 🐳 Docker Setup

You can run the entire environment using **Docker Compose**.

```yaml
version: "3.8"
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  redis:
    image: redis:7
    ports:
      - "6379:6379"
