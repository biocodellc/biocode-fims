package digester;


import renderers.Message;
import renderers.error;
import renderers.warning;
import settings.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Define rules-- this class does the work, as defined by XML validation file
 */
public class Rules {
    LinkedList rule = new LinkedList();
    //private bVWorksheet worksheet;
    private String validMessage = "VALID";
    private String notImplementedMessage = "NOT IMPLEMENTED";
    private ArrayList errors = new ArrayList();
    private ArrayList valids = new ArrayList();
    private ArrayList warnings = new ArrayList();
    private boolean haserrors = true;
    private boolean haswarnings = true;
    //private mainPage mp = null;


    public void addRule(Rule r) {
        rule.addLast(r);
    }

    public void print() {
        System.out.println("There are " + rule.size() + " rules to process");

        for (Iterator i = rule.iterator(); i.hasNext();) {
            Rule r = (Rule) i.next();
            r.print();
        }
    }

    public boolean hasWarnings() {
        return haswarnings;
    }

    public boolean hasErrors() {
        return haserrors;
    }

    //public void run(mainPage mp) {
    public void run() {
        System.out.println("rules method");
      //  this.mp = mp;
        // Run System rules first (low level rules that apply to ALL applications)

        /*
        Rule duplicateColumnNames = new Rule();
        duplicateColumnNames.setType("duplicateColumnNames");
        duplicateColumnNames.setLevel("warning");
        duplicateColumnNames.setName(worksheet.getSheetName());
        rule.add(duplicateColumnNames);

        Rule rowNumMismatch = new Rule();
        rowNumMismatch.setType("rowNumMismatch");
        rowNumMismatch.setLevel("warning");
        rowNumMismatch.setName(worksheet.getSheetName());
        rule.add(rowNumMismatch);
        */


        // Loop through Rules as defined in the XML Configuration File
        int count = 1;
        for (Iterator i = rule.iterator(); i.hasNext();) {
            Rule r = (Rule) i.next();


            // Invoke Methods in this class based on values in the XML validation file
            Method method = null;
            try {
                  //mp.insertMsg("Processing rule " + r.getType() + " for " + r.getColumn() + " (" + count + " of " + rule.size() + ")");
                System.out.println("Processing rule " + r.getType() + " for " + r.getColumn() + " (" + count + " of " + rule.size() + ")");
                method = this.getClass().getMethod(r.getType(), Rule.class);
            } catch (SecurityException e) {
                addMessage(r, "Application could not process " + r.getType() + " rule");
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                addMessage(r, "No such method " + r.getType() + ".  This may indicate your version of bioValidator needs to be upgraded.");
                e.printStackTrace();
            }

            count++;
            if (method != null) {
                try {
                    method.invoke(this, r);
                } catch (IllegalAccessException e) {
                    addMessage(r, "Application could not process " + r.getType() + " rule (" + r.getColumn() + ")");
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    addMessage(r, "Application could not process " + r.getType() + " rule (" + r.getColumn() + ")");
                    e.printStackTrace();
                } catch (Exception e) {
                    addMessage(r, "Application could not process " + r.getType() + " rule (" + r.getColumn() + ")");
                    e.printStackTrace();
                }
            }
        }

        Iterator eit = this.errors.iterator();
        if (!eit.hasNext()) this.haserrors = false;

        Iterator wit = this.warnings.iterator();
        if (!wit.hasNext()) this.haswarnings = false;
    }

