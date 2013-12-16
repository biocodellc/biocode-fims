package digester;

import com.sun.org.apache.xpath.internal.operations.*;
import reader.plugins.TabularDataReader;
import renderers.RowMessage;
import settings.Connection;
import settings.RegEx;
import settings.fimsPrinter;

import javax.xml.transform.Result;
import java.lang.String;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Rule does the heavy lift for the Validation Components. This is where the code is written for each of the rules
 * encountered in the XML configuration file.
 */
public class Rule {

    // General values
    private String level;
    private String type;
    private String column;
    private String list;

    // A reference to the validation object this rule belongs to
    //private Validation validation;
    // A reference to the worksheet object this rule belongs to
    // TODO: Remove the remaining references to worksheet.  We are transitioning to using a SQLlite database connection
    private TabularDataReader worksheet;
    private Worksheet digesterWorksheet;
    // Now a reference to a SQLLite connection
    private java.sql.Connection connection;

    // Rules can also own their own fields
    private final LinkedList<String> fields = new LinkedList<String>();
    //private List fields = new List();

    // NOTE: not sure i want these values in this class, maybe define a sub-class?
    // values for DwCLatLngChecker (optional)
    private String decimalLatitude;
    private String decimalLongitude;
    private String maxErrorInMeters;
    private String horizontalDatum;

    private String plateName;
    private String wellNumber;

    private LinkedList<RowMessage> messages = new LinkedList<RowMessage>();

    public LinkedList<RowMessage> getMessages() {
        return messages;
    }

    public Worksheet getDigesterWorksheet() {
        return digesterWorksheet;
    }

    public void setDigesterWorksheet(Worksheet digesterWorksheet) throws Exception {
        this.digesterWorksheet = digesterWorksheet;
    }

    public void setConnection(java.sql.Connection connection) {
        this.connection = connection;
    }

    public TabularDataReader getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(TabularDataReader worksheet) throws Exception {
        this.worksheet = worksheet;
        // Synchronize the Excel Worksheet instance with the digester worksheet instance
        //fimsPrinter.out.println("setting to "+ digesterWorksheet.getSheetname());
        worksheet.setTable(digesterWorksheet.getSheetname());
    }


    public String getDecimalLatitude() {
        return decimalLatitude;
    }

    public void setDecimalLatitude(String decimalLatitude) {
        this.decimalLatitude = decimalLatitude;
    }

    public String getDecimalLongitude() {
        return decimalLongitude;
    }

    public void setDecimalLongitude(String decimalLongitude) {
        this.decimalLongitude = decimalLongitude;
    }

    public String getMaxErrorInMeters() {
        return maxErrorInMeters;
    }

    public void setMaxErrorInMeters(String maxErrorInMeters) {
        this.maxErrorInMeters = maxErrorInMeters;
    }

    public String getHorizontalDatum() {
        return horizontalDatum;
    }

    public void setHorizontalDatum(String horizontalDatum) {
        this.horizontalDatum = horizontalDatum;
    }

    public String getPlateName() {
        return plateName;
    }

    public void setPlateName(String plateName) {
        this.plateName = plateName;
    }

    public String getWellNumber() {
        return wellNumber;
    }

