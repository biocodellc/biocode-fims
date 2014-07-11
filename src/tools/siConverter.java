package tools;

import com.sun.rowset.internal.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class reads a specially built Excel spreadsheet file and generates spreadsheet templates
 */
public class siConverter {
    static public ArrayList<siProjects> projects = new ArrayList<siProjects>();
    static File inputFile;
    static File output_directory;
    static Sheet MatrixSheet;
    static Sheet ListsSheet;
    static Sheet PreparationsSheet;


    static Integer columnIndex;
    static Integer definitionIndex;
    static Integer uriIndex;
    static Integer groupIndex;
    static Integer SIFieldTemplate;
    static Integer globalValidationRuleIndex;

    static String worksheetUniqueKey = "Primary Coll. Number";

    static ArrayList<String> requiredColumns = new ArrayList<String>();
    static ArrayList<String> desiredColumns = new ArrayList<String>();
    static ArrayList<siRuleProcessor> globalValidationRules = new ArrayList<siRuleProcessor>();

    public siConverter() throws IOException, InvalidFormatException {


    }

    public static Integer getColumnIndex(String columnName) {
        Row row = MatrixSheet.getRow(0);

        Iterator rowIt = row.iterator();
        int count = 0;
        while (rowIt.hasNext()) {
            Cell cell = row.getCell(count);
            if (cell.getStringCellValue().equalsIgnoreCase(columnName))
                return count;
            count++;
        }
        return null;
    }


    public static String metadata(String projectName) {
        return "<metadata " +
                " doi=\"a doi\" " +
                " shortname=\"Smithsonian " + projectName + "\" " +
                " eml_location=\"eml_location\" " +
                " target=\"http://data.biscicol.org/ds/data\" " +
                " queryTarget=\"http://data.biscicol.org/ds\">\n" +
                "\t<![CDATA[Some text with abstract]]>\n" +
                "</metadata>\n";
    }

    public static String header() {
        return "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<fims>\n";

    }

    public static String mapping(siProjects p) {
        StringBuilder sb = new StringBuilder();

        Integer projectIndex = getColumnIndex(p.columnName);
        Integer rows = MatrixSheet.getLastRowNum();

        // Write the mapping element
        sb.append("<mapping>\n" +
                "\t<entity " +
                "worksheet=\"Samples\" " +
                "worksheetUniqueKey=\"" + worksheetUniqueKey + "\" " +
                "conceptAlias=\"Resource\" " +
                "conceptURI=\"http://www.w3.org/2000/01/rdf-schema#Resource\" " +
                "entityID=\"1\">");

        for (int i = 0; i < rows; i++) {
            Row row = MatrixSheet.getRow(i);
            String value = "", siTemplateValue = "", globalValidationValue = "";
            try {
                Cell cell = row.getCell(projectIndex);
                value = cell.getStringCellValue();
            } catch (Exception e) {
                System.err.println("Unable to process value on line " + row.getRowNum());
            }

            try {
                Cell siTemplateCell = row.getCell(SIFieldTemplate);
                siTemplateValue = siTemplateCell.getStringCellValue();
            } catch (Exception e) {
                System.err.println("Unable to process siTemplateCell on line " + row.getRowNum());
            }

            try {
                Cell globalValidationCell = row.getCell(globalValidationRuleIndex);
                globalValidationValue = globalValidationCell.getStringCellValue();
            } catch (Exception e) {
                System.err.println("Unable to process globalValidationRule on line " + row.getRowNum());
            }

            // Must have template value as Y and some designation in sheet of p/m/d
            if (siTemplateValue.equalsIgnoreCase("Y") &&
                    (value.equalsIgnoreCase("p") ||
                            value.equalsIgnoreCase("m") ||
                            value.equalsIgnoreCase("d"))) {

                String column = row.getCell(columnIndex).toString();
                String definition = row.getCell(definitionIndex).toString();
                String uri = "urn:" + row.getCell(uriIndex).toString();
                String group = row.getCell(groupIndex).toString();

                String defined_by = uri;

                sb.append("\t\t<attribute ");
                sb.append("column='" + column + "' ");
                sb.append("uri='" + uri + "' ");
                sb.append("group='" + group + "' ");
                sb.append("defined_by='" + defined_by + "'>");
                sb.append("<![CDATA[" + definition + "]]>");
                sb.append("</attribute>\n");

                // Populate required and desired columns here, used in validation step
                if (value.equalsIgnoreCase("m")) {
                    requiredColumns.add(column);
                } else if (value.equalsIgnoreCase("d")) {
                    desiredColumns.add(column);
                }

                // Populate other global validation Rules
                if (globalValidationValue != null && !globalValidationValue.equals("")) {
                    try {
                        globalValidationRules.add(new siRuleProcessor(globalValidationValue, column));
                    } catch (Exception e) {
                        System.err.println("Unable to process " + globalValidationValue);
                    }
                }

            }
        }

        // Close the entity / mapping sections
        sb.append(
                "\t</entity>\n" +
                        "</mapping>\n");
        return sb.toString();
    }

