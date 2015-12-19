package settings;

import org.json.simple.JSONObject;
import org.jsoup.Jsoup;

/**
 * Handle data about availableProjects coming from BCID
 */
public class AvailableProject {
    String projectTitle;
    String validationXml;
    String projectCode;
    String projectId;
    JSONObject o;

    public AvailableProject(JSONObject o) {
        this.o = o;
        projectTitle = o.get("projectTitle").toString();
        validationXml = o.get("validationXml").toString();
        projectCode = o.get("projectCode").toString();
        projectId = o.get("projectId").toString();
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getValidationXml() {
        return validationXml;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectId() {
        return projectId;
    }

}
