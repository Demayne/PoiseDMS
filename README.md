
# PoiseDMS - Project Management System

## Description
PoiseDMS is a simple project management system written in Java that allows users to interact with a MySQL database. It provides a command-line interface for managing projects, including viewing, searching, adding, updating, deleting, and finalizing projects.

## Features
- View all projects
- View incomplete projects
- View overdue projects
- Search projects by number or name
- Add a new project
- Update an existing project
- Delete a project
- Finalize a project

## Technologies Used
- Java
- MySQL
- JDBC (Java Database Connectivity)

## Files in the Project

### 1. `Main.java`
This file serves as the entry point for the application. It provides a menu-driven interface for interacting with the system and calls relevant methods from `ProjectManager.java`.

### 2. `ProjectManager.java`
Handles the core logic for project management, including CRUD (Create, Read, Update, Delete) operations on the database.

### 3. `DatabaseConnection.java`
Manages the database connection, ensuring the application connects to the MySQL database using JDBC.

### 4. `TableFormatter.java`
Formats and displays the retrieved project data in a structured tabular format for better readability in the terminal.

## Setup Instructions

### 1. Clone the Repository
```bash
 git clone https://github.com/yourusername/PoiseDMS.git
 cd PoiseDMS
```

### 2. Database Configuration
- Ensure MySQL is installed and running.
- Create a database named `PoiseDMS`.
- Update the `DatabaseConnection.java` file with your MySQL credentials:
  ```java
  private static final String USER = "your_mysql_username";
  private static final String PASSWORD = "your_mysql_password";
  ```

### 3. Compile and Run the Program
```bash
javac *.java
java Main
```

## How to Add the README to GitHub Repository from Command Line

### 1. Navigate to Your Project Directory
```bash
cd /path/to/your/project
```

### 2. Initialize a Git Repository (If Not Already Initialized)
```bash
git init
```

### 3. Add the README File
```bash
git add README.md
```

### 4. Commit the Changes
```bash
git commit -m "Added README file"
```

### 5. Connect to GitHub Repository (If Not Connected)
```bash
git remote add origin https://github.com/yourusername/PoiseDMS.git
```

### 6. Push the Changes to GitHub
```bash
git push -u origin master
```

Your README file should now be visible in your GitHub repository!

## License
This project is open-source and available under the MIT License.

## Author
**Demayne Govender**