    public void setWellNumber(String wellNumber) {
        this.wellNumber = wellNumber;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public void addField(String field) {
        fields.add(field);
    }

    public LinkedList<String> getFields() {
        return fields;
    }


    public void print() {
        //fimsPrinter.out.println("    rule type = " + this.type + "; column = " + this.column + "; level = " + this.level);

        for (Iterator i = fields.iterator(); i.hasNext(); ) {
            String field = (String) i.next();
            fimsPrinter.out.println("      field data : " + field);
        }
    }

    public void run(Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Call the run method here to make sure values are initialized properly when running rules...
     * @param parent
     */
    /*
    public void run(Object parent) {
        worksheet = (Worksheet) parent;
    }
    */

    /**
     * Lookup the fields belonging to a particular list
     */
    /*
    public void lookupList() {
        fimsPrinter.out.println("\tlookupList rule for list=" + getList() + " on column " + getColumn());
        // Fetch the fields belonging to this list and create a List object
       java.util.List list = digesterWorksheet.getValidation().findList(getList()).getFields();
        // Loop values
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            fimsPrinter.out.println("\t\t" + i.next());
        }
    }  */


    /**
     * Warn user if the number of rows the application is looking at differs from what is on the spreadsheet
     */
    public void rowNumMismatch() {
        int bVRows = worksheet.getNumRows();
        int poiRows = worksheet.getSheet().getLastRowNum();
        if (bVRows < poiRows) {
            String theWarning = "bioValidator processed " + bVRows + " rows, but it appears there may be " + poiRows +
                    " rows on your sheet. This usually occurs when cell formatting has extended below the area of data but " +
                    "may also occur when you have left blank rows in the data.";
            messages.addLast(new RowMessage(theWarning, RowMessage.WARNING));
        }
    }

    /**
     * Check to see if there duplicateColumn Headers on a worksheet
     */

    public void duplicateColumnNames() {
        java.util.List<String> listSheetColumns = worksheet.getColNames();
        Set<String> output = new HashSet<String>();  // Set does not allow duplicates

        for (int i = 0; i < listSheetColumns.size(); i++) {
            String columnValue = worksheet.getStringValue(i, 0);
            for (int j = 0; j < listSheetColumns.size(); j++) {
                if (j != i) {
                    String columnValue2 = worksheet.getStringValue(j, 0);
                    if (columnValue != null && columnValue2 != null) {
                        if (columnValue.equals(columnValue2)) {
                            output.add("row heading " + columnValue + " used more than once");
                            // quite this if we found at least one match
                            j = listSheetColumns.size();
                        }
                    }
                }
            }
        }

        // Loop through output set
        Iterator outputit = output.iterator();
        while (outputit.hasNext()) {
            String message = (String) outputit.next();
            messages.addLast(new RowMessage(message, RowMessage.WARNING));
        }
    }


    /**
     * Check a particular column to see if all the values are unique
     *
     * @throws Exception
     */
    public void uniqueValue() throws Exception {
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.createStatement();
            rs = null;
            rs = statement.executeQuery(
                    "select " + getColumn() + ",count(*) from " + digesterWorksheet.getSheetname() +
                            " group by " + getColumn() +
                            " having count(*) > 1");

            StringBuilder values = new StringBuilder();
            int count = 0;
            while (rs.next()) {
                if (count > 0) values.append(", ");
                values.append(rs.getString(getColumn()));
                count++;
            }
            if (count > 0) {
                addMessage(getColumn() + " column is defined as unique but some values used more than once: " + values.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("SQL exception processing uniqueValue rule " + e.getMessage());
        } finally {
            statement.close();
            rs.close();
        }
    }


    /**
     * Check that coordinates are inside a bounding box
     *
     * @throws Exception
     */
    public void BoundingBox() throws Exception {

        // Build List of XML Fields
        java.util.List<String> listFields = getFields();

        // Parse the BOX3D well known text box
        String field = listFields.get(0);
        field = field.replace("BOX3D(", "").replace(")", "");
        String[] points = field.split(",");
        double minLat = Double.parseDouble(points[0].split(" ")[0]);
        double maxLat = Double.parseDouble(points[1].split(" ")[0]);
        double minLng = Double.parseDouble(points[0].split(" ")[1]);
        double maxLng = Double.parseDouble(points[1].split(" ")[1]);

        String msg = "";
        // Loop All Rows in this list and see if they are unique
        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            Double latValue = 0.0;
            Double longValue = 0.0;

            if (!checkValidNumber(worksheet.getStringValue(getDecimalLatitude(), j)) ||
                    !checkValidNumber(worksheet.getStringValue(getDecimalLongitude(), j))) {
                addMessage(j, "Unable to perform BoundingBox check due to illegal Latitude or Longitude value");
            } else {

                latValue = worksheet.getDoubleValue(getDecimalLatitude(), j);
                longValue = worksheet.getDoubleValue(getDecimalLongitude(), j);

                if (latValue != null && latValue != 0.0 && (latValue < minLat || latValue > maxLat)) {
                    msg = getDecimalLatitude() + " " + latValue + " outside of " + getColumn() + " bounding box.";
                    addMessage(j, msg);
                }
                if (longValue != null && longValue != 0.0 && (longValue < minLng || longValue > maxLng)) {
                    msg = getDecimalLongitude() + " " + longValue + " outside of " + getColumn() + " bounding box.";
                    addMessage(j, msg);
                }
            }
        }
    }


    /**
     * Check the depthInMeters Numbers are entered correctly
     *
     * @throws Exception
     */
    public void minimumMaximumNumberCheck() throws Exception {
        String minimum = getColumn().split(",")[0];
        String maximum = getColumn().split(",")[1];
        String minMaxArray[] = new String[]{minimum, maximum};

        // Don't run this method if one of these columns doesn't exist
        Boolean minimumExists = checkColumnExists(minimum);
        Boolean maximumExists = checkColumnExists(maximum);
        // No warning message if neither exist
        if (!minimumExists && !maximumExists) {
           // If neither minimum or maximum exist then just ignore this
           // messages.addLast(new RowMessage(
           //         "Unable to run minimumMaximumNumberCheck rule since Neither " + minimum + " or " + maximum + " columns exist",
            //        RowMessage.WARNING));
            return;
        } else if (!minimumExists && maximumExists) {
            messages.addLast(new RowMessage("Column " + maximum + " exists but must have corresponding column " + minimum, RowMessage.WARNING));
            return;
        } else if (minimumExists && !maximumExists) {
            messages.addLast(new RowMessage("Column " + minimum + " exists but must have corresponding column " + maximum, RowMessage.WARNING));
            return;
        }

        Statement statement;
        ResultSet resultSet;
        String msg;
        statement = connection.createStatement();

        // Look for non numeric values in minimum & maximum columns
        for (String thisColumn : Arrays.asList(minMaxArray)) {
            try {
                String sql = "select " + thisColumn + " from  " + digesterWorksheet.getSheetname() +
                        " where abs(" + thisColumn + ") == 0 AND " +
                        "trim(" + thisColumn + ") != '0' AND " +
                        thisColumn + " != \"\";";
                resultSet = statement.executeQuery(sql);
                while (resultSet.next()) {
                    msg = "non-numeric value " + resultSet.getString(thisColumn) + " for " + thisColumn;
                    addMessage(msg);
                }
            } catch (Exception e) {
                throw new Exception("minimumMaximumCheck exception", e);
            }
        }

        // Check to see that minimum is less than maximum
        try {
            String sql = "select " + minimum + "," + maximum + " from " + digesterWorksheet.getSheetname() +
                    " where abs(" + minimum + ") > abs(" + maximum + ")";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "Illegal values! " + minimum + " = " +
                        resultSet.getString(minimum) +
                        " while " + maximum + " = " +
                        resultSet.getString(maximum);
                addMessage(msg);
            }

        } catch (Exception e) {
            throw new Exception("minimumMaximumCheck exception", e);
        }

    }

    /**
     * Check that lowestTaxonLevel and LowestTaxon are entered correctly
     *
     * @throws Exception
     */

    public void checkLowestTaxonLevel() throws Exception {

        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            String LowestTaxonLevelValue = worksheet.getStringValue("LowestTaxonLevel", j);
            String LowestTaxonValue = worksheet.getStringValue("LowestTaxon", j);

            if (LowestTaxonLevelValue == null && LowestTaxonValue != null) {
                addMessage(j, "LowestTaxon entered without a LowestTaxonLevel");
            } else if (LowestTaxonLevelValue != null && LowestTaxonValue == null) {
                addMessage(j, "LowestTaxonLevel entered without a LowestTaxon");
            }
        }

    }

