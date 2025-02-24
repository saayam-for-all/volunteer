# Saayam For All - Volunteer Service

![License](https://img.shields.io/github/license/saayam-for-all/volunteer)
![Build Status](https://img.shields.io/github/actions/workflow/status/saayam-for-all/volunteer/ci.yml)
![Contributors](https://img.shields.io/github/contributors/saayam-for-all/volunteer)

## Overview
Saayam For All - Volunteer Service is a Java-based backend service designed to manage volunteer and user profiles. The system provides RESTful APIs for creating, updating, and managing volunteers, users, and their availability status.

## Features
- **User and Volunteer Management**
- **Role-Based API Access**
- **Exception Handling with Global Exception Handler**
- **Logging with AOP**
- **Internationalization Support**
- **Database Repository Integration**
- **Docker and Kubernetes Deployment Ready**

## Technologies Used
- 🛠 **Spring Boot** - Backend Framework
- 🔧 **Maven** - Build Automation
- 🐳 **Docker** - Containerization
- ☸️ **Kubernetes** - Deployment
- 🗄 **MySQL/PostgreSQL** - Database (Configurable)
- 📜 **Swagger** - API Documentation
- ✨ **Lombok** - Boilerplate Code Reduction

## Project Structure & Explanation
```bash
saayam-for-all-volunteer/
├── src/
│   ├── main/java/org/sfa/volunteer/
│   │   ├── controller/     # API Controllers handling HTTP requests
│   │   ├── service/        # Business logic and core functionalities
│   │   ├── repository/     # Database repositories for data access
│   │   ├── dto/            # Data Transfer Objects for request/response handling
│   │   ├── exception/      # Custom exceptions and global error handling
│   │   ├── util/           # Utility classes for helper functions
│   ├── resources/lang/     # Localization files for multi-language support
│   ├── test/java/          # Unit and integration tests
├── Dockerfile              # Configuration for containerization using Docker
├── deployment.yaml         # Kubernetes deployment configuration
├── ingress.yaml            # Configuration for managing external access via Kubernetes Ingress
├── pom.xml                 # Maven dependencies and project build configuration
└── README.md               # Documentation
```

## Folder & File Descriptions
### `controller/`
Contains controllers that handle HTTP requests and route them to the appropriate services.
- `UserController.java`: Handles user-related API endpoints.
- `VolunteerController.java`: Manages volunteer-specific requests.

### `service/`
Contains the business logic.
- `UserService.java`: Defines user-related services.
- `VolunteerService.java`: Implements logic for volunteer functionalities.

### `repository/`
Interfaces for database access using Spring Data JPA.
- `UserRepository.java`: Repository for managing user data.
- `VolunteerRepository.java`: Repository for managing volunteer records.

### `dto/`
Data transfer objects used for structured API communication.
- `request/`: DTOs for API request bodies.
- `response/`: DTOs for API response payloads.

### `exception/`
Handles global exception handling.
- `GlobalExceptionHandler.java`: Manages application-wide exceptions.
- `UserNotFoundException.java`: Custom exception for handling user-related errors.

### `util/`
Contains utility/helper classes.
- `MessageSourceUtil.java`: Supports internationalization.
- `ResponseBuilder.java`: Helps in constructing API responses.

### `resources/lang/`
Localization support for multiple languages (e.g., English, Hindi, Chinese).
- `messages.properties`: Default language properties.
- `messages_hi_IN.properties`: Hindi language support.
- `messages_zh_CN.properties`: Chinese language support.

### `test/java/`
Includes unit and integration tests for application components.
- `VolunteerApplicationTests.java`: Ensures correctness of the application.

## Installation & Setup
### Prerequisites
- ☕ JDK 17 or later
- ⚙️ Maven 3.8+
- 🐳 Docker (optional, for containerized deployment)
- ☸️ Kubernetes (optional, for cloud deployment)

### Clone Repository
```sh
git clone https://github.com/saayam-for-all/volunteer.git
cd volunteer
```

### Build & Run
#### Run Locally
```sh
mvn clean install
mvn spring-boot:run
```
#### Run with Docker
```sh
docker build -t saayam-volunteer .
docker run -p 8080:8080 saayam-volunteer
```

## API Documentation
Once the application is running, Swagger API documentation is available at:
```sh
http://localhost:8080/swagger-ui/
```

## Deployment
To deploy using Kubernetes:
```sh
kubectl apply -f deployment.yaml
kubectl apply -f serviceDemo.yaml
kubectl apply -f ingress.yaml
```

## Contributing
1. **Fork** the repository
2. **Create** a new branch (`feature/your-feature`)
3. **Commit** your changes
4. **Push** to your fork and submit a **pull request**

## License
This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

## Contact
For any queries or contributions, reach out to: 
- Saayam for all volunteers
- 📌 **GitHub Repository**: [Saayam For All - Volunteer](https://github.com/saayam-for-all/volunteer)

---

✨ Happy coding! 🚀