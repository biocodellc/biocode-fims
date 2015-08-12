package utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import settings.bcidConnector;

import java.util.Iterator;


/**
 * class to generate the users dashboard
 */
public class dashboardGenerator {
    private String accessToken;
    private String refreshToken;

    public dashboardGenerator(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getDashboard(String username) {
        StringBuilder sb = new StringBuilder();
        bcidConnector bcidConnector = new bcidConnector(accessToken, refreshToken);
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        String serviceRoot = sm.retrieveValue("fims_service_root");

        JSONObject projects = ((JSONObject) JSONValue.parse(bcidConnector.getMyGraphs()));

        sb.append("<h1>");
        sb.append(username);
        sb.append("'s Datasets</h1>\\n");

        // iterate over each project
        for (Iterator it = projects.keySet().iterator(); it.hasNext(); ) {
            String project_title = (String) it.next();
            JSONArray projectDatasets = (JSONArray) projects.get(project_title);
            sb.append("<br>\\n<a class='expand-content' id='");
            sb.append(project_title.replace(" ", "_").replace(".", "_").replace("#","_"));
            sb.append("' href='javascript:void(0);'>\\n");
            sb.append("\\t <img src='images/right-arrow.png' id='arrow' class='img-arrow'>");
            sb.append(project_title);
            sb.append("</a>\\n");
//            sb.append("<h2>");
//            sb.append(project_title);
//            sb.append("</h2>\\n");
            sb.append("<div class='toggle-content' id='");
            sb.append(project_title.replace(" ", "_").replace(".", "_").replace("#","_"));
            sb.append("'>");
            sb.append("<table>\\n");
            sb.append("\\t<tr>\\n");
            sb.append("\\t\\t<th>Name</th>\\n");
            sb.append("\\t\\t<th>Public</th>\\n");
            sb.append("\\t\\t<th class='align_center'>Date</th>\\n");
            sb.append("\\t\\t<th>Download</th>\\n");
            sb.append("\\t\\t<th>Edit</th>\\n");
            sb.append("\\t\\t<th>Dataset Persistent Identifier (add header rdf+xml for RDF)</th>\\n");
            sb.append("\\t</tr>\\n");

            // inform the user that there is no datasets in the project
            if (projectDatasets.isEmpty()) {
                sb.append("\\t<tr>\\n");
                sb.append("\\t\\t<td colspan='4'>You have no datasets in this project.</td>\\n");
                sb.append("\\t</tr>\\n");
            }

            // iterate over the project's datasets
            for (Object d : projectDatasets) {
                JSONObject dataset = (JSONObject) d;
                sb.append("\\t<tr>\\n");

                sb.append("\\t\\t<td>");
                sb.append((String) dataset.get("expedition_title"));
                sb.append("</td>\\n");

                sb.append("\\t\\t<td class='align_center'>");
                if (dataset.get("public").equals("1")) {
                    sb.append("yes");
                } else {
                    sb.append("no");
                }
                sb.append("</td>\\n");

                sb.append("\\t\\t<td>");
                sb.append((String) dataset.get("ts"));
                sb.append("</td>\\n");

                // Excel option
                sb.append("\\t\\t<td class='align_center'>");
                sb.append("<a href='");
                sb.append(serviceRoot);
                sb.append("query/excel?graphs=");
                sb.append((String) dataset.get("graph"));
                sb.append("&project_id=");
                sb.append((String) dataset.get("project_id"));
                sb.append("'>.xlsx</a>");

                sb.append("&nbsp;&nbsp;");

                // TAB delimited option
                sb.append("<a href='");
                sb.append(serviceRoot);
                sb.append("query/tab?graphs=");
                sb.append((String) dataset.get("graph"));
                sb.append("&project_id=");
                sb.append((String) dataset.get("project_id"));
                sb.append("'>.txt</a>");

                sb.append("&nbsp;&nbsp;");

                /*
                // Demo user should have direct link to webaddress
                if (username.equalsIgnoreCase("demo")) {
                    sb.append("<a href='");
                    sb.append((String) dataset.get("webaddress"));
                    sb.append("'>n3</a>");
                    // All other users will have ark which can redirect
                } else {
                    sb.append("<a href='");
                    sb.append("http://n2t.net/" + (String) dataset.get("ark"));
                    sb.append("'>n3</a>");
                }  */

                sb.append("&nbsp;&nbsp;");

                sb.append("</td>\\n");

                sb.append("<td><a href='#' onclick=\\\"editDataset('");
                sb.append(dataset.get("project_id"));
                sb.append("', '");
                sb.append(dataset.get("expedition_code"));
                sb.append("', this)\\\">edit</a></td>");

                // Direct Link
                String ark = (String) dataset.get("ark");
                if (ark.contains("99999") || username.equalsIgnoreCase("demo")) {
                    sb.append("<td>not available for demonstration server or demo account</td>");
                } else {
                    sb.append("<td><a href='");
                    sb.append("http://n2t.net/" + (String) dataset.get("ark"));
                    sb.append("'>");
                    sb.append("http://n2t.net/" + (String) dataset.get("ark"));
                    sb.append("</a></td>");
                }

                sb.append("\\t</tr>\\n");
            }

            sb.append("</table>\\n");
            sb.append("</div>\\n");
        }

        return sb.toString();
    }

    public String getNMNHDashboard(String username) {
        StringBuilder sb = new StringBuilder();
        bcidConnector bcidConnector = new bcidConnector(accessToken, refreshToken);
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        JSONObject projects = ((JSONObject) JSONValue.parse(bcidConnector.getMyDatasets()));

        sb.append("<h1>");
        sb.append(username);
        sb.append("'s Datasets</h1>\\n");

        // iterate over each project
        for (Iterator it = projects.keySet().iterator(); it.hasNext(); ) {
            String project_title = (String) it.next();
            sb.append("<br>\\n<a class='expand-content' id='");
            sb.append(project_title.replace(" ", "_").replace(".", "_").replace("#","_"));
            sb.append("' href='javascript:void(0);'>\\n");
            sb.append("\\t <img src='/fims/images/right-arrow.png' id='arrow' class='img-arrow'>");
            sb.append(project_title);
            sb.append("</a>\\n");
            sb.append("<div class='toggle-content' id='");
            sb.append(project_title.replace(" ", "_").replace(".", "_").replace("#","_"));
            sb.append("'>");

            // iterate over each expedition
            for (Iterator it2 = ((JSONObject) projects.get(project_title)).keySet().iterator(); it2.hasNext(); ) {
                String expedition_title = (String) it2.next();
                JSONArray expeditionDatasets = (JSONArray) ((JSONObject) projects.get(project_title)).get(expedition_title);
                sb.append("<br>\\n<a class='expand-content' id='");
                sb.append(expedition_title.replace(" ", "_").replace(".", "_").replace("#","_"));
                sb.append("' href='javascript:void(0);'>\\n");
                sb.append("\\t <img src='/fims/images/right-arrow.png' id='arrow' class='img-arrow'>");
                sb.append(expedition_title);
                sb.append("</a>\\n");
                sb.append("<div class='toggle-content' id='");
                sb.append(expedition_title.replace(" ", "_").replace(".", "_").replace("#","_"));
                sb.append("'>");
                sb.append("<table>\\n");
                sb.append("\\t<tr>\\n");
                sb.append("\\t\\t<th class='align_center'>Date</th>\\n");
                sb.append("\\t\\t<th>finalCopy</th>\\n");
                sb.append("\\t</tr>\\n");

                // inform the user that there is no datasets in the project
                if (expeditionDatasets.isEmpty()) {
                    sb.append("\\t<tr>\\n");
                    sb.append("\\t\\t<td colspan='4'>You have no datasets in this project.</td>\\n");
                    sb.append("\\t</tr>\\n");
                } else {

                    // iterate over the expeditions's datasets
                    for (Object d : expeditionDatasets) {
                        JSONObject dataset = (JSONObject) d;
                        sb.append("\\t<tr>\\n");

                        sb.append("\\t\\t<td>");
                        sb.append((String) dataset.get("ts"));
                        sb.append("</td>\\n");

                        sb.append("\\t\\t<td class='align_center'>");
                        if (dataset.get("finalCopy").equals("1")) {
                            sb.append("yes");
                        } else {
                            sb.append("no");
                        }
                        sb.append("</td>\\n");

                        sb.append("\\t</tr>\\n");
                    }
                }
//                sb.append("<br>");
                sb.append("</table>\\n");
                sb.append("</div>\\n");
            }

            sb.append("</div>\\n");
//            sb.append("<br>");
        }

        return sb.toString();
    }

    public static void main(String args[]) {
        dashboardGenerator dg = new dashboardGenerator("WdqS_BTeSE-gxkSJ-D6c", "v5VUgUB6jUV_YvKNpyH8");
        System.out.println(dg.getNMNHDashboard("demo"));
    }
}
