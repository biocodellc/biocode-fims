package bcid;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * ResourceTypes class is a controlled list of available ResourceTypes.  This is built into code since these
 * types have rarely changed and in fact are central to so many coding operations and we don't want to rely on
 * instance level configuration control.
 * <p/>
 * ResourceTypes draw from  Dublin Core DCMI Resource Types, Dublin Darwin Core Classes, Darwin Core, Information Artifact Ontology, and ENVO
 * <p/>
 */
public class ResourceTypes {

    static ArrayList list = new ArrayList();

    // DCMI METADATA
    public static int DATASET = 1;
    public static int EVENT = 2;
    public static int IMAGE = 3;
    public static int MOVINGIMAGE = 4;
    public static int PHYSICALOBJECT = 5;
    public static int SERVICE = 6;
    public static int SOUND = 7;
    public static int TEXT = 8;

    public static int SPACER1 = 9;

    // Dublin Core Classes
    public static int LOCATION = 10;
    public static int AGENT = 11;

    public static int SPACER2 = 12;

    // IAO
    public static int INFORMATIONCONTENTENTITY = 13;

    public static int SPACER3 = 14;

    // OBI
    public static int MATERIALSAMPLE = 15;

    public static int SPACER4 = 16;

    // DARWIN CORE TYPES
    public static int PRESERVEDSPECIMEN = 17;
    public static int FOSSILSPECIMEN = 18;
    public static int LIVINGSPECIMEN = 19;
    public static int HUMANOBSERVATION = 20;
    public static int MACHINEOBSERVATION = 21;

    public static int SPACER5 = 22;

    // DARWIN CORE TERMS
    public static int OCCURRENCE = 23;
    public static int IDENTIFICATION = 24;
    public static int TAXON = 25;
    public static int RESOURCERELATIONSHIP = 26;
    public static int MEASUREMENTORFACT = 27;
    public static int GEOLOGICALCONTEXT = 28;

    public static int SPACER6 = 29;

    // ENVO
    public static int BIOME = 30;
    public static int FEATURE = 31;
    public static int MATERIAL = 32;

    public static int SPACER7 = 33;

    // Catch All
    public static int RESOURCE = 34;

    public static int SPACER8 = 35;

    // Sequencing Terms
    public static int NUCLEICACIDSEQUENCESOURCE = 36;
    public static int SEQUENCING = 37;

    public static int COLLECTION = 38;


