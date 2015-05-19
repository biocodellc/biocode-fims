package fims;

import com.hp.hpl.jena.rdf.model.*;
import digester.Attribute;
import digester.Mapping;
import digester.QueryWriter;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Model representing FIMS object.   The data structure we start with in fimsModel is a Jena/ARQ Model and
 * the structure we end up with is an Apache POI sheet, useful for building other types of return statements.
 * This is an extremely useful way of working with data and converting to other structures, but it DOES place a
 * theoretical limit on the number of distinct records in a particular project.  Enabling much larger projects
 * by using indexes and some kind of document storage engine is on our list of things to make happen.
 *
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

    boolean getOnlySpecifiedProperties;

    /**
     *
     * @param model
     * @param queryWriter
     * @param mapping
     * @param getOnlySpecifiedProperties  whether or not to fetch a constrained list of specified properties
     */
    public fimsModel(Model model, QueryWriter queryWriter, Mapping mapping, boolean getOnlySpecifiedProperties) {
        this.model = model;
        this.queryWriter = queryWriter;
        this.getOnlySpecifiedProperties = getOnlySpecifiedProperties;

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
     *
     * @return
     */
    public String getRowLabel(Resource subject) {
        return subject.getProperty(model.getProperty("rdfs:Label")).getObject().toString();
    }

    /**
     * Iterate through statements with "resource" as object.  This is ALL
     *
     * @param resource
     *
     * @return
     */
    public void readRows(String resource) {
        RDFNode n = model.createResource(model.expandPrefix(resource));
        SimpleSelector selector = new SimpleSelector(null, null, n);

        // Get a list of statements for the root Resource
        StmtIterator i = model.listStatements(selector);

        // Loop each resource
        while (i.hasNext()) {
            // Statement representing a particular resource, typically with multiple properties attached to it
            Statement s = i.next();

            // Create a row object here, so when we related objects, properties below we can write it out
            row = queryWriter.createRow(countRows);

            // List all properties available for this statement and if no values found, remove it.
            if (!createRowFromStatemenetProperties(s.getSubject())) {
                queryWriter.removeRow(countRows);
            } else {
                countRows++;
            }

            // Loop each subject resource, which follows each node to any objects expressing graph type relations
            // TODO: look at relations and directed graph relations
            loopObjects(getRelations(s.getSubject()));


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
            createRowFromStatemenetProperties(statement.getSubject());
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
     *
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
    public boolean createRowFromStatemenetProperties(Resource resource) {

        StmtIterator stmtIterator = resource.listProperties();
        String BCIDString = null;
        boolean rowWithValues = false;
        int count = 0;
        while (stmtIterator.hasNext()) {
            Statement s = stmtIterator.next();

            // Set the BCID String
            if (count == 0) {
                BCIDString = s.getSubject().toString();
            }
            // System.out.println(s.getSubject() + " "  + s.getPredicate().toString() + " " + s.getObject());

            // Print just the predicates we care about
            if (!s.getPredicate().equals(getProperty(type)) && !s.getPredicate().equals(getProperty(depends_on))) {
                // Don't want local name to be null
                if (s.getPredicate().getLocalName() != null &&
                        !s.getPredicate().getLocalName().equals("null")
                        ) {

                    String predicate = s.getPredicate().toString();

                    if (getOnlySpecifiedProperties &&
                            configurationFileAttributeURIs.contains(predicate)) {
                        queryWriter.createCell(row, s.getPredicate().toString(), s.getObject().toString());
                        rowWithValues = true;
                    }
                }
            }


        }

        // Write out the BCID String
        if (rowWithValues) {
            queryWriter.createCell(row, "BCID", BCIDString);
        }

        stmtIterator.close();
        return rowWithValues;
    }

    /**
     * Get all relations for a particular subject resource
     *
     * @param subject
     *
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

    public void close() {
        model.close();
    }
}
