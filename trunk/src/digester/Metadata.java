package digester;

/**
 * Metadata defines metadata for this FIMS installation
 */
public class Metadata {
    private String doi;
    private String shortname;
    private String eml_location;
    private String target;
    private String text_abstract;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getEml_location() {
        return eml_location;
    }

    public void setEml_location(String eml_location) {
        this.eml_location = eml_location;
    }


    public String getText_abstract() {
        return text_abstract;
    }

    public void addText_abstract(String text_abstract) {
        this.text_abstract = text_abstract;
    }

    public void print() {
        System.out.println("\tMetadata");
        System.out.println("\t\tdoi = " + doi);
        System.out.println("\t\tshortname = " + shortname);
        System.out.println("\t\teml_locaiton = " + eml_location);
        System.out.println("\t\ttarget = " + target);
        System.out.println("\t\ttext_abstract = " + text_abstract);
    }
}
