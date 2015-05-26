package run;

import digester.Attribute;
import digester.Mapping;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import settings.FIMSRuntimeException;

import java.io.*;
import java.util.ArrayList;

/**
 * Adds a BCID column at the end of a specified spreadsheet
 */
public class SIServerSideSpreadsheetTools {

    private XSSFSheet sheet;
    private XSSFWorkbook workbook;
    private String bcidRoot;
    private String localIDColumnName;
    private Integer userID;
    private Boolean SIMethod = false; // default SIMethod to false
    private String ezidColumnName = "BCID";

    /**
     * Create the guidIfier with everything it needs to accomplish its mission:
     * reading an inputWoorkbook, using the locally unique identifier,
     * and appending it onto an ARKRoot and writing it to the last column of the specified sheet.
     * wait for the run() method to be called to actually write our output spreadsheet
     *
     * @param sourceFile
     * @param sheetName
     * @param localIDColumnName
     * @param bcidRoot
     */
    public SIServerSideSpreadsheetTools(
            File sourceFile,
            String sheetName,
            String localIDColumnName,
            String bcidRoot) {
        // Assign class level variables
       try {
            FileInputStream fis = new FileInputStream(sourceFile);
            this.workbook = new XSSFWorkbook(fis);
        } catch (FileNotFoundException e) {
            throw new FIMSRuntimeException(500, e);
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
        this.sheet = workbook.getSheet(sheetName);
        this.bcidRoot = bcidRoot;
        this.localIDColumnName = localIDColumnName;
        this.SIMethod = false;

    }

    /**
     * Create the guidIfier with the SI-specific rules for guidifying an identifier.
     * reading an inputWoorkbook, using the userID (as an argument), and appending the rowNum (based on sheet).
     * and appending it onto an ARKRoot and writing it to the last column of the specified sheet.
     * wait for the run() method to be called to actually write our output spreadsheet
     *
     * @param sourceFile
     * @param sheetName
     * @param userID
     * @param bcidRoot
     */
    public SIServerSideSpreadsheetTools(
            File sourceFile,
            String sheetName,
            Integer userID,
            String bcidRoot) {
        // Assign class level variables
        try {
            FileInputStream fis = new FileInputStream(sourceFile);
            this.workbook = new XSSFWorkbook(fis);
        } catch (FileNotFoundException e) {
            throw new FIMSRuntimeException(500, e);
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
        this.sheet = workbook.getSheet(sheetName);
        this.bcidRoot = bcidRoot;
        this.userID = userID;
        this.SIMethod = true;
    }

    /**
     * Write GUIDs onto last column of spreadsheet
     */
    public void guidify() {
        // Add the suffixPassthroughGUID (the BCID) to the end of the sheet
        if (SIMethod)
            addSuffixPassthroughGuidSIMethod();
        else
            addSuffixPassthroughGuid();

    }


    /**
     * Get the column number to use for the EZID column
     *
     * @return
     */
    private Integer getEZIDColumnNum() {
        // See if the column name exists, and if so, return that position
        Integer columnIndex = getColumnIndex(sheet.getRow(0), ezidColumnName);
        // A value of 0 means it was NOT found, negative i presume is an error and positive integers are a good sign
        if (columnIndex > 0) {
            return columnIndex;
        }
        // Put the BCID in the last column place.  Num Columns works out because this is an absolute number,
        // while the ezidColumnName is an array index, so it gets placed in the spot after the last column
        else {
            return sheet.getRow(0).getPhysicalNumberOfCells();
        }

    }

    /**
     * ONLY use this method for SI cases... it is specifically only to their implementation
     */
    private void addSuffixPassthroughGuidSIMethod() {
        int ezidColumnNum = getEZIDColumnNum();

        //int localIdColumnNum = getColumnIndex(sheet.getRow(0), localIDColumnName);
        DataFormatter fmt = new DataFormatter();

        int rowNum = 0;
        for (Row row : sheet) {
            // This is the header
            if (rowNum == 0) {
                Cell cell = row.createCell(ezidColumnNum);
                cell.setCellValue(ezidColumnName);
            }
            // The EZID cell contents
            else {
                //Cell localIDCell = row.getCell(localIdColumnNum);
                String guid = bcidRoot;
                guid += userID + "." + rowNum;

                // Set the Cell's value and Style
                Cell guidCell = row.createCell(ezidColumnNum);
                guidCell.setCellValue(guid);

            }
            rowNum++;
        }
    }

    /**
     * This is the preferred method of implementation
     */
    private void addSuffixPassthroughGuid() {
        int ezidColumnNum = getEZIDColumnNum();

        int localIdColumnNum = getColumnIndex(sheet.getRow(0), localIDColumnName);
        DataFormatter fmt = new DataFormatter();

        int count = 0;
        for (Row row : sheet) {
            // This is the header
            if (count == 0) {
                Cell cell = row.createCell(ezidColumnNum);
                cell.setCellValue(ezidColumnName);
            }
            // The EZID cell contents
            else {
                Cell localIDCell = row.getCell(localIdColumnNum);
                String guid = bcidRoot;

                // If the localID Is a formula try and guess the results
                if (localIDCell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                    switch (localIDCell.getCachedFormulaResultType()) {
                        case Cell.CELL_TYPE_NUMERIC:
                            // Remove any trailing .0
                            guid += String.valueOf(localIDCell.getNumericCellValue()).split("\\.0")[0];
                            break;
                        case Cell.CELL_TYPE_STRING:
                            guid += String.valueOf(localIDCell.getRichStringCellValue());
                            break;
                    }
                } else {
                    guid += fmt.formatCellValue(localIDCell);
                }

                // Set the Cell's value and Style
                Cell guidCell = row.createCell(ezidColumnNum);
                guidCell.setCellValue(guid);

            }
            count++;
        }
    }

    private int getColumnIndex(Row row, String localIDColumnName) {
        for (Cell cell : row) {
            if (cell.getStringCellValue().equalsIgnoreCase(localIDColumnName)) {
                return cell.getColumnIndex();
            }
        }
        return 0;
    }

    /**
     * Write the resulting spreadsheet
     * @param outputFile
     */
    public void write(File outputFile) {
        try {
            FileOutputStream fileOut = new FileOutputStream(outputFile);
            workbook.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

    /**
     * For testing
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            File configFile = new File("/Users/xxxx/IdeaProjects/biocode-fims/sampledata/indoPacificConfiguration_v2.xml");
            processController pc = new processController();

            process p = new process(
                    "/Users/xxxx/Downloads/indoPacificTemplate.1.xlsx",
                    "/Users/xxxx/jetty-files",
                    pc,
                    configFile
            );

            p.runValidation();
            Mapping map = p.getProcessController().getValidation().getMapping();

            SIServerSideSpreadsheetTools tools = new SIServerSideSpreadsheetTools(
                    new File("/Users/xxxx/Downloads/indoPacificTemplate.1.xlsx"),
                    "Samples",
                    "Preparator Number",
                    "ark:/whosyourdaddy/"
            );
            tools.addInternalRowToHeader(map, true);

            tools.write(new File("/Users/xxxx/jetty-files/indoPacificTemplate.2.xlsx"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a row to header that gets written internally
     * @param mapping
     * @param replaceHeader
     */
    public void addInternalRowToHeader(Mapping mapping, Boolean replaceHeader) {
        Row columnInternalRow;
        // Get the header. This should be the 1st row in the sheet and will contain the column names.
        Row columnRow = sheet.getRow(0);

        // If we aren't replacing the header, then create a new row ontop of the current header.
        if (!replaceHeader) {
            sheet.shiftRows(0, sheet.getLastRowNum(), 1);
            sheet.createRow(0);

            // assign the newly created row
            columnInternalRow = sheet.getRow(0);

        } else {
            // set the columnInternalRow equal to the columnRow, since we are replacing the header
            columnInternalRow = columnRow;
        }
        ArrayList<Attribute> attributeList = mapping.getAllAttributes(sheet.getSheetName());

        for (Cell c : columnRow) {
            for (Attribute attribute : attributeList) {
                // when we find the corresponding attribute to the column, insert the column_internal prop.
                if (c.getStringCellValue().equalsIgnoreCase(attribute.getColumn())) {
                    Cell columnInternalCell = columnInternalRow.createCell(c.getColumnIndex());
                    columnInternalCell.setCellValue(attribute.getColumn_internal());
                }
            }
        }
    }
}