    /**
     * Return the index of particular columns
     *
     * @param columns
     * @return
     */
    protected int[] getColumnIndices(String[] columns) {
        int[] b = new int[columns.length];
        java.util.List<String> listSheetColumns = worksheet.getColNames();
        for (int col = 0; col < columns.length; col++) {
            b[col] = -1;
            for (int cname = 0; cname < listSheetColumns.size(); cname++) {
                String heading = worksheet.getStringValue(cname, 0).trim();
                if (columns[col].equals(heading)) {
                    b[col] = cname;
                }
            }
        }
        return b;
    }


    /**
     * Smithsonian created this function to validate genus & species counts.
     * Collectors SHOULD collect exactly 4 samples for each Genus species
     *
     * @throws Exception
     */
    public void checkGenusSpeciesCountsSI() throws Exception {
        String genusHeading = "Genus";
        String speciesHeading = "Species";
        String[] headings = {genusHeading, speciesHeading};
        int[] columnIndices = this.getColumnIndices(headings);
        //Collectors SHOULD collect exactly 4 samples for each Genus species
        //This is just intended as a warning.
        int genusIndex = columnIndices[0];
        int speciesIndex = columnIndices[1];

        if (genusIndex == -1 || speciesIndex == -1) {
            addMessage(0, "Did not find Genus / species column headings in spreadsheet");
            return;
        }

        Hashtable<String, Integer> genusSpeciesCombos = new Hashtable<String, Integer>();
        for (int row = 1; row <= worksheet.getNumRows(); row++) {
            String genusSpecies = worksheet.getStringValue(genusIndex, row).trim() + " " +
                    worksheet.getStringValue(speciesIndex, row).trim();
            Integer gsCount = genusSpeciesCombos.get(genusSpecies);
            if (gsCount == null) {
                gsCount = 0;
            }
            gsCount += 1;
            genusSpeciesCombos.put(genusSpecies, gsCount);
        }

        Set<String> set = genusSpeciesCombos.keySet();

        Iterator<String> itr = set.iterator();

        String key;
        while (itr.hasNext()) {
            key = itr.next();
            Integer count = genusSpeciesCombos.get(key);
            if (count > 4) {
                addMessage(0, "You collected " + count + " " + key + ". Should collect 4.");
            } else if (count < 4) {
                addMessage(0, "You collected " + count + " " + key + ". Should collect at least 4.");
            }
        }
    }


