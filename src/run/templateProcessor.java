package run;

import digester.*;
import org.apache.commons.digester.Digester;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import settings.FIMSException;
import settings.PathManager;

import java.io.File;
import java.io.FileOutputStream;
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

    HSSFCellStyle headingStyle, regularStyle, requiredStyle;

    final int NAME = 0;
    final int ENTITY = 1;
    final int URI = 2;
    final int DEFINITION = 3;


    public templateProcessor(String outputFolder, File configFile) throws Exception {
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
        String output = "";
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
                    if (requiredColumns.contains(a.getColumn()))
                        aRequiredColumn = true;

                    // Construct the checkbox text
                    output += "<label class='checkbox'>\n" +
                            "\t<input type='checkbox' class='check_boxes' value='" + column + "'";
                    if (aRequiredColumn)
                        output += " checked disabled";

                    output += ">" + column + " \n" +
                            "\t<a href='#' class='def_link' name='" + column + "'>DEF</a>\n" +
                            "</label>\n";
                }

                // Now that we've added this to the output, add it to the ArrayList so we don't add it again
                addedNames.add(column);

            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException("exception handling templates " + e.getMessage(), e);
        }
        return output;
    }

    /**
     * This function creates a sheet called "Lists" and then creates the pertinent validations for each of the lists
     *
     * @param fields
     */
    public void createListsSheetAndValidations(List<String> fields) {
        int column;
        HSSFSheet listsSheet = workbook.createSheet("Lists");

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
                for (int i = 0, length = validationFields.length; i < length; i++) {
                    String value;
                    HSSFCellStyle style;
                    // Write header
                    if (i == 0) {
                        value = list.getAlias();
                        style = headingStyle;
                    }
                    // Write cell values
                    else {
                        value = validationFields[i];
                        style = regularStyle;
                    }

                    HSSFRow row = listsSheet.getRow(i);
                    if (row == null)
                        row = listsSheet.createRow(i);

                    HSSFCell cell = row.createCell(listColumnNumber);
                    cell.setCellValue(value);
                    cell.setCellStyle(style);
                }


                // Get the letter of this column
                String columnLetter = CellReference.convertNumToColString(listColumnNumber);
                Name namedCell = workbook.createName();
                namedCell.setNameName("Lists" + columnLetter);
                namedCell.setRefersToFormula("Lists!$" + columnLetter + "$2:$" + columnLetter + "$" + validationFields.length);
                CellRangeAddressList addressList = new CellRangeAddressList(1, 100000, column, column);
                // Set the Constraint to the Lists Table
                DVConstraint dvConstraint = DVConstraint.createFormulaListConstraint("Lists" + columnLetter);
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
     * Create the instructions sheet
     *
     * @param instructionSheetName
     */
    public void createInstructions(String instructionSheetName) {

        // Create the Instructions Sheet, which is always first
        HSSFSheet instructionsSheet = workbook.createSheet(instructionSheetName);

        // Loop through all fields in schema and provide names, uris, and definitions
        Iterator entitiesIt = getMapping().getEntities().iterator();
        int rowNum = 0;
        Row row = instructionsSheet.createRow(rowNum++);

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
                row = instructionsSheet.createRow(rowNum++);
                row.createCell(NAME).setCellValue(a.getColumn());
                row.createCell(ENTITY).setCellValue(e.getConceptAlias());
                row.createCell(URI).setCellValue(a.getUri());
                row.createCell(DEFINITION).setCellValue(a.getDefinition());
            }
        }
        // Set column width
        instructionsSheet.autoSizeColumn(NAME);
        instructionsSheet.autoSizeColumn(ENTITY);
        instructionsSheet.autoSizeColumn(URI);
        instructionsSheet.setColumnWidth(DEFINITION, 80 * 256);
    }

    /**
     * Create the default Sheet
     *
     * @param defaultSheetname
     * @param fields
     */
    public void createDefaultSheet(String defaultSheetname, List<String> fields) {
        // Create the Default Sheet sheet
        defaultSheet = workbook.createSheet(defaultSheetname);

        //Create the header row
        HSSFRow row = defaultSheet.createRow(0);

        // Loop the fields that the user wants in the default sheet
        int columnNum = 0;
        Iterator itFields = fields.iterator();
        while (itFields.hasNext()) {
            String field = (String) itFields.next();
            Cell cell = row.createCell(columnNum++);
            //Set value to new value
            cell.setCellValue(field);
            cell.setCellStyle(headingStyle);
        }
    }

    /**
     * Create the Excel File for output
     *
     * @param instructionSheetName
     * @param defaultSheetname
     * @param uploadPath
     * @param fields
     * @return
     * @throws Exception
     */
    public File createExcelFile(String instructionSheetName, String defaultSheetname, String uploadPath, List<String> fields) throws Exception {


        createInstructions(instructionSheetName);
        createDefaultSheet(defaultSheetname, fields);
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
        File configFile = new configurationFileFetcher(1, "tripleOutput", false).getOutputFile();

        templateProcessor t = new templateProcessor("tripleOutput", configFile);
        ArrayList<String> a = new ArrayList<String>();
        a.add("materialSampleID");
        a.add("country");
        a.add("phylum");
        a.add("habitat");
        a.add("sex");

        System.out.println(t.createExcelFile("instructions", "Samples", "tripleOutput", a).getAbsoluteFile().toString());
        //t.getRequiredColumns();
        //System.out.println(t.printCheckboxes());
    }
}