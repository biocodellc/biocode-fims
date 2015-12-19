package digester;

import settings.FimsPrinter;

/**
 * Metadata defines metadata for this FIMS installation
 */
public class Metadata {
    private String doi;
    private String shortname;
    private String eml_location;
    private String target;
    private String queryTarget;
    private String nmnh;

    private String textAbstract;

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

    public String getNmnh() {
        return nmnh;
    }

    public void setNmnh(String nmnh) {
        this.nmnh = nmnh;
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


    public String getTextAbstract() {
        return textAbstract;
    }

    public void addTextAbstract(String textAbstract) {
        this.textAbstract = textAbstract;
    }

    public void print() {
        FimsPrinter.out.println("\tMetadata");
        FimsPrinter.out.println("\t\tdoi = " + doi);
        FimsPrinter.out.println("\t\tshortName = " + shortname);
        FimsPrinter.out.println("\t\teml_locaiton = " + eml_location);
        FimsPrinter.out.println("\t\ttarget = " + target);
        FimsPrinter.out.println("\t\ttextAbstract = " + textAbstract);
    }
}