    /**
     * Smithsonian created rule to check Voucher heading
     *
     * @param worksheet
     * @throws Exception
     */
    public void checkVoucherSI(TabularDataReader worksheet) throws Exception {
        String[] headings = {"Voucher Specimen?", "Herbarium Accession No./Catalog No.", "Herbarium Acronym"};
        int[] columnIndices = this.getColumnIndices(headings);
        int vsIdx = columnIndices[0];
        int hanIdx = columnIndices[1];
        int haIdx = columnIndices[2];

        if (vsIdx == -1) {
            addMessage(0, "Did not find Voucher Specimen heading in spreadsheet.");
        }

        if (hanIdx == -1) {
            addMessage(0, "Did  not find Herbarium Accession No./Catalog No. column heading in spreadsheet.");
        }

        if (haIdx == -1) {
            addMessage(0, "Did not find Herbarium Acronym heading in spreadsheet.");
        }

        if (vsIdx == -1 || hanIdx == -1 || haIdx == -1) {
            return;
        }

        for (int row = 1; row <= worksheet.getNumRows(); row++) {
            String voucher = worksheet.getStringValue(vsIdx, row);
            if (voucher == null) {
                addMessage(row, "Missing value for 'Voucher Specimen?'. Must be Y or N.");
                continue;
            }
            voucher = voucher.trim();
            if (voucher.equals("Y")) {
                String han = worksheet.getStringValue(hanIdx, row);
                if (han == null) {
                    addMessage(row, "Missing Herbarium Accession No./Catalog No. for voucher specimen.");
                } else if (han.trim().length() <= 2) {
                    addMessage(row, "Herbarium Accession No./Catalog No. must be at least two characters long.");
                }

                String ha = worksheet.getStringValue(haIdx, row);
                if (ha == null) {
                    addMessage(row, "Missing Herbarium Acronym for voucher specimen.");
                } else if (ha.trim().length() == 0) {
                    addMessage(row, "Herbarium Acronym must be at least one character long.");
                }

            }
        }
    }

