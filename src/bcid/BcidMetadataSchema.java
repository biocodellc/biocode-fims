package bcid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

/**
 * Metadata Schema for Describing a Bcid--
 * These are the metadata elements building blocks that we can use to express this Bcid either via RDF or HTML
 * and consequently forms the basis of what the "outside" world sees about the bcids.
 * This class is used by Renderers to structure content.
 */
public class BcidMetadataSchema {
    // Core Elements for rendering
    public metadataElement about = null;
    public metadataElement resource = null;
    public metadataElement dcCreator = null;
    public metadataElement dcTitle = null;
    public metadataElement dcDate = null;
    public metadataElement dcRights = null;
    public metadataElement dcIsReferencedBy = null;
    public metadataElement dcIsPartOf = null;
    public metadataElement dcSource = null;
    public metadataElement dcMediator = null;
    public metadataElement dcHasVersion = null;
    public metadataElement bscSuffixPassthrough = null;
    public metadataElement dcPublisher = null;

    public Bcid bcid = null;

    private static Logger logger = LoggerFactory.getLogger(BcidMetadataSchema.class);

    public BcidMetadataSchema() {
    }

    public void BCIDMetadataInit(Bcid bcid) {
        this.bcid = bcid;
        dcPublisher = new metadataElement("dc:publisher",bcid.projectCode , "The BCID project to which this resource belongs.");

        String identifier = null;
        Iterator iterator = bcid.getMetadata().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            String bcidKey = (String) pairs.getKey();
            try {
                if (bcidKey.equalsIgnoreCase("identifier")) {
                    identifier = pairs.getValue().toString();
                    about = new metadataElement("rdf:Description", bcid.resolverTargetPrefix + identifier, "The current bcid resolution service.");
                } else if (bcidKey.equalsIgnoreCase("resourceType")) {
                    resource = new metadataElement("rdf:type", pairs.getValue().toString(), "What is this object.");
                } else if (bcidKey.equalsIgnoreCase("ts")) {
                    dcDate = new metadataElement("dc:date",  pairs.getValue().toString(), "Date that metadata was last updated for this bcid.");
                } else if (bcidKey.equalsIgnoreCase("who")) {
                    dcCreator = new metadataElement("dc:creator", pairs.getValue().toString(), "Who created the group definition.");
                } else if (bcidKey.equalsIgnoreCase("title")) {
                    dcTitle = new metadataElement("dc:title", pairs.getValue().toString(), "Title");
                } else if (bcidKey.equalsIgnoreCase("suffix")) {
                    dcSource = new metadataElement("dc:source", pairs.getValue().toString(), "The locally-unique bcid.");
                } else if (bcidKey.equalsIgnoreCase("rights")) {
                    dcRights = new metadataElement("dcterms:rights", pairs.getValue().toString(), "Rights applied to the metadata content describing this bcid.");
                } else if (bcidKey.equalsIgnoreCase("prefix")) {
                    //Don't print this line for the Test Account
                    if (!bcid.getMetadata().get("who").equals("Test Account")) {
                        dcIsReferencedBy = new metadataElement("dcterms:isReferencedBy", "http://n2t.net/" + pairs.getValue().toString(), "The group level bcid, registered with EZID.");
                    }
                } else if (bcidKey.equalsIgnoreCase("doi")) {
                    // Create mapping here for DOI if it only shows the identifier
                    String doi = pairs.getValue().toString().replace("doi:", "http://dx.doi.org/");
                    dcIsPartOf = new metadataElement("dcterms:isReferencedBy", doi, "A DOI describing the dataset which this bcid belongs to.");
               // } else if (bcidKey.equalsIgnoreCase("projectCode")) {
                    // Create mapping here for DOI if it only shows the identifier
                 //   dcPublisher = new metadataElement("dc:publisher", pairs.getValue().toString(), "The BCID project to which this resource belongs.");
                } else if (bcidKey.equalsIgnoreCase("webAddress")) {
                    dcHasVersion = new metadataElement("dcterms:hasVersion", pairs.getValue().toString(), "The redirection target for this bcid.");
                } else if (bcidKey.equalsIgnoreCase("bcidsSuffixPassThrough")) {
                    bscSuffixPassthrough = new metadataElement("bsc:suffixPassthrough", pairs.getValue().toString(), "Indicates that this bcid supports suffixPassthrough.");
                }
            } catch (NullPointerException e) {
                //TODO should we silence this exception?
                logger.warn("NullPointerException thrown for bcid: {}", bcid);
            }
        }
        if (identifier != null) {
            try {
                dcMediator = new metadataElement("dcterms:mediator", bcid.getMetadataTarget().toString(), "Metadata mediator");
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException thrown", e);
            }
        }
    }

    /**
     * A convenience class for holding metadata elements
     */
    public final class metadataElement {
        private String key;
        private String value;
        private String description;

        public metadataElement(String key, String value, String description) {
            this.key = key;
            this.value = value;
            this.description = description;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public String setValue(String value) {
            String old = this.value;
            this.value = value;
            return old;
        }

        /**
         * Replace prefixes with fully qualified URL's
         *
         * @return
         */
        public String getFullKey() {
            String tempKey = key;
            tempKey = tempKey.replace("dc:", "http://purl.org/dc/elements/1.1/");
            tempKey = tempKey.replace("dcterms:", "http://purl.org/dc/terms/");
            tempKey = tempKey.replace("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
            tempKey = tempKey.replace("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            tempKey = tempKey.replace("bsc:", "http://biscicol.org/terms/index.html#");
            return tempKey;
        }

        /**
         * Return the human readable name of the uri if possible
         * @return
         */
        public String getShortValue() {
            String[] splitValue = value.split("/");
            if (splitValue.length > 0) {
                return splitValue[splitValue.length - 1];
            }
            return value;
        }
    }
}
