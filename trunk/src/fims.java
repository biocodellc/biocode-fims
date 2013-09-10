import digester.*;
import triplify.triplifier;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Level;

import java.io.*;


/**
 * Test the Apache Digester
 */
public class fims {

    static String configFilename = "sampledata/configuration.xml";
    static String inputFilename = "sampledata/biocode_template.xls";
    static String outputFolder = System.getProperty("user.dir") + File.separator + "tripleOutput" + File.separator;

    //public static Mapping mapping;


    public static void main(String args[]) {
        // Setup logging
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // Digester setup
        Digester d = new Digester();

        triplifier t = new triplifier(inputFilename, outputFolder);

        try {
            System.out.println("Initializing ...");

            // Setup validation
            //Validation validation = new Validation();
            //d.push(validation);
            //addValidationRules(d);

            // Setup triplifier mapping
            Mapping mapping = new Mapping(t);
            d.push(mapping);
            addMappingRules(d);

            System.out.println("\tSuccess!");

            // Digester Parsing configuration File
            System.out.println("Parse configuration ...");
            d.parse(new File(configFilename));
            System.out.println("\tSuccess!");

            // Validate
            System.out.println("Validate ...");
            System.out.println("\tNot yet implemented");

            // Triplify
            System.out.println("Triplify ...");
            String results = t.getTriples(mapping);
            System.out.println("\tSuccess! see: " + results);

            // Upload
            System.out.println("Upload ...");
            System.out.println("\tNot yet implemented");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }


    /**
     * Process validation component rules
     *
     * @param d
     */
    private static void addValidationRules(Digester d) {
        // Create worksheet objects
        d.addObjectCreate("fims/validation/worksheet", Worksheet.class);
        d.addSetProperties("fims/validation/worksheet");
        d.addSetNext("fims/validation/worksheet", "addWorksheet");

        // Create rule objects
        d.addObjectCreate("fims/validation/worksheet/rule", Rule.class);
        d.addSetProperties("fims/validation/worksheet/rule");
        d.addSetNext("fims/validation/worksheet/rule", "addRule");
        d.addCallMethod("fims/validation/worksheet/rule/field", "addField", 0);
    }

    /**
     * Process mapping component rules
     *
     * @param d
     */
    private static void addMappingRules(Digester d) {
        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Create relation objects
        d.addObjectCreate("fims/mapping/relation", Relation.class);
        d.addSetNext("fims/mapping/relation", "addRelation");
        //d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);

        //d.addObjectCreate("fims/mapping/relation/subject", Entity.class);
        //d.addSetProperties("fims/mapping/relation/subject");
        d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);
        d.addCallMethod("fims/mapping/relation/predicate", "addPredicate", 0);


        //d.addObjectCreate("fims/mapping/relation/object", Entity.class);
        //d.addSetProperties("fims/mapping/relation/object");
        d.addCallMethod("fims/mapping/relation/object", "addObject", 0);

    }


}
