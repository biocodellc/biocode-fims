package fims;

import com.hp.hpl.jena.rdf.model.*;
import digester.Attribute;
import digester.Mapping;
import digester.QueryWriter;
import digester.Validation;
import org.apache.poi.ss.usermodel.Row;
import run.processController;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Model representing FIMS object.
 * The model is refined somewhat by ONLY the currently definated attributes in the XML configuration file that
 * is defined.  That is, anything previously defined and then NOT defined will not be displayed here.
 * Understanding this is CRUCIALLY important and stated elsewhere in the documentation that one should never toss,
 * change, or otherwise muck with URIs
 */
public class fimsModel {
    // A list of the Attribute URIs contained in the configuration file
    ArrayList<String> configurationFileAttributeURIs;

    Model model;


    String type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    String depends_on = "http://biscicol.org/terms/index.html#depends_on";
    int depth = 1;
    int countRows = 0;

    QueryWriter queryWriter;
    Row row;    // class level variable we use to assign values to using various methods here

    StringBuilder stringBuilder = new StringBuilder();

    public fimsModel(Model model, QueryWriter queryWriter, Mapping mapping) {
        this.model = model;
        this.queryWriter = queryWriter;

        Iterator attributesIt = mapping.getAllAttributes(mapping.getDefaultSheetName()).iterator();
        configurationFileAttributeURIs = new ArrayList<String>();
        while (attributesIt.hasNext()) {
            Attribute a = (Attribute) attributesIt.next();
            configurationFileAttributeURIs.add(a.getUri());
        }

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

            //System.out.println(s.getPredicate() + s.getObject().toString() + " " + s.getSubject());

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
     * List the properties for display, based on the properties as defined in the configuration XML file
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
                     queryWriter.createCell(row, "BCID", s.getSubject().toString());
            }

           // System.out.println(s.getSubject() + " "  + s.getPredicate().toString() + " " + s.getObject());

            // Print just the predicates we care about
            if (!s.getPredicate().equals(getProperty(type)) && !s.getPredicate().equals(getProperty(depends_on))) {
                // Don't want local name to be null
                if (s.getPredicate().getLocalName() != null &&
                      !s.getPredicate().getLocalName().equals("null")
                        ) {

                    // Filter predicates based on Attributes contained in the Configuration File
                    if (    configurationFileAttributeURIs.contains(s.getPredicate().toString())) {
                        queryWriter.createCell(row, s.getPredicate().toString(), s.getObject().toString());
                    }
                }
            }
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
    public String writeJSON(File file) {
        //return stringBuilder.toString();
        return queryWriter.writeJSON(file);
    }

    /**
     * Return output as an Excel file
     */
    public String writeExcel(File file) {
        return queryWriter.writeExcel(file);
    }
      /**
     * Return output as an HTML table
     */
    public String writeHTML(File file) {
        return queryWriter.writeHTML(file);
    }


    public String writeKML(File file) {
        return queryWriter.writeKML(file);
    }

     public String writeCSPACE(File file) {
        return queryWriter.writeCSPACE(file);
    }
    public  void close() {
        model.close();
    }
}