    /**
     * Smithsonian created rule.
     * No more than 96 rows per plate No.
     * Row letter between A-H
     * Column No between 01-12
     * No duplicate plate_row_column
     *
     * @throws Exception
     */
    public void checkTissueColumnsSI() throws Exception {
        String plateNoHeading = "Plate No.";
        String rowLetterHeading = "Row Letter";
        String columnNumberHeading = "Column No.";
        int plateNoIndex = -1;
        int rowLetterIndex = -1;
        int colNoIndex = -1;


        java.util.List<String> listSheetColumns = worksheet.getColNames();
        for (int col = 0; col < listSheetColumns.size(); col++) {
            String columnValue = worksheet.getStringValue(col, 0);
            if (columnValue.equals(plateNoHeading)) {
                plateNoIndex = col;
            } else if (columnValue.equals(rowLetterHeading)) {
                rowLetterIndex = col;
            } else if (columnValue.equals(columnNumberHeading)) {
                colNoIndex = col;
            }
        }

        //This check may be redundant...
        if (colNoIndex == -1 || rowLetterIndex == -1 || plateNoIndex == -1) {
            addMessage(0, "Did not find required headings for Plate / Well Row / Well Column in spreadsheet");
            return;
        }

        //Check no more than 96 rows per plate no. using a hashtable of plate numbers for the counters.
        Hashtable<String, Integer> plateCounts = new Hashtable<String, Integer>();
        //Check we have no duplicate Plate + Row + Column combinations
        Hashtable<String, Integer> plateRowColumnCombined = new Hashtable<String, Integer>();
        for (int row = 1; row <= worksheet.getNumRows(); row++) {
            //Get column values
            String plateNo = worksheet.getStringValue(plateNoIndex, row).trim();
            Integer plateCount = plateCounts.get(plateNo);
            if (plateCount == null) {
                plateCount = 0; //This is the initializer
            }
            plateCount += 1;

            if (plateCount > 96) {
                addMessage(row, "Too many rows for plate " + plateNo);
            }
            plateCounts.put(plateNo, plateCount);

            //Row letter must be A-H only (no lowercase)
            String rowLetter = worksheet.getStringValue(rowLetterIndex, row).trim();
            if (!RegEx.verifyValue("^[A-H]$", rowLetter)) {
                addMessage(row, "Bad row letter " + rowLetter + " (must be between A and H)");
            }

            //Column Number must be 01-12
            String colNo = worksheet.getStringValue(colNoIndex, row).trim();
            int col;
            try {
                col = Integer.parseInt(colNo);
                if (col < 1 && col > 12) {
                    addMessage(row, "Bad Column Number " + colNo + " (must be between 01 and 12).");
                }
            } catch (NumberFormatException e) {
                addMessage(row, "Invalid number format for Column Number");
            }

            String prc = plateNo + rowLetter + colNo;
            Integer prcRow = plateRowColumnCombined.get(prc);
            if (prcRow != null) {
                addMessage(row, "Duplicate Plate / Row / Column combination (previously defined at row " + prcRow + ")");
            } else {
                plateRowColumnCombined.put(prc, row);
            }
        }
    }


    /**
     * Check that well number and plate name are entered together and correctly
     *
     * @throws Exception
     */
    public void checkTissueColumns() throws Exception {

        for (int j = 1; j <= worksheet.getNumRows(); j++) {

            String format_name96Value = worksheet.getStringValue(getPlateName(), j);
            String well_number96Value = worksheet.getStringValue(getWellNumber(), j);

            if (format_name96Value == null && well_number96Value != null) {
                addMessage(j, "Well Number (well_number96) entered without a Plate Name (format_name96)");
            } else if (format_name96Value != null && well_number96Value == null) {
                addMessage(j, "Plate Name (format_name96) entered without a Well Number (well_number96)");
            } else if (format_name96Value == null && well_number96Value == null) {
                // ignore case where both are null (just means no tissue entered)
            } else {
                if (RegEx.verifyValue("(^[A-Ha-h])(\\d+)$", well_number96Value)) {
                    Integer intNumber = null;

                    try {
                        String strNumber = well_number96Value.substring(1, well_number96Value.length());
                        intNumber = Integer.parseInt(strNumber);
                    } catch (NumberFormatException nme) {
                        // Not a valid integer
                        addMessage(j, "Bad Well Number " + well_number96Value);
                    } catch (Exception e) {
                        addMessage(j, "Bad Well Number " + well_number96Value);
                    } finally {
                        if (intNumber <= 12 && intNumber >= 1) {
                            // ok
                        } else {
                            // Number OK but is out of range
                            addMessage(j, "Bad Well Number " + well_number96Value);
                        }
                    }
                } else {
                    // Something bigger wrong with well number syntax
                    addMessage(j, "Bad Well Number " + well_number96Value);
                }
            }

        }
    }


