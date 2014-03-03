package fims;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.log4j.Level;

import java.io.UnsupportedEncodingException;
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
        String queryString = "CONSTRUCT {?s ?p ?o} \n" +
                // String queryString = "DESCRIBE ?s ?p ?o \n" +
                getFrom() +
                "WHERE {\n" +
                //" { SELECT ?p WHERE {?s ?p ?o FILTER (?p = <urn:geneticTissueType>)}} \n" +
                "   ?s a <http://www.w3.org/2000/01/rdf-schema#Resource> . \n" +
                "   ?s ?p ?o .\n" +
                //"   ?s <urn:geneticTissueType> ?o . \n" +
                getObjectFilter() +
                //"   FILTER (?p = <urn:geneticTissueType>)\n" +
                "}";
       // System.out.println(queryString);
       // System.out.println(sparqlService);

        QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, queryString);

        /* ResultSet rs = qexec.execSelect();

        while (rs.hasNext()) {
            QuerySolution s = rs.next();

            RDFNode p = s.get("?p");
            RDFNode o = s.get("?o");
            RDFNode su = s.get("?s");
            String object = "";
            String triple = "";
            if (o.isLiteral() == true) {
                object = "\"" + o.toString() + "\"";
            } else
                object = "<" + o.toString() + ">";

            triple = "<" + su + ">" + " " + "<" + p + ">" + " " + object + " " + ".";

        }
        */


        Model model = qexec.execConstruct();
        qexec.close();

        /*
        com.hp.hpl.jena.rdf.model.Resource r = model.getResource("ark:/21547/Hz2F9198780");
        StmtIterator s = r.listProperties();
        while (s.hasNext()) {
            Statement st = s.nextStatement();
            System.out.println("HERE" + st.getSubject().toString() + " " + st.getPredicate().toString() + " " + st.getObject().toString());
        }
        */
        return model;
    }

    public String getPropertyFilter() {
        return propertyFilter;
    }

    public void setPropertyFilter(ArrayList properties) {
        this.propertyFilter = propertyFilter;
    }

    /**
     * For object filters, we support multiple field/value specifications using an AND specification
     * Where a match is made to a particular property the match is "equals"
     * for Filters on all fields the match is "contains"
     *
     * @param pFilter
     */
    public void setObjectFilter(String pFilter) {
        java.net.URLDecoder decoder = new java.net.URLDecoder();
        String filterExpression = "";
        String[] kvs = pFilter.split(",");

        for (String kv : kvs) {
            String[] kvSplit = kv.split(":");
            // There is only 1 value here... so use filter ALL.. this looks for "contains" value not equals
            if (kvSplit.length == 1) {
                filterExpression = "   ?s ?propertyFilter ?objectFilter . \n";
                filterExpression += "   FILTER regex(?objectFilter,\"" + kvSplit[0].toString() + "\") \n";
                break;
            } else if (kvSplit.length < 1) {
                break;
            } else {
                try {
                    String predicate = decoder.decode(kvSplit[0], "UTF8").toString();
                    String object = decoder.decode(kvSplit[1], "UTF8").toString();

                    //TODO: lookup proper mapped URI name here in configuration file, for now this is just assuming
                    filterExpression += "   ?s <urn:" + predicate + "> ?" + predicate + " . \n";
                    filterExpression += "   FILTER regex(?" + predicate + ", \"^" + object + "$\", \"i\") .\n";

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }


        objectFilter = filterExpression;
        //objectFilter = "   FILTER regex(?o, \"" + pFilter + "\", \"i\") \n";

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
     *
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
        //graphArray[0] = "urn:uuid:c4cc9f83-5338-48d7-8f92-9bd23802ae7f";
        //graphArray[1] = "urn:uuid:0fe114da-07c9-4f50-8a0d-743b7d456dfc";
        graphArray[0] = "urn:uuid:ded8e057-75b9-4e42-a74d-c711762d757b";

        //fimsQueryBuilder q = new fimsQueryBuilder(graphArray, "http://biscicol.org:3030/ds/query");
        fimsQueryBuilder q = new fimsQueryBuilder(graphArray, " http://data.biscicol.org/ds/query");
        //q.setObjectFilter("Mfl1090");
        print(q.getModel());

    }


}