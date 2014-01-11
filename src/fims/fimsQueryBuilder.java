package fims;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.log4j.Level;

import java.util.ArrayList;


/**
 * Class for testing queries using ARQ/Sparql
 */
public class fimsQueryBuilder {
    String sparqlService;
    String graphArray[];
    String objectFilter = null;
    String propertyFilter = null;

    public fimsQueryBuilder(String[] graphArray, String sparqlService) {
        this.graphArray = graphArray;
        this.sparqlService = sparqlService;
    }

    public Model getModel() {
        String queryString = "DESCRIBE ?s ?p ?o \n" +
                getFrom() +
                "WHERE {" +
                //" { SELECT ?p WHERE {?s ?p ?o FILTER (?p = <urn:geneticTissueType>)}} \n" +
                "   ?s a <http://www.w3.org/2000/01/rdf-schema#Resource> . \n" +
                "   ?s ?p ?o .\n"  +
                //"   ?s <urn:geneticTissueType> ?o . \n" +
                getObjectFilter() +
               //"   FILTER (?p = <urn:geneticTissueType>)\n" +
                "}";
        //System.out.println(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, queryString);
        Model model = qexec.execDescribe();
        qexec.close();

        return model;
    }

    public String getPropertyFilter() {
        return propertyFilter;
    }

    public void setPropertyFilter(ArrayList properties) {
        this.propertyFilter = propertyFilter;
    }

    public void setObjectFilter(String pFilter) {
        objectFilter = "   FILTER regex(?o, \"" + pFilter + "\", \"i\") \n";
    }

    private String getObjectFilter() {
        if (objectFilter == null)
            return "";
        else
            return objectFilter;
    }


    /**
     * build the FROM statements
     *
     * @return
     */
    private String getFrom() {
        StringBuilder sb = new StringBuilder();

        for (String graph : graphArray) {
            sb.append(" FROM <" + graph + "> \n");
        }
        return sb.toString();
    }

    /**
     * Print the Model
     * @param model
     */
    private static void print(Model model) {
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
     * Used only for testing
     *
     * @param args
     */
    public static void main(String[] args) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        String[] graphArray = new String[2];
        graphArray[0] = "urn:uuid:c4cc9f83-5338-48d7-8f92-9bd23802ae7f";
        graphArray[1] = "urn:uuid:0fe114da-07c9-4f50-8a0d-743b7d456dfc";

        fimsQueryBuilder q = new fimsQueryBuilder(graphArray, "http://biscicol.org:3030/ds/query");
        q.setObjectFilter("Mfl1090");
        print(q.getModel());

    }

}