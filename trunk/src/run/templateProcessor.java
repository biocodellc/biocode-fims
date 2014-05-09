package run;

import digester.*;
import org.apache.commons.digester3.Digester;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import settings.FIMSException;
import settings.PathManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * This is a convenience class for working with templates (the spreadsheet generator).
 * We handle building working with the "process" class and the digester rules for mapping & fims,
 * in addition to providing methods for looking up definitions, and building form output
 */
public class templateProcessor {

    private process p;
    private Mapping mapping;
    private Fims fims;
    private Validation validation;
    HSSFSheet defaultSheet;
    HSSFWorkbook workbook;

    HSSFCellStyle headingStyle, regularStyle, requiredStyle, wrapStyle;

    final int NAME = 0;
    final int ENTITY = 1;
    final int URI = 2;
    final int DEFINITION = 3;


    String instructionsSheetName = "Instructions";
    String dataFieldsSheetName = "Data Fields";
    String listsSheetName = "Lists";

    File configFile;
    Integer project_id;

    public templateProcessor(Integer project_id, String outputFolder, Boolean useCache) throws Exception {

        // Instantiate the project output Folder
        configurationFileFetcher fetcher = new configurationFileFetcher(project_id, outputFolder, useCache);
        configFile = fetcher.getOutputFile();

        this.project_id = project_id;
        this.p = new process(outputFolder, configFile);

        mapping = new Mapping();
        p.addMappingRules(new Digester(), mapping);

        fims = new Fims(mapping);
        p.addFimsRules(new Digester(), fims);

        validation = new Validation();
        p.addValidationRules(new Digester(), validation);

        // Create the workbook
        workbook = new HSSFWorkbook();

        // Set the default heading style
        headingStyle = workbook.createCellStyle();
        HSSFFont bold = workbook.createFont();
        bold.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        headingStyle.setFont(bold);

        requiredStyle = workbook.createCellStyle();
        HSSFFont redBold = workbook.createFont();
        redBold.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        redBold.setColor(HSSFFont.COLOR_RED);
        requiredStyle.setFont(redBold);

        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);

