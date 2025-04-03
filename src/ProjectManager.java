import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * ProjectManager class allows interaction with the project database
 * to manage and manipulate project information such as adding new projects,
 * viewing existing projects, searching projects, and viewing incomplete and overdue projects.
 * 
 * <p>Javadoc documentation is provided for each method for better understanding
 * and adherence to coding standards.</p>
 * 
 * @author Demayne Govender
 * @version 1.0
 */
public class ProjectManager {

    /**
     * Displays all projects from the database.
     * 
     * @param connection the database connection
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/sql/Connection.html">JDBC Connection documentation</a>
     */
    public void viewAllProjects(Connection connection) {
        String query = "SELECT * FROM project";
        try (Statement stmt = connection.createStatement(); ResultSet resultSet = stmt.executeQuery(query)) {
            TableFormatter.displayAllProjects(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays incomplete projects from the database.
     * 
     * @param connection the database connection
     * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/date-and-time-literals.html">MySQL Date and Time Literals</a>
     */
    public void viewIncompleteProjects(Connection connection) {
        String query = "SELECT * FROM project WHERE Finalised = 'No'";
        try (Statement stmt = connection.createStatement(); ResultSet resultSet = stmt.executeQuery(query)) {
            TableFormatter.displayIncompleteProjects(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays overdue projects from the database.
     * 
     * @param connection the database connection
     * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html">MySQL Date Functions</a>
     */
    public void viewOverdueProjects(Connection connection) {
        String query = "SELECT * FROM project WHERE Deadline < CURDATE()";
        try (Statement stmt = connection.createStatement(); ResultSet resultSet = stmt.executeQuery(query)) {
            TableFormatter.displayOverdueProjects(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows the user to search for projects by project number or name.
     * Displays project details if found; otherwise, informs the user that no data is available.
     * 
     * @param connection the database connection
     * @param scanner the scanner object for user input
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/sql/PreparedStatement.html">JDBC PreparedStatement documentation</a>
     */
    public void searchProjects(Connection connection, Scanner scanner) {
        System.out.print("Enter project number or name to search: ");
        String searchTerm = scanner.nextLine();
        String query = "SELECT * FROM project WHERE ProjectNumber LIKE ? OR ProjectName LIKE ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "%" + searchTerm + "%");
            pstmt.setString(2, "%" + searchTerm + "%");

            try (ResultSet resultSet = pstmt.executeQuery()) {
                if (!resultSet.isBeforeFirst()) { // Check if the result set is empty
                    System.out.println("NO data for project name or number entered.");
                    return;
                }
                TableFormatter.displayProjectsByNumberOrName(resultSet);
            }
        } catch (SQLException e) {
            System.err.println("Error searching for projects: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Adds a new project to the database.
     * This method performs validation for inputs like ERF number, project fee, architect ID, etc.
     * It also checks the existence of Architect, Contractor, and Customer IDs before proceeding.
     * 
     * @param connection the database connection
     * @param scanner the scanner object for user input
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/sql/PreparedStatement.html">PreparedStatement documentation</a>
     * @see <a href="https://www.journaldev.com/1044/java-input-validation">Java Input Validation</a>
     */
    public void addNewProject(Connection connection, Scanner scanner) {
        try {
            // Get project details
            System.out.print("Enter project number (e.g., 1234): ");
            String projectNumber = scanner.nextLine().trim();

            if (projectExists(connection, projectNumber)) {
                System.out.println("Project with this number already exists.");
                return;
            }

            System.out.print("Enter project name (e.g., New Office Building): ");
            String projectName = scanner.nextLine().trim();

            System.out.print("Enter project due date (YYYY-MM-DD, e.g., 2025-12-31): ");
            String dueDate = scanner.nextLine().trim();

            System.out.print("Enter building type (e.g., Residential, Commercial): ");
            String buildingType = scanner.nextLine().trim();

            System.out.print("Enter physical address (e.g., 123 Main St, City, Country): ");
            String physicalAddress = scanner.nextLine().trim();

            // ERF number validation (must start with "ERF")
            String erfNumber = "";
            while (!erfNumber.startsWith("ERF")) {
                System.out.print("Enter ERF number (e.g., ERF5678): ");
                erfNumber = scanner.nextLine().trim();
                if (!erfNumber.startsWith("ERF")) {
                    System.out.println("Invalid ERF number. It must start with 'ERF'.");
                }
            }

            // Handles total fee input validation
            double totalFee = getValidDoubleInput(scanner, "Enter total fee (R, e.g., 150000.50): ");
            double totalPaid = getValidDoubleInput(scanner, "Enter total paid (R, e.g., 50000.75): ");

            // Validate and ensure architect, contractor, and customer details
            String architectID = validateAndGetEntity(connection, scanner, "Architect", "ARC");
            String contractorID = validateAndGetEntity(connection, scanner, "Contractor", "CON");
            String customerID = validateAndGetEntity(connection, scanner, "Customer", "CUS");

            // Prepare the SQL query
            String query = "INSERT INTO project (ProjectNumber, ProjectName, Deadline, BuildingType, PhysicalAddress, ERFNumber, TotalFee, TotalPaid, ArchitectID, ContractorID, CustomerID, Finalised) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'No');";

            // Executes the query
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, projectNumber);
                pstmt.setString(2, projectName);
                pstmt.setString(3, dueDate);
                pstmt.setString(4, buildingType);
                pstmt.setString(5, physicalAddress);
                pstmt.setString(6, erfNumber);
                pstmt.setDouble(7, totalFee);
                pstmt.setDouble(8, totalPaid);
                pstmt.setString(9, architectID);
                pstmt.setString(10, contractorID);
                pstmt.setString(11, customerID);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Project added successfully.");
                } else {
                    System.out.println("Failed to add the project.");
                }
            } catch (SQLException e) {
                System.out.println("Error adding project to the database: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double getValidDoubleInput(Scanner scanner, String prompt) {
        double value = -1;
        while (value < 0) {
            System.out.print(prompt);
            try {
                value = Double.parseDouble(scanner.nextLine().trim());
                if (value < 0) {
                    System.out.println("Value cannot be negative.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
        return value;
    }

    private boolean projectExists(Connection connection, String projectNumber) {
        String query = "SELECT COUNT(*) FROM project WHERE ProjectNumber = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, projectNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking project existence: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }


    private String validateAndGetEntity(Connection connection, Scanner scanner, String entityType, String prefix) {
        String entityID = "";
        while (!entityID.startsWith(prefix) || entityID.length() != 6) {
            System.out.print("Enter " + entityType + " ID (e.g., " + prefix + "101): ");
            entityID = scanner.nextLine();
            if (!entityID.startsWith(prefix) || entityID.length() != 6) {
                System.out.println("Invalid " + entityType + " ID. It must start with '" + prefix + "' followed by a 3-digit number.");
            }
        }

        boolean exists = isValidForeignKey(connection, entityType.toLowerCase(), entityType + "ID", entityID);
        if (!exists) {
            System.out.println(entityType + " ID " + entityID + " does not exist.");
            System.out.print("Do you want to add this " + entityType + "? (y/n): ");
            String response = scanner.nextLine();
            if (response.equalsIgnoreCase("y")) {
                addEntity(connection, scanner, entityType, entityID);
            }
        }
        return entityID;
    }

    private void addEntity(Connection connection, Scanner scanner, String entityType, String entityID) {
        System.out.print("Enter " + entityType + "'s Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter " + entityType + "'s Telephone Number: ");
        String telephone = scanner.nextLine();
        System.out.print("Enter " + entityType + "'s Email: ");
        String email = scanner.nextLine();
        System.out.print("Enter " + entityType + "'s Physical Address: ");
        String physicalAddress = scanner.nextLine();

        String query = "INSERT INTO " + entityType.toLowerCase() + " (" + entityType + "ID, Name, Telephone, Email, PhysicalAddress) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entityID);
            pstmt.setString(2, name);
            pstmt.setString(3, telephone);
            pstmt.setString(4, email);
            pstmt.setString(5, physicalAddress);
            pstmt.executeUpdate();
            System.out.println(entityType + " added successfully.");
        } catch (SQLException e) {
            System.out.println("Error adding " + entityType + ": " + e.getMessage());
        }
    }


    /**
     * Checks if a foreign key exists in the specified table and column.
     *
     * @param connection The active database connection.
     * @param table The table to search.
     * @param column The column containing the foreign key.
     * @param id The foreign key value to validate.
     * @return true if the key exists, false otherwise.
     * @see <a href="https://www.w3schools.com/sql/sql_foreignkey.asp">SQL Foreign Keys</a>
     */
    private boolean isValidForeignKey(Connection connection, String table, String column, String id) {
        String query = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, id);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            System.err.println("Error validating foreign key: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a new Architect to the database.
     * 
     * @param connection The database connection.
     * @param architectID The unique ID for the architect.
     * @param scanner The Scanner object for input collection.
     */
    private void addArchitect(Connection connection, String architectID, Scanner scanner) {
        System.out.print("Enter architect name: ");
        String name = scanner.nextLine();

        String query = "INSERT INTO architect (ArchitectID, Name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, architectID);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
            System.out.println("Architect added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new Contractor to the database.
     * 
     * @param connection The database connection.
     * @param contractorID The unique ID for the contractor.
     * @param scanner The Scanner object for input collection.
     */
    private void addContractor(Connection connection, String contractorID, Scanner scanner) {
        System.out.print("Enter contractor name: ");
        String name = scanner.nextLine();

        String query = "INSERT INTO contractor (ContractorID, Name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, contractorID);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
            System.out.println("Contractor added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new Customer to the database.
     * 
     * @param connection The database connection.
     * @param customerID The unique ID for the customer.
     * @param scanner The Scanner object for input collection.
     */
    private void addCustomer(Connection connection, String customerID, Scanner scanner) {
        System.out.print("Enter customer name: ");
        String name = scanner.nextLine();

        String query = "INSERT INTO customer (CustomerID, Name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerID);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
            System.out.println("Customer added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a project's details such as name and due date.
     *
     * @param connection The active database connection.
     * @param scanner Scanner object for user input.
     * @see <a href="https://www.w3schools.com/sql/sql_update.asp">SQL UPDATE Statement</a>
     */
    public void updateProject(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter project number to update: ");
            String projectNumber = scanner.nextLine();

            String verifyQuery = "SELECT * FROM project WHERE ProjectNumber = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(verifyQuery)) {
                pstmt.setString(1, projectNumber);
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    if (resultSet.next()) {
                        System.out.print("Enter new project name (Leave blank to keep current): ");
                        String newName = scanner.nextLine();
                        if (newName.isEmpty()) newName = resultSet.getString("ProjectName");

                        System.out.print("Enter new due date (YYYY-MM-DD): ");
                        String newDueDate = scanner.nextLine();

                        String updateQuery = "UPDATE project SET ProjectName = ?, Deadline = ? WHERE ProjectNumber = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, newName);
                            updateStmt.setString(2, newDueDate);
                            updateStmt.setString(3, projectNumber);
                            updateStmt.executeUpdate();
                            System.out.println("Project updated successfully.");
                        }
                    } else {
                        System.out.println("Project not found.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating project: " + e.getMessage());
        }
    }
    
    /**
     * Finalizes a project by marking it as 'Finalised' in the database and setting the completion date.
     * If the project is already finalized, the user is prompted to update the completion date.
     *
     * @param connection The database connection.
     * @param scanner The Scanner object for input collection.
     * 
     * @see <a href="https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html">JDBC Basics</a>
     * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/PreparedStatement.html">PreparedStatement API</a>
     */
    public void finaliseProject(Connection connection, Scanner scanner) {
        try {
            // Prompts user to enter the project number
            System.out.print("Enter project number to finalize: ");
            String projectNumber = scanner.nextLine();

            // Query's to check if the project exists and retrieve its finalization status
            String verifyQuery = "SELECT Finalised, CompletionDate FROM project WHERE ProjectNumber = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(verifyQuery)) {
                pstmt.setString(1, projectNumber);
                
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    if (resultSet.next()) {
                        String finalisedStatus = resultSet.getString("Finalised");
                        Date completionDate = resultSet.getDate("CompletionDate");

                        // If the project is already finalized and has a completion date, ask if it should be updated
                        if ("Yes".equalsIgnoreCase(finalisedStatus) && completionDate != null) {
                            System.out.print("This project is already finalized with a completion date of " 
                                + completionDate + ". Do you want to update the completion date? (y/n): ");
                            String response = scanner.nextLine().trim().toLowerCase();

                            // If the user chooses not to update, exit the method
                            if (!response.equals("y")) {
                                System.out.println("Project finalization unchanged.");
                                return;
                            }
                        }

                        // Updates query to finalize the project and set the completion date to the current date
                        String updateQuery = "UPDATE project SET Finalised = 'Yes', CompletionDate = CURRENT_DATE WHERE ProjectNumber = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, projectNumber);
                            updateStmt.executeUpdate();
                            System.out.println("Project finalized successfully with updated completion date.");
                        }
                    } else {
                        System.out.println("Project not found.");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error finalizing project: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes a project from the database.
     *
     * @param connection The active database connection.
     * @param scanner Scanner object for user input.
     * @see <a href="https://www.w3schools.com/sql/sql_delete.asp">SQL DELETE Statement</a>
     */
    public void deleteProject(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter project number to delete: ");
            String projectNumber = scanner.nextLine();

            String verifyQuery = "SELECT * FROM project WHERE ProjectNumber = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(verifyQuery)) {
                pstmt.setString(1, projectNumber);
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    if (resultSet.next()) {
                        String deleteQuery = "DELETE FROM project WHERE ProjectNumber = ?";
                        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                            deleteStmt.setString(1, projectNumber);
                            deleteStmt.executeUpdate();
                            System.out.println("Project deleted successfully.");
                        }
                    } else {
                        System.out.println("Project not found.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error deleting project: " + e.getMessage());
        }
    }
}

