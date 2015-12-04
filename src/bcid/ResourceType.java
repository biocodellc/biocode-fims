package bcid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Components used to describe individual resource types.
 */
public class ResourceType {
    public String string;
    public String uri;
    public String description;
    public Integer resourceType;

    private static Logger logger = LoggerFactory.getLogger(ResourceType.class);


    /**
     * @param string      String is the short description (e.g. PhysicalObject, Image, Text)
     * @param uri         URI represents the URI that describes this particular resource.
     * @param description Expanded text describing what this refers to.
     */
    public ResourceType(int resourceType, String string, String uri, String description) {
        this.resourceType = resourceType;
        this.string = string;
        this.uri = uri;
        this.description = description;
    }

    /**
     * Empty resourceType
     */
    public ResourceType(int resourceType) {
        this.resourceType = resourceType;
        this.string = "spacer";
        this.uri = null;
        this.description = null;
    }

    /**
     * Return the prefix of the "string". So, for "string" = "dwcterms:PreservedSpecimen", this would return "dwcterms"
     *
     * @return
     */
    public String getPrefix() {
        return string.split(":")[0];
    }

    /**
     * Return the prefix of the "string". So, for "string" = "dwcterms:PreservedSpecimen", this would return "PreservedSpecimen"
     *
     * @return
     */
    public String getShortName() {
        if (string.equals("spacer"))
            return "";
        else
            return string.split(":")[1];
    }

    public static void main(String args[]) {
        ResourceType rt = new ResourceType(ResourceTypes.PRESERVEDSPECIMEN, "dwcterms:PreservedSpecimen", "http://rs.tdwg.org/dwc/dwctype/PreservedSpecimen", "A resource describing a preserved specimen.");
    }
}
