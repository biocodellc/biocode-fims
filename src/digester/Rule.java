package digester;

import reader.plugins.TabularDataReader;
import renderers.Message;
import settings.RegEx;

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
    private TabularDataReader worksheet;
    private Worksheet digesterWorksheet;

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

    private LinkedList<Message> messages = new LinkedList<Message>();

    public LinkedList<Message> getMessages() {
        return messages;
    }

    public Worksheet getDigesterWorksheet() {
        return digesterWorksheet;
    }

    public void setDigesterWorksheet(Worksheet digesterWorksheet) throws Exception {
        this.digesterWorksheet = digesterWorksheet;
    }

    public TabularDataReader getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(TabularDataReader worksheet) throws Exception {
        this.worksheet = worksheet;
         // Synchronize the Excel Worksheet instance with the digester worksheet instance
        //System.out.println("setting to "+ digesterWorksheet.getSheetname());
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
        //System.out.println("    rule type = " + this.type + "; column = " + this.column + "; level = " + this.level);

        for (Iterator i = fields.iterator(); i.hasNext(); ) {
            String field = (String) i.next();
            System.out.println("      field data : " + field);
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
     * Make sure this column contains unique values
     */
    /*
    public void uniqueValue() {
        System.out.println("\tunique value rule " + worksheet.getSheetname());
    }
    */

    /**
     * Lookup the fields belonging to a particular list
     */
    /*
    public void lookupList() {
        System.out.println("\tlookupList rule for list=" + getList() + " on column " + getColumn());
        // Fetch the fields belonging to this list and create a List object
       java.util.List list = digesterWorksheet.getValidation().findList(getList()).getFields();
        // Loop values
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            System.out.println("\t\t" + i.next());
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
            messages.addLast(new Message(theWarning, Message.WARNING));
        }
    }

    /**
     * Check to see if there duplicateColumn Headers in this sheet
     */

    public void duplicateColumnNames() {

        // Create a list of Column Names in Worksheet

        // List<String> listSheetColumns = worksheet.getSheet().getColNames();
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
            messages.addLast(new Message(message, Message.WARNING));
        }
    }


    public void uniqueValue() throws Exception {
        String cellValue = "";
        java.util.List values = new ArrayList();
        java.util.List<String> listFields = getFields();

        String rowValue = "";
        String field = "";

        // populate the ArrayList
        for (int j = 0; j <= worksheet.getNumRows(); j++) {
            String[] rowValues = getColumn().split(",");

            rowValue = "";
            for (int m = 0; m < rowValues.length; m++) {
                String cell = null;
                try {
                    cell = worksheet.getStringValue(rowValues[m], j);
                } catch (NullPointerException e1) {
                    break;
                }
                if (cell != null) {
                    rowValue += cell;
                }

            }
            if (rowValue != "") {
                values.add(rowValue);
                //System.out.println(rowValue);
            }
        }

        // Loop all the values in this list and be sure they are all unique
        for (int j = 1; j < values.size(); j++) {
            for (int i = 1; i < values.size(); i++) {
                if (j != i) {
                    if (values.get(j) != null) {
                        if (values.get(j).equals(values.get(i))) {
                            addMessage(j, values.get(j) + " used more than once in " + getColumn() + " column");
                            // increment to end of loop
                            i = values.size();
                        }
                    }
                }
            }
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
    public void checkInXMLFields() {
        String rowValue = "", field = "", msg = "";
        boolean booFound = false;
        java.util.List<String> listFields;

        // Build List of XML Fields, but check first to see if it is pointing
        // to the metadata list element
        try {
            listFields = getListElements();
        } catch (Exception e) {
            listFields = getFields();
        }

        String colName = "";

        //try {
       //System.out.println("looking at  = " + worksheet.getSheet()+ this.getType() + this.getColumn() + this.getList());

        // Check that photoListRow Data matches Field Definitions
        for (int j = 1; j <= worksheet.getNumRows(); j++) {
            // handle case where name is separated by commas (allowing multiple fields to
            // be parsed at once)
            String[] rowValues = getColumn().split(",");
            for (int m = 0; m < rowValues.length; m++) {
                colName = rowValues[m];
                rowValue = "";
                try {
                    rowValue = worksheet.getStringValue(colName, j);
                } catch (Exception e) {
                    // Do nothing for exception, likely this indicates null values or not found
                    // columns, which is OK--- they are caught by other rules
                }
                if (rowValue != null && !rowValue.equals("")) {
                    booFound = false;
                    for (int k = 0; k < listFields.size(); k++) {
                        field = "";
                        try {
                            field = listFields.get(k).toString();
                        } catch (Exception e) {
                            // do nothing
                        }
                        if (rowValue.equals(field)) {
                            booFound = true;
                        }

                        //System.out.println(field + " _ " + rowValue);
                    }
                    if (!booFound) {
                        msg = "Did not find " + rowValues[m] + " " + rowValue;

                         //" <a href='" + rowValues[m] + " acceptable values\n\n" + listFields.toString().replaceAll("'", "&rsquo;") + "' target='popup'>View List</a>";
                        addMessage(msg, listFields, j);
                    }
                }
            }

        }
    }

    /**
     * RequiredColumns looks for required columns in spreadsheet by looking for them in the <field> tags
     */
    public void RequiredColumns() {
        String strNotFound = "", reqFieldName = "", msg = "";
        boolean booFound = false;
        // Create a list of Column Names in Worksheet
        //List<String> listSheetColumns = worksheet.getSheet().getColNames();
        java.util.List<String> listSheetColumns = worksheet.getColNames();

        // Loop through the list of required fields using the iterator
        Iterator itRequiredField = getFields().iterator();
        while (itRequiredField.hasNext()) {
            // Get the next required field name
            reqFieldName = itRequiredField.next().toString().trim();
            booFound = false;
            // Loop all the columns in the sheet
            for (int i = 0; i < listSheetColumns.size(); i++) {
                String columnValue = null;
                try {
                    columnValue = worksheet.getStringValue(reqFieldName, 0);
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

                if (columnValue != null) {
                    columnValue = columnValue.trim();
                    if (reqFieldName.equals(columnValue)) {
                        booFound = true;
                    }
                }
            }

            // Error message if column not found
            if (!booFound) {
                strNotFound += reqFieldName + " ";
                // Examine column contents -- required columns need some content
            } else {
                for (int i = 0; i <= worksheet.getNumRows(); i++) {
                    String rowValue = null;
                    try {
                        rowValue = worksheet.getStringValue(reqFieldName, i);
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    //System.out.println(i + "=" + rowValue + "="+ reqFieldName);
                    if (rowValue == null || rowValue.trim().equals("")) {
                        addMessage(i, reqFieldName + " has missing cell value");
                    }
                }
            }
        }

        if (!strNotFound.equals("")) {
            msg = "Did not find columns: " + strNotFound + " (make sure no spaces at end of name)";
            addMessage(msg);
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
     * @param row
     * @param message
     */
    private void addMessage(Integer row, String message) {
        messages.addLast(new Message(message, getMessageLevel(), row));
    }

    /**
     * Add a message with just a message and no row assigned
     * @param message
     */
    private void addMessage(String message) {
        messages.addLast(new Message(message, getMessageLevel()));
    }

    private void addMessage(String message, java.util.List list, Integer row) {
        messages.addLast(new Message(message, list,getMessageLevel(),row));
    }

    /**
     * Get the message level we're working with for this rule
     * @return
     */
    private Integer getMessageLevel() {
        if (this.getLevel().equals("warning"))
            return Message.WARNING;
        else
            return Message.ERROR;
    }

}