    /**
     * Warn user if the number of rows the application is looking at differs from what is on the spreadsheet
     * @param r
     */
    /*
    public void rowNumMismatch(Rule r) {
        int bVRows = this.worksheet.getNumRows();
        int poiRows = this.worksheet.getSheet().getLastRowNum();
        if (bVRows < poiRows) {
            String theWarning = "bioValidator processed " + bVRows + " rows, but it appears there may be " + poiRows +
                    " rows on your sheet. This usually occurs when cell formatting has extended below the area of data but " +
                    "may also occur when you have left blank rows in the data.";
            warnings.add(new warning(r, (theWarning)));
        }
    }
    */
    /**
     * Check to see if there duplicateColumn Headers in this sheet
     *
     * @param r
     */
    /*
    public void duplicateColumnNames(Rule r) {

        // Create a list of Column Names in Worksheet

        // List<String> listSheetColumns = this.worksheet.getSheet().getColNames();
        List<String> listSheetColumns = this.worksheet.getColNames();
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
            warnings.add(new warning(r, (String) outputit.next()));
        }
    }
    */

    /**
     * Nothing here yet
     */
    public void validYearMonthDay(Rule r) {

    }

    /*
    public void uniqueValue(Rule r) throws Exception {
        String cellValue = "";
        List values = new ArrayList();
        List<String> listFields = r.getFields();

        String rowValue = "";
        String field = "";

        // populate the ArrayList
        for (int j = 0; j <= worksheet.numRows; j++) {
            String[] rowValues = r.getName().split(",");

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
                            addMessage(j, r, values.get(j) + " used more than once in " + r.getName() + " column");
                            // increment to end of loop
                            i = values.size();
                        }
                    }
                }
            }
        }
    }
    */

    /**
     * @param r
     */
    /*
    public void BoundingBox(Rule r) throws Exception {

        // Build List of XML Fields
        List<String> listFields = r.getFields();

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
        for (int j = 1; j <= worksheet.numRows; j++) {
            Double latValue = 0.0;
            Double longValue = 0.0;

            if (!checkValidNumber(worksheet.getStringValue(r.getDecimalLatitude(), j)) ||
                    !checkValidNumber(worksheet.getStringValue(r.getDecimalLongitude(), j))) {
                addMessage(j, r, "Unable to perform BoundingBox check due to illegal Latitude or Longitude value");
            } else {

                latValue = worksheet.getDoubleValue(r.getDecimalLatitude(), j);
                longValue = worksheet.getDoubleValue(r.getDecimalLongitude(), j);

                if (latValue != null && latValue != 0.0 && (latValue < minLat || latValue > maxLat)) {
                    msg = r.getDecimalLatitude() + " " + latValue + " outside of " + r.getName() + " bounding box.";
                    addMessage(j, r, msg);
                }
                if (longValue != null && longValue != 0.0 && (longValue < minLng || longValue > maxLng)) {
                    msg = r.getDecimalLongitude() + " " + longValue + " outside of " + r.getName() + " bounding box.";
                    addMessage(j, r, msg);
                }
            }
        }
    }
    */

    /*
    public void checkLowestTaxonLevel(Rule r) throws Exception {

        for (int j = 1; j <= worksheet.numRows; j++) {
            String LowestTaxonLevelValue = worksheet.getStringValue("LowestTaxonLevel", j);
            String LowestTaxonValue = worksheet.getStringValue("LowestTaxon", j);

            if (LowestTaxonLevelValue == null && LowestTaxonValue != null) {
                addMessage(j, r, "LowestTaxon entered without a LowestTaxonLevel");
            } else if (LowestTaxonLevelValue != null && LowestTaxonValue == null) {
                addMessage(j, r, "LowestTaxonLevel entered without a LowestTaxon");
            }
        }

    }
    */
    /*
    protected int[] getColumnIndices( String[] columns) {
        int [] b = new int[columns.length];
        List<String> listSheetColumns = this.worksheet.getColNames();
        for(int col=0; col < columns.length; col++) {
            b[col] = -1;
            for(int cname=0; cname < listSheetColumns.size(); cname++) {
                String heading = worksheet.getStringValue(cname,0).trim();
                if (columns[col].equals(heading)){
                    b[col] = cname;
                }
            }
        }
        return b;
    }
    */

