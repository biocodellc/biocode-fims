package digester;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import settings.PathManager;
import settings.fimsPrinter;

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
    Workbook wb = new HSSFWorkbook();
    Sheet sheet;

    /**
     * @param attributes ArrayList of attributes passed as argument is meant to come from digester.Mapping instance
     * @throws IOException
     */
    public QueryWriter(ArrayList<Attribute> attributes, String sheetName) throws IOException {
        this.sheetName = sheetName;
        this.attributes = attributes;
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
        //System.out.println(colName);
        String datatype = null;
        // Loop attributes and use column names instead of URI value in column position lookups
        Iterator it = attributes.iterator();
          while (it.hasNext()) {
            Attribute attribute = (Attribute) it.next();
            // map column names to datatype
            // TODO: this part bombs when the configuration file does not have a URI specified, thus we need to write a configuration file validator?
            try {
                if (attribute.getUri().equals(predicate)) {
                    colName = attribute.getColumn();
                    datatype = attribute.getDatatype();
                }

            } catch (Exception e) {
                // For now, do nothing.
            }
        }
        Cell cell = row.createCell(getColumnPosition(colName));

        // Set the value conditionally, we can specify datatypes in the configuration file so interpret them
        // as appropriate here.
        if (datatype != null && datatype.equals("integer")) {
            //fimsPrinter.out.println("value = " + value);
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
        /*
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
            fimsPrinter.out.println("file output : " + queryWriter.write());
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    /**
     * Write output to a file
     */
    public String writeExcel(File file) throws Exception {
        // Header Row
        createHeaderRow(sheet);
        // Write the output to a file
        FileOutputStream fileOut = null;
        try {
            //File file = new File(fileLocation);
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


    public String writeJSON(File file) throws Exception {
        // Header Row
        createHeaderRow(sheet);

        // Start constructing JSON.
        JSONObject json = new JSONObject();

        // Iterate through the rows.
        int count = 0;
        int LIMIT = 10000;
        JSONArray rows = new JSONArray();
        for (Row row : sheet) {
            if (count < LIMIT) {
                JSONObject jRow = new JSONObject();
                JSONArray cells = new JSONArray();

                for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                    Cell cell = row.getCell(cn, Row.CREATE_NULL_AS_BLANK);
                    if (cell == null) {
                        cells.add("");
                    } else {
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_STRING:
                                cells.add(cell.getRichStringCellValue().getString());
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    cells.add(cell.getDateCellValue());
                                } else {
                                    cells.add(cell.getNumericCellValue());
                                }
                                break;
                            case Cell.CELL_TYPE_BOOLEAN:
                                cells.add(cell.getBooleanCellValue());
                                break;
                            case Cell.CELL_TYPE_FORMULA:
                                cells.add(cell.getCellFormula());
                                break;
                            default:
                                cells.add(cell.toString());
                        }
                    }

                }
                if (count == 0) {
                    json.put("header", cells);
                } else {
                    jRow.put("row", cells);
                    rows.add(jRow);
                }
            }
            count++;
        }

        // Create the JSON.
        json.put("data", rows);

        // Get the JSON text.
        return writeFile(json.toString(), file);
    }

    public String writeHTML(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        // Header Row
        createHeaderRow(sheet);

        // Iterate through the rows.
        ArrayList rows = new ArrayList();
        for (Iterator<Row> rowsIT = sheet.rowIterator(); rowsIT.hasNext(); ) {
            Row row = rowsIT.next();
            //JSONObject jRow = new JSONObject();

            // Iterate through the cells.
            ArrayList cells = new ArrayList();
            for (Iterator<Cell> cellsIT = row.cellIterator(); cellsIT.hasNext(); ) {
                Cell cell = cellsIT.next();
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
                    cells.add(cell.getNumericCellValue());
                else
                    cells.add(cell.getStringCellValue());
            }
            rows.add(cells);
        }

        Iterator rowsIt = rows.iterator();
        int count = 0;
        sb.append("<table>\n");
        while (rowsIt.hasNext()) {
            ArrayList cells = (ArrayList) rowsIt.next();
            Iterator cellsIt = cells.iterator();
            sb.append("\t<tr>");
            while (cellsIt.hasNext()) {
                sb.append("<td>" + cellsIt.next() + "</td>");
            }
            sb.append("</tr>\n");
            count++;
        }
        sb.append("</table>\n");
        return writeFile(sb.toString(), file);
    }

    private String writeFile(String content, File file) {

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

            //System.out.println("Done");

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
        return file.getAbsolutePath();
    }


    public String writeKML(File file) {
        createHeaderRow(sheet);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "\t<Document>\n");


        // Iterate through the rows.
        ArrayList rows = new ArrayList();
        for (Iterator<Row> rowsIT = sheet.rowIterator(); rowsIT.hasNext(); ) {
            Row row = rowsIT.next();
            //JSONObject jRow = new JSONObject();

            // Iterate through the cells.
            ArrayList cells = new ArrayList();
            for (Iterator<Cell> cellsIT = row.cellIterator(); cellsIT.hasNext(); ) {
                Cell cell = cellsIT.next();
                /* if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
                cells.add(cell.getNumericCellValue());
            else
                cells.add(cell.getStringCellValue());
                */
                cells.add(cell);
            }
            rows.add(cells);
        }

        Iterator rowsIt = rows.iterator();
        int count = 0;

        /*   <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2">
          <Document>
            <Placemark>
              <name>CDATA example</name>
              <description>
                <![CDATA[
                  <h1>CDATA Tags are useful!</h1>
                  <p><font color="red">Text is <i>more readable</i> and
                  <b>easier to write</b> when you can avoid using entity
                  references.</font></p>
                ]]>
              </description>
              <Point>
                <coordinates>102.595626,14.996729</coordinates>
              </Point>
            </Placemark>
          </Document>
        </kml>*/
        while (rowsIt.hasNext()) {
            ArrayList cells = (ArrayList) rowsIt.next();
            Iterator cellsIt = cells.iterator();

            // don't take the first row, its a header.
            if (count > 1) {
                StringBuilder header = new StringBuilder();

                StringBuilder description = new StringBuilder();
                StringBuilder name = new StringBuilder();

                header.append("\t<Placemark>\n");
                description.append("\t\t<description>\n");
                String decimalLatitude = null;
                String decimalLongitude = null;
                description.append("\t\t<![CDATA[");

                int fields = 0;
                // take all the fields
                while (cellsIt.hasNext()) {
                    Cell c = (Cell) cellsIt.next();
                    Integer index = c.getColumnIndex();
                    String value = c.toString();
                    String fieldname = sheet.getRow(0).getCell(index).toString();

                    //Only take the first 10 fields for data....
                    if (fields < 10)
                        description.append("<br>" + fieldname + "=" + value);

                    if (fieldname.equalsIgnoreCase("decimalLatitude") && !value.equals(""))
                        decimalLatitude = value;
                    if (fieldname.equalsIgnoreCase("decimalLongitude") && !value.equals(""))
                        decimalLongitude = value;
                    if (fieldname.equalsIgnoreCase("materialSampleID"))
                        name.append("\t\t<name>" + value + "</name>\n");

                    fields++;
                }
                description.append("\t\t]]>\n");
                description.append("\t\t</description>\n");

                if (decimalLatitude != null && decimalLongitude != null) {
                    sb.append(header);
                    sb.append(name);
                    sb.append(description);

                    sb.append("\t\t<Point>\n");
                    sb.append("\t\t\t<coordinates>" + decimalLongitude + "," + decimalLatitude + "</coordinates>\n");
                    sb.append("\t\t</Point>\n");

                    sb.append("\t</Placemark>\n");
                }

            }
            count++;
        }

        sb.append("</Document>\n" +
                "</kml>");
        return writeFile(sb.toString(), file);
    }
}
