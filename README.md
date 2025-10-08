# 🚀 Distributed Rate Limiting System using Redis & Spring Cloud Gateway


## 🏗️ System Architecture


![Healthcare System Architecture](https://github.com/Snigdah/images/blob/main/Ratelimit.png)

**Components:**
1. **Spring Cloud Gateway** – Intercepts incoming requests and applies distributed rate limiting.
2. **Bucket4j** for rate-limiting logic  
3. **Redis** – Stores token bucket states to synchronize rate limits across gateways.
4. **Kafka** — topic(s) for configuration change events (durable, ordered propagation). 
5. **License Service** – A backend microservice used to demonstrate rate limiting.
6. **Clients** – Send API requests through the gateway.


## 🧠 Overview
Client-specific rate limit rules are defined per `clientId` and per endpoint.  
The **License Service** saves configuration data to the database and **publishes update events to Kafka** whenever limits or endpoints change.  
All **API Gateways** consume these Kafka events to automatically refresh their in-memory cache and update Bucket4j configurations without restart.  
The **Redis** backend stores the distributed Bucket4j token states, ensuring consistent rate limiting across multiple gateway instances in a clustered environment.


## ⚙️ Key Features

✅ Distributed rate limiting using **Bucket4j + Redis**  
✅ Works across multiple **API Gateway** instances  
✅ Supports both **per-user** and **per-route** rate-limiting rules  
✅ Fully **reactive** (non-blocking I/O) setup  
✅ Ready for **containerized deployment** with Docker Compose  

---

## ⚙️ Why Kafka + DB + Redis?

- **DB**: durable source of truth, audit, versioning.  
- **Kafka**: reliable and replayable propagation of config changes to many consumers (gateways).  
- **Redis**: low-latency token state for global/consistent rate limits across gateway instances.

This hybrid approach gives you durability, low latency, and scalability.

---

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