    /*
    public void checkGenusSpeciesCountsSI(Rule r) throws Exception {
        String genusHeading = "Genus";
        String speciesHeading = "Species";
        String[] headings = { genusHeading, speciesHeading };
        int[] columnIndices = this.getColumnIndices( headings );
        //Collectors SHOULD collect exactly 4 samples for each Genus species
        //This is just intended as a warning.
        int genusIndex = columnIndices[0];
        int speciesIndex = columnIndices[1];

        if(genusIndex == -1 || speciesIndex == -1) {
            addMessage(0,r,"Did not find Genus / species column headings in spreadsheet");
            return;
        }

        Hashtable<String,Integer> genusSpeciesCombos = new Hashtable<String, Integer>();
        for(int row=1; row<= worksheet.numRows; row++) {
            String genusSpecies = worksheet.getStringValue( genusIndex, row).trim() + " " +
                                    worksheet.getStringValue(speciesIndex, row).trim();
            Integer gsCount = genusSpeciesCombos.get(genusSpecies);
            if(gsCount == null){
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
            if( count > 4 ) {
                addMessage(0,r,"You collected " + count + " " + key + ". Should collect 4.");
            }
            else if( count < 4 ) {
                addMessage(0,r,"You collected " + count + " " + key + ". Should collect at least 4.");
            }
        }
    }
    */

    /*
    public void checkVoucherSI(Rule r) throws Exception {
        String[] headings = { "Voucher Specimen?", "Herbarium Accession No./Catalog No.", "Herbarium Acronym"};
        int [] columnIndices = this.getColumnIndices(headings);
        int vsIdx = columnIndices[0];
        int hanIdx = columnIndices[1];
        int haIdx = columnIndices[2];

        if (vsIdx == -1){
            addMessage(0,r, "Did not find Voucher Specimen heading in spreadsheet.");
        }

        if (hanIdx == -1){
            addMessage(0,r, "Did  not find Herbarium Accession No./Catalog No. column heading in spreadsheet.");
        }

        if (haIdx == -1) {
            addMessage(0,r, "Did not find Herbarium Acronym heading in spreadsheet.");
        }

        if (vsIdx == -1 || hanIdx == -1 || haIdx == -1) {
            return;
        }

        for(int row=1;row<=worksheet.numRows;row++){
            String voucher = worksheet.getStringValue(vsIdx, row);
            if(voucher == null) {
                addMessage(row, r, "Missing value for 'Voucher Specimen?'. Must be Y or N.");
                continue;
            }
            voucher = voucher.trim();
            if (voucher.equals("Y")) {
                String han = worksheet.getStringValue(hanIdx, row);
                if (han == null) {
                    addMessage(row, r, "Missing Herbarium Accession No./Catalog No. for voucher specimen.");
                }
                else if (han.trim().length() <=2) {
                    addMessage(row, r, "Herbarium Accession No./Catalog No. must be at least two characters long.");
                }

                String ha = worksheet.getStringValue(haIdx, row);
                if (ha == null) {
                    addMessage(row, r, "Missing Herbarium Acronym for voucher specimen.");
                }
                else if (ha.trim().length() == 0) {
                    addMessage(row, r, "Herbarium Acronym must be at least one character long.");
                }

            }
        }
    }
    */

