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
    public metadataElement about;
    public metadataElement resource;
    public metadataElement dcCreator;
    public metadataElement dcTitle;
    public metadataElement dcDate;
    public metadataElement dcRights;
    public metadataElement dcIsReferencedBy;
    public metadataElement dcIsPartOf;
    public metadataElement dcSource;
    public metadataElement dcMediator;
    public metadataElement dcHasVersion;
    public metadataElement bscSuffixPassthrough;
    public metadataElement dcPublisher;
    public metadataElement forwardingResolution;
    public metadataElement resolutionTarget;
    public metadataElement identifier;

    public Bcid bcid = null;

    private static Logger logger = LoggerFactory.getLogger(BcidMetadataSchema.class);

    public BcidMetadataSchema() {
    }

    public void BCIDMetadataInit(Bcid bcid) {
        this.bcid = bcid;
        init();
        dcPublisher.setValue(bcid.projectCode);

        String identifier = null;
        Iterator iterator = bcid.getMetadata().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            String bcidKey = (String) pairs.getKey();
            try {
                if (bcidKey.equalsIgnoreCase("identifier")) {
                    identifier = pairs.getValue().toString();
                    this.identifier.setValue(identifier);
                    about.setValue(bcid.resolverTargetPrefix + identifier);
                } else if (bcidKey.equalsIgnoreCase("resourceType")) {
                    resource.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("ts")) {
                    dcDate.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("who")) {
                    dcCreator.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("title")) {
                    dcTitle.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("suffix")) {
                    dcSource.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("rights")) {
                    dcRights.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("prefix")) {
                    //Don't print this line for the Test Account
                    if (!bcid.getMetadata().get("who").equals("Test Account")) {
                        dcIsReferencedBy.setValue("http://n2t.net/" + pairs.getValue().toString());
                    }
                } else if (bcidKey.equalsIgnoreCase("doi")) {
                    // Create mapping here for DOI if it only shows the identifier
                    String doi = pairs.getValue().toString().replace("doi:", "http://dx.doi.org/");
                    dcIsPartOf.setValue(doi);
               // } else if (bcidKey.equalsIgnoreCase("projectCode")) {
                    // Create mapping here for DOI if it only shows the identifier
                 //   dcPublisher = new metadataElement("dc:publisher", pairs.getValue().toString(), "The BCID project to which this resource belongs.");
                } else if (bcidKey.equalsIgnoreCase("webAddress")) {
                    dcHasVersion.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("bcidsSuffixPassThrough")) {
                    bscSuffixPassthrough.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("forwardingResolution")) {
                    forwardingResolution.setValue(pairs.getValue().toString());
                } else if (bcidKey.equalsIgnoreCase("resolutionTarget")) {
                    resolutionTarget.setValue(pairs.getValue().toString());
                }
            } catch (NullPointerException e) {
                //TODO should we silence this exception?
                logger.warn("NullPointerException thrown for bcid: {}", bcid);
            }
        }
        if (identifier != null) {
            try {
                dcMediator.setValue(bcid.getMetadataTarget().toString());
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException thrown", e);
            }
        }
    }

    /**
     * Register all of the bcid metadataElements
     */
    private void init() {
        dcPublisher = new metadataElement("dc:publisher", "", "The BCID project to which this resource belongs.");
        about = new metadataElement("rdf:Description", "", "The current bcid resolution service.");
        resource = new metadataElement("rdf:type", "", "What is this object.");
        dcDate = new metadataElement("dc:date",  "", "Date that metadata was last updated for this bcid.");
        dcCreator = new metadataElement("dc:creator", "", "Who created the group definition.");
        dcTitle = new metadataElement("dc:title", "", "Title");
        dcSource = new metadataElement("dc:source", "", "The locally-unique bcid.");
        dcRights = new metadataElement("dcterms:rights", "", "Rights applied to the metadata content describing this bcid.");
        dcIsReferencedBy = new metadataElement("dcterms:isReferencedBy", "", "The group level bcid, registered with EZID.");
        dcIsPartOf = new metadataElement("dcterms:isPartOf", "", "A DOI describing the dataset which this bcid belongs to.");
        dcHasVersion = new metadataElement("dcterms:hasVersion", "", "The redirection target for this bcid.");
        bscSuffixPassthrough = new metadataElement("bsc:suffixPassThrough", "", "Indicates that this bcid supports suffixPassthrough.");
        forwardingResolution = new metadataElement("forwardingResolution", "", "Indicates that this bcid has a suffix and should be forwarded to the fowardingResolutionTarget.");
        resolutionTarget = new metadataElement("resolutionTarget", "", "The target uri for the locally-unique bcid.");
        identifier = new metadataElement("identifier", "", "The identifier this metadata represents.");
        dcMediator = new metadataElement("dcterms:mediator", "", "Metadata mediator");
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

        public void setValue(String value) {
            if (value == null) {
                this.value = "";
            } else {
                this.value = value;
            }
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
