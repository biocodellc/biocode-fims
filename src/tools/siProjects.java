package tools;

/**
 * A class to define the core components of an SI project
 */
public class siProjects {
    public String abbreviation;
    public String columnName; // The column in the SI provided sheet that designates this resource
    public String worksheetUniqueKey;

    siProjects( String abbreviation, String columnName, String worksheetUniqueKey) {
        this.abbreviation = abbreviation;
        this.columnName = columnName;
        this.worksheetUniqueKey = worksheetUniqueKey;
    }
}
