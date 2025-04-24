import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;

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
   * Displays overdue projects from the database that are not yet finalised.
   *
   * <p>Projects are considered overdue if their deadline has passed and they
   * have not been marked as finalised ('YES'). This method also handles cases
   * where the 'Finalised' column is NULL or contains 'No'.</p>
   *
   * @param connection the database connection
   * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html">MySQL Date Functions</a>
   */
  public void viewOverdueProjects(Connection connection) {
    // SQL query to fetch projects that are overdue and not finalised
    String query = "SELECT * FROM project " +
                   "WHERE Deadline < CURDATE() AND (Finalised IS NULL OR Finalised = 'No')";

    try (Statement stmt = connection.createStatement();
         ResultSet resultSet = stmt.executeQuery(query)) {
         
      // Display the result using your custom table formatter
      TableFormatter.displayOverdueProjects(resultSet);
      
    } catch (SQLException e) {
      System.err.println("❌ Error retrieving overdue projects: " + e.getMessage());
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
          System.out.println("❌ NO data for project name or number entered.");
          return;
        }
        TableFormatter.displayProjectsByNumberOrName(resultSet);
      }
    } catch (SQLException e) {
      System.err.println("❌ Error searching for projects: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Adds a new project to the database.
   * Performs validation on input fields including ERF number, fee logic,
   * entity IDs, and date format. If the project name is not provided,
   * it is automatically generated using the customer's surname and building type.
   *
   * @param connection the database connection
   * @param scanner    the Scanner object for user input
   */
  public void addNewProject(Connection connection, Scanner scanner) {
    try {
      // Get project number with numeric validation
      String projectNumber;
      while (true) {
        System.out.print("Enter project number (e.g., 1234): ");
        String input = scanner.nextLine().trim();
        if (input.matches("\\d+")) {
          projectNumber = input;
          break;
        } else {
          System.out.println("❌ Invalid input. Please enter a numeric project number.");
        }
      }

      // Check if project already exists
      if (projectExists(connection, projectNumber)) {
        System.out.println("❌ Project with this number already exists.");
        return;
      }

      // Get project name or allow for auto-generation
      System.out.print("Enter project name (press Enter to generate automatically): ");
      String projectName = scanner.nextLine().trim();

      // Due date validation
      LocalDate dueDate = getValidFutureDate(
          scanner, "Enter project due date (YYYY-MM-DD, e.g., 2025-12-31): ");

      System.out.print("Enter building type (e.g., Residential, Commercial, House, Apartment): ");
      String buildingType = scanner.nextLine().trim();

      System.out.print("Enter physical address (e.g., 123 Main St, City, Country): ");
      String physicalAddress = scanner.nextLine().trim();

      // ERF number validation
      String erfNumber = "";
      while (!erfNumber.startsWith("ERF")) {
        System.out.print("Enter ERF number (e.g., ERF5678): ");
        erfNumber = scanner.nextLine().trim();
        if (!erfNumber.startsWith("ERF")) {
          System.out.println("❌ Invalid ERF number. It must start with 'ERF'.");
        }
      }

      // Fee validation
      double totalFee;
      double totalPaid;
      while (true) {
        totalFee = getValidDoubleInput(scanner, "Enter total fee (R, e.g., 150000.50): ");
        totalPaid = getValidDoubleInput(scanner, "Enter total paid (R, e.g., 50000.75): ");

        if (totalFee < 0 || totalPaid < 0) {
          System.out.println("Amounts cannot be negative. Please re-enter values.");
        } else if (totalPaid > totalFee) {
          System.out.println("Total paid cannot exceed total fee. Please try again.");
        } else {
          break;
        }
      }

      // Validate and fetch existing entity IDs
      String architectId = validateAndGetEntity(connection, scanner, "Architect", "ARC");
      String contractorId = validateAndGetEntity(connection, scanner, "Contractor", "CON");
      String customerId = validateAndGetEntity(connection, scanner, "Customer", "CUS");

      // Use helper method to validate all entities
      if (!validateEntityPresence(architectId, "Architect") ||
          !validateEntityPresence(contractorId, "Contractor") ||
          !validateEntityPresence(customerId, "Customer")) {
        return;
      }

      // Auto-generate project name if not provided
      if (projectName.isEmpty()) {
        projectName = generateProjectName(connection, customerId, buildingType);
        System.out.println("Project name automatically set to: " + projectName);
      }

      // Prepare SQL insert query
      String query =
          "INSERT INTO project (ProjectNumber, ProjectName, Deadline, BuildingType, "
              + "PhysicalAddress, ERFNumber, TotalFee, TotalPaid, ArchitectID, "
              + "ContractorID, CustomerID, Finalised) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'No');";

      // Execute insertion
      try (PreparedStatement pstmt = connection.prepareStatement(query)) {
        pstmt.setString(1, projectNumber);
        pstmt.setString(2, projectName);
        pstmt.setString(3, dueDate.toString());
        pstmt.setString(4, buildingType);
        pstmt.setString(5, physicalAddress);
        pstmt.setString(6, erfNumber);
        pstmt.setDouble(7, totalFee);
        pstmt.setDouble(8, totalPaid);
        pstmt.setString(9, architectId);
        pstmt.setString(10, contractorId);
        pstmt.setString(11, customerId);

        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          System.out.println("✅ Project added successfully.");
        } else {
          System.out.println("Failed to add the project.");
        }
      } catch (SQLException e) {
        System.out.println("❌ Error adding project to the database: " + e.getMessage());
        e.printStackTrace();
      }
    } catch (Exception e) {
      System.out.println("❌ An unexpected error occurred: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Validates that the given entity ID is not null. Prints an error message if it is.
   *
   * @param id   the ID to validate
   * @param role the role name (e.g., "Architect", "Contractor", "Customer")
   * @return true if the ID is valid, false otherwise
   */
  private boolean validateEntityPresence(String id, String role) {
    if (id == null) {
      System.out.println("❌ Error: Project cannot be added without a valid " + role.toLowerCase() + ".");
      return false;
    }
    return true;
  }


  /**
   * Generates a project name based on the customer's surname and building type.
   *
   * @param connection   the database connection
   * @param customerID   the ID of the customer
   * @param buildingType the type of building
   * @return the generated project name
   */
  private String generateProjectName(Connection connection, String customerID, String buildingType) {
      String surname = "Unknown";
      try (PreparedStatement pstmt = connection.prepareStatement("SELECT Surname FROM customer WHERE CustomerID = ?")) {
          pstmt.setString(1, customerID);
          ResultSet rs = pstmt.executeQuery();
          if (rs.next()) {
              surname = rs.getString("Surname");
          }
      } catch (SQLException e) {
          System.out.println("Error fetching customer surname: " + e.getMessage());
      }

      switch (buildingType.toLowerCase()) {
          case "house":
              return "House " + surname;
          case "apartment":
              return "Apartment " + surname;
          default:
              return "Project " + surname;
      }
  }

  /**
   * Validates the date input and ensures it is in the future.
   *
   * @param scanner the scanner object for user input
   * @param prompt  the prompt for user input
   * @return a valid future date
   */
  private LocalDate getValidFutureDate(Scanner scanner, String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        LocalDate date = LocalDate.parse(input, DATE_FORMAT);
        if (date.isBefore(LocalDate.now())) {
          System.out.println("Error: The date cannot be in the past. Please enter a future date.");
        } else {
          return date;
        }
      } catch (DateTimeParseException e) {
        System.out.println("❌ Invalid date format! Please enter the date in YYYY-MM-DD format.");
      }
    }
  }

  /**
   * Validates double inputs for fee values.
   *
   * @param scanner the scanner object for user input
   * @param prompt  the prompt for user input
   * @return a valid double value
   */
  private double getValidDoubleInput(Scanner scanner, String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      try {
        double value = Double.parseDouble(input);
        if (value >= 0) {
          return value;
        } else {
          System.out.println("❌ Error: Amount cannot be negative. Please enter a valid amount.");
        }
      } catch (NumberFormatException e) {
        System.out.println("❌ Invalid amount! Please enter a valid numeric value.");
      }
    }
  }
  /**
   * Checks if a project exists in the database.
   *
   * @param connection     the database connection
   * @param projectNumber  the project number to check
   * @return true if the project exists, false otherwise
   */
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
      System.out.println("❌ Error checking project existence: " + e.getMessage());
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Validates and retrieves the entity ID (e.g., Architect, Contractor, Customer).
   * Supports both prefixed (e.g., ARC123) and numeric (e.g., 1, 2) IDs.
   * Allows adding the entity if it does not exist.
   *
   * @param connection   the database connection
   * @param scanner      the scanner object for user input
   * @param entityType   the type of entity (e.g., Architect, Contractor, Customer)
   * @param prefix       the prefix for the entity ID (e.g., "ARC" for Architect)
   * @return the validated entity ID
   */
  private String validateAndGetEntity(Connection connection, Scanner scanner, String entityType, String prefix) {
    String entityID;

    while (true) {
      // Display available entity IDs
      System.out.println("Available " + entityType + " IDs:");
      try (
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(
          "SELECT " + entityType + "ID, FirstName, Surname FROM " + entityType.toLowerCase()
        )
      ) {
        while (rs.next()) {
          System.out.println(" - " + rs.getString(1) + ": " + rs.getString(2) + " " + rs.getString(3));
        }
      } catch (SQLException e) {
        System.out.println("❌ Error retrieving existing " + entityType + "s: " + e.getMessage());
      }

      // Prompt for ID
      System.out.print("Enter " + entityType + " ID (e.g., " + prefix + "123 or 1): ");
      entityID = scanner.nextLine().trim();

      // Validate format: allow either numeric or prefixed
      if (!entityID.matches("\\d+") && !entityID.matches(prefix + "\\d{3}")) {
        System.out.println("Invalid " + entityType + " ID. It must be numeric or start with '" + prefix + "' followed by 3 digits.");
        continue;
      }

      // Check if the ID exists
      if (isValidForeignKey(connection, entityType.toLowerCase(), entityType + "ID", entityID)) {
        return entityID;
      }

      // If ID does not exist, prompt user for next steps
      System.out.println(entityType.toUpperCase() + " ID DOES NOT EXIST.");
      System.out.print("Would you like to use an existing " + entityType + " from the list? (y/n): ");
      String useExisting = scanner.nextLine().trim().toLowerCase();

      if (useExisting.equals("y")) {
        System.out.print("Enter a valid " + entityType + " ID from the list: ");
        continue; // Loop back for another ID
      } else if (useExisting.equals("n")) {
    	System.out.print("Enter new Architect details below: ");  
        addEntity(connection, scanner, entityType, entityID);
        return entityID;
      } else {
        System.out.println("❌ Invalid input. Please enter 'y' or 'n'.");
      }
    }
  }


  // Email and phone validation patterns
  private static final Pattern EMAIL_PATTERN =
          Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  private static final Pattern PHONE_PATTERN =
          Pattern.compile("^[0-9]{10,15}$"); // Allows 10 to 15 digits

  /**
   * Method to add a new entity (e.g., Contractor, Architect).
   *
   * @param connection   the database connection
   * @param scanner      the scanner object for user input
   * @param entityType   the type of entity (e.g., Architect, Contractor, Customer)
   * @param entityID     the entity ID (e.g., "ARC101")
   */
  public void addEntity(Connection connection, Scanner scanner, String entityType, String entityID) {
    // Get first and last names separately
    System.out.print("Enter " + entityType + "'s First Name: ");
    String firstName = scanner.nextLine().trim();

    System.out.print("Enter " + entityType + "'s Surname: ");
    String surname = scanner.nextLine().trim();

    // Validate telephone number
    String telephone;
    while (true) {
      System.out.print("Enter " + entityType + "'s Telephone Number (10-15 digits, numbers only): ");
      telephone = scanner.nextLine().trim();
      if (PHONE_PATTERN.matcher(telephone).matches()) {
        break;
      }
      System.out.println("❌ Invalid telephone number! Please enter a valid number.");
    }

    // Validate email
    String email;
    while (true) {
      System.out.print("Enter " + entityType + "'s Email: ");
      email = scanner.nextLine().trim();
      if (EMAIL_PATTERN.matcher(email).matches()) {
        break;
      }
      System.out.println("❌ Invalid email format! Please enter a valid email (e.g., user@example.com).");
    }

    // Validate physical address
    String physicalAddress;
    while (true) {
      System.out.print("Enter physical address (e.g., 123 Main St, City, Country): ");
      physicalAddress = scanner.nextLine().trim();
      if (physicalAddress.length() > 5 && physicalAddress.contains(",")) {
        break;
      }
      System.out.println("Invalid address format! Ensure it includes street, city, and country.");
    }

    // Insert into database (Fixed Name column issue)
    String query = "INSERT INTO " + entityType.toLowerCase() +
        " (" + entityType + "ID, FirstName, Surname, Telephone, Email, PhysicalAddress) VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
      pstmt.setString(1, entityID);
      pstmt.setString(2, firstName);
      pstmt.setString(3, surname);
      pstmt.setString(4, telephone);
      pstmt.setString(5, email);
      pstmt.setString(6, physicalAddress);
      pstmt.executeUpdate();
      System.out.println(entityType + " added successfully. ✅");
    } catch (SQLException e) {
      System.out.println("❌ Error adding " + entityType + ": " + e.getMessage());
    }
  }

  /**
   * Checks if a foreign key exists in the specified table and column.
   *
   * @param connection The active database connection.
   * @param table      The table to search.
   * @param column     The column containing the foreign key.
   * @param id         The foreign key value to validate.
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
      System.err.println("❌ Error validating foreign key: " + e.getMessage());
      return false;
    }
  }

  /**
   * Updates a project's details such as name, due date, and total paid.
   *
   * @param connection The active database connection.
   * @param scanner    Scanner object for user input.
   * @see <a href="https://www.w3schools.com/sql/sql_update.asp">SQL UPDATE Statement</a>
   */
  public void updateProject(Connection connection, Scanner scanner) {
    try {
      while (true) {
        System.out.print("Enter project number to update (or type 'menu' to return): ");
        String projectNumber = scanner.nextLine().trim();

        if (projectNumber.equalsIgnoreCase("menu")) {
          System.out.println("Returning to main menu...");
          return;
        }

        String verifyQuery = "SELECT * FROM project WHERE ProjectNumber = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(verifyQuery)) {
          pstmt.setString(1, projectNumber);
          try (ResultSet resultSet = pstmt.executeQuery()) {
            if (resultSet.next()) {
              String currentName = resultSet.getString("ProjectName");
              String currentDeadline = resultSet.getString("Deadline");
              double currentPaid = resultSet.getDouble("TotalPaid");

              // Allow user to leave project name unchanged by pressing Enter
              System.out.print("Enter new project name (press Enter to keep '" + currentName + "'): ");
              String newName = scanner.nextLine().trim();
              if (newName.isEmpty()) {
                newName = currentName;
              }

              // Allow user to leave deadline unchanged by pressing Enter
              String newDueDate;
              do {
                System.out.print("Enter new deadline date (YYYY-MM-DD) (current: " + currentDeadline + "): ");
                newDueDate = scanner.nextLine().trim();
                if (newDueDate.isEmpty()) {
                  newDueDate = currentDeadline;
                }
              } while (newDueDate.isEmpty());

              // Allow user to update the total paid or keep it unchanged
              double newPaid = currentPaid;
              while (true) {
                System.out.print("Enter new total paid (current: R" + currentPaid + "): ");
                String paidInput = scanner.nextLine().trim();
                if (paidInput.isEmpty()) {
                  break; // Keep existing value if nothing is entered
                }
                try {
                  newPaid = Double.parseDouble(paidInput);
                  break;
                } catch (NumberFormatException e) {
                  System.out.println("Invalid amount. Please enter a numeric value.");
                }
              }

              String updateQuery = "UPDATE project SET ProjectName = ?, Deadline = ?, TotalPaid = ? WHERE ProjectNumber = ?";
              try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                updateStmt.setString(1, newName);
                updateStmt.setString(2, newDueDate);
                updateStmt.setDouble(3, newPaid);
                updateStmt.setString(4, projectNumber);
                updateStmt.executeUpdate();
                System.out.println("✅ Project updated successfully.");
              }
              break;
            } else {
              System.out.println("❌ Project not found. Please enter a valid project number or type 'menu' to return.");
            }
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("❌ Error updating project: " + e.getMessage());
      e.printStackTrace();
    }
  }

  
  /**
   * Finalizes a project by marking it as 'Finalised' in the database and setting the completion date.
   * If the project is already finalized, the user is prompted to update the completion date.
   *
   * @param connection The database connection.
   * @param scanner    The Scanner object for input collection.
   * @see <a href="https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html">JDBC Basics</a>
   * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/PreparedStatement.html">PreparedStatement API</a>
   */
  public void finaliseProject(Connection connection, Scanner scanner) {
    try {
      // Prompts user to enter the project number
      System.out.print("Enter project number to finalize: ");
      String projectNumber = scanner.nextLine();

      // Queries to check if the project exists and retrieve its finalization status
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
              System.out.println("✅ Project finalized successfully with updated completion date.");
            }
          } else {
            System.out.println("❌ Project not found.");
          }
        }
      }
    } catch (SQLException e) {
      System.out.println("❌ Error finalizing project: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Deletes a project from the database.
   *
   * @param connection The active database connection.
   * @param scanner    Scanner object for user input.
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
              System.out.println("✅ Project deleted successfully.");
            }
          } else {
            System.out.println("❌ Project not found.");
          }
        }
      }
    } catch (SQLException e) {
      System.err.println("❌ Error deleting project: " + e.getMessage());
    }
  }
}

