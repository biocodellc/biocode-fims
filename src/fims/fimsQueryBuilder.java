package fims;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import digester.Attribute;
import digester.Fims;
import digester.Mapping;
import digester.Validation;
import org.apache.commons.digester3.Digester;
import org.apache.log4j.Level;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import run.configurationFileFetcher;
import run.process;
import run.templateProcessor;
import settings.PathManager;
import settings.bcidConnector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class for building queries against FIMS database
 *
 * The FIMS database is a fuseki triplestore composed of many small graphs, grouped by project.
 *
 * The order of operations for querying the FIMS database looks like this:
 *
 * 1. Do a simple (?s ?p ?o) query of all graphs of a specified set and return a Model
 * 2. Query the returned model and run filter statements on it.
 * 3. Loop through specified properties (or not) and call the fimsModel (using Excel data structure)
 *
 * This approach is actually MORE efficient than using just one SPARQL query on multiple graphs.
 */
public class fimsQueryBuilder {
    String graphArray[];
    Fims fims;
    Mapping mapping;
    Validation validation;
    run.process process;
    String sparqlServer;
    String output_directory;// = System.getProperty("user.dir") + File.separator + "tripleOutput";

    // Hold all the available attributes we want to look at
    ArrayList<Attribute> attributesArrayList;

     // ArrayList of filter conditions
    private ArrayList<fimsFilterCondition> filterArrayList = new ArrayList<fimsFilterCondition>();

    public fimsQueryBuilder(run.process process, String[] graphArray, String output_directory) {
        this.output_directory = output_directory;

        this.process = process;
        this.graphArray = graphArray;

        // Build Mapping object
        mapping = new Mapping();
        process.addMappingRules(new Digester(), mapping);

        validation = new Validation();
        process.addValidationRules(new Digester(), validation);

        // Build FIMS object
        fims = new Fims(mapping, validation);
        process.addFimsRules(new Digester(), fims);

        // Build the "query" location for SPARQL queries
        sparqlServer = fims.getMetadata().getQueryTarget().toString() + "/query";

        // For testing
        attributesArrayList = mapping.getAllAttributes(mapping.getDefaultSheetName());

    }

    /**
     * Add a single Filter condition
     *
     * @param f
     */
    public void addFilter(fimsFilterCondition f) {
        if (f != null)
            filterArrayList.add(f);
    }

    /**
     * Add multipler Filter conditions
     *
     * @param f
     */
    public void addFilter(ArrayList<fimsFilterCondition> f) {
        if (f != null && f.size() > 0)
            filterArrayList.addAll(f);
    }


    /**
     * Query a Model and  pass in Filter conditions and then return another model with those conditions applied
     * @param model
     * @return
     */
    public Model getFilteredModel(Model model) {
        String queryString = "CONSTRUCT {?s ?p ?o} \n" +
                        //buildFromStatement() +
                        "WHERE {\n" +
                        "   ?s a <http://www.w3.org/2000/01/rdf-schema#Resource> . \n" +
                        "   ?s ?p ?o . \n" +
                        buildFilterStatements() +
                        "}";

                System.out.println(queryString);
                QueryExecution qexec = QueryExecutionFactory.create(queryString, model);
                Model outputModel = qexec.execConstruct();

                qexec.close();
                return outputModel;
    }

    /**
     * Build the model by using the CONSTRUCT statement
     *
     * @return
     */
    public Model getModel() {
        String queryString = "CONSTRUCT {?s ?p ?o} \n" +
                buildFromStatement() +
                "WHERE {\n" +
                "   ?s a <http://www.w3.org/2000/01/rdf-schema#Resource> . \n" +
                "   ?s ?p ?o . \n" +
                "}";

        System.out.println(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlServer, queryString);
        Model model = qexec.execConstruct();
        qexec.close();
        return model;
    }

    /**
     * Take the filter statements that the user has specified and put them together to form the portion of the SPARQL
     * statement that asks particular questions of the data.
     *
     * @return
     */
    private String buildFilterStatements() {
        StringBuilder sb = new StringBuilder();
        Iterator filterArrayListIt = filterArrayList.iterator();
        int count = 1;
        while (filterArrayListIt.hasNext()) {
            fimsFilterCondition f = (fimsFilterCondition) filterArrayListIt.next();

            // The fimsFilterCondition uriProperty corresponds to the uri value in the configuration file
            if (f.uriProperty == null) {
                if (f.value != null) {
                    sb.append("\t?s ?propertyFilter" + count + " ?objectFilter" + count + " . \n");
                    sb.append("\tFILTER regex(?objectFilter" + count +",\"" + f.value + "\") . \n");
                }
            } else {
                if (f.value != null) {
                    sb.append("\t?s <" + f.uriProperty.toString() + "> \"" + f.value + "\" .\n");
                }
            }
            count++;
            // TODO: the current filter statement only builds AND conditions, need to account for OR and NOT
        }
        return sb.toString();
    }

    /**
     * build the FROM statements
     *
     * @return
     */
    private String buildFromStatement() {
        StringBuilder sb = new StringBuilder();

        for (String graph : graphArray) {
            if (graph != null)
                sb.append(" FROM <" + graph + "> \n");
        }
        return sb.toString();
    }