        // Set the style for all other cells
        regularStyle = workbook.createCellStyle();
    }

    public Mapping getMapping() {
        return mapping;
    }

    public Fims getFims() {
        return fims;
    }

    public Validation getValidation() {
        return validation;
    }

    /**
     * Get a definition for a particular column name
     *
     * @param column_name
     * @return
     * @throws settings.FIMSException
     */
    public String definition(String column_name) throws FIMSException {
        String output = "";
        try {

            Iterator attributes = mapping.getAllAttributes(mapping.getDefaultSheetName()).iterator();
            while (attributes.hasNext()) {
                Attribute a = (Attribute) attributes.next();
                String column = a.getColumn();
                if (column_name.trim().equals(column.trim())) {

                    if (a.getUri() != null)
                        output += "<b>URI:</b><p><a href='" + a.getUri() + "' target='_blank'>" + a.getUri() + "</a>";
                    else
                        output += "<b>URI:</b><p>No URI available";
                    if (!a.getDefinition().trim().equals(""))
                        output += "<p><b>Definition:</b><p>" + a.getDefinition();
                    else
                        output += "<p><b>Definition:</b><p>No custom definition available";

                    return output;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException("exception handling templates " + e.getMessage(), e);
        }
        return "No definition found for " + column_name;
    }

    /**
     * Generate checkBoxes/Column Names for the mappings in a template
     *
     * @return
     * @throws FIMSException
     */
    public String printCheckboxes() throws FIMSException {
        LinkedList<String> requiredColumns = getRequiredColumns();
        StringBuilder output = new StringBuilder();
        // A list of names we've already added
        ArrayList addedNames = new ArrayList();
        try {
            Iterator attributes = mapping.getAllAttributes(mapping.getDefaultSheetName()).iterator();
            while (attributes.hasNext()) {
                Attribute a = (Attribute) attributes.next();

                // Set the column name
                String column = a.getColumn();

                // Check that this name hasn't been already.  This is necessary in some situations where
                // column names are repeated for different entities in the configuration file
                if (!addedNames.contains(column)) {
                    // Set boolean to tell us if this is a requiredColumn
                    Boolean aRequiredColumn = false;
                    if (requiredColumns == null) {
                        aRequiredColumn = false;
                    } else if (requiredColumns.contains(a.getColumn())) {
                        aRequiredColumn = true;
                    }
                    // Construct the checkbox text
                    //output.append("<label class='checkbox'>\n" +
                    output.append("<input type='checkbox' class='check_boxes' value='" + column + "'");
                    if (aRequiredColumn)
                        output.append(" checked disabled");

                    output.append(">" + column + " \n" +
                            "<a href='#' class='def_link' name='" + column + "'>DEF</a>\n" +
                            "<br>\n");// +
                    //"</label>\n";
                }

                // Now that we've added this to the output, add it to the ArrayList so we don't add it again
                addedNames.add(column);

            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException("exception handling templates " + e.getMessage(), e);
        }
        return output.toString();
    }

    /**
     * This function creates a sheet called "Lists" and then creates the pertinent validations for each of the lists
     *
     * @param fields
     */

    private void createListsSheetAndValidations(List<String> fields) {
        int column;
        HSSFSheet listsSheet = workbook.createSheet(listsSheetName);

        Iterator listsIt = validation.getLists().iterator();
        int listColumnNumber = 0;

        // Loop our array of lists
        while (listsIt.hasNext()) {
            digester.List list = (digester.List) listsIt.next();
            // List of fields from this validation rule
            String[] validationFields = (String[]) list.getFields().toArray(new String[list.getFields().size()]);
            column = fields.indexOf(list.getAlias());

            // Validation Fields
            if (validationFields.length > 0) {

                // populate this validation list in the Lists sheet
                int counterForRows = 0;
                for (int i = 0, length = validationFields.length; i < length; i++) {
                    String value;
                    HSSFCellStyle style;
                    // Write header
                    if (i == 0) {
                        value = list.getAlias();
                        style = headingStyle;
                        HSSFRow row = listsSheet.getRow(i);
                        if (row == null)
                            row = listsSheet.createRow(i);

                        HSSFCell cell = row.createCell(listColumnNumber);
                        cell.setCellValue(value);
                        cell.setCellStyle(style);
                    }
                    // Write cell values
                    // else {
                    value = validationFields[i];
                    style = regularStyle;
                    //counterForRows++;
                    //}

                    // Set the row counter to +1 because of the header issues
                    counterForRows = i + 1;
                    HSSFRow row = listsSheet.getRow(counterForRows);
                    if (row == null)
                        row = listsSheet.createRow(counterForRows);

                    HSSFCell cell = row.createCell(listColumnNumber);
                    cell.setCellValue(value);
                    cell.setCellStyle(style);

                }

                // autosize this column
                listsSheet.autoSizeColumn(listColumnNumber);

                // Get the letter of this column
                String columnLetter = CellReference.convertNumToColString(listColumnNumber);
                Name namedCell = workbook.createName();
                namedCell.setNameName(listsSheetName + columnLetter);
                namedCell.setRefersToFormula(listsSheetName + "!$" + columnLetter + "$2:$" + columnLetter + "$" + counterForRows+2);
                CellRangeAddressList addressList = new CellRangeAddressList(1, 100000, column, column);
                // Set the Constraint to the Lists Table
                DVConstraint dvConstraint = DVConstraint.createFormulaListConstraint(listsSheetName + columnLetter);
                // Create the data validation object
                DataValidation dataValidation = new HSSFDataValidation(addressList, dvConstraint);
                dataValidation.setSuppressDropDownArrow(false);
                dataValidation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
                defaultSheet.addValidationData(dataValidation);
                listColumnNumber++;
            }
        }
    }


    /**
     * Find the required columns on this sheet
     *
     * @return
     */
    public LinkedList<String> getRequiredColumns() {
        LinkedList<String> allRequiredColumns = new LinkedList<String>();
        Iterator worksheetsIt = validation.getWorksheets().iterator();
        while (worksheetsIt.hasNext()) {
            Worksheet w = (Worksheet) worksheetsIt.next();
            Iterator rIt = w.getRules().iterator();
            while (rIt.hasNext()) {
                Rule r = (Rule) rIt.next();
                //System.out.println(r.getType() + r.getColumn() + r.getFields());
                if (r.getType().equals("RequiredColumns")) {
                    allRequiredColumns.addAll(r.getFields());
                }
            }
        }
        if (allRequiredColumns.size() < 1)
            return null;
        else
            return allRequiredColumns;
    }

    /**
     * Create the DataFields sheet
     */
    private void createDataFields() {

        // Create the Instructions Sheet, which is always first
        HSSFSheet dataFieldsSheet = workbook.createSheet(dataFieldsSheetName);

        // First find all the required columns so we can look them up
        LinkedList<String> requiredColumns = getRequiredColumns();


        // Loop through all fields in schema and provide names, uris, and definitions
        Iterator entitiesIt = getMapping().getEntities().iterator();
        int rowNum = 0;
        Row row = dataFieldsSheet.createRow(rowNum++);

        // HEADER ROWS
        Cell cell = row.createCell(NAME);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("ColumnName");

        cell = row.createCell(ENTITY);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Entity");

        cell = row.createCell(URI);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("URI");

        cell = row.createCell(DEFINITION);
        cell.setCellStyle(headingStyle);
        cell.setCellValue("Definition");


        // Must loop entities first
        while (entitiesIt.hasNext()) {
            digester.Entity e = (digester.Entity) entitiesIt.next();
            // Loop attributes
            Iterator attributesIt = ((LinkedList<Attribute>) e.getAttributes()).iterator();

            // Then loop attributes
            while (attributesIt.hasNext()) {
                Attribute a = (Attribute) attributesIt.next();
                row = dataFieldsSheet.createRow(rowNum++);

                // Column Name
                Cell nameCell = row.createCell(NAME);
                nameCell.setCellValue(a.getColumn());
                if (requiredColumns.contains(a.getColumn()))
                    nameCell.setCellStyle(requiredStyle);

                row.createCell(ENTITY).setCellValue(e.getConceptAlias());
                row.createCell(URI).setCellValue(a.getUri());

                // Definition Cell
                Cell defCell = row.createCell(DEFINITION);
                defCell.setCellValue(a.getDefinition());
                defCell.setCellStyle(wrapStyle);
            }
        }
        // Set column width
        dataFieldsSheet.autoSizeColumn(NAME);
        dataFieldsSheet.autoSizeColumn(ENTITY);
        dataFieldsSheet.autoSizeColumn(URI);
        dataFieldsSheet.setColumnWidth(DEFINITION, 80 * 256);
    }

    /**
     * Create the default Sheet
     *
     * @param defaultSheetname
     * @param fields
     */
    private void createDefaultSheet(String defaultSheetname, List<String> fields) {
        // Create the Default Sheet sheet
        defaultSheet = workbook.createSheet(defaultSheetname);

        //Create the header row
        HSSFRow row = defaultSheet.createRow(0);

        // First find all the required columns so we can look them up
        LinkedList<String> requiredColumns = getRequiredColumns();

        // Loop the fields that the user wants in the default sheet
        int columnNum = 0;
        Iterator itFields = fields.iterator();
        while (itFields.hasNext()) {
            String field = (String) itFields.next();
            Cell cell = row.createCell(columnNum++);
            //Set value to new value
            cell.setCellValue(field);
            cell.setCellStyle(headingStyle);

            // Make required columns red
            if (requiredColumns.contains(field))
                cell.setCellStyle(requiredStyle);

        }

        // Auto-size the columns so we can see them all to begin with
        for (int i = 0; i <= columnNum; i++) {
            defaultSheet.autoSizeColumn(i);
        }

    }

    /**
     * Create an instructions sheet
     *
     * @param defaultSheetName
     */
    private void createInstructions(String defaultSheetName) {
        // Create the Instructions Sheet, which is always first
        HSSFSheet instructionsSheet = workbook.createSheet(instructionsSheetName);
        Row row;
        Cell cell;

        // Center align & bold for title
        HSSFCellStyle titleStyle = workbook.createCellStyle();
        HSSFFont bold = workbook.createFont();
        bold.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        titleStyle.setFont(bold);
        titleStyle.setAlignment(CellStyle.ALIGN_CENTER);

        // Make a big first column
        instructionsSheet.setColumnWidth(0, 160 * 256);

        //Fetch the project title from the BCID system
        // NOTE, getting this particular name from the BCID system throws a connection exception
        /* availableProjectsFetcher fetcher = new availableProjectsFetcher();
        availableProject aP = fetcher.getProject(project_id);
        String project_title = aP.getProject_title();
        */
        // Use the shortName
        String project_title = getFims().getMetadata().getShortname();

        // Hide the project_id in the first row
        row = instructionsSheet.createRow(0);
        cell = row.createCell(0);
        cell.setCellValue("~project_id=" + project_id + "~");
        row.setZeroHeight(true);

        // The name of this project as specified by the sheet
        row = instructionsSheet.createRow(2);
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        cell.setCellValue(project_title);

        // Print todays date
        row = instructionsSheet.createRow(3);
        cell = row.createCell(0);
        cell.setCellStyle(titleStyle);
        DateFormat dateFormat = new SimpleDateFormat("MMMMM dd, yyyy");
        Calendar cal = Calendar.getInstance();
        cell.setCellValue("Template generated on " + dateFormat.format(cal.getTime()));

        // Default sheet instructions
        row = instructionsSheet.createRow(5);
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(defaultSheetName + " Tab");

        row = instructionsSheet.createRow(6);
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue("Please fill out each field in the \"" + defaultSheetName + "\" tab as completely as possible. " +
                "Fields in red are required (data cannot be uploaded to the database without these fields). " +
                "Required and recommended fields are usually placed towards the beginning of the template. " +
                "Some fields have a controlled vocabulary associated with them in the \"" + listsSheetName + "\" tab " +
                "and are provided as data validation in the provided cells" +
                "If you have more than one entry to a field (i.e. a list of publications), " +
                "please delimit your list with semicolons (;).  Also please make sure that there are no newline " +
                "characters (=carriage returns) in any of your metadata. Fields in the " + defaultSheetName + " tab may be re-arranged " +
                "in any order so long as you don't change the field names.");

        // data Fields sheet
        row = instructionsSheet.createRow(8);
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(dataFieldsSheetName + " Tab");

        row = instructionsSheet.createRow(9);
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue("This tab contains column names, associated URIs and definitions for each column.");

        //Lists Tab
        row = instructionsSheet.createRow(11);
        cell = row.createCell(0);
        cell.setCellStyle(headingStyle);
        cell.setCellValue(listsSheetName + " Tab");

        row = instructionsSheet.createRow(12);
        cell = row.createCell(0);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue("This tab contains controlled vocabulary lists for certain fields.  DO NOT EDIT this sheet!");
    }

    /**
     * Create the Excel File for output
     *
     * @param defaultSheetname
     * @param uploadPath
     * @param fields
     * @return
     * @throws Exception
     */
    public File createExcelFile(String defaultSheetname, String uploadPath, List<String> fields) throws Exception {

        // Create each of the sheets
        createInstructions(defaultSheetname);
        createDefaultSheet(defaultSheetname, fields);
        createDataFields();
        createListsSheetAndValidations(fields);

        // Write the excel file
        String filename = "output";
        if (getFims().getMetadata().getShortname() != null && !getFims().getMetadata().getShortname().equals(""))
            filename = getFims().getMetadata().getShortname();
        String outputName = filename + ".xls";

        // Create the file
        File file = null;
        file = PathManager.createUniqueFile(outputName, uploadPath);
        FileOutputStream out = new FileOutputStream(file);
        workbook.write(out);
        out.close();

        return file;
    }

    /**
     * main method for testing only
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // File configFile = new configurationFileFetcher(1, "tripleOutput", false).getOutputFile();

        templateProcessor t = new templateProcessor(1, "tripleOutput", false);

        ArrayList<String> a = new ArrayList<String>();
        a.add("materialSampleID");
        a.add("country");
        a.add("phylum");
        a.add("habitat");
        a.add("sex");

        File outputFile = t.createExcelFile("Samples", "tripleOutput", a);
        System.out.println(outputFile.getAbsoluteFile().toString());

        //t.getRequiredColumns();

        //System.out.println(t.printCheckboxes());
    }

    /**
     * Print the abstract text
     * @return
     */
    public String printAbstract() {
        return getFims().getMetadata().getText_abstract();
    }
}
