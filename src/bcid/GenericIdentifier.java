package bcid;

import util.SettingsManager;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The Identifier class encapsulates all types of identifiers that we deal with in the BCID system
 */
public abstract class GenericIdentifier implements GenericIdentifierInterface {

    /* The identifier itself */
    public URI identifier;
    SettingsManager sm;

    public String rights;
    public String resolverTargetPrefix;
    public String resolverMetadataPrefix;

    protected GenericIdentifier() {
         // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();

        rights = sm.retrieveValue("rights");
        resolverTargetPrefix = sm.retrieveValue("resolverTargetPrefix");
        resolverMetadataPrefix = sm.retrieveValue("resolverMetadataPrefix");
    }

    /**
     * The resolution target for this identifier
     *
     * @return
     * @throws java.net.URISyntaxException
     */
    public URI getResolutionTarget() throws URISyntaxException {
        return new URI(resolverTargetPrefix);
    }

    /**
     * The metadata target for this identifier
     *
     * @return
     * @throws java.net.URISyntaxException
     */
    public URI getMetadataTarget() throws URISyntaxException {
        return new URI(resolverMetadataPrefix);
    }

    public String getRights() {
        return rights;
    }
}
