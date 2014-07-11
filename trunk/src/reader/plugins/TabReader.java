package reader.plugins;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.SystemOutLogger;
import settings.PathManager;

import java.io.*;
import java.util.*;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.LinkedList;
import java.util.NoSuchElementException;


/**
 * Provides the ability to parse CSV formatted files.  This implementation is
 * mostly compliant with the RFC 4180 CSV specification.  In particular, 1) each
 * record is expected to be on a single line; 2) fields may be enclosed in
 * double quotes; 3) double quotes inside of a double-quoted string should be
 * escaped with a double quote; 4) space and tab characters are not trimmed from
 * the beginning or end of fields.
 * <p/>
 * This implementation has some additional features and/or limitations that
 * might not be supported by other CSV libraries.  First, if a field is
 * double-quoted, there should be no additional characters outside of the double
 * quotes.  Second, records must not span multiple lines.  Third, standard
 * escape sequences (e.g., "\"\t") are also supported inside of quoted fields.
 * Fourth, blank lines are ignored.
 * <p/>
 * Finally, note that this class expects text to consist of simple ASCII
 * (1 byte) characters, and that characters 0-8 and 10-31 are all treated as
 * empty whitespace and ignored (unless they occur within a quoted string).
 */
public class TabReader extends ExcelReader {
    private StreamTokenizer st;
    private boolean hasnext = false;
    private LinkedList<String> reclist;
    private int currtable;

    private String sheetName = "Samples";

    public TabReader() {
        reclist = new LinkedList<String>();
    }

    public String getFormatString() {
        return "Txt";
    }

    public String getShortFormatDesc() {
        return "Txt";
    }

    public String getFormatDescription() {
        return "tab-separated values";
    }

    public String[] getFileExtensions() {
        return new String[]{"txt"};
    }

    /**
     * See if the specified file is a TAb Text file.  Since no "magic number" can
     * be defined for Tab files, this test is limited to seeing if the file
     * extension is "txt".  This method also tests if the file actually exists.
     *
     * @param filepath The file to test.
     *
     * @return True if the specified file exists and appears to be a txt file,
     *         false otherwise.
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

            if (ext.equals("txt"))
                return true;
        }

        return false;
    }



    public boolean openFile(String filepath, String defaultSheetName, String outputFolder) throws Exception {
        if (defaultSheetName != null) {
            this.sheetName = defaultSheetName;
        }
        if (outputFolder == null) {
            throw new Exception("No outputfolder specified for tab format conversion");
        }
         Sheet sheet1;
        try {
            excelwb = new HSSFWorkbook();
            sheet1 = excelwb.createSheet(this.sheetName);
            currsheet = 0;
        } catch (Exception e) {
            return false;
        }

        int i = 0;
        int field = 0;
        int rowNum = 0;
        Row row = sheet1.createRow(rowNum);

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filepath));

            String line;
            while ((line = br.readLine()) != null) {
                row = sheet1.createRow(rowNum);
                String[] vals = line.split("\t");
                for (int j = 0; j < vals.length; j++) {
                    row.createCell(j).setCellValue(vals[j]);
                }
                rowNum++;
            }
            br.close();

        } catch (Exception e) {
            throw new Exception("Trouble converting TAB format",e);
        }
        currtable = -1;

        // Create a new DataFormatter and FormulaEvaluator to use for cells with
        // formulas.
        df = new DataFormatter();
        fe = excelwb.getCreationHelper().createFormulaEvaluator();

        // Write the output to a file
        try {

            inputFile = PathManager.createUniqueFile("temporaryWorkbook.xls", outputFolder);
               System.out.println("writing to " + inputFile.getAbsolutePath());
            FileOutputStream fileOut = new FileOutputStream(inputFile);

            excelwb.write(fileOut);

            fileOut.close();

        } catch (Exception e) {
            throw new Exception("Trouble saving file",e);
        }

        return true;
    }


}
