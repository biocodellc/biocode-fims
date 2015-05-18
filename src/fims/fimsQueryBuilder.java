package fims;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import digester.Attribute;
import digester.Fims;
import digester.Mapping;
import digester.Validation;
import org.apache.commons.digester3.Digester;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import run.configurationFileFetcher;
import run.process;
import run.processController;
import settings.FIMSException;
import settings.PathManager;
import settings.bcidConnector;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Class for testing queries using ARQ/Sparql
 */
public class fimsQueryBuilder {
    String graphArray[];
    Fims fims;
    Mapping mapping;
    Validation validation;
    run.process process;
    String sparqlServer;
    String output_directory;// = System.getProperty("user.dir") + File.separator + "tripleOutput";
    ArrayList<Attribute> attributesArrayList;


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
     * Build the model by using the CONSTRUCT statement.  This is the section where we put the SPARQL query
     * together.
     *
     * @return
     */
    public Model getModel() {
        String queryString = "CONSTRUCT {?s ?p ?o} \n" +
                // String queryString = "DESCRIBE ?s ?p ?o \n" +
                buildFromStatement() +
                "WHERE {\n" +
                //" { SELECT ?p WHERE {?s ?p ?o FILTER (?p = <urn:geneticTissueType>)}} \n" +
                "   ?s a <http://www.w3.org/2000/01/rdf-schema#Resource> . \n" +
                "   ?s ?p ?o;\n" +
                //"      <urn:family> ?o2 .\n" +
                //"      <urn:genus> ?o3 . \n" +

                // buildPropertiesListUsing UNIONS
                // This next step is SLOW for many graphs (approach being many unions)
                //buildPropertyLists() +

                //"   ?s <urn:geneticTissueType> ?o . \n" +

                // This next step is SLOW for many graphs
                buildFilterStatements() +
                //"   FILTER (?p = <urn:geneticTissueType>)\n" +
                "}";

        System.out.println(queryString);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlServer, queryString);

        Model model = qexec.execConstruct();
        /*
        // Declare equivalencies
        Model infModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        Property predicate = infModel.createProperty("http://www.w3.org/2002/07/owl#equivalentProperty");
        Resource subject = infModel.createResource("http://rs.tdwg.org/dwc/terms/materialSampleID");
        Resource object = ResourceFactory.createResource("http://rs.tdwg.org/dwc/terms/MaterialSampleID");
        subject.addProperty( predicate,object);

        infModel.add(model);
        //model.add(infModel);

        model = infModel;
        */
        qexec.close();

        return model;
    }

    /**
     * Build a list of properties that match the declared syntax in the current XML configuration file
     * @return
     */
    private String buildPropertyLists() {
        ArrayList<Attribute> attributes = mapping.getAllAttributes(mapping.getDefaultSheetName());
        Iterator attributesIt = attributes.iterator();
        StringBuilder sb = new StringBuilder();
        while (attributesIt.hasNext()) {
            Attribute attribute = (Attribute)attributesIt.next();
            sb.append("{?s <" + attribute.getUri() + "> ?o }");
            if (attributesIt.hasNext()) {
                sb.append (" UNION ");
            }
        }
        return sb.toString();
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
        while (filterArrayListIt.hasNext()) {
            fimsFilterCondition f = (fimsFilterCondition) filterArrayListIt.next();

            // The fimsFilterCondition uriProperty corresponds to the uri value in the configuration file
            if (f.uriProperty == null) {
                if (f.value != null) {
                    sb.append("\t?s ?propertyFilter ?objectFilter . \n");
                    sb.append("\tFILTER regex(?objectFilter,\"" + f.value + "\") . \n");
                }
            } else {
                if (f.value != null) {
                    sb.append("\t?s <" + f.uriProperty.toString() + "> \"" + f.value + "\" .\n");
                }
            }

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

        // Construct a  fimsModel
        fimsModel = fims.getFIMSModel(getModel());
        if (format.equals("model"))
            return fimsModel.model.toString();
        if (format == null)
            format = "json";

        if (format.equals("excel"))
            outputPath = fimsModel.writeExcel(PathManager.createUniqueFile("output.xls", output_directory));
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

        /*
        SELECT ?s ?p ?o
         FROM <urn:uuid:e22c08ae-1da5-44e9-8d43-674b5dbd3897>
         FROM <urn:uuid:be684543-fac4-4cb3-b831-0347809d1a32>
        WHERE {
           ?s a <http://www.w3.org/2000/01/rdf-schema#Resource> .
           ?s ?p ?o .
          {?s <urn:collectedBy> ?o }
          UNION
          {?s <urn:genus> ?o }
        UNION
          {?s <urn:family> ?o }
         */
        // Construct an array of graphs
        /*String[] graphArray = new String[2];
        //graphArray[0] = "urn:uuid:c4cc9f83-5338-48d7-8f92-9bd23802ae7f";
        //graphArray[1] = "urn:uuid:0fe114da-07c9-4f50-8a0d-743b7d456dfc";
        //graphArray[0] = "urn:uuid:ded8e057-75b9-4e42-a74d-c711762d757b";
        graphArray[0] = "urn:uuid:70c8f3b9-e3e7-4c02-92d3-b1577b422bc5";
        */

        // Build the query Object
        // fimsQueryBuilder q = new fimsQueryBuilder(p, graphArray, output_directory);

        fimsQueryBuilder q = new fimsQueryBuilder(p, getAllGraphs(5), output_directory);

        // Add filter conditions to the object
        //q.addFilter(new fimsFilterCondition(new URI("urn:phylum"), "Echinodermata", fimsFilterCondition.AND));
        //q.addFilter(new fimsFilterCondition(new URI("urn:materialSampleID"), "IN0123.01", fimsFilterCondition.AND));


        //q.addFilter(new fimsFilterCondition(null, "IN0123.01", fimsFilterCondition.AND));

        // TODO: clean up the filter conditions here to handle all cases
        //q.addFilter(new fimsFilterCondition(null, "10", fimsFilterCondition.AND));


        // Run the query and specify the output format
        String outputFileLocation = q.run("excel");

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