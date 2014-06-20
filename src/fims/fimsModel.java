package fims;

import com.hp.hpl.jena.rdf.model.*;
import digester.QueryWriter;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;

/**
 * Model representing FIMS
 */
public class fimsModel {
    Model model;
    //Property rowLabelProperty;
    //String rowLabelProperty = "rdfs:Label";
    //String rowClass = "http://www.w3.org/2000/01/rdf-schema#Resource";

    String type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    String depends_on = "http://biscicol.org/terms/index.html#depends_on";
    int depth = 1;
    int countRows = 0;

    QueryWriter queryWriter;
    Row row;    // class level variable we use to assign values to using various methods here

    StringBuilder stringBuilder = new StringBuilder();

    public fimsModel(Model model, QueryWriter queryWriter) {
        this.model = model;
        this.queryWriter = queryWriter;

    }

    /**
     * Get the label for this row
     *
     * @param subject
     * @return
     */
    public String getRowLabel(Resource subject) {
        return subject.getProperty(model.getProperty("rdfs:Label")).getObject().toString();
    }

    /**
     * Iterate through statements with "resource" as object
     *
     * @param resource
     * @return
     */
    public void readRows(String resource) {
        RDFNode n = model.createResource(model.expandPrefix(resource));
        SimpleSelector selector = new SimpleSelector(null, null, n);
        StmtIterator i = model.listStatements(selector);
        while (i.hasNext()) {
            Statement s = i.next();
            // Create a row object here, so when we related objects, properties below we can write it out
            row = queryWriter.createRow(countRows);
            listProperties(s.getSubject());
            // Loop each subject, TODO: expand to all BiSciCol relations
            loopObjects(getRelations(s.getSubject()));
            countRows++;
            // Set depth back to 1
            depth = 1;
        }
        i.close();


    }

    /**
     * Loop each object
     *
     * @param stmtIterator
     */
    public void loopObjects(StmtIterator stmtIterator) {
        depth++;
        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();
            listProperties(statement.getSubject());
            loopObjects(getRelations(statement.getSubject()));
        }
    }

    /**
     * Output tabstops depending on depth
     *
     * @return
     */
    private String getTabStops() {
        String tabstops = "";
        for (int i = 0; i < depth; i++)
            tabstops += "\t";
        return tabstops;
    }

    /**
     * get a property named by a particular string (in URI format)
     *
     * @param propertyAsString
     * @return
     */
    public Property getProperty(String propertyAsString) {
        return model.getProperty(propertyAsString);
    }

    /**
     * List the properties we want to display
     *
     * @param resource
     */
    public void listProperties(Resource resource) {
        StmtIterator stmtIterator = resource.listProperties();
        int count = 0;
        while (stmtIterator.hasNext()) {
            Statement s = stmtIterator.next();
           // Print the BCID as a property
            if (count == 0) {
                     queryWriter.createCell(row, "EZID", s.getSubject().toString());
            }
            // Print just the predicates we care about
            if (!s.getPredicate().equals(getProperty(type)) && !s.getPredicate().equals(getProperty(depends_on))) {
                // Don't want local name to be null
                if (s.getPredicate().getLocalName() != null &&
                      !s.getPredicate().getLocalName().equals("null")  ) {
                    queryWriter.createCell(row, s.getPredicate().toString(), s.getObject().toString());
                }
            }
            /*
             if (s.getPredicate().equals(getProperty("urn:basisOfIdentification"))) {
                queryWriter.createCell(row, s.getPredicate().toString(), s.getObject().toString());
            }
            */
        }
        stmtIterator.close();
    }

    /**
     * Get all relations for a particular subject resource
     *
     * @param subject
     * @return
     */
    public StmtIterator getRelations(Resource subject) {
        RDFNode node = model.createResource(model.expandPrefix(subject.asNode().toString()));
        SimpleSelector selector = new SimpleSelector(null, null, node);
        return model.listStatements(selector);
    }

    /**
     * Return output as JSON
     * TODO: build a renderor so different formats are easy to output
     *
     * @return
     */
    public String writeJSON(File file) throws Exception {
        //return stringBuilder.toString();

        return queryWriter.writeJSON(file);
    }

    /**
     * Return output as an Excel file
     */
    public String writeExcel(File file) throws Exception {
        return queryWriter.writeExcel(file);
    }
      /**
     * Return output as an HTML table
     */
    public String writeHTML(File file) throws Exception {
        return queryWriter.writeHTML(file);
    }


    public String writeKML(File file) {
        return queryWriter.writeKML(file);
    }

    public  void close() {
        model.close();
    }
}
