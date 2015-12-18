package settings;

import org.json.simple.JSONObject;
import org.jsoup.Jsoup;

/**
 * Handle data about availableProjects coming from BCID
 */
public class availableProject {
    String projectTitle;
    String validationXml;
    String projectCode;
    String projectId;
    JSONObject o;

    public availableProject(JSONObject o) {
        this.o = o;
        projectTitle = o.get("projectTitle").toString();
        validationXml = o.get("validationXml").toString();
        projectCode = o.get("projectCode").toString();
        projectId = o.get("projectId").toString();
    }

    public String getProject_title() {
        return projectTitle;
    }

    public String getValidationXml() {
        return validationXml;
    }

    public String getProject_code() {
        return projectCode;
    }

    public String getProject_id() {
        return projectId;
    }

}
