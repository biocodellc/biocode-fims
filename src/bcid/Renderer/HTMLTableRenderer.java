package bcid.Renderer;

import bcid.*;
import biocode.fims.SettingsManager;

/**
 * HTMLTableRenderer renders Identifier results as an HTMLTable
 */
public class HTMLTableRenderer extends Renderer {
    private Integer userId = null;
    private Resolver resolver = null;
    static SettingsManager sm;

    static {
        sm = SettingsManager.getInstance();
    }

    /**
     * constructor for displaying private dataset information
     * @param username
     */
    public HTMLTableRenderer(String username, Resolver resolver) {
        Database db = new Database();
        userId = db.getUserId(username);
        this.resolver = resolver;
    }

    public void enter() {
        outputSB.append("<h1>" + bcid.getIdentifier() + " is a <a href=\"" + resource.getValue() + "\">" +
                resource.getShortValue() + "</a></h1>\n\n");
        outputSB.append("<table>\n");
        outputSB.append("\t<tr>\n" +
                "\t\t<th>Description</th>\n" +
                "\t\t<th>Value</th>\n" +
                "\t\t<th>Definition</th>\n" +
                "\t</tr>\n");
    }

    public void printMetadata() {
        tableResourceRowAppender(resource);
        tableResourceRowAppender(about);
        tableResourceRowAppender(dcMediator);
        tableResourceRowAppender(dcHasVersion);
        tableResourceRowAppender(dcIsReferencedBy);
        tableResourceRowAppender(dcRights);
        tableResourceRowAppender(dcIsPartOf);
        tablePropertyRowAppender(dcDate);
        tablePropertyRowAppender(dcCreator);
        tablePropertyRowAppender(dcTitle);
        tablePropertyRowAppender(dcSource);
        tablePropertyRowAppender(bscSuffixPassthrough);
        outputSB.append("</table>\n");
        appendExpeditionOrDatasetData(resource);
    }

    public void leave() {
    }

    public boolean validIdentifier() {
        if (this.bcid == null) {
            outputSB.append("<h1>Unable to find bcid</h1>");
            return false;
        } else {
            return true;
        }
    }

    /**
     * append each property
     *
     * @param map
     */
    private void tablePropertyRowAppender(metadataElement map) {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<tr>\n" +
                        "\t\t<td>" + map.getValue() + "</td>\n" +
                        "\t\t<td><a href=\"" + map.getFullKey() + "\">" + map.getKey() + "</a></td>\n" +
                        "\t\t<td>" + map.getDescription() + "</td>\n" +
                        "\t</tr>\n");
            }
        }
    }

    /**
     * append each property
     *
     * @param map
     */
    private void tableResourceRowAppender(metadataElement map) {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<tr>\n" +
                        "\t\t<td><a href=\"" + map.getValue() + "\">" + map.getValue() + "</a></td>\n" +
                        "\t\t<td><a href=\"" + map.getFullKey() + "\">" + map.getKey() + "</a></td>\n" +
                        "\t\t<td>" + map.getDescription() + "</td>\n" +
                        "\t</tr>\n");
            }
        }

    }

    /**
     * check if the resource is a collection or dataset and append the dataset(s)
     * @param resource
     */
    private void appendExpeditionOrDatasetData(metadataElement resource) {
        ResourceTypes rts = new ResourceTypes();
        ResourceType rt = rts.get(resource.getValue());

        // check if the resource is a dataset or a collection
        if (rts.get(1).equals(rt)) {
            appendDataset();
        } else if (rts.get(38).equals(rt)) {
            appendExpeditionDatasets();
        }
    }

    private void appendExpeditionDatasets() {
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        if (displayDatasets()) {
            outputSB.append(expeditionMinter.listExpeditionDatasetsAsTable(resolver.getExpeditionId()));
        }
    }

    private void appendDataset() {
        if (displayDatasets()) {
            String rootName = sm.retrieveValue("rootName");
            String projectId = resolver.getProjectID(resolver.getBcidId());
            String graph = resolver.graph;

            outputSB.append("<table>\n");
            outputSB.append("\t<tr>\n");
            outputSB.append("\t\t<th>Download:</th>\n");
            // Excel option
            outputSB.append("\t\t<th>");
            outputSB.append("<a href='");
            outputSB.append(rootName);
            outputSB.append("query/excel?graphs=");
            outputSB.append(graph);
            outputSB.append("&projectId=");
            outputSB.append(projectId);
            outputSB.append("'>.xlsx</a>");

            outputSB.append("&nbsp;&nbsp;");

            // TAB delimited option
            outputSB.append("<a href='");
            outputSB.append(rootName);
            outputSB.append("query/tab?graphs=");
            outputSB.append(graph);
            outputSB.append("&projectId=");
            outputSB.append(projectId);
            outputSB.append("'>.txt</a>");

            outputSB.append("&nbsp;&nbsp;");

            // n3 option
            outputSB.append("<a href='");
            outputSB.append(bcid.getWebAddress());
            outputSB.append("'>n3</a>");

            outputSB.append("&nbsp;&nbsp;");
            outputSB.append("&nbsp;&nbsp;");

            outputSB.append("\t\t</td>");
            outputSB.append("\t</tr>\n");
            outputSB.append("</table>\n");
        }
    }

    private Boolean displayDatasets() {
        Boolean ignore_user = Boolean.getBoolean(sm.retrieveValue("ignore_user"));
        Integer projectId = Integer.parseInt(resolver.getProjectID(resolver.getBcidId()));
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();

        //if public expedition, return true
        if (expeditionMinter.isPublic(resolver.getExpeditionCode(), projectId)) {
            return true;
        }
        // if ignore_user and user in project, return true
        else if (ignore_user && expeditionMinter.userExistsInProject(userId, projectId)) {
            return true;
        }
        // if !ignore_user and userOwnsExpedition, return true
        else if (!ignore_user && expeditionMinter.userOwnsExpedition(userId, resolver.getExpeditionCode(), projectId)) {
            return true;
        }

        return false;
    }
}
