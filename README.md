# Backend - Similar Products API

## 📋 Technical Specification

### Business Requirement
We want to offer a new feature to our customers showing similar products to the one they are currently seeing. To do this we created a new REST API operation that provides the product detail of similar products for a given product.

### API Contract
The API orchestrates two existing endpoints:
1. **Get Similar Product IDs**: `GET /product/{productId}/similarids` → Returns array of product IDs
2. **Get Product Detail**: `GET /product/{productId}` → Returns product details (id, name, price, availability)

The frontend applications consume our new endpoint to display similar products information.

### Evaluation Criteria
- **Code clarity and maintainability**: Clean architecture, readable code, proper separation of concerns
- **Performance**: Efficient parallel processing, optimized API calls, sub-second response times
- **Resilience**: Exception handling, graceful degradation, timeout management, retry logic

---

## Overview

This is a **Spring Boot 3.1.5** backend application that implements a REST API to fetch similar products. The API orchestrates calls to external mock APIs and provides product details with resilience and performance monitoring.

**Technology Stack:**
- Java 17
- Spring Boot 3.1.5
- Maven 3.9.5
- Docker & Docker Compose
- K6 for load testing
- Grafana for metrics visualization
- InfluxDB for metrics storage

---

## Project Structure

```
backend/
├── pom.xml                          # Maven configuration
├── Dockerfile                       # Multi-stage Docker build
├── docker-compose.yml              # Docker services orchestration
├── README.md                        # This file
├── src/
│   ├── main/
│   │   ├── java/com/itx/
│   │   │   ├── controller/
│   │   │   │   └── ProductController.java      # REST endpoints
│   │   │   ├── service/
│   │   │   │   ├── ProductService.java         # Business logic
│   │   │   │   └── ExternalApiResilientClient.java  # HTTP client
│   │   │   ├── model/
│   │   │   │   └── ProductDetail.java          # DTO
│   │   │   └── Application.java                # Entry point
│   │   └── resources/
│   │       └── application.properties          # Configuration
│   └── test/
│       └── java/com/itx/...                    # Test cases
├── k6/
│   └── test.js                      # Load testing scenarios
├── simulado/
│   └── mocks.json                   # Mock data
├── dashboards/
│   └── dashboard.yml                # Grafana dashboard config
└── datasources/
    └── datasource.yml               # Grafana datasource config
```

---

## API Contract

### Endpoint: Get Similar Products

**Request:**
```
GET /product/{productId}/similar
```

**Path Parameters:**
- `productId` (String): The product ID to find similar products for

**Response (200 OK):**
```json
[
  {
    "id": "2",
    "name": "Product Name",
    "price": 899.99,
    "availability": true
  },
  {
    "id": "3",
    "name": "Another Product",
    "price": 799.99,
    "availability": false
  }
]
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2024-10-09T12:00:00Z",
  "status": 404,
  "error": "Product not found",
  "path": "/product/invalid/similar"
}
```

---

## Configuration

### `application.properties`

```properties
server.port=5000
server.servlet.context-path=/

# External API Configuration
external-api.base-url=http://localhost:3001
external-api.timeout=5000
external-api.max-retries=2

# Logging
logging.level.root=INFO
logging.level.com.itx=DEBUG
```

