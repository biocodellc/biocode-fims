package fims;

import com.hp.hpl.jena.rdf.model.*;
import digester.QueryWriter;
import org.apache.poi.ss.usermodel.Row;

/**
 * Model representing FIMS
 */
public class fimsModel {
    Model model;
    //Property rowLabelProperty;
    String rowLabelProperty = "rdfs:Label";
    String rowClass = "http://www.w3.org/2000/01/rdf-schema#Resource";
    String type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    String depends_on = "http://biscicol.org/terms/index.html#depends_on";
    int depth = 1;
    int countRows = 0;
    // int countProperties = 0; // the number of properties for each object

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
        while (stmtIterator.hasNext()) {
            Statement s = stmtIterator.next();
            // Print just the predicates we care about
            if (!s.getPredicate().equals(getProperty(type)) && !s.getPredicate().equals(getProperty(depends_on))) {
                queryWriter.createCell(row, s.getPredicate().toString(), s.getObject().toString());
            }
        }
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
    public String toJSON() {
        //return stringBuilder.toString();
        return null;
    }

    /**
     * Return output as an Excel file
     */
    public String toExcel() throws Exception {
        return queryWriter.write();
    }

}
