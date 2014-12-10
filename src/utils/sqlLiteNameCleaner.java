package utils;

/**
 * Utility for scrubbing SQLLite based column and table names...
 * This is made its own class so it can be called consistently by various
 * class in the FIMS packages for consistent interpretation.
 */
public class sqlLiteNameCleaner {
    /**
     * Generic constructor
     */
    public sqlLiteNameCleaner() {
    }

    /**
     * Ensures that table and column names are valid SQLite identifiers that do
     * not require quoting in brackets for maximum compatibility.  Spaces and
     * periods are replaced with underscores, and if the name starts with a
     * digit, an underscore is added to the beginning of the name.  Any other
     * non-alphanumeric characters are removed.
     *
     * @param tname The table name to fix, if needed.
     * @return The corrected table name.
     */
    public String fixNames(String tname) {
        String newname;

        // replace spaces with underscores
        newname = tname.replace(' ', '_');

        // replace periods with underscores
        newname = newname.replace('.', '_');

        // Remove any remaining non-alphanumeric characters.
        // JBD Note 12/10/2014 added comma as an acceptable character here explicitly so folks can refer to multiple columns
        newname = newname.replaceAll("[^_a-zA-Z0-9(),]", "");

        // if the table name starts with a digit, prepend an underscore
        if (newname.matches("[0-9].*"))
            newname = "_" + newname;

        return newname;
    }
}