    public static String validation() {
        StringBuilder sbValidation = new StringBuilder();

        // header
        sbValidation.append("<validation>\n");

        // Beginning of the Samples worksheet validation section
        sbValidation.append("\t<worksheet sheetname='Samples'>\n");

        // generic rule for all columns
        sbValidation.append("\t\t<rule type='duplicateColumnNames' level='error'></rule>\n");

        // uniqueValue constraint
        sbValidation.append("\t\t<rule type='uniqueValue' column='materialSampleID' level='error'></rule>\n");

        // Required columns
        sbValidation.append("\t\t<rule type='RequiredColumns' column='RequiredColumns' level='error'>\n");
        Iterator mIt = requiredColumns.iterator();
        while (mIt.hasNext()) {
            sbValidation.append("\t\t\t<field>" + mIt.next().toString() + "</field>\n");
        }
        sbValidation.append("\t\t</rule>\n");

        // Desired columns
        sbValidation.append("\t\t<rule type='RequiredColumns' column='RequiredColumns' level='warning'>\n");
        Iterator dIt = desiredColumns.iterator();
        while (dIt.hasNext()) {
            sbValidation.append("\t\t\t<field>" + dIt.next().toString() + "</field>\n");
        }
        sbValidation.append("\t\t</rule>\n");

        // Any other rules that we found when looking at the sheet which are defined by JSON
        Iterator gIt = globalValidationRules.iterator();
        while (gIt.hasNext()) {
            //siRuleProcessor ruleProcessor = new siRuleProcessor(gIt.next().toString());
            //sbValidation.append("\t\t\tANOTHER RULE:" + gIt.next().toString() + "\n");
            sbValidation.append(((siRuleProcessor) gIt.next()).print());
        }

        // end of worksheet specific section
        sbValidation.append("\t</worksheet>\n");

        // Generate Lists section
        sbValidation.append(lists());

        sbValidation.append("</validation>\n");

        return sbValidation.toString();
    }

    /**
     * Generate the elements for the Lists worksheet
     *
     * @return
     */
    private static String lists() {
        //ListsSheet.
        StringBuilder sb = new StringBuilder();

        sb.append("\t<lists>\n");

        // Build the column references
        List<String> columns = new ArrayList<String>();
        Boolean lastColumn = false;
        int columnNum = 0;
        Row header = ListsSheet.getRow(0);
        while (!lastColumn) {
            Cell c = header.getCell(columnNum++);
            if (c != null) {
                columns.add(c.getStringCellValue());
            } else {
                lastColumn = true;
            }
        }

        // Now get all the elements in this column
        Iterator columnIt = columns.iterator();
        int columnCounter = 0;
        while (columnIt.hasNext()) {
            String columnName = columnIt.next().toString();
            if (!columnName.equals("")) {
                sb.append("\t\t<list alias='" + columnName + "' caseInsensitive='true'>\n");

                // Loop through each fow for this column
                for (Row row : ListsSheet) {
                    Cell c = row.getCell(columnCounter);
                    String value = null;
                    if (row.getRowNum() > 0) {
                        // Grab value if Cell !=null
                        if (c != null) {
                            value = c.getStringCellValue().toString();
                        }
                        // populate field if value !=null
                        if (value != null && !value.trim().equals("")) {
                            sb.append("\t\t\t<field>" + value + "</field>\n");
                        }
                    }
                }
                sb.append("\t\t</list>\n");
            }
            columnCounter++;

        }

        sb.append("\t</lists>\n");

        return sb.toString();
    }

