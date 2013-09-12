package digester;

/**
 * Metadata defines metadata for this FIMS installation
 */
public class Metadata {
    private String doi;
    private String shortname;
    private String eml_location;

    private String sdb_user;
    private String sdb_driver;
    private String sdb_configuration_file;
    private String text_abstract;


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

    public String getSdb_user() {
        return sdb_user;
    }

    public void setSdb_user(String sdb_user) {
        this.sdb_user = sdb_user;
    }

    public String getSdb_driver() {
        return sdb_driver;
    }

    public void setSdb_driver(String sdb_driver) {
        this.sdb_driver = sdb_driver;
    }

    public String getSdb_configuration_file() {
        return sdb_configuration_file;
    }

    public void setSdb_configuration_file(String sdb_configuration_file) {
        this.sdb_configuration_file = sdb_configuration_file;
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
        System.out.println("\t\tsdb_user = " + sdb_user);
        System.out.println("\t\tsdb_driver = " + sdb_driver);
        System.out.println("\t\tsdb_configuration = " + sdb_configuration_file);
        System.out.println("\t\ttext_abstract = " + text_abstract);
    }
}
