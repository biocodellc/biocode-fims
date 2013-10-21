package digester;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Loop a bunch of attributes, queried from some model and write to a spreadsheet
 * Keep track of columns we want to display, starting with an ArrayList of attributes, corresponding
 * to column names and tracking URI references as defined by the Mapping digester class.
 */
public class QueryWriter {
    // Loop all the columns associated with this worksheet
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    ArrayList extraColumns;
    Integer totalColumns;
    String sheetName;
    String fileLocation;
    Workbook wb = new HSSFWorkbook();
    Sheet sheet;

    /**
     * @param attributes ArrayList of attributes passed as argument is meant to come from digester.Mapping instance
     * @throws IOException
     */
    public QueryWriter(ArrayList<Attribute> attributes, String sheetName, String fileLocation) throws IOException {
        this.sheetName = sheetName;
        this.attributes = attributes;
        this.fileLocation = fileLocation;
        totalColumns = attributes.size() - 1;
        extraColumns = new ArrayList();

        sheet = wb.createSheet(sheetName);

    }

    /**
     * Find the column position for this array
     *
     * @param columnName
     * @return
     */
    public Integer getColumnPosition(String columnName) {
        Iterator it = attributes.iterator();
        int count = 0;
        while (it.hasNext()) {
            Attribute attribute = (Attribute) it.next();
            if (columnName.equals(attribute.getColumn())) {
                return count;
            }
            count++;
        }

        // Track any extra columns we find
        Iterator itExtraColumns = extraColumns.iterator();
        // position counter at end of known columns
        int positionExtraColumn = attributes.size();
        // loop the existing extracolumns array, looking for matches, returning position if found
        while (itExtraColumns.hasNext()) {
            String col = (String) itExtraColumns.next();
            if (col.equals(columnName)) {
                return positionExtraColumn;
            }
            positionExtraColumn++;
        }

        // If we don't find it then add it to the extracolumns
        extraColumns.add(columnName);
        totalColumns++;
        return totalColumns;
    }

    /**
     * Create a header row for all columns (initial + extra ones encountered)
     *
     * @param sheet
     * @return
     */
    public org.apache.poi.ss.usermodel.Row createHeaderRow(Sheet sheet) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow((short) 0);

        Iterator it = attributes.iterator();
        int count = 0;
        while (it.hasNext()) {
            Attribute attribute = (Attribute) it.next();
            String colName = attribute.getColumn();
            row.createCell(count).setCellValue(colName);
            count++;
        }

        Iterator itExtraColumns = extraColumns.iterator();

        while (itExtraColumns.hasNext()) {
            String colName = (String) itExtraColumns.next();
            row.createCell(count).setCellValue(colName);
            count++;
        }

        return row;
    }


    /**
     * create a row at a specified index
     *
     * @param rowNum
     * @return
     */
    public Row createRow(int rowNum) {
        // make ALL rows one more than the expected rowNumber to account for the header row
        return sheet.createRow((short) rowNum + 1);
    }

    /**
     * Write data to a particular cell given the row/column(predicate) and a value
     *
     * @param row
     * @param predicate
     * @param value
     */
    public void createCell(Row row, String predicate, String value) {
        String colName = predicate;
        String datatype = null;
        // Loop attributes and use column names instead of URI value in column position lookups
        Iterator it = attributes.iterator();
        while (it.hasNext()) {
            Attribute attribute = (Attribute) it.next();
            if (attribute.getUri().equals(predicate)) {
                colName = attribute.getColumn();
                datatype = attribute.getDatatype();
            }
        }
        Cell cell = row.createCell(getColumnPosition(colName));

        // Set the value conditionally, we can specify datatypes in the configuration file so interpret them
        // as appropriate here.
        if (datatype.equals("integer")) {
            //System.out.println("value = " + value);
            //Its a number(int or float).. Excel treats both as numeric
            HSSFCellStyle style = (HSSFCellStyle) wb.createCellStyle();
            style.setDataFormat(HSSFDataFormat.getBuiltinFormat("0"));
            cell.setCellStyle(style);
            cell.setCellValue(Float.parseFloat(value));
        } else {
            cell.setCellValue(value);
        }
    }

    /**
     * Sample usage for the QueryWriter class
     *
     * @param args
     */
    public static void main(String[] args) {

        // Setup columns -- construct these in XML and use Digester to populate
        ArrayList attributes = new ArrayList();
        Attribute a = new Attribute();
        a.setColumn("Specimen_num_collector");
        attributes.add(a);
        a = new Attribute();
        a.setColumn("Phylum");
        attributes.add(a);
        a = new Attribute();
        a.setColumn("IdentifiedBy");
        attributes.add(a);

        // Data Values
        QueryWriter queryWriter = null;
        try {
            queryWriter = new QueryWriter(attributes, "myworksheet", "tripleOutput/workbook.xls");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Row r = queryWriter.createRow(1);
        queryWriter.createCell(r, "Specimen_num_collector", "MBIO57");
        queryWriter.createCell(r, "Phylum", "Chordata");
        queryWriter.createCell(r, "IdentifiedBy", "Meyer");
        queryWriter.createCell(r, "SomeNewValue", "fungal attack!");


        try {
            System.out.println("file output : " + queryWriter.write());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write output to a file
     */
    public String write() throws Exception {
        // Header Row
        createHeaderRow(sheet);
        // Write the output to a file
        FileOutputStream fileOut = null;
        try {
            File file = new File(fileLocation);
            fileOut = new FileOutputStream(file);

            wb.write(fileOut);
            fileOut.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }
}