    /*
	public void checkTissueColumnsSI(Rule r) throws Exception {
		String plateNoHeading = "Plate No.";
		String rowLetterHeading = "Row Letter";
		String columnNumberHeading = "Column No.";
		int plateNoIndex = -1;
		int rowLetterIndex = -1;
		int colNoIndex = -1;

		//No more than 96 rows per plate No.
		//Row letter between A-H
		//Column No between 01-12
		//No duplicate plate_row_column

		List<String> listSheetColumns = this.worksheet.getColNames();
		for (int col=0; col < listSheetColumns.size(); col++) {
			String columnValue = worksheet.getStringValue(col, 0);
			if (columnValue.equals( plateNoHeading )) {
				plateNoIndex = col;
			}
			else if (columnValue.equals(rowLetterHeading)) {
				rowLetterIndex = col;
			}
			else if (columnValue.equals(columnNumberHeading)) {
				colNoIndex = col;
			}
		}

        //This check may be redundant...
		if (colNoIndex == -1 || rowLetterIndex == -1 || plateNoIndex == -1) {
			addMessage( 0,r, "Did not find required headings for Plate / Well Row / Well Column in spreadsheet" );
			return;
		}

        //Check no more than 96 rows per plate no. using a hashtable of plate numbers for the counters.
		Hashtable<String,Integer> plateCounts = new Hashtable<String, Integer>();
        //Check we have no duplicate Plate + Row + Column combinations
        Hashtable<String,Integer> plateRowColumnCombined = new Hashtable<String, Integer>();
		for (int row=1; row <= worksheet.numRows; row++) {
            //Get column values
	        String plateNo   = worksheet.getStringValue(plateNoIndex, row).trim();
            Integer plateCount = plateCounts.get(plateNo);
            if(plateCount == null){
                plateCount = 0; //This is the initializer
            }
            plateCount += 1;

            if(plateCount > 96) {
                addMessage(row, r, "Too many rows for plate " + plateNo);
            }
            plateCounts.put(plateNo, plateCount);

            //Row letter must be A-H only (no lowercase)
            String rowLetter = worksheet.getStringValue(rowLetterIndex, row).trim();
            if(!RegEx.verifyValue("^[A-H]$", rowLetter)) {
                addMessage(row, r, "Bad row letter " + rowLetter + " (must be between A and H)");
            }

            //Column Number must be 01-12
            String colNo     = worksheet.getStringValue(colNoIndex, row).trim();
            int col;
            try {
                col = Integer.parseInt(colNo);
                if ( col < 1 && col > 12 ) {
                    addMessage(row, r, "Bad Column Number " + colNo + " (must be between 01 and 12).");
                }
            } catch (NumberFormatException e) {
                addMessage(row, r, "Invalid number format for Column Number");
            }

            String prc = plateNo + rowLetter + colNo;
            Integer prcRow = plateRowColumnCombined.get(prc);
            if(prcRow != null) {
                addMessage(row, r, "Duplicate Plate / Row / Column combination (previously defined at row " + prcRow + ")");
            }
            else {
                plateRowColumnCombined.put(prc, row);
            }
		}
	}
    */

    /*
    public void checkTissueColumns(Rule r) throws Exception {

        for (int j = 1; j <= worksheet.numRows; j++) {

            String format_name96Value = worksheet.getStringValue(r.getPlateName(), j);
            String well_number96Value = worksheet.getStringValue(r.getWellNumber(), j);

            if (format_name96Value == null && well_number96Value != null) {
                addMessage(j, r, "Well Number (well_number96) entered without a Plate Name (format_name96)");
            } else if (format_name96Value != null && well_number96Value == null) {
                addMessage(j, r, "Plate Name (format_name96) entered without a Well Number (well_number96)");
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
                        addMessage(j, r, "Bad Well Number " + well_number96Value);
                    } catch (Exception e) {
                        addMessage(j, r, "Bad Well Number " + well_number96Value);
                    }
                    finally {
                        if (intNumber <= 12 && intNumber >= 1) {
                            // ok
                        } else {
                            // Number OK but is out of range
                            addMessage(j, r, "Bad Well Number " + well_number96Value);
                        }
                    }
                } else {
                    // Something bigger wrong with well number syntax
                    addMessage(j, r, "Bad Well Number " + well_number96Value);
                }
            }

        }
    }
    */


    /**
     * Check if this is a number
     *
     */
     /*
    public void isNumber(Rule r) throws Exception {

        for (int j = 1; j <= worksheet.numRows; j++) {
            String rowValue = worksheet.getStringValue(r.getName(), j);
            if (!checkValidNumber(rowValue)) {
                addMessage(j, r, rowValue + " not a number for " + r.getName());
            }
        }
    }
    */


