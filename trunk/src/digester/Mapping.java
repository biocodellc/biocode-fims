package digester;

import renderers.RendererInterface;
import settings.BCIDDatabase;
import triplify.triplifier;
import settings.Connection;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Mapping implements RendererInterface {
    public Connection connection;

    private final LinkedList<Entity> entities = new LinkedList<Entity>();
    private final LinkedList<Relation> relations = new LinkedList<Relation>();
    private triplifier triplifier;
    private String project_code;

    public Mapping(triplifier t, String project_code) throws Exception {
        this.project_code = project_code;
        triplifier = t;
        // Create a connection to a SQL Lite Instance
        try {
            this.connection = new Connection(t.createSqlLite());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("unable to establish connection to SQLLite");
        }
    }

    public triplifier getTriplifier() {
        return triplifier;
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
    public String getPersistentIdentifier(Entity entity) {
        String bcid = lookupBCID(project_code, entity.getConceptURI());
        //return "\td2rq:uriPattern \"" + entity.getBcid() + "_@@" + entity.getColumn() + "@@\";";
        return "\td2rq:uriPattern \"" + bcid + "_@@" + entity.getColumn() + "@@\";";
    }

    /**
     * Find the appropriate BCID for this project
     * TODO: Make this a REST service call, FOR NOW ...  hardcoding database connection
     * @param project_code defines the BCID project_code to lookup
     * @param conceptURI   defines the conceptURI to narrow this to
     * @return returns the BCID for this project and conceptURI combination
     */
    private String lookupBCID(String project_code, String conceptURI) {
        //
        String bcid = null;
        try {
            BCIDDatabase db = new BCIDDatabase();
            Statement stmt = db.conn.createStatement();

            String query = "select \n" +
                    "d.prefix as prefix \n" +
                    "from \n" +
                    "datasets d, projectsBCIDs pb, projects p \n" +
                    "where \n" +
                    "d.datasets_id=pb.datasets_id && \n" +
                    "pb.project_id=p.project_id && \n" +
                    "p.project_code='" + project_code + "' && \n" +
                    "d.resourceType='" + conceptURI + "'";
            ResultSet rs= stmt.executeQuery(query);
            rs.next();
            bcid = rs.getString("prefix");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bcid;
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
    public boolean run() throws Exception {
        triplifier.getTriples(this);
        return true;
    }

    /**
     * Just tell us where the file is stored...
     */
    public void print() {
        System.out.println("Triplify ...");
        System.out.println("\ttriple output file = " + triplifier.getTripleOutputFile());
        System.out.println("\tsparql update file = " + triplifier.getUpdateOutputFile());
    }

    /**
     * Loop through the entities and relations we have defined...
     */
    public void printObject() {
        System.out.println("Mapping has " + entities.size() + " entries");

        for (Iterator<Entity> i = entities.iterator(); i.hasNext(); ) {
            Entity e = i.next();
            e.print();
        }

        for (Iterator<Relation> i = relations.iterator(); i.hasNext(); ) {
            Relation r = i.next();
            r.print();
        }
    }
}