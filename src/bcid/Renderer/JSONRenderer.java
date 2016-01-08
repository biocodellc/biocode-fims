package bcid.Renderer;

import bcid.*;
import biocode.fims.SettingsManager;
import org.json.simple.JSONObject;

/**
 * jsonRenderer renders objects as JSON
 */
public class JSONRenderer extends Renderer {
    private JSONObject json;
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
    public JSONRenderer(String username, Resolver resolver) {
        Database db = new Database();
        userId = db.getUserId(username);
        this.resolver = resolver;
    }

    public JSONRenderer() {

    }

    public void enter() {
        json = new JSONObject();
    }

    public void printMetadata() {
        appender(identifier);
        appender(resource);
        appender(about);
        appender(dcMediator);
        appender(dcHasVersion);
        appender(dcIsReferencedBy);
        appender(dcRights);
        appender(dcIsPartOf);
        appender(dcDate);
        appender(dcCreator);
        appender(dcTitle);
        appender(dcSource);
        appender(bscSuffixPassthrough);
        appender(dcPublisher);
        appender(forwardingResolution);
        appender(resolutionTarget);
        appendExpeditionOrDatasetData(resource);
    }

    public void leave() {
        outputSB.append(json.toJSONString());
    }

    public boolean validIdentifier()  {
        if (this.bcid == null) {
            outputSB.append("{\"Identifier\":{\"status\":\"not found\"}}");
            return false;
        } else {
            return true;
        }
    }

    private void appender(metadataElement map) {
        JSONObject metadataElement = new JSONObject();
        metadataElement.put("description", map.getDescription());
        metadataElement.put("value", map.getValue());
        metadataElement.put("shortValue", map.getShortValue());
        metadataElement.put("fullKey", map.getFullKey());
        json.put(map.getKey(), metadataElement);
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
            json.put("datasets", expeditionMinter.getExpeditionsDatasets(resolver.getExpeditionId()));
        }
    }

    private void appendDataset() {
        if (displayDatasets()) {
            JSONObject download = new JSONObject();
            String rootName = sm.retrieveValue("rootName");
            String projectId = resolver.getProjectID(resolver.getBcidId());
            String graph = resolver.graph;

            // Excel option
            download.put("excel", rootName + "query/excel?graphs=" + graph + "&projectId=" + projectId);

            // TAB delimited option
            download.put("tab", rootName + "query/tab?graphs=" + graph + "&projectId=" + projectId);

            // n3 option
            download.put("n3", bcid.getWebAddress());

            json.put("download", download);
        }
    }

    private Boolean displayDatasets() {
        Boolean ignore_user = Boolean.getBoolean(sm.retrieveValue("ignore_user"));
        Integer projectId = Integer.parseInt(resolver.getProjectID(resolver.getBcidId()));
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();

        //if public expedition, return true
        if (expeditionMinter.isPublic(resolver.getExpeditionCode(), projectId)) {
            return true;
        } else if (userId != null) {
            // if ignore_user and user in project, return true
            if (ignore_user && expeditionMinter.userExistsInProject(userId, projectId)) {
                return true;
            }
            // if !ignore_user and userOwnsExpedition, return true
            else if (!ignore_user && expeditionMinter.userOwnsExpedition(userId, resolver.getExpeditionCode(), projectId)) {
                return true;
            }
        }

        return false;
    }

}
