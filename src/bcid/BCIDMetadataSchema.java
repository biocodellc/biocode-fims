package bcid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

/**
 * Metadata Schema for Describing an identifier --
 * These are the metadata elements building blocks that we can use to express this identifier either via RDF or HTML
 * and consequently forms the basis of what the "outside" world sees about the identifiers.
 * This class is used by Renderers to structure content.
 */
public class BCIDMetadataSchema {
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

    private static Logger logger = LoggerFactory.getLogger(BCIDMetadataSchema.class);



    public bcid identifier;

    public BCIDMetadataSchema() {
    }

    public void BCIDMetadataInit(GenericIdentifier identifier) {
        this.identifier = (bcid) identifier;
        dcPublisher = new metadataElement("dc:publisher",this.identifier.projectCode , "The BCID project to which this resource belongs.");

        String ark = null;
        Iterator iterator = identifier.getMetadata().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            String bcidKey = (String) pairs.getKey();
            try {
                if (bcidKey.equalsIgnoreCase("ark")) {
                    ark = pairs.getValue().toString();
                    about = new metadataElement("rdf:Description", identifier.resolverTargetPrefix + ark, "The current identifier resolution service.");
                } else if (bcidKey.equalsIgnoreCase("what")) {
                    resource = new metadataElement("rdf:type", pairs.getValue().toString(), "What is this object.");
                } else if (bcidKey.equalsIgnoreCase("when")) {
                    dcDate = new metadataElement("dc:date",  pairs.getValue().toString(), "Date that metadata was last updated for this identifier.");
                } else if (bcidKey.equalsIgnoreCase("who")) {
                    dcCreator = new metadataElement("dc:creator", pairs.getValue().toString(), "Who created the group definition.");
                } else if (bcidKey.equalsIgnoreCase("title")) {
                    dcTitle = new metadataElement("dc:title", pairs.getValue().toString(), "Title");
                } else if (bcidKey.equalsIgnoreCase("suffix")) {
                    dcSource = new metadataElement("dc:source", pairs.getValue().toString(), "The locally-unique identifier.");
                } else if (bcidKey.equalsIgnoreCase("rights")) {
                    dcRights = new metadataElement("dcterms:rights", pairs.getValue().toString(), "Rights applied to the metadata content describing this identifier.");
                } else if (bcidKey.equalsIgnoreCase("datasetsPrefix")) {
                    //Don't print this line for the Test Account
                    if (!identifier.getMetadata().get("who").equals("Test Account")) {
                        dcIsReferencedBy = new metadataElement("dcterms:isReferencedBy", "http://n2t.net/" + pairs.getValue().toString(), "The group level identifier, registered with EZID.");
                    }
                } else if (bcidKey.equalsIgnoreCase("doi")) {
                    // Create mapping here for DOI if it only shows the prefix
                    String doi = pairs.getValue().toString().replace("doi:", "http://dx.doi.org/");
                    dcIsPartOf = new metadataElement("dcterms:isReferencedBy", doi, "A DOI describing the dataset which this identifier belongs to.");
               // } else if (bcidKey.equalsIgnoreCase("projectCode")) {
                    // Create mapping here for DOI if it only shows the prefix
                 //   dcPublisher = new metadataElement("dc:publisher", pairs.getValue().toString(), "The BCID project to which this resource belongs.");
                } else if (bcidKey.equalsIgnoreCase("webaddress")) {
                    dcHasVersion = new metadataElement("dcterms:hasVersion", pairs.getValue().toString(), "The redirection target for this identifier.");
                } else if (bcidKey.equalsIgnoreCase("datasetsSuffixPassThrough")) {
                    bscSuffixPassthrough = new metadataElement("bsc:suffixPassthrough", pairs.getValue().toString(), "Indicates that this identifier supports suffixPassthrough.");
                }
            } catch (NullPointerException e) {
                //TODO should we silence this exception?
                logger.warn("NullPointerException thrown for identifier: {}", identifier);
            }
        }
        if (ark != null) {
            try {
                dcMediator = new metadataElement("dcterms:mediator", identifier.getMetadataTarget().toString(), "Metadata mediator");
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
