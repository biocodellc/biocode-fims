package tools;

public class siProjects {
    public Integer project_id;
    public String abbreviation;
    public String columnName; // The column in the SI provided sheet that designates this resource
    public String worksheetUniqueKey;

    siProjects(Integer project_id, String abbreviation, String columnName, String worksheetUniqueKey) {
        this.project_id = project_id;
        this.abbreviation = abbreviation;
        this.columnName = columnName;
        this.worksheetUniqueKey = worksheetUniqueKey;
    }
}