    /**
     * Print the Model
     *
     * @param model
     */
    public static void print(Model model) {
        // list the statements in the Model
        StmtIterator iter = model.listStatements();

        // print out the predicate, subject and object of each statement
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();  // get next statement
            Resource subject = stmt.getSubject();     // get the subject
            Property predicate = stmt.getPredicate();   // get the predicate
            RDFNode object = stmt.getObject();      // get the object

            System.out.print(subject.toString());
            System.out.print(" " + predicate.toString() + " ");
            if (object instanceof Resource) {
                System.out.print(object.toString());
            } else {
                // object is a literal
                System.out.print(" \"" + object.toString() + "\"");
            }

            System.out.println(" .");
        }
    }

    /**
     * Run the query, which builds the Model from all the information we have collected by specifying the SPARQL query
     * service endpoint and filter conditions.  It builds a "fimsModel" from which we can re-direct to the specified
     * formats.
     *
     * @param format
     *
     * @return
     */
    public String run(String format) {

        fimsModel fimsModel = null;
        String outputPath;


        /* Set the flag of whether to look at only specified properties (from configuration file)
        when returning data or filtering */
        boolean getOnlySpecifiedProperties = true;

        // Construct a fimsModel, wrapping a filtered model around a model only when necessary
        if (filterArrayList.size() > 0) {
            fimsModel = fims.getFIMSModel(getFilteredModel(getModel()),getOnlySpecifiedProperties);
        } else {
            fimsModel = fims.getFIMSModel(getModel(),getOnlySpecifiedProperties);
        }

        if (format.equals("model"))
            return fimsModel.model.toString();
        if (format == null)
            format = "json";

        if (format.equals("excel")) {
            outputPath = fimsModel.writeExcel(PathManager.createUniqueFile("output.xlsx", output_directory));

            // Here we attach the other components of the excel sheet found with
            // TODO: finish this part up
            XSSFWorkbook justData = null;
            try {
                 justData = new XSSFWorkbook(new FileInputStream(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

            templateProcessor t = new templateProcessor(1, output_directory, false, justData);
            outputPath = t.createExcelFileFromExistingSources("Samples", output_directory).getAbsolutePath();
        }
        else if (format.equals("html"))
            outputPath = fimsModel.writeHTML(PathManager.createUniqueFile("output.html", output_directory));
        else if (format.equals("kml"))
            outputPath = fimsModel.writeKML(PathManager.createUniqueFile("output.kml", output_directory));
        else if (format.equals("cspace"))
            outputPath = fimsModel.writeCSPACE(PathManager.createUniqueFile("output.cspace.xml", output_directory));
        else
            outputPath = fimsModel.writeJSON(PathManager.createUniqueFile("output.json", output_directory));

        fimsModel.close();

        return outputPath;
    }

    /**
     * Used only for testing this class directly
     *
     * @param args
     */
    public static void main(String[] args) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
        String output_directory = System.getProperty("user.dir") + File.separator + "tripleOutput";

        ArrayList<fimsFilterCondition> filters = new ArrayList<fimsFilterCondition>();
        //String graphs;

        // configuration file
        File file = new configurationFileFetcher(1, output_directory, false).getOutputFile();

        run.process p = new process(
                output_directory,
                file
        );


        // Build the query Object
        // fimsQueryBuilder q = new fimsQueryBuilder(p, graphArray, output_directory);
        fimsQueryBuilder q = new fimsQueryBuilder(p, getAllGraphs(5), output_directory);

        // Add filter conditions to the object
        //q.addFilter(new fimsFilterCondition(new URI("urn:phylum"), "Echinodermata", fimsFilterCondition.AND));
        //q.addFilter(new fimsFilterCondition(new URI("urn:materialSampleID"), "IN0123.01", fimsFilterCondition.AND));


        q.addFilter(new fimsFilterCondition(null, "Magnoliophyta", fimsFilterCondition.AND));
        //q.addFilter(new fimsFilterCondition(null, "Kimana", fimsFilterCondition.AND));


        // TODO: clean up the filter conditions here to handle all cases
        //q.addFilter(new fimsFilterCondition(null, "10", fimsFilterCondition.AND));

        //fimsModel fimsModel = new fimsModel(q.getModel());


        // Run the query and specify the output format
        String outputFileLocation = q.run("json");

        // Print out the file location
        System.out.println("File location: " + outputFileLocation);
    }

    /**
     * Get a list of all graphs
     * @param project_id
     * @return
     */
    private static String[] getAllGraphs(int project_id) {
        bcidConnector connector = new bcidConnector();
        ArrayList<String> graphs = new ArrayList<String>();
        JSONObject response = ((JSONObject) JSONValue.parse(connector.getGraphs(project_id)));
        JSONArray jArray = ((JSONArray) response.get("data"));
        Iterator it = jArray.iterator();

        while (it.hasNext()) {
            JSONObject obj = (JSONObject) it.next();
            graphs.add((String) obj.get("graph"));
        }

        return graphs.toArray(new String[graphs.size()]);
    }


}