package reader.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.joda.time.DateTime;

/**
 * TabularDataReader for Excel-format spreadsheet files.  Both Excel 97-2003
 * format (*.xls) and Excel XML (*.xlsx) format files are supported.  The reader
 * attempts to infer if cells containing numerical values actually contain dates
 * by checking if the cell is date-formatted.  It so, the numerical value is
 * converted to a standard ISO8601 date/time string (yyyy-MM-ddTHH:mm:ss.SSSZZ).
 * This should work properly with both the Excel "1900 Date System" and the
 * "1904 Date System".  Also, the first row in each worksheet is assumed to
 * contain the column headers for the data and determines how many columns are
 * examined for all subsequent rows.
 */
public class ExcelReader implements TabularDataReader {
    // iterator for moving through the active worksheet
    private Iterator<Row> rowiter = null;
    private boolean hasnext = false;

    Row nextrow;

    // The number of columns in the active worksheet (set by the first row).
    private int numcols;

    // The index for the active worksheet.
    private int currsheet;

    // The entire workbook (e.g., spreadsheet file).
    private Workbook excelwb;

    // DataFormatter and FormulaEvaluator for dealing with cells with formulas.
    private DataFormatter df;
    private FormulaEvaluator fe;

    public String getShortFormatDesc() {
        return "Microsoft Excel";
    }

    public String getFormatString() {
        return "EXCEL";
    }

    public String getFormatDescription() {
        return "Microsoft Excel 97-2003, 2007+";
    }

    public String[] getFileExtensions() {
        return new String[]{"xls", "xlsx"};
    }

    /**
     * See if the specified file is an Excel file.  As currently implemented,
     * this method simply tests if the file extension is "xls" or "xlsx".  A
     * better approach would be to actually test for a specific "magic number."
     * This method also tests if the file actually exists.
     *
     * @param filepath The file to test.
     * @return True if the specified file exists and appears to be an Excel
     *         file, false otherwise.
     */
    public boolean testFile(String filepath) {
        // test if the file exists
        File file = new File(filepath);
        if (!file.exists())
            return false;

        int index = filepath.lastIndexOf('.');

        if (index != -1 && index != (filepath.length() - 1)) {
            // get the extension
            String ext = filepath.substring(index + 1);

            if (ext.equals("xls") || ext.equals("xlsx"))
                return true;
        }

        return false;
    }

    public boolean openFile(String filepath) {
        //System.out.println(filepath);
        FileInputStream is;

        try {
            is = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            return false;
        }

        try {
            excelwb = WorkbookFactory.create(is);

            currsheet = 0;
        } catch (Exception e) {
            return false;
        }

        // Create a new DataFormatter and FormulaEvaluator to use for cells with
        // formulas.
        df = new DataFormatter();
        fe = excelwb.getCreationHelper().createFormulaEvaluator();

        return true;
    }

    public boolean hasNextTable() {
        if (excelwb == null)
            return false;
        else
            return (currsheet < excelwb.getNumberOfSheets());
    }

    public void moveToNextTable() {
        if (hasNextTable()) {
            Sheet exsheet = excelwb.getSheetAt(currsheet++);
            rowiter = exsheet.rowIterator();
            numcols = -1;
            testNext();
        } else
            throw new NoSuchElementException();
    }

    public String getCurrentTableName() {
        return excelwb.getSheetName(currsheet - 1);
    }

    public boolean tableHasNextRow() {
        if (rowiter == null)
            return false;
        else
            return hasnext;
    }

    /**
     * Internal method to see if there is another line with data remaining in
     * the current table.  Any completely blank lines will be skipped.  This
     * method is necessary because the POI row iterator does not always reliably
     * end with the last data-containing row.
     */
    private void testNext() {
        int lastcellnum = 0;
        while (rowiter.hasNext() && lastcellnum < 1) {
            nextrow = rowiter.next();
            lastcellnum = nextrow.getLastCellNum();
        }

        hasnext = lastcellnum > 0;
    }

    public String[] tableGetNextRow() {
        if (!tableHasNextRow())
            throw new NoSuchElementException();

        Row row = nextrow;
        Cell cell;

        // If this is the first row in the sheet, use it to determine how many
        // columns this sheet has.  This is necessary to make sure that all rows
        // have the same number of cells for SQLite.
        if (numcols < 0)
            numcols = row.getLastCellNum();

        String[] ret = new String[numcols];

        // Unfortunately, we can't use a cell iterator here because, as
        // currently implemented in POI, iterating over cells in a row will
        // silently skip blank cells.
        for (int cnt = 0; cnt < numcols; cnt++) {
            cell = row.getCell(cnt, Row.CREATE_NULL_AS_BLANK);

            // inspect the data type of this cell and act accordingly
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    ret[cnt] = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    // There is no date data type in Excel, so we have to check
                    // if this cell contains a date-formatted value.
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                        // Convert the value to a Java date object, then to
                        // ISO 8601 format using Joda-Time.
                        DateTime date;
                        date = new DateTime(cell.getDateCellValue());
                        ret[cnt] = date.toString();
                    } else
                        ret[cnt] = Double.toString(cell.getNumericCellValue());
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    if (cell.getBooleanCellValue())
                        ret[cnt] = "true";
                    else
                        ret[cnt] = "false";
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    // Use the FormulaEvaluator to determine the result of the
                    // cell's formula, then convert the result to a String.
                    ret[cnt] = df.formatCellValue(cell, fe);
                    break;
                default:
                    ret[cnt] = "";
            }
        }

        // Determine if another row is available after this one.
        testNext();

        return ret;
    }

    public void closeFile() {
    }
}