    // public boolean checkValidNumber(String rowValue, int row, Rule r, String s) {

    public boolean checkValidNumber(String rowValue) {


        if (rowValue != null && !rowValue.equals("")) {

            if (rowValue.indexOf(".") > 0) {
                try {
                    Double.parseDouble(rowValue);
                }
                catch (NumberFormatException nme) {
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
     *
     * @param r
     */
    /*
    public void DwCLatLngChecker(Rule r) throws Exception {
        String msg = "";
        for (int j = 1; j <= worksheet.numRows; j++) {
            Double latValue = null;
            Double lngValue = null;

            latValue = worksheet.getDoubleValue(r.getDecimalLatitude(), j);
            lngValue = worksheet.getDoubleValue(r.getDecimalLongitude(), j);

            String datumValue = worksheet.getStringValue(r.getHorizontalDatum(), j);
            String maxerrorValue = worksheet.getStringValue(r.getMaxErrorInMeters(), j);

            if (!checkValidNumber(worksheet.getStringValue(r.getDecimalLatitude(), j))) {
                addMessage(j, r, worksheet.getStringValue(r.getDecimalLatitude(), j) + " not a valid " + r.getDecimalLatitude());
            }
            if (!checkValidNumber(worksheet.getStringValue(r.getDecimalLongitude(), j))) {
                addMessage(j, r, worksheet.getStringValue(r.getDecimalLongitude(), j) + " not a valid " + r.getDecimalLongitude());
            }

            if (lngValue != null && latValue == null) {
                msg = r.getDecimalLongitude() + " entered without a " + r.getDecimalLatitude();
                addMessage(j, r, msg);
            }

            if (lngValue == null && latValue != null) {
                msg = r.getDecimalLatitude() + " entered without a " + r.getDecimalLongitude();
                addMessage(j, r, msg);
            }

            if (datumValue != null && (lngValue == null && latValue == null)) {
                msg = r.getHorizontalDatum() + " entered without a " + r.getDecimalLatitude() + " or " + r.getDecimalLongitude();
                addMessage(j, r, msg);
            }

            if (maxerrorValue != null && (lngValue == null && latValue == null)) {
                msg = r.getMaxErrorInMeters() + " entered without a " + r.getDecimalLatitude() + " or " + r.getDecimalLongitude();
                addMessage(j, r, msg);
            }

        }
    }
    */


    /**
     * checkInXMLFields checks that Rows under the "name" attribute column in Excel Spreadsheet
     * match values in the XML <field> categories
     *
     * @param r
     */
    /*
    public void checkInXMLFields(Rule r) {
        String rowValue = "", field = "", msg = "";
        boolean booFound = false;
        List<String> listFields;

        // Build List of XML Fields, but check first to see if it is pointing
        // to the metadata list element
        try {
            listFields = r.getListElements(r.getList());
        } catch (Exception e) {
            listFields = r.getFields();
        }

        String colName = "";

        //try {

        // Check that photoListRow Data matches Field Definitions
        for (int j = 1; j <= worksheet.numRows; j++) {
            // handle case where name is separated by commas (allowing multiple fields to
            // be parsed at once)
            String[] rowValues = r.getName().split(",");
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
                    }
                    if (!booFound) {
                        msg = "Did not find " + rowValues[m] + " " + rowValue +
                                " <a href='" + rowValues[m] + " acceptable values\n\n" + listFields.toString().replaceAll("'", "&rsquo;") + "' target='popup'>View List</a>";
                        addMessage(j, r, msg);
                    }
                }
            }

        }
        //} catch (Exception e) {
        //    msg = "Could not run check for " + colName + ".  Ignore this message if you do not need this field";
        //   addMessage(r,msg);
        //}

        if (msg.equals("")) {
            valids.add(new valid(r));
        }
    }
*/

    /**
     * RequiredColumns looks for required columns in spreadsheet by looking for them in the <field> tags
     *
     * @param r
     */
    /*
    public void RequiredColumns(Rule r)  {
        String strNotFound = "", reqFieldName = "", msg = "";
        boolean booFound = false;
        // Create a list of Column Names in Worksheet
        //List<String> listSheetColumns = this.worksheet.getSheet().getColNames();
        List<String> listSheetColumns = this.worksheet.getColNames();

        // Loop through the list of required fields using the iterator
        Iterator itRequiredField = r.getFields().iterator();
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
                for (int i = 0; i <= worksheet.numRows; i++) {
                    String rowValue = null;
                    try {
                        rowValue = worksheet.getStringValue(reqFieldName, i);
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    //System.out.println(i + "=" + rowValue + "="+ reqFieldName);
                    if (rowValue == null || rowValue.trim().equals("")) {
                        addMessage(i, r, reqFieldName + " has missing cell value");
                    }
                }
            }
        }

        if (!strNotFound.equals("")) {
            msg = "Did not find columns: " + strNotFound + " (make sure no spaces at end of name)";
            addMessage(r, msg);
        }

        if (msg.equals("")) {
            valids.add(new valid(r));
        }

    }
    */

    /**
     * Gather messages encountered while running Rules
     *
     * @return
     */
    public String printMessages(String radioValue) {
        String msg = "";
        msg += messagePrinter(this.errors.iterator(), "errors", radioValue);
        msg += messagePrinter(this.warnings.iterator(), "warnings", radioValue);

        return msg;
    }

    /**
     * messagePrinter collects all messages for each Message subclass and displays by row or message
     *
     * @param it         Iterator
     * @param type       can be "Error" or "Warning"
     * @param radioValue can be "row" or "message"
     * @return
     */
    public String messagePrinter(Iterator it, String type, String radioValue) {
        // Initialize variables
        String msg = "";
        ArrayList linemessages = new ArrayList();
        ArrayList messages = new ArrayList();
        Map<String, Integer> List = new HashMap<String, Integer>();

        // Header
        if (it.hasNext()) {
            //msg += "<h3>Displaying " + type + " for " + worksheet.getSheetName() + " sorted by " + radioValue + "</h3>\n";
            msg += "<h3>Displaying " + type + " for [some worksheet] sorted by " + radioValue + "</h3>\n";
        }


        // "row" displays all rows so use ArrayList to sort based on Row
        if (radioValue.equalsIgnoreCase("row")) {
            ArrayList strArr = new ArrayList();

            while (it.hasNext()) {
                Message m = (Message) it.next();
                strArr.add(m.getLineMessage());// + "\n";
            }

            // IntuitiveStringComparator sorts logically (e.g. Row1, Row2, Row10)
            Collections.sort(strArr, new IntuitiveStringComparator());
            for (int i = 0; i < strArr.size(); i++) {
                msg += strArr.get(i) + "<br>\n";
            }

            // "message" is a unique list so we create a map with messages, and wehen we sort
            // it, it deletes duplicates for us automatically
        } else {

            // Populate Map
            while (it.hasNext()) {
                Message m = (Message) it.next();
                List.put(m.message, m.getRow());
            }
            // Sort based on Value (message)
            SortedSet<String> sortedList = new TreeSet<String>(List.keySet());
            Iterator<String> sortedIterator = sortedList.iterator();
            while (sortedIterator.hasNext()) {
                msg += (sortedIterator.next()) + "<br>\n";
            }
        }
        return msg;
    }

    public void addMessage(int row, Rule r, String message) {
        //row = row + Integer.parseInt(mp.FieldHeadingsOnRow.getSelectedItem().toString()) -1;

        if (r.getLevel().equals("warning")) {
            warnings.add(new warning(row, r, message));
        } else {
            errors.add(new error(row, r, message));
        }
    }

    public void addMessage(Rule r, String message) {
        if (r.getLevel().equals("warning")) {
            warnings.add(new warning(r, message));
        } else {
            errors.add(new error(r, message));
        }
    }

}