    /**
     * Check if this is a number
     */
    public void isNumber() throws Exception {

        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            String rowValue = worksheet.getStringValue(getColumn(), j);
            if (!checkValidNumber(rowValue)) {
                addMessage(j, rowValue + " not a number for " + getColumn());
            }
        }
    }


    /**
     * Check that this is a valid Number, for internal use only
     *
     * @param rowValue
     * @return
     */
    private boolean checkValidNumber(String rowValue) {
        if (rowValue != null && !rowValue.equals("")) {

            if (rowValue.indexOf(".") > 0) {
                try {
                    Double.parseDouble(rowValue);
                } catch (NumberFormatException nme) {
                    return false;
                }
            } else {
                try {
                    Integer.parseInt(rowValue);
                } catch (NumberFormatException nme) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks for valid lat/lng values and warns about maxerrorinmeters and horizontaldatum values
     */
    public void DwCLatLngChecker() throws Exception {
        String msg = "";
        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            Double latValue = null;
            Double lngValue = null;

            latValue = worksheet.getDoubleValue(getDecimalLatitude(), j);
            lngValue = worksheet.getDoubleValue(getDecimalLongitude(), j);

            String datumValue = worksheet.getStringValue(getHorizontalDatum(), j);
            String maxerrorValue = worksheet.getStringValue(getMaxErrorInMeters(), j);

            if (!checkValidNumber(worksheet.getStringValue(getDecimalLatitude(), j))) {
                addMessage(j, worksheet.getStringValue(getDecimalLatitude(), j) + " not a valid " + getDecimalLatitude());
            }
            if (!checkValidNumber(worksheet.getStringValue(getDecimalLongitude(), j))) {
                addMessage(j, worksheet.getStringValue(getDecimalLongitude(), j) + " not a valid " + getDecimalLongitude());
            }

            if (lngValue != null && latValue == null) {
                msg = getDecimalLongitude() + " entered without a " + getDecimalLatitude();
                addMessage(j, msg);
            }

            if (lngValue == null && latValue != null) {
                msg = getDecimalLatitude() + " entered without a " + getDecimalLongitude();
                addMessage(j, msg);
            }

            if (datumValue != null && (lngValue == null && latValue == null)) {
                msg = getHorizontalDatum() + " entered without a " + getDecimalLatitude() + " or " + getDecimalLongitude();
                addMessage(j, msg);
            }

            if (maxerrorValue != null && (lngValue == null && latValue == null)) {
                msg = getMaxErrorInMeters() + " entered without a " + getDecimalLatitude() + " or " + getDecimalLongitude();
                addMessage(j, msg);
            }

        }
    }


    /**
     * checkInXMLFields checks that Rows under the "name" attribute column in Excel Spreadsheet
     * match values in the XML <field> categories
     */
    public void checkInXMLFields() throws Exception {
        StringBuilder lookupSB = new StringBuilder();
        java.util.List<String> listFields;
        String msg;
        ResultSet resultSet = null;
        Statement statement = null;

        // First check that this column exists before running this rule
        Boolean columnExists = checkColumnExists(getColumn());
        if (!columnExists) {
            // No need to return a message here if column does not exist
            //messages.addLast(new RowMessage("Column name " + getColumn() + " does not exist", RowMessage.WARNING));
            return;
        }

        // Convert XML Field values to a Stringified list
        try {
            listFields = getListElements();
        } catch (Exception e) {
            listFields = getFields();
        }
        // Loop the fields and put in a StringBuilder
        int count = 0;
        for (int k = 0; k < listFields.size(); k++) {
            try {
                if (count > 0)
                    lookupSB.append(",");
                lookupSB.append("\'" + listFields.get(k).toString() + "\'");
                count++;
            } catch (Exception e) {
                // do nothing
            }
        }
        // Query the SQLlite instance to see if these values are contained in a particular row
        try {
            statement = connection.createStatement();
            String sql = "select " + getColumn() + " from " + digesterWorksheet.getSheetname() +
                    " where " + getColumn() + " not in (" + lookupSB.toString() + ")";

            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                msg = "\"" + resultSet.getString(getColumn()) + "\" not an approved " + getColumn() + ", see list";
                addMessage(msg, listFields, null);
            }

        } catch (SQLException e) {
            //e.printStackTrace();
            throw new Exception("SQL exception processing checkInXMLFields rule " + e.getMessage());
        } catch (Exception e) {
            //e.printStackTrace();
            throw new Exception("Unhandled exception processing checkInXMLFields for " + getColumn());
        } finally {
            statement.close();
            resultSet.close();
        }
    }

    /**
     * RequiredColumns looks for required columns in spreadsheet by looking for them in the <field> tags
     */
    public void RequiredColumns() {
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }

           // Set text for this warning values
        String levelValue = "required";
        if (getMessageLevel() == RowMessage.WARNING) {
            levelValue = "suggested";
        }

        String strNotFound = "", reqFieldName = "", msg = "";
        boolean booFound = false;
        // Create a hashset of column names for easy lookup
        Set<String> hashset = new HashSet<String>(worksheet.getColNames());
        // Loop through the list of required fields using the iterator
        Iterator itRequiredField = getFields().iterator();
        while (itRequiredField.hasNext()) {
            booFound = false;

            // Get the next required field name
            reqFieldName = itRequiredField.next().toString().trim();

            // Simple search in hashset for required field name
            if (hashset.contains(reqFieldName)) {
                booFound = true;
            }

            // Error message if column not found
            if (!booFound) {
                strNotFound += reqFieldName + " ";
                // Examine column contents -- required columns need some content
            } else {

                try {
                    rs = statement.executeQuery("select count(*) from " + digesterWorksheet.getSheetname() + " where " + reqFieldName + "='' or " + reqFieldName + " is null");
                    if (rs.getInt(1) > 0) {
                        addMessage(levelValue + " column " + reqFieldName + " has a missing cell value");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }


        if (!strNotFound.equals("")) {
            msg = "Did not find " + levelValue + " columns: " + strNotFound + " (make sure no spaces at end of name)";
            addMessage(msg);
        }
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get field values associated with a particular list
     *
     * @return
     */
    private java.util.List getListElements() {
        return digesterWorksheet.getValidation().findList(getList()).getFields();
    }

    /**
     * Add a message with a given row and message
     *
     * @param row
     * @param message
     */
    private void addMessage(Integer row, String message) {
        messages.addLast(new RowMessage(message, getMessageLevel(), row));
    }

    /**
     * Add a message with just a message and no row assigned
     *
     * @param message
     */
    private void addMessage(String message) {
        messages.addLast(new RowMessage(message, getMessageLevel()));
    }

    private void addMessage(String message, java.util.List list, Integer row) {
        messages.addLast(new RowMessage(message, list, getMessageLevel(), row));
    }

    /**
     * Get the message level we're working with for this rule
     *
     * @return
     */
    private Integer getMessageLevel() {
        if (this.getLevel().equals("warning"))
            return RowMessage.WARNING;
        else
            return RowMessage.ERROR;
    }

    /**
     * A simple check to see if a column exists in the SQLLite database
     *
     * @param column
     * @return
     * @throws SQLException
     */
    private boolean checkColumnExists(String column) throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT sql FROM sqlite_master WHERE sql like '%" + column + "%'");

        if (rs.next())
            return true;
        else
            return false;
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}