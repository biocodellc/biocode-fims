package utils;

import bcid.projectMinter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Iterator;


/**
 * class to generate the users dashboard
 */
public class dashboardGenerator {
    private String username;

    public dashboardGenerator(String username) {
        this.username = username;
    }

    public String getDashboard() {
        StringBuilder sb = new StringBuilder();
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        int projectCounter = 1;

        String serviceRoot = sm.retrieveValue("fims_service_root");

        projectMinter projectMinter = new projectMinter();
        JSONObject projects = (JSONObject) JSONValue.parse(projectMinter.getMyLatestGraphs(username));
        projectMinter.close();

        sb.append("<h1>");
        sb.append(username);
        sb.append("'s Datasets</h1>\\n");

        // iterate over each project
        for (Iterator it = projects.keySet().iterator(); it.hasNext(); ) {
            String projectTitle = (String) it.next();
            JSONArray projectDatasets = (JSONArray) projects.get(projectTitle);
            sb.append("<br>\\n<a class='expand-content' id='");
            sb.append("project" + projectCounter);
            sb.append("' href='javascript:void(0);'>\\n");
            sb.append("\\t <img src='/biocode-fims/images/right-arrow.png' id='arrow' class='img-arrow'>");
            sb.append(projectTitle);
            sb.append("</a>\\n");
            sb.append("<div class='toggle-content' id='");
            sb.append("project" + projectCounter);
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
                sb.append((String) dataset.get("expeditionTitle"));
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
                sb.append("&projectId=");
                sb.append((String) dataset.get("projectId"));
                sb.append("'>.xlsx</a>");

                sb.append("&nbsp;&nbsp;");

                // TAB delimited option
                sb.append("<a href='");
                sb.append(serviceRoot);
                sb.append("query/tab?graphs=");
                sb.append((String) dataset.get("graph"));
                sb.append("&projectId=");
                sb.append((String) dataset.get("projectId"));
                sb.append("'>.txt</a>");

                sb.append("&nbsp;&nbsp;");

                /*
                // Demo user should have direct link to webAddress
                if (username.equalsIgnoreCase("demo")) {
                    sb.append("<a href='");
                    sb.append((String) dataset.get("webAddress"));
                    sb.append("'>n3</a>");
                    // All other users will have ark which can redirect
                } else {
                    sb.append("<a href='");
                    sb.append("http://n2t.net/" + (String) dataset.get("ark"));
                    sb.append("'>n3</a>");
                }  */

                sb.append("&nbsp;&nbsp;");

                sb.append("</td>\\n");

                sb.append("<td><a href='#' onclick=\\\"editExpedition('");
                sb.append(dataset.get("projectId"));
                sb.append("', '");
                sb.append(dataset.get("expeditionCode"));
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
            projectCounter ++;
        }

        return sb.toString();
    }

    public String getNMNHDashboard() {
        StringBuilder sb = new StringBuilder();
        int projectCounter = 1;
        int datasetCounter = 1;

        projectMinter projectMinter = new projectMinter();
        JSONObject projects = (JSONObject) JSONValue.parse(projectMinter.getMyTemplatesAndDatasets(username));
        projectMinter.close();

        sb.append("<h1>");
        sb.append(username);
        sb.append("'s Templates and Datasets</h1>\\n");

        // iterate over each project
        for (Iterator it = projects.keySet().iterator(); it.hasNext(); ) {
            String projectTitle = (String) it.next();
            sb.append("<br>\\n<a class='expand-content' id='");
            sb.append("project" + projectCounter);
            sb.append("' href='javascript:void(0);'>\\n");
            sb.append("\\t <img src='/fims/images/right-arrow.png' id='arrow' class='img-arrow'>");
            sb.append(projectTitle);
            sb.append("</a>\\n");
            sb.append("<div class='toggle-content' id='");
            sb.append("project" + projectCounter);
            sb.append("'>");

            // iterate over each expedition
            for (Iterator it2 = ((JSONObject) projects.get(projectTitle)).keySet().iterator(); it2.hasNext(); ) {
                String expeditionTitle = (String) it2.next();
                JSONArray expeditionDatasets = (JSONArray) ((JSONObject) projects.get(projectTitle)).get(expeditionTitle);
                sb.append("<br>\\n<a class='expand-content' id='");
                sb.append("dataset" + datasetCounter);
                sb.append("' href='javascript:void(0);'>\\n");
                sb.append("\\t <img src='/fims/images/right-arrow.png' id='arrow' class='img-arrow'>");
                sb.append(expeditionTitle);
                sb.append("</a>\\n");
                sb.append("<div class='toggle-content' id='");
                sb.append("dataset" + datasetCounter);
                sb.append("'>");
                sb.append("<table>\\n");
                sb.append("\\t<tr>\\n");
                sb.append("\\t\\t<th class='align_center'>Date</th>\\n");
                sb.append("\\t\\t<th>finalCopy</th>\\n");
                sb.append("\\t\\t<th class='align_center'>Dataset ARK</th>\\n");
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

                        // Direct Link
                        String ark = (String) dataset.get("ark");
                        if (ark.contains("99999") || username.equalsIgnoreCase("demo")) {
                            sb.append("<td>not available for demonstration server or demo account</td>");
                        } else {
                            sb.append("<td><a href='");
                            sb.append("http://cdlib.org/id/" + (String) dataset.get("ark"));
                            sb.append("'>");
                            sb.append("http://cdlib.org/id/" + (String) dataset.get("ark"));
                            sb.append("</a></td>");
                        }

                        sb.append("\\t</tr>\\n");
                    }
                }
                sb.append("</table>\\n");
                sb.append("</div>\\n");
                datasetCounter ++;
            }

            sb.append("</div>\\n");
            projectCounter ++;
        }

        return sb.toString();
    }

    public static void main(String args[]) {
        dashboardGenerator dg = new dashboardGenerator("demo");
        System.out.println(dg.getNMNHDashboard());
    }
}
