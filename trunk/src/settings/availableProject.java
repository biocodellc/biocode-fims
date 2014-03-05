package settings;

import org.json.simple.JSONObject;

/**
 * Handle data about availableProjects coming from BCID
 */
public class availableProject {
    String project_title;
    String biovalidator_validation_xml;
    String project_code;
    String project_id;
    JSONObject o;

    public availableProject(JSONObject o) {
        this.o = o;
        project_title = o.get("project_title").toString();
        biovalidator_validation_xml = o.get("biovalidator_validation_xml").toString();
        project_code = o.get("project_code").toString();
        project_id = o.get("project_id").toString();
    }

    public String getProject_title() {
        return project_title;
    }

    public String getBiovalidator_validation_xml() {
        return biovalidator_validation_xml;
    }

    public String getProject_code() {
        return project_code;
    }

    public String getProject_id() {
        return project_id;
    }

}
