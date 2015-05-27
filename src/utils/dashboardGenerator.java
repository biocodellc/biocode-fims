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

    public String getDashboard() {
        StringBuilder sb = new StringBuilder();
        bcidConnector bcidConnector = new bcidConnector(accessToken, refreshToken);
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        String serviceRoot = sm.retrieveValue("fims_service_root");

        JSONObject response = ((JSONObject) JSONValue.parse(bcidConnector.getMyGraphs()));
        JSONArray jArray = ((JSONArray) response.get("data"));
        Iterator it = jArray.iterator();

        sb.append("<h2>My Datasets</h2>\\n");
        sb.append("<table>\\n");
        sb.append("\\t<tr>\\n");
        sb.append("\\t\\t<th>Name</th>\\n");
        sb.append("\\t\\t<th>Public</th>\\n");
        sb.append("\\t\\t<th class='align_center'>Date</th>\\n");
        sb.append("\\t\\t<th>Download</th>\\n");
        sb.append("\\t</tr>\\n");

        if (!it.hasNext()) {
            sb.append("\\t<tr>\\n");
            sb.append("\\t\\t<td colspan='4'>You have no datasets.</td>\\n");
            sb.append("\\t</tr>\\n");
        }

        while (it.hasNext()) {
            JSONObject obj = (JSONObject) it.next();
            sb.append("\\t<tr>\\n");

            sb.append("\\t\\t<td>");
            sb.append((String) obj.get("expedition_title"));
            sb.append("</td>\\n");

            sb.append("\\t\\t<td class='align_center'>");
            sb.append("<input type='checkbox' disabled");
            if (obj.get("public").equals("1")) {
                sb.append(" checked='checked'");
            }
            sb.append(" />");
            sb.append("</td>\\n");

            sb.append("\\t\\t<td>");
            sb.append((String) obj.get("ts"));
            sb.append("</td>\\n");

            sb.append("\\t\\t<td class='align_center'>");
            sb.append("<a href='");
            sb.append(serviceRoot);
            sb.append("query/excel?graphs=");
            sb.append((String) obj.get("graph"));
            sb.append("&project_id=");
            sb.append((String) obj.get("project_id"));
            sb.append("'>.xlsx</a>");

            sb.append("&nbsp;&nbsp;");

            sb.append("<a href='");
            sb.append((String) obj.get("webaddress"));
            sb.append("'>n3</a>");

            sb.append("</td>\\n");

            sb.append("\\t</tr>\\n");
        }

        sb.append("</table>\\n");
        return sb.toString();
    }
}
