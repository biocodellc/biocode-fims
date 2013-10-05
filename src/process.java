import digester.*;
import org.xml.sax.SAXException;
import reader.ReaderManager;
import reader.plugins.TabularDataReader;
import settings.PathManager;
import triplify.triplifier;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Level;

import java.io.*;

/**
 * Core class for running fims processes.  Here you specify the input file, configuration file, output folder, and
 * a project code, which is used to specify identifier roots in the BCID (http://code.google.com/p/bcid/) system.
 * The main class is configured to run this from the command-line while the class constructor can also be adapted
 * to any future REST interface calls.
 */
public class process {

    String configFilename;
    String inputFilename;
    String outputFolder;
    String project_code;

    /**
     * process is the main function for validating, triplifying, & uploading fims data
     *
     * @param configFilename A configuration file in XML format for a group of projects
     * @param inputFilename The data to process, usually an Excel spreadsheet
     * @param outputFolder  Where to store output files
     * @param project_code A distinct project code for the project being loaded, used to lookup projects in the BCID system
    for assigning identifier roots.
     */
    public process(String configFilename, String inputFilename, String outputFolder, String project_code) {
        // Set class variables
        this.configFilename = configFilename;
        this.inputFilename = inputFilename;
        this.outputFolder = outputFolder;
        this.project_code = project_code;

        // Setup logging
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
    }

    /**
     * runAll method is designed to go through entire fims process: validate, triplify, upload
     */
    public void runAll() {
        boolean validationGood = true;
        boolean triplifyGood = true;
        boolean updateGood = true;

        try {
            // Initializing
            System.out.println("Initializing ...");
            System.out.println("\tinputFilename = " + inputFilename);
            System.out.println("\tconfigFilename = " + configFilename);

            // Read the input file & create the ReaderManager and load the plugins.
            ReaderManager rm = new ReaderManager();
            rm.loadReaders();
            TabularDataReader tdr = rm.openFile(inputFilename);

            // Validation
            Validation validation = new Validation(tdr);
            addValidationRules(new Digester(), validation);
            validationGood = validation.run();
            validation.print();

            // Triplify if we validate
            if (validationGood) {
                Mapping mapping = new Mapping(new triplifier(tdr, project_code + "_output", outputFolder), project_code);
                addMappingRules(new Digester(), mapping);
                triplifyGood = mapping.run();
                mapping.print();


                // Upload after triplifying
                if (triplifyGood) {
                    Fims fims = new Fims(mapping);
                    addFimsRules(new Digester(), fims);
                    fims.run();
                    fims.print();

                    System.out.println("\tspreadsheet = " + fims.write());

                }
            }

        } catch (Exception e) {
            System.out.println("Stopping Execution, Error: " + e.getMessage());
            //e.printStackTrace();
            System.exit(-1);
        }
    }


    /**
     * Process metadata component rules
     *
     * @param d
     */
    private void addFimsRules(Digester d, Fims fims) throws IOException, SAXException {
        d.push(fims);
        d.addObjectCreate("fims/metadata", Metadata.class);
        d.addSetProperties("fims/metadata");
        d.addCallMethod("fims/metadata", "addText_abstract", 0);
        d.addSetNext("fims/metadata", "addMetadata");

        d.parse(new File(configFilename));
    }

    /**
     * Process validation component rules
     *
     * @param d
     */
    private void addValidationRules(Digester d, Validation validation) throws IOException, SAXException {
        d.push(validation);

        // Create worksheet objects
        d.addObjectCreate("fims/validation/worksheet", Worksheet.class);
        d.addSetProperties("fims/validation/worksheet");
        d.addSetNext("fims/validation/worksheet", "addWorksheet");

        // Create rule objects
        d.addObjectCreate("fims/validation/worksheet/rule", Rule.class);
        d.addSetProperties("fims/validation/worksheet/rule");
        d.addSetNext("fims/validation/worksheet/rule", "addRule");
        d.addCallMethod("fims/validation/worksheet/rule/field", "addField", 0);

        // Create list objects
        d.addObjectCreate("fims/validation/lists/list", List.class);
        d.addSetProperties("fims/validation/lists/list");
        d.addSetNext("fims/validation/lists/list", "addList");
        d.addCallMethod("fims/validation/lists/list/field", "addField", 0);

         // Create column objects
        d.addObjectCreate("fims/validation/worksheet/column", Column_trash.class);
        d.addSetProperties("fims/validation/worksheet/column");
        d.addSetNext("fims/validation/worksheet/column", "addColumn");

        d.parse(new File(configFilename));
    }

    /**
     * Process mapping component rules
     *
     * @param d
     */
    private void addMappingRules(Digester d, Mapping mapping) throws IOException, SAXException {
        d.push(mapping);

        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Add attributes associated with this entity
        d.addObjectCreate("fims/mapping/entity/attribute", Attribute.class);
        d.addSetProperties("fims/mapping/entity/attribute");
        d.addSetNext("fims/mapping/entity/attribute", "addAttribute");

        // Create relation objects
        d.addObjectCreate("fims/mapping/relation", Relation.class);
        d.addSetNext("fims/mapping/relation", "addRelation");
        d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);
        d.addCallMethod("fims/mapping/relation/predicate", "addPredicate", 0);
        d.addCallMethod("fims/mapping/relation/object", "addObject", 0);

        d.parse(new File(configFilename));
    }

    /**
     * Run the program from the command-line
     *
     * @param args
     */
    public static void main(String args[]) {
        String project_code = "DEMOH";
        String configuration = "sampledata/indoPacificConfiguration_v2.xml";
        String input_file = "sampledata/indoPacificTemplate_v2.xlsx";
        //String configuration = "sampledata/configuration.xml";
        //String input_file = "sampledata/biocode_template.xls";
        String output_directory = System.getProperty("user.dir") + File.separator + "tripleOutput" + File.separator;

        process p = new process(
                configuration,
                input_file,
                output_directory,
                project_code
        );

        p.runAll();
    }
}
