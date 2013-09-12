package digester;

import triplify.triplifier;
import settings.Connection;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Mapping implements MappingInterface {
    public Connection connection;

    private final LinkedList<Entity> entities = new LinkedList<Entity>();
    private final LinkedList<Relation> relations = new LinkedList<Relation>();
    private triplifier triplifier;

    public Mapping(triplifier t) throws Exception {
        triplifier = t;
        // Create a connection to a SQL Lite Instance
        try {
            this.connection = new Connection(t.createSqlLite());
        } catch (Exception e) {
            throw new Exception("unable to establish connection to SQLLite");
        }
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
        //String result = "";
        //if (entity.getWorksheetUniqueKey().equalsIgnoreCase("") || entity.getWorksheetUniqueKey() == null) {
        //  result += "\td2rq:uriColumn \"" + entity.getColumn() + "\";";
        // This assigns the default urn:x-biscicol: pattern before the identifier, ensuring it is a URI.
        //} else {
        //  result += "\td2rq:uriPattern \"" + entity.getBcid() + "@@" + entity.getColumn() + "@@\";";
        //}
        //return result;
        return "\td2rq:uriPattern \"" + entity.getBcid() + "_@@" + entity.getColumn() + "@@\";";
    }


    public void print() {
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
     * @throws Exception
     */
    public void run() throws Exception {
        triplifier.getTriples(this);
    }


}