# Roadmap SEP Microservices - Catalog Service

![Java](https://img.shields.io/badge/Language-Java-orange?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

A robust microservice for managing and cataloging products within the Roadmap SEP ecosystem. This service provides APIs for product management, inventory tracking, and catalog operations.

## 📋 Table of Contents
- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#️-configuration)
- [Usage](#-usage)
- [API Endpoints](#-api-endpoints)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features
- **Product Management**: Create, read, update, and delete product information
- **Inventory Tracking**: Real-time inventory management and updates
- **Catalog Operations**: Complete catalog management capabilities
- **RESTful APIs**: Clean and intuitive REST API endpoints
- **Database Integration**: Persistent data storage
- **Error Handling**: Comprehensive error handling and validation
- **Logging**: Detailed logging for debugging and monitoring

## 📦 Prerequisites
- **Java 11+** or higher
- **Maven 3.6+** or Gradle
- **MySQL/PostgreSQL** database
- **Spring Boot** (version specified in `pom.xml`)

## 🚀 Installation

### Clone the Repository
```bash
git clone https://github.com/roadmap-sep/roadmap-sep-ms-catalog.git
cd roadmap-sep-ms-catalog
```

### Build the Project
**Using Maven:**
```bash
mvn clean install
```
**Using Gradle:**
```bash
gradle build
```

### Run the Application
**Using Maven:**
```bash
mvn spring-boot:run
```
**Using Gradle:**
```bash
gradle bootRun
```
The application will start on `http://localhost:8080` by default.

## ⚙️ Configuration
Create an `application.properties` or `application.yml` file in the `src/main/resources` directory:

**application.properties**
```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/catalog_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Application Name
spring.application.name=roadmap-sep-catalog-service
```

**application.yml**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/catalog_db
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
  application:
    name: roadmap-sep-catalog-service
```

## 📖 Usage

**Example: Creating a Product**
```http
POST /api/products
Content-Type: application/json

{
  "name": "Sample Product",
  "description": "A sample product description",
  "price": 99.99,
  "quantity": 100,
  "sku": "SKU-001"
}
```

**Example: Retrieving Products**
```http
GET /api/products
```

**Example: Updating a Product**
```http
PUT /api/products/{id}
Content-Type: application/json

{
  "name": "Updated Product Name",
  "price": 129.99,
  "quantity": 150
}
```

**Example: Deleting a Product**
```http
DELETE /api/products/{id}
```

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | Retrieve all products |
| GET | `/api/products/{id}` | Retrieve a specific product |
| POST | `/api/products` | Create a new product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |
| GET | `/api/catalog/inventory` | Get inventory status |
| POST | `/api/catalog/inventory/update` | Update inventory |

## 📁 Project Structure
```text
roadmap-sep-ms-catalog/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/roadmapsep/catalog/
│   │   │       ├── CatalogApplication.java
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── model/
│   │   │       └── exception/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application.yml
│   └── test/
│       └── java/
├── pom.xml (for Maven)
├── build.gradle (for Gradle)
├── README.md
└── .gitignore
```

## 🤝 Contributing
We welcome contributions! Please follow these steps:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

**Code Style Guidelines**
- Follow Java conventions and naming standards
- Write meaningful commit messages
- Include unit tests for new features
- Document public APIs with JavaDoc

## 📝 License
This project is licensed under the MIT License - see the LICENSE file for details.

## 📧 Support
For issues, questions, or suggestions, please open an issue on GitHub Issues.

## 🔗 Related Projects
Roadmap SEP Organization

---
*Last Updated: August 21, 2026*
*Made with ❤️ by the Roadmap SEP Team*
