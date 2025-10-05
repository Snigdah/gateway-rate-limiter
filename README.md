## 🚦 Rate Limiting Overview

![Rate Limiting Architecture](https://miro.medium.com/v2/resize:fit:4800/format:webp/1*nBXfm6BcTvyfPf0f9MJ_-Q.jpeg)

### 🧩 Components

1. **Client**  
   The consumer of your API — sends multiple requests that need to be controlled.  

2. **Rate Limiting**  
   Implemented using **Bucket4j** inside **Spring Cloud Gateway**, this defines how many requests a client can make per second or per minute.  

3. **Redis Cache**  
   Acts as a **distributed store** to synchronize token bucket states across multiple gateway instances.  
   Ensures consistent rate limiting even when running in a load-balanced environment.  

4. **API Server**  
   The actual microservice(s) that handle client requests after passing the rate limit checks.  

---
