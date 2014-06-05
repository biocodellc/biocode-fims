package tools;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class reads a specially built Excel spreadsheet file and generates spreadsheet templates
 */
public class siConverter {
    static public ArrayList<siProjects> projects = new ArrayList<siProjects>();
    static File inputFile;
    static File output_directory;
    static Sheet sheet;

    static Integer columnIndex;
    static Integer definitionIndex;
    static Integer uriIndex;
    static Integer groupIndex;
    static String worksheetUniqueKey = "Primary Coll. Number";

    public siConverter() throws IOException, InvalidFormatException {


    }

    public static Integer getColumnIndex(String columnName) {
        Row row = sheet.getRow(0);

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
        Integer rows = sheet.getLastRowNum();

        // Write the mapping element
        sb.append("<mapping>\n" +
                "\t<entity " +
                "worksheet=\"Samples\" " +
                "worksheetUniqueKey=\"" + worksheetUniqueKey + "\" " +
                "conceptAlias=\"Resource\" " +
                "conceptURI=\"http://www.w3.org/2000/01/rdf-schema#Resource\" " +
                "entityID=\"1\">");

        for (int i = 0; i < rows; i++) {
            Row row = sheet.getRow(i);
            Cell cell = row.getCell(projectIndex);
            String value = cell.getStringCellValue();
            if (value.equalsIgnoreCase("x")) {
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

            }
        }

        // Close the entity / mapping sections
        sb.append(
                "\t</entity>\n" +
                        "</mapping>\n");
        return sb.toString();
    }

    public static String validation() {
        String validation =
                "<validation>\n" +
                        "\t<worksheet sheetname='Samples'>\n" +
                        "\t\t<rule type='duplicateColumnNames' level='error'></rule>\n" +
                        "\t\t<rule type='uniqueValue' column='materialSampleID' level='error'></rule>\n" +
                        "\t</worksheet>\n" +
                        "</validation>\n";
        return validation;
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
        projects.add(new siProjects(15, "SIENT", "Entomology"));
        projects.add(new siProjects(16, "SIINV", "Invertebrate Zoology"));
        projects.add(new siProjects(17, "SIVZA", "VZ-Amphibians and Reptiles"));
        projects.add(new siProjects(18, "SIVZB", "VZ-Birds"));
        projects.add(new siProjects(19, "SIVZF", "VZ-Fishes"));
        projects.add(new siProjects(20, "SIVZM", "VZ-Mammals"));
        projects.add(new siProjects(21, "SIMIN", "Mineral Sciences"));

        InputStream inp = new FileInputStream(inputFile);
        Workbook workbook = WorkbookFactory.create(inp);
        sheet = workbook.getSheetAt(0);

    }

    public static void main(String[] args) throws IOException, InvalidFormatException {
        init();

        columnIndex = getColumnIndex("EMu Field Label (Vernacular)");
        definitionIndex = getColumnIndex("Definition");
        uriIndex = getColumnIndex("Field Name");
        groupIndex = getColumnIndex("Field Group");

        // Loop each of the projects
        Iterator projectsIt = projects.iterator();
        while (projectsIt.hasNext()) {
            StringBuilder sb = new StringBuilder();

            siProjects project = (siProjects) projectsIt.next();

            // Print the header
            sb.append(header());

            // Write the metadata element
            sb.append(metadata(project.columnName));

            // Print the validations element
            sb.append(validation());

            // Print the mapping element
            sb.append(mapping(project));

            // Print the footers
            sb.append(footer());

            // Write the actual file
            File outputFile = new File(
                    output_directory.getAbsolutePath() +
                            System.getProperty("file.separator") +
                            project.abbreviation +
                            ".xml");
            writeFile(outputFile, sb.toString());
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

