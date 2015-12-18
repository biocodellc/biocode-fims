package digester;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fimsExceptions.FIMSRuntimeException;

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
    Validation validation;
    ArrayList extraColumns;
    Integer totalColumns;
    String sheetName;
    XSSFWorkbook wb = new XSSFWorkbook();
    Sheet sheet;
    private static Logger logger = LoggerFactory.getLogger(QueryWriter.class);

    /**
     * @param attributes ArrayList of attributes passed as argument is meant to come from digester.Mapping instance
     */
    public QueryWriter(ArrayList<Attribute> attributes, String sheetName, Validation validation) {
        this.sheetName = sheetName;
        this.attributes = attributes;
        this.validation = validation;
        totalColumns = attributes.size() - 1;
        extraColumns = new ArrayList();

        sheet = wb.createSheet(sheetName);
    }

    /**
     * Find the column position for this array
     *
     * @param columnName
     *
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
     *
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
     *
     * @return
     */
    public Row createRow(int rowNum) {
        // make ALL rows one more than the expected rowNumber to account for the header row
        return sheet.createRow((short) rowNum + 1);
    }

    /**
     * Remove a row by its index
     *
     * @param rowIndex a 0 based index of removing row
     */
    public void removeRow(int rowIndex) {

        // account for header row
        int rowtoRemove = rowIndex + 1;

        int lastRowNum = sheet.getLastRowNum();
        if (rowtoRemove >= 0 && rowtoRemove < lastRowNum) {
            sheet.shiftRows(rowtoRemove + 1, lastRowNum, -1);
        }

        if (rowIndex == lastRowNum) {
            org.apache.poi.ss.usermodel.Row removingRow = sheet.getRow(rowtoRemove);
            if (removingRow != null) {
                sheet.removeRow(removingRow);
            }
        }
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
                    datatype = attribute.getDataType();
                }

            } catch (Exception e) {
                //TODO should we be catching Exception?
                logger.warn("Exception", e);
                // For now, do nothing.
            }
        }


        Cell cell = row.createCell(getColumnPosition(colName));

        // Set the value conditionally, we can specify datatypes in the configuration file so interpret them
        // as appropriate here.
        if (datatype != null && datatype.equals("integer")) {
            //fimsPrinter.out.println("value = " + value);
            //Its a number(int or float).. Excel treats both as numeric
            XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
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

        queryWriter = new QueryWriter(attributes, "myworksheet", null);

        Row r = queryWriter.createRow(1);
        queryWriter.createCell(r, "Specimen_num_collector", "MBIO57");
        queryWriter.createCell(r, "Phylum", "Chordata");
        queryWriter.createCell(r, "IdentifiedBy", "Meyer");
        queryWriter.createCell(r, "SomeNewValue", "fungal attack!");


        try {
            System.out.println("file output : " + queryWriter.writeTAB(new File("tripleOutput/output.tab")));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Write output to a file
     */
    public String writeExcel(File file) {
        // Header Row
        createHeaderRow(sheet);
        // Write the output to a file
        FileOutputStream fileOut = null;
        try {
            //File file = new File(fileLocation);
            fileOut = new FileOutputStream(file);

            //File outputFile = t.createExcelFile("Samples", "tripleOutput", a);

            wb.write(fileOut);

            fileOut.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            throw new FIMSRuntimeException(500, e);
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }


    public String writeJSON(File file) {
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

    /**
     * Return the default sheet used for processing
     *
     * @return
     */
    public XSSFWorkbook getWorkbook() {
        return wb;
    }

    public String writeHTML(File file) {
        StringBuilder sbHeader = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        // Header Row
        createHeaderRow(sheet);

        // Iterate through the rows.
        int count = 0;
        int LIMIT = 10000;
        for (Row row : sheet) {
            if (count < LIMIT) {

                StringBuilder sbRow = new StringBuilder();

                for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                    Cell cell = row.getCell(cn, Row.CREATE_NULL_AS_BLANK);
                    if (cell == null) {
                        sbRow.append("<td></td>");

                    } else {
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_STRING:
                                sbRow.append("<td>" + cell.getRichStringCellValue().getString() + "</td>");
                                break;
                            case Cell.CELL_TYPE_NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    sbRow.append("<td>" + cell.getDateCellValue() + "</td>");
                                } else {
                                    sbRow.append("<td>" + cell.getNumericCellValue() + "</td>");
                                }
                                break;
                            case Cell.CELL_TYPE_BOOLEAN:
                                sbRow.append("<td>" + cell.getBooleanCellValue() + "</td>");
                                break;
                            case Cell.CELL_TYPE_FORMULA:
                                sbRow.append("<td>" + cell.getCellFormula() + "</td>");
                                break;
                            default:
                                sbRow.append("<td>" + cell.toString() + "</td>");
                        }
                    }

                }
                if (count == 0) {
                    sbHeader.append("<tr>\n");
                    sbHeader.append("\t" + sbRow + "\n");
                    sbHeader.append("</tr>\n");
                } else {
                    sb.append("<tr>\n");
                    sb.append("\t" + sbRow + "\n");
                    sb.append("\t<tr>\n");
                }
            }
            count++;
        }


        return writeFile("<table border=1>\n" + sbHeader.toString() + sb.toString() + "</table>", file);
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

        } catch (IOException e) {
            logger.warn("IOException", e);
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                logger.warn("IOException", e);
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


    /**
     * A special method for writing output to CSPACE...
     * TODO: Make the writeCSPACE function general purpose using a library (apache?) for variable substitution
     *
     * @param file
     *
     * @return
     */
    public String writeCSPACE(File file) {
        createHeaderRow(sheet);

        // Store all String elements
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<imports>\n");

        // Build Row Iterator
        ArrayList rows = new ArrayList();
        for (Iterator<Row> rowsIT = sheet.rowIterator(); rowsIT.hasNext(); ) {
            Row row = rowsIT.next();
            // Iterate through the cells.
            ArrayList cells = new ArrayList();
            for (Iterator<Cell> cellsIT = row.cellIterator(); cellsIT.hasNext(); ) {
                Cell cell = cellsIT.next();
                cells.add(cell);
            }
            rows.add(cells);
        }
        Iterator rowsIt = rows.iterator();
        int count = 0;

        // Loop each record
        while (rowsIt.hasNext()) {

            ArrayList cells = (ArrayList) rowsIt.next();
            Iterator cellsIt = cells.iterator();

            //Variables pertaining to row level only
            String year = "", month = "", day = "", taxon = "", identBy = "";
            String identyear = "", identmonth = "", identday = "";
            String Locality = "", Country = "", State_Province = "", County = "", Elevation = "", Elevation_Units = "", Latitude = "", Longitude = "", Coordinate_Source = "";
            StringBuilder common = new StringBuilder();
            StringBuilder naturalhistory = new StringBuilder();

            // don't take the first row, its a header.
            if (count > 1) {
                sb.append("<import service='CollectionObjects' type='CollectionObject'>\n");

                int fields = 0;

                // Loop all fields in row
                while (cellsIt.hasNext()) {
                    Cell c = (Cell) cellsIt.next();
                    Integer index = c.getColumnIndex();
                    // Set the fieldName Value
                    String fieldName = sheet.getRow(0).getCell(index).toString();
                    // Assign the value as a simple string
                    String value = c.toString();

                    // Write out XML Values
                    if (fieldName.equals("BCID")) {
                        // check if resolution mechanism is attached
                        if (!value.contains("http")) {
                            value = "http://n2t.net/" + value;
                        }
                        common.append("\t<otherNumberList>\n" +
                                "\t\t<otherNumber>\n" +
                                "\t\t\t" + writeXMLValue("numberValue", value) + "\n" +
                                "\t\t\t<numberType>FIMS Identifier</numberType>\n" +
                                "\t\t</otherNumber>\n" +
                                "\t</otherNumberList>\n");
                    } else if (fieldName.equals("Habitat")) {
                        common.append("\t" + writeXMLValue("fieldCollectionNote", value) + "\n");
                    } else if (fieldName.equals("Barcode_Number")) {
                        common.append("\t" + writeXMLValue("objectNumber", value) + "\n");
                    } else if (fieldName.equals("All_Collectors")) {
                        common.append("\t<fieldCollectors>\n");
                        common.append("\t\t" + writeXMLValue("fieldCollector", fieldURILookup("Collector", value)) + "\n");
                        common.append("\t</fieldCollectors>\n");
                    } else if (fieldName.equals("Coll_Num")) {
                        common.append("\t" + writeXMLValue("fieldCollectionNumber", value) + "\n");
                    } else if (fieldName.equals("Coll_Year")) {
                        year = value;
                    } else if (fieldName.equals("Coll_Month")) {
                        month = value;
                    } else if (fieldName.equals("Coll_Day")) {
                        day = value;
                    } else if (fieldName.equals("Plant_Description")) {
                        common.append("\t<briefDescriptions>\n" +
                                "\t\t" + writeXMLValue("briefDescription", value) + "\n" +
                                "\t</briefDescriptions>\n");
                    } else if (fieldName.equals("Label_Header")) {
                        naturalhistory.append("\t" + writeXMLValue("labelHeader", fieldURILookup("Label_Header", value)) + "\n");
                    } else if (fieldName.equals("Main_Collector")) {
                        naturalhistory.append("\t" + writeXMLValue("fieldCollectionNumberAssignor", fieldURILookup("Collector", value)) + "\n");
                    } else if (fieldName.equals("ScientificName")) {
                        taxon = writeXMLValue("taxon", fieldURILookup("ScientificName", value));
                    } else if (fieldName.equals("DeterminedBy")) {
                        identBy = writeXMLValue("identBy", fieldURILookup("DeterminedBy", value));
                    } else if (fieldName.equals("Comments")) {
                        common.append("\t<comments>\n");
                        common.append("\t\t" + writeXMLValue("comment", value) + "\n");
                        common.append("\t</comments>\n");
                    } else if (fieldName.equals("Det_Year")) {
                        identyear = value;
                    } else if (fieldName.equals("Det_Month")) {
                        identmonth = value;
                    } else if (fieldName.equals("Det_Day")) {
                        identday = value;
                    } else if (fieldName.equals("Locality")) {
                        Locality = value;
                    } else if (fieldName.equals("Country")) {
                        Country = value;
                    } else if (fieldName.equals("State_Province")) {
                        State_Province = value;
                    } else if (fieldName.equals("County")) {
                        County = value;
                    } else if (fieldName.equals("Elevation")) {
                        Elevation = value;
                    } else if (fieldName.equals("Elevation_Units")) {
                        Elevation_Units = value;
                    } else if (fieldName.equals("Latitude")) {
                        Latitude = value;
                    } else if (fieldName.equals("Longitude")) {
                        Longitude = value;
                    } else if (fieldName.equals("Coordinate_Source")) {
                        Coordinate_Source = value;
                    } else {
                        // All the rest of the values
                        //naturalhistory.append("\t" + writeXMLValue(fieldName, value) + "\n");
                    }

                    fields++;
                }

                // Field Date Group
                common.append("\t<fieldCollectionDateGroup>\n");
                common.append("\t\t<dateDisplayDate>" + year + "-" + month + "-" + day + "</dateDisplayDate>\n");
                common.append("\t\t<dateEarliestSingleDay>" + day + "</dateEarliestSingleDay>\n");
                common.append("\t\t<dateEarliestSingleMonth>" + month + "</dateEarliestSingleMonth>\n");
                common.append("\t\t<dateEarliestSingleYear>" + year + "</dateEarliestSingleYear>\n");
                common.append("\t\t<dateEarliestScalarValue>" + year + "-" + month + "-" + day + "T00:00:00Z</dateEarliestScalarValue>\n");
                common.append("\t</fieldCollectionDateGroup>\n");

                // TaxonomicIdentGroup
                naturalhistory.append("\t<taxonomicIdentGroupList>\n" +
                        "\t\t<taxonomicIdentGroup>\n" +
                        "\t\t\t" + taxon + "\n" +
                        "\t\t\t<qualifier></qualifier>\n" +
                        "\t\t\t" + identBy + "\n" +
                        "\t\t\t<identDateGroup>\n" +
                        "\t\t\t\t<dateDisplayDate>" + identyear + "-" + identmonth + "-" + identday + "</dateDisplayDate>\n" +
                        "\t\t\t\t<dateEarliestSingleDay>" + identday + "</dateEarliestSingleDay>\n" +
                        "\t\t\t\t<dateEarliestSingleMonth>" + identmonth + "</dateEarliestSingleMonth>\n" +
                        "\t\t\t\t<dateEarliestSingleYear>" + identyear + "</dateEarliestSingleYear>\n" +
                        "\t\t\t\t<dateEarliestScalarValue>" + identyear + "-" + identmonth + "-" + identday + "T00:00:00Z</dateEarliestScalarValue>\n" +
                        "\t\t\t</identDateGroup>\n" +
                        "\t\t</taxonomicIdentGroup>\n" +
                        "\t</taxonomicIdentGroupList>\n");

                naturalhistory.append("\t<localityGroupList>\n" +
                        "\t\t<localityGroup>\n" +
                        "\t\t\t<fieldLocVerbatim>" + Locality + "</fieldLocVerbatim>\n" +
                        "\t\t\t<fieldLocCountry>" + Country + "</fieldLocCountry>\n" +
                        "\t\t\t<fieldLocState>" + State_Province + "</fieldLocState>\n" +
                        "\t\t\t<fieldLocCounty>" + County + "</fieldLocCounty>\n" +
                        "\t\t\t<minElevation>" + Elevation + "</minElevation>\n" +
                        "\t\t\t<elevationUnit>" + Elevation_Units + "</elevationUnit>\n" +
                        "\t\t\t<decimalLatitude>" + Latitude + "</decimalLatitude>\n" +
                        "\t\t\t<decimalLongitude>" + Longitude + "</decimalLongitude>\n" +
                        "\t\t\t<vLatitude>" + Latitude + "</vLatitude>\n" +
                        "\t\t\t<vLongitude>" + Longitude + "</vLongitude>\n" +
                        "\t\t\t<geoRefSource>" + Coordinate_Source + "</geoRefSource>\n" +
                        "\t\t\t<localitySource>" + Coordinate_Source + "</localitySource>\n" +
                        "\t\t</localityGroup>\n" +
                        "\t</localityGroupList>\n");

                // collectionobjects_common element
                sb.append("<schema xmlns:collectionobjects_common=\"http://collectionspace.org/services/collectionobject\" name=\"collectionobjects_common\">\n");
                sb.append(common);
                sb.append("</schema>\n");

                // collectionobjects_naturalhistory element
                sb.append("<schema xmlns:collectionobjects_naturalhistory=\"http://collectionspace.org/services/collectionobject/domain/naturalhistory\" name=\"collectionobjects_naturalhistory\">\n");
                sb.append(naturalhistory);
                sb.append("</schema>\n");

                // End of this object/row
                sb.append("</import>\n");

            }
            count++;
        }

        // closing document tag
        sb.append("</imports>\n");

        return writeFile(sb.toString(), file);
    }

    /**
     * Special function built for CSPACE case that looks up the Field.uri and appends Field.value to it
     *
     * @param fieldName
     * @param value
     *
     * @return
     */

    private String fieldURILookup(String fieldName, String value) {
        // Loop XML attribute value of ScientificName to get the REFNAME
        LinkedList<digester.List> lists = validation.getLists();
        Iterator listsIt = lists.iterator();
        while (listsIt.hasNext()) {
            digester.List l = (digester.List) listsIt.next();
            if (l.getAlias().equals(fieldName)) {
                java.util.List fieldlist = l.getFields();
                Iterator fieldlistIt = fieldlist.iterator();
                while (fieldlistIt.hasNext()) {
                    Field f = (Field) fieldlistIt.next();
                    if (f.getValue().equals(value)) {
                        return f.getUri() + "'" + f.getValue() + "'";
                    }
                }
            }
        }
        return value;
    }

    private String writeXMLValue(String field, String value) {
        if (value == null || value.trim().equals("")) {
            return "<" + field + "/>";
        }
        if (StringUtils.containsAny(value, "<>&")) {
            return "<" + field + "><![CDATA[" + value + "]]></" + field + ">";
        } else {
            return "<" + field + ">" + value + "</" + field + ">";
        }

    }

    /**
     * Write a tab delimited output
     *
     * @param file
     *
     * @return
     */
    public String writeTAB(File file) {
        // Header Row
        createHeaderRow(sheet);
        // Write the output to a file
        FileOutputStream fileOut = null;
        try {
            //File file = new File(fileLocation);
            fileOut = new FileOutputStream(file);

            Iterator rowIt = sheet.rowIterator();
            while (rowIt.hasNext()) {
                Row row = (Row) rowIt.next();

                for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                    Cell cell = row.getCell(cn, Row.CREATE_NULL_AS_BLANK);
                    if (cell == null) {
                        fileOut.write(("\t").getBytes());
                    } else {
                        byte[] contentInBytes = cell.getStringCellValue().getBytes();
                        fileOut.write(contentInBytes);
                        fileOut.write(("\t").getBytes());
                    }
                }

                 /*
                Iterator cellIt = row.cellIterator();
                while (cellIt.hasNext()) {
                    Cell cell = (Cell) cellIt.next();
                    byte[] contentInBytes = cell.getStringCellValue().getBytes();
                    fileOut.write(contentInBytes);
                    fileOut.write(("\t").getBytes());
                }
                */
                fileOut.write(("\n").getBytes());
            }
            fileOut.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            throw new FIMSRuntimeException(500, e);
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }
}
