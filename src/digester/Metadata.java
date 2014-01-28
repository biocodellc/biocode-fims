package digester;

import settings.fimsPrinter;

/**
 * Metadata defines metadata for this FIMS installation
 */
public class Metadata {
    private String doi;
    private String shortname;
    private String eml_location;
    private String target;
    private String queryTarget;

    private String text_abstract;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getQueryTarget() {
        return queryTarget;
    }

    public void setQueryTarget(String queryTarget) {
        this.queryTarget = queryTarget;
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
        fimsPrinter.out.println("\tMetadata");
        fimsPrinter.out.println("\t\tdoi = " + doi);
        fimsPrinter.out.println("\t\tshortname = " + shortname);
        fimsPrinter.out.println("\t\teml_locaiton = " + eml_location);
        fimsPrinter.out.println("\t\ttarget = " + target);
        fimsPrinter.out.println("\t\ttext_abstract = " + text_abstract);
    }
}
