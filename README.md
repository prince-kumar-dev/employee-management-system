# Employee Management System (EMS)

An intuitive and feature-rich web application for managing employees and related HR processes. This system provides distinct functionalities for two primary user roles: **Admin** and **Employee**, with a focus on data scoping where each admin manages their own set of employees.

---

## 🚀 Features

This project is a full-stack Employee Management System built with a modern technology stack. It includes the following key features:

### User & Authentication
- **Role-Based Access Control:** Distinct dashboards and functionalities for **Admin** and **Employee** roles.
- **Multi-Admin Support:** Allows multiple admins to sign up and manage their own isolated set of employees. No admin can see or manage employees belonging to another admin.
- **Self-Registration with OTP Verification:** Both Admins and Employees can sign up through a secure OTP (One-Time Password) email verification process.
- **Employee Self-Signup with Manager Selection:** New employees can select their managing admin from a list during registration, ensuring they are correctly scoped from the start.
- **Admin-Initiated Employee Creation:** Admins can directly create new employee accounts from their dashboard. New employees receive a welcome email with their credentials and can log in immediately.

### Admin Dashboard & Management
- **Scoped Admin Dashboard:** A comprehensive dashboard with charts and graphs (using Chart.js) visualizing key metrics *only for employees managed by the logged-in admin*.
  - Total Managed Employees
  - Department Count (for managed employees)
  - Average Age, Salary, and Gender Distribution of managed employees
- **Employee CRUD:** Admins can Create, Read, Update, and Delete employee records within their scope.
- **Department CRUD:** Admins can manage their own set of departments. Different admins can create departments with the same name.
- **Leave Management:**
  - View all leave requests submitted by their managed employees.
  - Filter leave requests by status (Pending, Approved, Rejected).
  - Approve or Reject leave requests with remarks.

### Employee Dashboard & Features
- **Personalized Dashboard:** Employees have a dashboard to view their own personal and employment details.
- **Leave Management:**
  - Submit leave requests with start date, end date, and a reason.
  - View a history of their leave requests with real-time status updates (Pending, Approved, Rejected).
  - Receive email notifications upon submitting a request and when it is actioned by their admin.
  - Cancel pending leave requests.

### General Features
- **Email Notifications:** Automated email sending for critical events like OTP verification, welcome messages for new employees, and leave request status updates.
- **Dynamic UI:** A responsive user interface built with vanilla HTML, CSS, and JavaScript, featuring dynamic form fields and a global loader for a better user experience during backend operations.
- **RESTful API:** A well-defined backend API built with Java and Spring Boot.

---

## 🛠️ Technology Stack

- **Frontend:**
  - HTML5
  - CSS3 (with Flexbox/Grid for layout)
  - JavaScript (Vanilla JS, ES6+)
  - [Chart.js](https://www.chartjs.org/) for data visualization

- **Backend:**
  - Java 21
  - Spring Boot 3.x
  - Spring Web (for REST APIs)
  - Spring Data JPA (for database interaction)
  - Maven (for dependency management)

- **Database:**
  - MySQL

- **Email Service:**
  - Spring Boot Mail Sender

---

## ⚙️ Setup and Installation

Follow these steps to get the project running on your local machine.

### Prerequisites
- Java JDK 21 or later
- Apache Maven
- MySQL Server
- An SMTP server for sending emails (e.g., a Gmail account with an "App Password")

### 1. Backend Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/employee-management-system.git
   cd employee-management-system
   ```

2. **Configure the Database:**
   - Open MySQL and create a new database:
     ```sql
     CREATE DATABASE ems_db;
     ```
   - Open `src/main/resources/application.properties`.
   - Update the `spring.datasource` properties with your MySQL username and password:
     ```properties
     spring.datasource.url=jdbc:mysql://localhost:3306/ems_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
     spring.datasource.username=your_mysql_username
     spring.datasource.password=your_mysql_password
     ```

3. **Configure Email Service:**
   - In the same `application.properties` file, update the `spring.mail` properties with your SMTP server details. For Gmail, you will need to generate an "App Password".
     ```properties
     spring.mail.host=smtp.gmail.com
     spring.mail.port=587
     spring.mail.username=your.email@gmail.com
     spring.mail.password=your_gmail_app_password
     spring.mail.properties.mail.smtp.auth=true
     spring.mail.properties.mail.smtp.starttls.enable=true
     ```

4. **Run the Spring Boot Application:**
   - You can run it from your IDE (like IntelliJ IDEA or Eclipse) by running the `EmsApplication.java` file.
   - Or, you can build and run from the command line using Maven:
     ```bash
     mvn clean install
     mvn spring-boot:run
     ```
   - The backend server will start on `http://localhost:8081` by default.

### 2. Frontend Setup
The frontend is served as a static resource by the Spring Boot application, so no separate setup is required.

1. **Access the Application:**
   - Open your web browser and navigate to `http://localhost:8081`.

---

## 📖 API Endpoints Overview

A brief overview of the primary API endpoints.

| Method | Endpoint                             | Description                                            | Role       |
|--------|--------------------------------------|--------------------------------------------------------|------------|
| POST   | `/api/auth/register`                 | Self-register a new user (sends OTP).                  | Public     |
| POST   | `/api/auth/verify-otp`               | Verify an OTP to activate an account.                  | Public     |
| POST   | `/api/auth/login`                    | Log in a user.                                         | Public     |
| GET    | `/api/admins/list`                   | Get a list of admins for dropdowns.                    | Public     |
| GET    | `/api/employees`                     | Get a list of employees managed by an admin.           | Admin      |
| POST   | `/api/employees`                     | Admin creates a new employee.                          | Admin      |
| PUT    | `/api/employees/{id}`                | Admin updates an employee.                             | Admin      |
| DELETE | `/api/employees/{id}`                | Admin deletes an employee.                             | Admin      |
| GET    | `/api/departments/my-managed`        | Admin gets a list of their own departments.            | Admin      |
| GET    | `/api/departments/for-admin?adminId` | Get departments for a specific admin (for registration).| Public     |
| POST   | `/api/departments`                   | Admin creates a new department.                        | Admin      |
| POST   | `/api/leaves/apply`                  | Employee applies for leave.                            | Employee   |
| GET    | `/api/leaves/my-requests/{empId}`    | Employee gets their own leave requests.                | Employee   |
| GET    | `/api/leaves/admin/all`              | Admin gets leave requests for their employees.         | Admin      |
| PUT    | `/api/leaves/admin/{leaveId}/action` | Admin approves or rejects a leave request.             | Admin      |
| GET    | `/api/dashboard/summary`             | Admin gets scoped dashboard statistics.                | Admin      |

---

## 🔮 Future Enhancements

This project serves as a solid foundation. Future features could include:
- **Password Hashing:** Implementing `BCryptPasswordEncoder` for secure password storage.
- **JWT Authentication:** Replacing the current non-secure user identification with JSON Web Tokens for proper stateless authentication and authorization.
- **Advanced Dashboard Filters:** Adding date range filters to the admin dashboard.
- **Payroll Module:** A basic module to manage salary components and generate payslips.
