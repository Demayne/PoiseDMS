import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Class responsible for formatting and displaying project data in a tabular format.
 */
public class TableFormatter {

    /**
     * Displays all projects in a table format.
     *
     * @param resultSet The result set containing project data.
     * @throws SQLException If a database access error occurs.
     */
    public static void displayAllProjects(ResultSet resultSet) throws SQLException {
        displayProjects(resultSet, "All Projects");
    }

    /**
     * Displays incomplete projects in a table format.
     *
     * @param resultSet The result set containing project data.
     * @throws SQLException If a database access error occurs.
     */
    public static void displayIncompleteProjects(ResultSet resultSet) throws SQLException {
        displayProjects(resultSet, "Incomplete Projects");
    }

    /**
     * Displays overdue projects in a table format.
     *
     * @param resultSet The result set containing project data.
     * @throws SQLException If a database access error occurs.
     */
    public static void displayOverdueProjects(ResultSet resultSet) throws SQLException {
        displayProjects(resultSet, "Overdue Projects");
    }

    /**
     * Displays projects found by project number or name in a table format.
     *
     * @param resultSet The result set containing project data.
     * @throws SQLException If a database access error occurs.
     */
    public static void displayProjectsByNumberOrName(ResultSet resultSet) throws SQLException {
        displayProjects(resultSet, "Projects Found by Number or Name");
    }

    /**
     * Formats and displays project data in a tabular format.
     *
     * @param resultSet The result set containing project data.
     * @param title     The title for the table (e.g., "Incomplete Projects").
     * @throws SQLException If a database access error occurs.
     */
    public static void displayProjects(ResultSet resultSet, String title) throws SQLException {
        List<Map<String, String>> data = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        Map<String, Integer> columnWidths = new HashMap<>();

        // Extract column names and initialize column widths based on header length
        int columnCount = resultSet.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = resultSet.getMetaData().getColumnLabel(i);
            columnNames.add(columnName);
            columnWidths.put(columnName, columnName.length()); // Initialize width with column name length
        }

        // Process rows of data
        while (resultSet.next()) {
            Map<String, String> row = new LinkedHashMap<>();
            for (String columnName : columnNames) {
                String value = resultSet.getString(columnName);
                value = (value == null || value.trim().isEmpty()) ? "N/A" : value; // Handle null or empty values

                // Convert 'Finalised' column back to 'Yes'/'No'
                if (columnName.equals("Finalised")) {
                    value = value.equals("1") ? "Yes" : (value.equals("0") ? "No" : value);
                }

                row.put(columnName, value);
                columnWidths.put(columnName, Math.max(columnWidths.get(columnName), value.length()));
            }
            data.add(row);
        }

        if (data.isEmpty()) {
            System.out.println("\nNo data found for " + title + ".\n");
            return;
        }

        // Build format string for the table
        StringBuilder formatBuilder = new StringBuilder("|");
        for (String columnName : columnNames) {
            formatBuilder.append(" %-" + columnWidths.get(columnName) + "s |");
        }
        formatBuilder.append("%n");
        String format = formatBuilder.toString();

        // Display table with title and borders
        System.out.println("\n" + title);
        printBorder(columnWidths, columnNames);
        System.out.printf(format, columnNames.toArray());
        printBorder(columnWidths, columnNames);

        // Print each row of data
        for (Map<String, String> row : data) {
            System.out.printf(format, row.values().toArray());
        }

        printBorder(columnWidths, columnNames); // Print final border
    }

    /**
     * Prints the border for the table based on column widths.
     *
     * @param columnWidths The map of column names to their widths.
     * @param columnNames  The list of column names.
     */
    private static void printBorder(Map<String, Integer> columnWidths, List<String> columnNames) {
        StringBuilder border = new StringBuilder("+");
        for (String columnName : columnNames) {
            border.append("-".repeat(columnWidths.get(columnName) + 2)).append("+");
        }
        System.out.println(border);
    }
}