    public ResourceTypes() {
        list.clear();
        ResourceType type = null;

        // DCMI Resource Types
        list.add(new ResourceType(this.DATASET, "dctype:Dataset", "http://purl.org/dc/dcmitype/Dataset", "Data encoded in a defined structure."));
        list.add(new ResourceType(this.EVENT, "dctype:Event", "http://purl.org/dc/dcmitype/Event", "A non-persistent, time-based occurrence."));
        list.add(new ResourceType(this.IMAGE, "dctype:Image", "http://purl.org/dc/dcmitype/Image", "A visual representation other than text."));
        list.add(new ResourceType(this.MOVINGIMAGE, "dctype:MovingImage", "http://purl.org/dc/dcmitype/MovingImage", "A series of visual representations imparting an impression of motion when shown in succession."));
        list.add(new ResourceType(this.PHYSICALOBJECT, "dctype:PhysicalObject", "http://purl.org/dc/dcmitype/PhysicalObject", "An inanimate, three-dimensional object or substance."));
        list.add(new ResourceType(this.SERVICE, "dctype:Service", "http://purl.org/dc/dcmitype/Service", "A system that provides one or more functions."));
        list.add(new ResourceType(this.SOUND, "dctype:Sound", "http://purl.org/dc/dcmitype/Sound", "A resource primarily intended to be heard."));
        list.add(new ResourceType(this.TEXT, "dctype:Text", "http://purl.org/dc/dcmitype/Text", "A resource consisting primarily of words for reading."));
        list.add(new ResourceType(this.SPACER1));

        // Dublin Core Classes
        list.add(new ResourceType(this.LOCATION, "dcterms:Location", "http://purl.org/dc/terms/Location", "A spatial region or named place."));
        list.add(new ResourceType(this.AGENT, "dcterms:Agent", "http://purl.org/dc/terms/Agent", "A resource that acts or has the power to act."));
        list.add(new ResourceType(this.SPACER2));

        // IAO
        list.add(new ResourceType(this.INFORMATIONCONTENTENTITY, "iao:InformationContentEntity", "http://purl.obolibrary.org/obo/IAO_0000030", "Examples of information content entites include journal articles, data, graphical layouts, and graphs."));
        list.add(new ResourceType(this.SPACER3));

        // OBI
        list.add(new ResourceType(this.MATERIALSAMPLE, "obi:Specimen", "http://purl.obolibrary.org/obo/OBI_0100051", "A material entity that has the specimen role"));
        list.add(new ResourceType(this.SPACER4));


        // DARWIN CORE TYPES
        list.add(new ResourceType(this.PRESERVEDSPECIMEN, "dwcterms:PreservedSpecimen", "http://rs.tdwg.org/dwc/dwctype/PreservedSpecimen", "A resource describing a preserved specimen."));
        list.add(new ResourceType(this.FOSSILSPECIMEN, "dwcterms:FossilSpecimen", "http://rs.tdwg.org/dwc/dwctype/FossilSpecimen", "A resource describing a fossilized specimen."));
        list.add(new ResourceType(this.LIVINGSPECIMEN, "dwcterms:LivingSpecimen", "http://rs.tdwg.org/dwc/dwctype/LivingSpecimen", "A resource describing a living specimen."));
        list.add(new ResourceType(this.HUMANOBSERVATION, "dwcterms:HumanObservation", "http://rs.tdwg.org/dwc/dwctype/HumanObservation", "A resource describing an observation made by one or more people."));
        list.add(new ResourceType(this.MACHINEOBSERVATION, "dwcterms:MachineObservation", "http://rs.tdwg.org/dwc/dwctype/MachineObservation", "A resource describing an observation made by a machine."));
        list.add(new ResourceType(this.SPACER5));

        // DARWIN CORE TERMS
        list.add(new ResourceType(this.OCCURRENCE, "dwc:Occurrence", "http://rs.tdwg.org/dwc/terms/Occurrence", "The category of information pertaining to evidence of an occurrence in nature, in a collection, or in a dataset (specimen, observation, etc.)"));
        list.add(new ResourceType(this.IDENTIFICATION, "dwc:Identification", "http://rs.tdwg.org/dwc/terms/Identification", "The category of information pertaining to taxonomic determinations (the assignment of a scientific name)."));
        list.add(new ResourceType(this.TAXON, "dwc:Taxon", "http://rs.tdwg.org/dwc/terms/Taxon", "The category of information pertaining to taxonomic names, taxon name usages, or taxon concepts."));
        list.add(new ResourceType(this.RESOURCERELATIONSHIP, "dwc:ResourceRelationship", "http://rs.tdwg.org/dwc/terms/ResourceRelationship", "The category of information pertaining to relationships between resources (instances of data records, such as Occurrences, Taxa, Locations, Events)."));
        list.add(new ResourceType(this.MEASUREMENTORFACT, "dwc:MeasurementOrFact", "http://rs.tdwg.org/dwc/terms/MeasurementOrFact", "The category of information pertaining to measurements, facts, characteristics, or assertions about a resource (instance of data record, such as Occurrence, Taxon, Location, Event)."));
        list.add(new ResourceType(this.GEOLOGICALCONTEXT, "dwc:GeologicalContext", "http://rs.tdwg.org/dwc/terms/GeologicalContext", "The category of information pertaining to a location within a geological context, such as stratigraphy."));
        list.add(new ResourceType(this.SPACER6));

        // ENVO
        list.add(new ResourceType(this.BIOME, "envo:Biome", "http://purl.obolibrary.org/obo/ENVO_00000428", "A major class of ecologically similar communities of plants, animals, and other organisms."));
        list.add(new ResourceType(this.FEATURE, "envo:Feature", "http://purl.obolibrary.org/obo/ENVO_00002297", "An environmental feature is a prominent or distinctive aspect, quality, or characteristic of a given biome."));
        list.add(new ResourceType(this.MATERIAL, "envo:Material", "http://purl.obolibrary.org/obo/ENVO_00010483", "Material in or on which organisms may live."));
        list.add(new ResourceType(this.SPACER7));

        // Catch All
        list.add(new ResourceType(this.RESOURCE, "rdfs:Resource", "http://www.w3.org/2000/01/rdf-schema#Resource", "Resource is the class of everything"));
        list.add(new ResourceType(this.SPACER8));

        // Sequencing Terms
        list.add(new ResourceType(this.NUCLEICACIDSEQUENCESOURCE, "mixs:NucleicAcidSequenceSource", "http://gensc.org/ns/mixs/NucleicAcidSequenceSource", "The category of information pertaining to nucleic acid sequence source."));
        list.add(new ResourceType(this.SEQUENCING, "mixs:Sequencing", "http://gensc.org/ns/mixs/Sequencing", "The category of information pertaining to sequencing."));

        // Additional DCMI Resource Type
        list.add(new ResourceType(this.COLLECTION, "dctype:Collection", "http://purl.org/dc/dcmitype/Collection", "An aggregation of resources."));


    }

