import java.sql.*;
import java.util.Scanner;

/**
 * The Main class serves as the entry point for the PoiseDMS project management system.
 * It provides a command-line interface for users to interact with the database,
 * allowing them to view, search, add, update, delete, and finalize projects.
 *
 * <p>This class establishes a database connection, presents a menu to the user,
 * and executes the corresponding actions based on user input.</p>
 * 
 * @author Demayne Govender
 * @version 1.0
 */
public class Main {

  /**
   * The main method initializes the program, establishes a database connection,
   * and handles user interaction through a menu-driven interface.
   *
   * @param args Command-line arguments (not used in this application)
   */
  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in); // Scanner resource initialization
    ProjectManager projectManager = new ProjectManager(); // Initialize ProjectManager instance

    try (Connection connection = DatabaseConnection.getConnection()) {
      while (true) {
        // Display options to the user
        System.out.println("Please choose an option:");
        System.out.println("1. View all projects");
        System.out.println("2. View incomplete projects");
        System.out.println("3. View overdue projects");
        System.out.println("4. Search projects by number or name");
        System.out.println("5. Add a new project");
        System.out.println("6. Update an existing project");
        System.out.println("7. Delete a project");
        System.out.println("8. Finalize a project");
        System.out.println("9. Exit");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline left-over

        switch (choice) {
          case 1:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.viewAllProjects(connection);
            break;

          case 2:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.viewIncompleteProjects(connection);
            break;

          case 3:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.viewOverdueProjects(connection);
            break;

          case 4:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.searchProjects(connection, scanner);
            break;

          case 5:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.addNewProject(connection, scanner);
            break;

          case 6:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.updateProject(connection, scanner);
            break;

          case 7:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.deleteProject(connection, scanner);
            break;

          case 8:
            if (!confirmContinue(scanner)) {
              continue;
            }
            projectManager.finaliseProject(connection, scanner);
            break;

          case 9:
            System.out.println("Exiting...");
            return;

          default:
            System.out.println("Invalid choice. Please try again.");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      // Closes the scanner to avoid resource leak
      scanner.close();
    }
  }

  /**
   * Prompts the user to confirm whether they want to proceed with an action.
   *
   * @param scanner The scanner object used to capture user input.
   * @return {@code true} if the user confirms to continue, otherwise {@code false}.
   */
  private static boolean confirmContinue(Scanner scanner) {
    System.out.println("Do you want to proceed? (y to continue, n to return to main menu)");
    String response = scanner.nextLine().trim().toLowerCase();
    return response.equals("y");
  }
}
