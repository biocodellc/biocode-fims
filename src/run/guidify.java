package run;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

/**
 * Adds a BCID column at the end of a specified spreadsheet
 */
public class guidify {

    private XSSFSheet sheet;
    private XSSFWorkbook workbook;
    private String bcidRoot;
    private String localIDColumnName;

    /**
     * Create the guidIfier with everything it needs to accomplish its mission:
     * reading an inputWoorkbook, and looking at a sheet, figuring out the locally unique identifier,
     * and appending it onto an ARKRoot and writing it to the last column of the specified sheet.
     * wait for the run() method to be called to actually write our output spreadsheet
     *
     * @param sourceFile
     * @param sheetName
     * @param localIDColumnName
     * @param bcidRoot
     *
     * @throws IOException
     */
    public guidify(
            File sourceFile,
            String sheetName,
            String localIDColumnName,
            String bcidRoot
    ) throws IOException {
        // Assign class level variables
        FileInputStream fis = new FileInputStream(sourceFile);
        this.workbook = new XSSFWorkbook(fis);
        this.sheet = workbook.getSheet(sheetName);
        this.bcidRoot = bcidRoot;
        this.localIDColumnName = localIDColumnName;
    }

    /**
     * Construct the output spreadsheet and then write it
     *
     * @throws IOException
     */
    public void getSpreadsheet(File outputFile) throws IOException {
        // Add the suffixPassthroughGUID (the BCID) to the end of the sheet
        addSuffixPassthroughGuid();
        // Write the output
        write(outputFile);
    }

    private void addSuffixPassthroughGuid() throws NullPointerException {
        int numColumns = sheet.getRow(0).getPhysicalNumberOfCells();
        // Put the BCID in the last column place.  Num Columns works out because this is an absolute number,
        // while the ezidColumnName is an array index, so it gets placed in the spot after the last column
        int ezidColumnNum = numColumns;
        int localIdColumnNum = getColumnIndex(sheet.getRow(0), localIDColumnName);
        DataFormatter fmt = new DataFormatter();

        int count = 0;
        for (Row row : sheet) {
            // This is the header
            if (count == 0) {
                Cell cell = row.createCell(ezidColumnNum);
                cell.setCellValue("BCID");
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

    private void write(File outputFile) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(outputFile);
        workbook.write(fileOut);
        fileOut.close();
    }

    /**
     * For testing
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            guidify guidIfier = new guidify(
                    new File("/Users/jdeck/Downloads/TMO_Beliz_16Sep2014.xlsx"),
                    "Samples",
                    "Preparator Number",
                    "ark:/whosyourdaddy/"
            );
            guidIfier.getSpreadsheet(new File("/Users/jdeck/Downloads/TMO_Beliz_16Sep2014_out.xlsx"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
