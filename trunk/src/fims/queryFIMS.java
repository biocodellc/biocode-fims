package fims;

import com.hp.hpl.jena.query.*;
import org.apache.log4j.Level;

import java.util.ArrayList;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * Class for testing queries using ARQ/Sparql
 */
public class queryFIMS {
    String sparqlService = "http://biscicol.org:3030/ds/query";
    String graphArray[];

    public queryFIMS(String[] graphArray) {
        this.graphArray = graphArray;
    }

    public Model getModel() {
        String queryString = "DESCRIBE ?s ?p ?o \n" +
                buildFromStatements() +
                "WHERE {" +
                "   ?s a <http://www.w3.org/2000/01/rdf-schema#Resource> . \n" +
                "   ?s ?p ?o . \n" +
                "}";
        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, queryString);
        Model model = qexec.execDescribe();
        qexec.close();
        return model;
    }

    /**
     * Used only for testing
     * @param args
     */
    public static void main(String[] args) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        String[] graphArray = new String[2];
        graphArray[0] = "urn:uuid:c4cc9f83-5338-48d7-8f92-9bd23802ae7f";
        graphArray[1] = "urn:uuid:0fe114da-07c9-4f50-8a0d-743b7d456dfc";

        queryFIMS q = new queryFIMS(graphArray);
        System.out.println(q.buildFromStatements());


    }

    /**
     * build the FROM statements
     * @return
     */
    private String buildFromStatements() {
        StringBuilder sb = new StringBuilder();

        for (String graph : graphArray) {
            sb.append(" FROM <" + graph + "> \n");
        }
        return sb.toString();
    }
}