    public static String footer() {
        return "</fims>";
    }

    public static void writeFile(File file, String content) {
        FileOutputStream fop = null;
        try {
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // get the content in bytes
            byte[] contentInBytes = content.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

            System.out.println("Done writing " + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initialize our environment
     *
     * @throws IOException
     * @throws InvalidFormatException
     */
    public static void init() throws IOException, InvalidFormatException {
        output_directory = new File(System.getProperty("user.dir") + System.getProperty("file.separator") +
                "Documents" + System.getProperty("file.separator") +
                "Smithsonian" + System.getProperty("file.separator"));

        inputFile = new File(output_directory.getAbsolutePath() + System.getProperty("file.separator") + "si_master.xlsx");

        System.out.println("Reading " + inputFile.getAbsoluteFile());

        projects.add(new siProjects(14, "SIBOT", "Botany"));
        /*    projects.add(new siProjects(15, "SIENT", "Entomology"));
        projects.add(new siProjects(16, "SIINV", "Invertebrate Zoology"));
        projects.add(new siProjects(17, "SIVZA", "VZ-Amphibians and Reptiles"));
        projects.add(new siProjects(18, "SIVZB", "VZ-Birds"));
        projects.add(new siProjects(19, "SIVZF", "VZ-Fishes"));
        projects.add(new siProjects(20, "SIVZM", "VZ-Mammals"));
        projects.add(new siProjects(21, "SIMIN", "Mineral Sciences"));
        */

        InputStream inp = new FileInputStream(inputFile);
        Workbook workbook = WorkbookFactory.create(inp);

        // Get all the sheets that we expect to be using
        MatrixSheet = workbook.getSheet("Matrix");
        ListsSheet = workbook.getSheet("Lists");
        PreparationsSheet = workbook.getSheet("Preparations");

    }

    public static void main(String[] args) throws IOException, InvalidFormatException {
        init();

        columnIndex = getColumnIndex("EMu Field Label (Vernacular)");
        definitionIndex = getColumnIndex("Definitions");
        uriIndex = getColumnIndex("Primary Field Name");
        groupIndex = getColumnIndex("Field Group");
        SIFieldTemplate = getColumnIndex("SI Field Template Flag");
        globalValidationRuleIndex = getColumnIndex("Global Validation Rule");

        // Loop each of the projects
        Iterator projectsIt = projects.iterator();
        while (projectsIt.hasNext()) {
            StringBuilder sb = new StringBuilder();

            siProjects project = (siProjects) projectsIt.next();

            // Print the header
            sb.append(header());

            // Write the metadata element
            sb.append(metadata(project.columnName));

            // Print the mapping element
            sb.append(mapping(project));

            // Print the validations element
            sb.append(validation());

            // Print the footers
            sb.append(footer());

            // Write the actual file
            File outputFile = new File(
                    output_directory.getAbsolutePath() +
                            System.getProperty("file.separator") +
                            project.abbreviation +
                            ".xml");
            writeFile(outputFile, sb.toString());

            System.out.println(sb.toString());
        }

    }


}

class siProjects {
    Integer project_id;
    String abbreviation;
    String columnName; // The column in the SI provided sheet that designates this resource

    siProjects(Integer project_id, String abbreviation, String columnName) {
        this.project_id = project_id;
        this.abbreviation = abbreviation;
        this.columnName = columnName;
    }
}

