package utils;

/**
 * Utility for scrubbing SQLLite based column and table names...
 * This is made its own class so it can be called consistently by various
 * class in the FIMS packages for consistent interpretation.
 */
public class SqlLiteNameCleaner {
    /**
     * Generic constructor
     */
    public SqlLiteNameCleaner() {
    }

    /**
     * Ensures that table and column names are valid SQLite identifiers that do
     * not require quoting in brackets for maximum compatibility.  Spaces and
     * periods are replaced with underscores, and if the name starts with a
     * digit, an underscore is added to the beginning of the name.  Any other
     * non-alphanumeric characters are removed.
     *
     * @param tName The table name to fix, if needed.
     * @return The corrected table name.
     */
    public String fixNames(String tName) {
        String newName;

        // replace spaces with underscores
        newName = tName.replace(' ', '_');

        // replace periods with underscores
        newName = newName.replace('.', '_');

        // Remove any remaining non-alphanumeric characters.
        // JBD Note 12/10/2014 added comma as an acceptable character here explicitly so folks can refer to multiple columns
        newName = newName.replaceAll("[^_a-zA-Z0-9(),]", "");

        // if the table name starts with a digit, prepend an underscore
        if (newName.matches("[0-9].*"))
            newName = "_" + newName;

        return newName;
    }
}
