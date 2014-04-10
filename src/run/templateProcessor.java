package run;

import digester.*;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester3.Rules;
import settings.FIMSException;
import sun.jvm.hotspot.debugger.ia64.IA64ThreadContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This is a convenience class for working with templates (the spreadsheet generator).
 * We handle building working with the "process" class and the digester rules for mapping & fims,
 * in addition to providing methods for looking up definitions, and building form output
 */
public class templateProcessor {

    private process p;
    private Mapping mapping;
    private Fims fims;
    private Validation validation;

    public templateProcessor(String outputFolder, File configFile) throws Exception {
        this.p = new process(outputFolder, configFile);

        mapping = new Mapping();
        p.addMappingRules(new Digester(), mapping);

        fims = new Fims(mapping);
        p.addFimsRules(new Digester(), fims);

        validation = new Validation();
        p.addValidationRules(new Digester(), validation);
    }

    public Mapping getMapping() {
        return mapping;
    }

    public Fims getFims() {
        return fims;
    }

    public Validation getValidation() {
        return validation;
    }

    /**
     * Get a definition for a particular column name
     *
     * @param column_name
     * @return
     * @throws settings.FIMSException
     */
    public String definition(String column_name) throws FIMSException {
        String output = "";
        try {

            Iterator attributes = mapping.getAllAttributes(mapping.getDefaultSheetName()).iterator();
            while (attributes.hasNext()) {
                Attribute a = (Attribute) attributes.next();
                String column = a.getColumn();
                if (column_name.trim().equals(column.trim())) {

                    if (a.getUri() != null)
                        output += "<b>URI:</b><p><a href='" + a.getUri() + "' target='_blank'>" + a.getUri() + "</a>";
                    else
                        output += "<b>URI:</b><p>No URI available";
                    if (!a.getDefinition().trim().equals(""))
                        output += "<p><b>Definition:</b><p>" + a.getDefinition();
                    else
                        output += "<p><b>Definition:</b><p>No custom definition available";

                    return output;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException("exception handling templates " + e.getMessage(), e);
        }
        return "No definition found for " + column_name;
    }

    /**
     * Generate checkBoxes/Column Names for the mappings in a template
     *
     * @return
     * @throws FIMSException
     */
    public String printCheckboxes() throws FIMSException {
        LinkedList<String> requiredColumns = getRequiredColumns();
        String output = "";
        // A list of names we've already added
        ArrayList addedNames = new ArrayList();
        try {
            Iterator attributes = mapping.getAllAttributes(mapping.getDefaultSheetName()).iterator();
            while (attributes.hasNext()) {
                Attribute a = (Attribute) attributes.next();

                // Set the column name
                String column = a.getColumn();

                // Check that this name hasn't been already.  This is necessary in some situations where
                // column names are repeated for different entities in the configuration file
                if (!addedNames.contains(column)) {
                    // Set boolean to tell us if this is a requiredColumn
                    Boolean aRequiredColumn = false;
                    if (requiredColumns.contains(a.getColumn()))
                        aRequiredColumn = true;

                    // Construct the checkbox text
                    output += "<label class='checkbox'>\n" +
                            "\t<input type='checkbox' class='check_boxes' value='" + column + "'";
                    if (aRequiredColumn)
                        output += " checked disabled";

                    output += ">" + column + " \n" +
                            "\t<a href='#' class='def_link' name='" + column + "'>DEF</a>\n" +
                            "</label>\n";
                }

                // Now that we've added this to the output, add it to the ArrayList so we don't add it again
                addedNames.add(column);

            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException("exception handling templates " + e.getMessage(), e);
        }
        return output;
    }

    /**
     * Find the required columns on this sheet
     *
     * @return
     */
    public LinkedList<String> getRequiredColumns() {
        Iterator worksheetsIt = validation.getWorksheets().iterator();
        while (worksheetsIt.hasNext()) {
            Worksheet w = (Worksheet) worksheetsIt.next();
            Iterator rIt = w.getRules().iterator();
            while (rIt.hasNext()) {
                Rule r = (Rule) rIt.next();
                //System.out.println(r.getType());
                if (r.getType().equals("RequiredColumns")) {
                    return r.getFields();
                }
            }
        }
        return null;
    }

    /**
     * main method for testing only
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        File configFile = new configurationFileFetcher(1, "tripleOutput", false).getOutputFile();

        templateProcessor t = new templateProcessor("tripleOutput", configFile);
        System.out.println(t.printCheckboxes());
    }
}
