package digester;

import org.jdom.Document;
import org.jsoup.Jsoup;
import renderers.RendererInterface;
import settings.fimsPrinter;
import triplify.triplifier;
import settings.Connection;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * Mapping builds the D2RQ structure for converting between relational format to RDF.
 */
public class Mapping implements RendererInterface {
    public Connection connection;

    private final LinkedList<Entity> entities = new LinkedList<Entity>();
    private final LinkedList<Relation> relations = new LinkedList<Relation>();
    private triplifier triplifier;
    private String project_code;
    private List<String> colNames;

    public Mapping() throws Exception {

    }

    public List<String> getColNames() {
        return colNames;
    }

    public triplifier getTriplifier() {
        return triplifier;
    }

    /**
     * The default sheetname is the one referenced by the first entity
     * TODO: set defaultSheetName in a more formal manner, currently we're basing this on a "single" spreadsheet model
     *
     * @return
     */
    public String getDefaultSheetName() {
        Iterator it = entities.iterator();
        while (it.hasNext()) {
            Entity entity = (Entity) it.next();
            return entity.getWorksheet();
        }
        return null;
    }

    /**
     * Add an Entity to this Mapping by appending to the LinkedList of entities
     *
     * @param e
     */
    public void addEntity(Entity e) {
        entities.addLast(e);
    }

    /**
     * Add a Relation to this Mapping by appending to the LinkedListr of relations
     *
     * @param r
     */
    public void addRelation(Relation r) {
        relations.addLast(r);
    }

    /**
     * Find Entity defined by given worksheet and worksheetUniqueKey
     *
     * @param Id
     * @return
     */
    Entity findEntity(String Id) {
        for (Entity entity : entities)
            if (Id.equals(entity.getEntityId()))
                return entity;
        return null;
    }

    /**
     * Sets the URI as a prefix to a column, or not, according to D2RQ conventions
     *
     * @param entity
     * @return
     */
    public String getPersistentIdentifier(Entity entity) throws Exception {
        //System.out.println(entity.getConceptAlias() + " " + entity.toString() + " " + entity.getColumn() + " " + entity.getConceptURI() + " " + entity.getWorksheetUniqueKey());
        String bcid = lookupBCID(project_code, entity.getConceptAlias());
        return "\td2rq:uriPattern \"" + bcid + "@@" + entity.getColumn() + "@@\";";
    }

    /**
     * Find the appropriate BCID for this project, calling the BCID projectService to find it.
     *
     * @param project_code defines the BCID project_code to lookup
     * @param conceptAlias defines the alias to narrow this,  a one-word reference denoting a BCID
     * @return returns the BCID for this project and conceptURI combination
     */
    private String lookupBCID(String project_code, String conceptAlias) throws Exception {
        String html = null;
        String projectService = "http://biscicol.org/id/projectService/";
        String connectionURL = "";
        try {
            connectionURL = projectService + project_code + "/" + conceptAlias;
            // set a 10 second timeout on this connection
            html = Jsoup.connect(connectionURL).timeout(10000).get().body().html();
        } catch (IOException e) {
            throw new Exception("Unable to connect to BCID service to get your project's unique ID using connectionURL =  " +
                    connectionURL +
                    ". This is a required part of the triplification run process.  If this problem persists, contact the System Administrator", e);
            //return "urn:x-biocode-fims";
        }
        return html;
    }

    /**
     * Generate D2RQ Mapping Language representation of this Mapping's connection, entities and relations.
     *
     * @param pw PrintWriter used to write output to.
     */
    public void printD2RQ(PrintWriter pw, Object parent) throws Exception {
        printPrefixes(pw);
        connection.printD2RQ(pw);
        for (Entity entity : entities)
            entity.printD2RQ(pw, this);
        for (Relation relation : relations)
            relation.printD2RQ(pw, this);
        //Join results to Dataset.... may not be necessary here
        //dataseturi.printD2RQ(pw, this);
    }

    /**
     * Generate all possible RDF prefixes.
     *
     * @param pw PrintWriter used to write output to.
     */
    private void printPrefixes(PrintWriter pw) {
        pw.println("@prefix map: <" + "" + "> .");
        pw.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
        pw.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
        pw.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
        pw.println("@prefix d2rq: <http://www.wiwiss.fu-berlin.de/suhl/bizer/D2RQ/0.1#> .");
        pw.println("@prefix jdbc: <http://d2rq.org/terms/jdbc/> .");
        pw.println("@prefix ro: <http://www.obofoundry.org/ro/ro.owl#> .");
        pw.println("@prefix bsc: <http://biscicol.org/terms/index.html#> .");
        pw.println();
    }

    /**
     * Run the triplifier using this class
     *
     * @throws Exception
     */
    public boolean run(Validation v, triplifier t, String project_code, List<String> colNames) throws Exception {
        fimsPrinter.out.println("Converting to RDF Triples ...");
        this.project_code = project_code;
        this.colNames = colNames;
        triplifier = t;
        // Create a connection to a SQL Lite Instance
        try {
            this.connection = new Connection(v.getSqliteFile());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("unable to establish connection to SQLLite");
        }
        triplifier.getTriples(this);
        return true;
    }

    /**
     * Just tell us where the file is stored...
     */
    public void print() {
        fimsPrinter.out.println("\ttriple output file = " + triplifier.getTripleOutputFile());
        fimsPrinter.out.println("\tsparql update file = " + triplifier.getUpdateOutputFile());
    }

    /**
     * Loop through the entities and relations we have defined...
     */
    public void printObject() {
        fimsPrinter.out.println("Mapping has " + entities.size() + " entries");

        for (Iterator<Entity> i = entities.iterator(); i.hasNext(); ) {
            Entity e = i.next();
            e.print();
        }

        for (Iterator<Relation> i = relations.iterator(); i.hasNext(); ) {
            Relation r = i.next();
            r.print();
        }
    }

    /**
     * Return a list of ALL attributes defined for entities for a particular worksheet
     *
     * @return
     */
    public ArrayList<Attribute> getAllAttributes(String worksheet) {
        ArrayList<Attribute> a = new ArrayList<Attribute>();
        for (Iterator<Entity> i = entities.iterator(); i.hasNext(); ) {
            Entity e = i.next();
            if (e.getWorksheet().equals(worksheet))
                a.addAll(e.getAttributes());
        }
        return a;
    }
}