# 🛒 ShopEase — E-Commerce Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![JWT](https://img.shields.io/badge/JWT-Security-red)
![Status](https://img.shields.io/badge/Status-Complete-brightgreen)

A production-ready REST API for a full-featured E-Commerce platform built 
with Spring Boot, Java 21, MySQL and JWT Security.

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.4.1 |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL 8.0 |
| Build Tool | Maven |

## ✅ Features

- 🔐 JWT Authentication & Authorization
- 👥 Role Based Access Control (USER / ADMIN)
- 📦 Product Management (CRUD)
- 🛒 Cart Management (Add, Update, Remove, Clear)
- 📋 Order Management (Place, Cancel, Status Update)
- 👨‍💼 Admin Panel APIs (Users, Products, Orders)
- ✅ Input Validation with Jakarta Validation
- 🌐 CORS configured for frontend integration

## 📌 API Endpoints

### Auth
| Method | Endpoint | Access |
|--------|----------|--------|
| POST | /auth/register | Public |
| POST | /auth/login | Public |

### Products
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | /products | Public |
| GET | /products/{id} | Public |
| GET | /products/search | Public |
| POST | /products | ADMIN |
| PUT | /products/{id} | ADMIN |
| DELETE | /products/{id} | ADMIN |

### Cart
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | /cart | USER |
| POST | /cart/add | USER |
| PUT | /cart/update/{itemId} | USER |
| DELETE | /cart/remove/{itemId} | USER |
| DELETE | /cart/clear | USER |

### Orders
| Method | Endpoint | Access |
|--------|----------|--------|
| POST | /orders | USER |
| GET | /orders | USER |
| GET | /orders/{id} | USER |
| PUT | /orders/{id}/cancel | USER |
| GET | /orders/all | ADMIN |
| PUT | /orders/{id}/status | ADMIN |

### Admin
| Method | Endpoint | Access |
|--------|----------|--------|
| GET | /admin/users | ADMIN |

## 🚀 Getting Started

### Prerequisites
- Java 21
- MySQL 8
- Maven

### Setup
1. Clone the repo
```bash
   git clone https://github.com/YOUR_USERNAME/ecommerce-backend.git
```
2. Create MySQL database
```sql
   CREATE DATABASE ecommerce_db;
```
3. Update `src/main/resources/application.properties`
```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   jwt.secret=your_jwt_secret
   jwt.expiration=86400000
```
4. Run the application
```bash
   ./mvnw spring-boot:run
```
5. API runs on `http://localhost:8085`

## 🔗 Frontend Repository
[ShopEase Frontend](https://github.com/YOUR_USERNAME/ecommerce-frontend)