**Environment Variables (Docker):**
- `SERVER_PORT`: Backend port (default: 5000)
- `EXTERNAL_API_URL`: Mock API base URL (default: http://localhost:3001)

---

## Quick Start

### Option 1: Docker Compose (Recommended)

**Prerequisites:** Docker & Docker Compose

**Start all services:**
```bash
docker-compose up -d
```

**Verify Backend is running:**
```bash
curl http://localhost:5000/product/1/similar
```

**Stop all services:**
```bash
docker-compose down
```

### Option 2: Local Development (Maven)

**Prerequisites:** Java 17+, Maven 3.9.5+

**Build:**
```bash
mvn clean package
```

**Start mock API (requires Docker):**
```bash
docker run -p 3001:80 -v $(pwd)/simulado:/app ldabiralai/simulado:latest ./bin/simulado -f /app/mocks.json
```

**Run Backend:**
```bash
java -jar target/product-similar-app-1.0.0.jar
```

**Test:**
```bash
curl http://localhost:5000/product/1/similar
```

---

## Testing and Self-Evaluation

### Prerequisites
- Docker & Docker Compose installed
- Docker file sharing enabled for this folder (Dashboard → Settings → Resources → File Sharing)

### Setup Mocks and Infrastructure

```bash
docker-compose up -d simulado influxdb grafana
```

### Verify Mocks are Working

```bash
curl http://localhost:3001/product/1/similarids
```

Expected response:
```json
["2", "3", "4"]
```

### Run Performance Tests with K6

```bash
docker-compose run --rm k6 run scripts/test.js
```

### View Test Results in Grafana

1. Navigate to http://localhost:3000
2. Login: admin/admin
3. Browse to dashboard: http://localhost:3000/d/Le2Ku9NMk/k6-performance-test
4. View real-time performance metrics and test results

---

## Load Testing with K6

### Run Tests with Docker Compose

**Start services:**
```bash
docker-compose up -d backend simulado influxdb grafana
```

**Run K6 tests:**
```bash
docker-compose run --rm k6 run /scripts/test.js
```

### Test Scenarios

1. **Normal**: 50 VUs for 30 seconds
   - Simulates regular traffic
   - Target: p(95) < 1000ms, p(99) < 5000ms

2. **Ramping**: 0 → 100 VUs over 70 seconds
   - Gradual load increase
   - Tests scalability limits

3. **Stress**: 200 VUs for 20 seconds
   - Extreme load test
   - Determines breaking points

### View Results in Grafana

1. Navigate to http://localhost:3000
2. Login: admin/admin
3. Import dashboard from `dashboards/dashboard.yml`
4. Select InfluxDB datasource
5. View real-time performance metrics

---

## Architecture Highlights

### Resilience
- Exception handling with try-catch blocks
- Null checks on all API responses
- Graceful degradation (empty lists on errors)
- Configured timeouts: 2s connect, 5s read

### Performance
- Parallel processing with `CompletableFuture`
- Connection pooling via RestTemplate
- Efficient batch API calls
- Request/response logging

### Code Quality
- Lombok reduces boilerplate code
- Clean separation: Controller → Service → Client
- Comprehensive DEBUG logging
- Type-safe DTO objects

### Monitoring
- SLF4J structured logging
- Performance timing measurements
- Error logging with full stack traces
- K6 integration for load metrics

---

## External Mock APIs

The application calls two mock endpoints:

1. **Get Similar Product IDs**
   ```
   GET http://localhost:3001/product/{productId}/similarids
   ```
   Returns: `["2", "3", "4"]`

2. **Get Product Detail**
   ```
   GET http://localhost:3001/product/{productId}
   ```
   Returns:
   ```json
   {
     "id": "2",
     "name": "Product Name",
     "price": 899.99,
     "availability": true
   }
   ```

---

## Scripts

```bash
# Build
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run locally
mvn spring-boot:run

# Run tests
mvn test

# View logs
docker logs backend-api
```

---

## Troubleshooting

### Port 5000 in use
```bash
lsof -ti:5000 | xargs kill -9
```

### Mock API not responding
```bash
curl http://localhost:3001/product/1/similarids
docker logs mock-api
```

### Grafana dashboard empty
- Ensure InfluxDB is running: `docker ps | grep influxdb`
- Check K6 tests completed: `docker logs k6-test`
- Re-import dashboard from `dashboards/dashboard.yml`

---

## Performance Targets (SLA)

| Metric | Target | Condition |
|--------|--------|-----------|
| P95 Response | < 1000ms | Normal load |
| P99 Response | < 5000ms | Peak load |
| Failure Rate | < 10% | All scenarios |
| Throughput | > 100 req/s | 100 VUs |

---

## Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Logging
- Lombok (development)
- Spring Boot Test Starter (testing)

**Lightweight & Minimal:** No databases, caching frameworks, or message queues.

---

## License

See LICENSE file
