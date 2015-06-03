package digester;

import settings.fimsPrinter;

import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * Attribute representation
 */
public class Attribute implements Comparable {
    private String isDefinedByURIString = "http://www.w3.org/2000/01/rdf-schema#isDefinedBy";

    private String group;
    private String column;
    private String column_internal;
    private String uri;
    private String defined_by;
    private String datatype = "string";  // string is default type
    private String definition;
    private String synonyms;
    private String dataformat;
    private String delimited_by;
    private String type;

    public String getColumn() {
        return column;
    }

    /**
     * set the Column name. Here we normalize column names to replace spaces with underscore and remove an forward /'s
     * @param column
     */
    public void setColumn(String column) {
        this.column = column.replace(" ","_").replace("/","");
    }

    public String getColumn_internal() {
        return column_internal;
    }

    public void setColumn_internal(String column_internal) {
        this.column_internal = column_internal;
    }

    public String getDelimited_by() {
        return delimited_by;
    }

    public void setDelimited_by(String delimited_by) {
        this.delimited_by = delimited_by;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDefined_by() {
        return defined_by;
    }

    public void setDefined_by(String defined_by) {
        this.defined_by = defined_by;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDefinition() {
        return definition;
    }

    public void addDefinition(String definition) {
        this.definition = definition;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void addSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getDataFormat() {
        return dataformat;
    }

    public void addDataFormat(String dataFormat) {
        this.dataformat = dataFormat;
    }

    /**
     * Basic Text printer
     */
    public void print() {
        fimsPrinter.out.println("  Attribute:");
        fimsPrinter.out.println("    column=" + column);
        fimsPrinter.out.println("    uri=" + uri);
        fimsPrinter.out.println("    datatype=" + datatype);
        fimsPrinter.out.println("    isDefinedBy=" + defined_by);
        fimsPrinter.out.println("    column_internal=" + column_internal);
    }

    /**
     * * Generate D2RQ Mapping Language representation of this Attribute.
     *
     * @param pw       PrintWriter used to write output to.
     * @param parent
     * @param colNames
     */
    public void printD2RQ(PrintWriter pw, Object parent, List<String> colNames) {

        String classMap = ((Entity) parent).classMap();
        String table = ((Entity) parent).getWorksheet();
        String classMapStringEquivalence = "";

        Boolean runColumn = false;

        if (colNames.contains(column)) {
            runColumn = true;
        }

        // Only print this column if it is in a list of colNames
        if (runColumn) {
            String classMapString = "map:" + classMap + "_" + column;
            pw.println(classMapString + " a d2rq:PropertyBridge;");
            pw.println("\td2rq:belongsToClassMap " + "map:" + classMap + ";");
            pw.println("\td2rq:property <" + uri + ">;");
            pw.println("\td2rq:column \"" + table + "." + column + "\";");
            pw.println("\td2rq:condition \"" + table + "." + column + " <> ''\";");
            // Specify an equivalence, which is isDefinedBy
            classMapStringEquivalence = classMapString + "_Equivalence";
            pw.println("\td2rq:additionalPropertyDefinitionProperty " + classMapStringEquivalence + ";");
            pw.println("\t.");

            // Always use isDefinedBy, even if the user has not expressed it explicitly.  We do this by
            // using the uri value if NO isDefinedBy is expressed.
            pw.println(classMapStringEquivalence + " a d2rq:AdditionalProperty;");
            pw.println("\td2rq:propertyName <" + isDefinedByURIString + ">;");
            if (defined_by != null) {
                pw.println("\td2rq:propertyValue <" + defined_by + ">;");
            } else {
                pw.println("\td2rq:propertyValue <" + uri + ">;");
            }
            pw.println("\t.");
            /*
           Loop multi-value columns
           This is used when the Configuration file indicates an attribute that should be composed of more than one column
            */
        } else if (column.contains(",")) {

            // TODO: clean this up and integrate with above code.
            String tempColumnName = column.replace(",", "");

            String[] columns = column.split(",");

            // Check if we should run this -- all columns need to be present in colNames list
            Boolean runMultiValueColumn = true;
            for (int i = 0; i < columns.length; i++) {
                if (!colNames.contains(columns[i])) {
                    runMultiValueColumn = false;
                }
            }

            // Only run this portion if the tempColumnName appears
            if (runMultiValueColumn) {

                String classMapString = "map:" + classMap + "_" + tempColumnName;
                pw.println(classMapString + " a d2rq:PropertyBridge;");
                pw.println("\td2rq:belongsToClassMap " + "map:" + classMap + ";");
                pw.println("\td2rq:property <" + uri + ">;");

                // Construct SQL Expression
                StringBuilder result = new StringBuilder();

                // Call this a sqlExpression
                result.append("\td2rq:sqlExpression \"");

                // Append ALL columns together using the delimiter... ALL are required
                if (type.equals("all")) {
                    for (int i = 0; i < columns.length; i++) {
                        if (i != 0)
                            result.append(" || '" + delimited_by + "' || ");
                        // Set required function parameters
                        if (type.equals("all"))
                            pw.println("\td2rq:condition \"" + table + "." + columns[i] + " <> ''\";");
                        result.append(columns[i]);
                    }
                    result.append("\";");
                }

                // This is the YMD case using a very special SQLIte function to format data
                // Assume that columns are Year, Month, and Day EXACTLY
                else if (type.equals("ymd")) {
                    // Require Year
                    pw.println("\td2rq:condition \"" + table + "." + columns[0] + " <> ''\";");

                    result.append("yearCollected ||  ifnull(nullif('-'||substr('0'||monthCollected,-2,2),'-0') || " +
                            "ifnull(nullif('-'||substr('0'||dayCollected,-2,2),'-0'),'')" +
                            ",'') ");
                    result.append("\";");

                }

                pw.println(result.toString());

                //pw.println("\td2rq:column \"" + table + "." + column + "\";");
                //pw.println("\td2rq:condition \"" + table + "." + column + " <> ''\";");

                // Specify an equivalence, which is isDefinedBy
                classMapStringEquivalence = classMapString + "_Equivalence";
                pw.println("\td2rq:additionalPropertyDefinitionProperty " + classMapStringEquivalence + ";");
                pw.println("\t.");

                // Always use isDefinedBy, even if the user has not expressed it explicitly.  We do this by
                // using the uri value if NO isDefinedBy is expressed.
                pw.println(classMapStringEquivalence + " a d2rq:AdditionalProperty;");
                pw.println("\td2rq:propertyName <" + isDefinedByURIString + ">;");
                if (defined_by != null) {
                    pw.println("\td2rq:propertyValue <" + defined_by + ">;");
                } else {
                    pw.println("\td2rq:propertyValue <" + uri + ">;");
                }
                pw.println("\t.");

            }
        }
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