    /**
     * Return a ResourceType object given an Integer
     *
     * @param typeIncrement
     * @return ResourceType
     */
    public static ResourceType get(int typeIncrement) {
        return (ResourceType) list.get(typeIncrement - 1);
    }

    /**
     * Return a ResourceType object given a string in the form of a URI
     * @param uri
     * @return
     */
    public  ResourceType get(String uri) {
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            ResourceType rt = (ResourceType) iterator.next();
            if (uri.equals(rt.uri)) {
                return rt;
            }
        }
        return null;
    }

    /**
     * Return a ResourceType object given the ResourceType string
     * @param name
     * @return
     */
    public ResourceType getByName(String name) {
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            ResourceType rt = (ResourceType) iterator.next();
            if (name.equals(rt.string)) {
                return rt;
            }
        }
        return null;
    }

    /**
     * shortName also equals "alias"
     * @param shortname
     * @return
     */
    public ResourceType getByShortName(String shortname) {
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
           ResourceType rt = (ResourceType) iterator.next();
            if (shortname.equals(rt.getShortName())) {
                return rt;
            }
        }
        return null;
    }

    /**
     * Return all the resources as JSON
     *
     * @return
     */
    public String getAllAsJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        Iterator it = list.iterator();
        int count = 0;
        while (it.hasNext()) {
            ResourceType rt = (ResourceType) it.next();
            if (count == 0) {
                json.append("\"0\":\"Select a Concept\"");
            }
            json.append(",");

            if (rt.string.equals("spacer")) {
                json.append("\"" + rt.resourceType + "\":\"---\"");
            } else {
                json.append("\"" + rt.resourceType + "\":\"" + rt.string + "\"");
            }
            count++;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Its often beneficial to return all options EXCEPT dataset, since providing Dataset as
     * an option technically means a "dataset of bcids" but this is most likely never desirable
     * for the purposes of this application.
     *
     * @return
     */
    public String getAllButDatasetAsJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        Iterator it = list.iterator();
        int count = 0;
        while (it.hasNext()) {
            ResourceType rt = (ResourceType) it.next();

            if (count == 0) {
                json.append("\"0\":\"Select a Concept\"");
            }
            json.append(",");

            if (rt.resourceType != this.DATASET) {
                // TODO: validate that nobody selects this option in the interface!
                if (rt.string.equals("spacer")) {
                    json.append("\"" + rt.resourceType + "\":\"---\"");
                } else {
                    json.append("\"" + rt.resourceType + "\":\"" + rt.string + "\"");
                }
                count++;
            }
        }
        json.append("}");
        return json.toString();
    }

    public static void main(String args[]) {
        ResourceTypes rts = new ResourceTypes();
        System.out.println(rts.get("http://purl.obolibrary.org/obo/IAO_0000030").string);
        //System.out.println(rts.getAllAsJSON());

    }

    /**
     * Create an HTML table respresentation of resourceTypes
     *
     * @return
     */
    public String getResourceTypesAsTable() {
        StringBuilder output = new StringBuilder();
        Iterator it = list.iterator();

        output.append("<table>");
        output.append("<tr>");
        output.append("<td><b>Name/URI</b></td>");
        output.append("<td><b>Description</b></td>");
        output.append("</tr>");

        while (it.hasNext()) {
            ResourceType rt = (ResourceType) it.next();
            if (!rt.string.equals("spacer")) {
                output.append("<tr>");
                output.append("<td><a href=\"" + rt.uri + "\">" + rt.string + "</a></td>");
                output.append("<td>" + rt.description + "</td>");
                output.append("</tr>");
            }
        }

        output.append("</table>");
        return output.toString();
    }
}
