
package ezid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * An EZIDServiceRequest request represents the data needed for a single request
 * to the EZID identifier service packaged as a Runnable task that can be executed
 * within a thread pool, typically provided by an Executor service.  The request  
 * is used within a queue to temporarily store requests before they are processed 
 * by the EZID service. EZIDServiceRequests are created only by the EZIDClient,
 * which provides methods for external applications to generate requests.
 * 
 * @author Matthew Jones, NCEAS, UC Santa Barbara
 */
public class EZIDServiceRequest implements Runnable
{    
    public static final int CREATE = 1;
    public static final int SETMETADATA = 2;
    public static final int DELETE = 3;

    private EZIDService ezid = null;
    private int method;
    private String identifier;
    private HashMap<String, String> metadata = null;
    
    protected static Logger log = LoggerFactory.getLogger(EZIDServiceRequest.class);

    protected EZIDServiceRequest(EZIDService ezid, int method, String identifier) {
        if (ezid == null) {
            throw new IllegalArgumentException("EZIDService argument must not be null.");
        }
        if (method < 1 || method > 3) {
            throw new IllegalArgumentException("Service must be an interger value between 1 and 4.");
        }
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier must not be null.");
        }
        this.ezid = ezid;
        this.method = method;
        this.identifier = identifier;
    }
    
    protected EZIDServiceRequest(EZIDService ezid, int method, String identifier, HashMap<String, String> metadata) {
        this(ezid, method, identifier);
        this.metadata = metadata;
    }

    public void run() {
        log.info("Service to execute: " + method + "/" + identifier + "/" + metadata);
        try {
            switch (method) {
            case CREATE:
                String newID = ezid.createIdentifier(identifier, metadata);
                log.info("Completed CREATE request for: " + identifier);
                break;
            case SETMETADATA:
                ezid.setMetadata(identifier, metadata);
                log.info("Completed SETMETADATA request for: " + identifier);
                break;
            case DELETE:
                ezid.deleteIdentifier(identifier);
                log.info("Completed DELETE request for: " + identifier);
                break;
            }
        } catch (EZIDException e) {
            log.info("FAILED Request " + method + " for: " + identifier + ". " + e.getMessage());
        }
    }
    
    
    /**
     * @return the method
     */
    public int getMethod() {
        return method;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return the metadata
     */
    public HashMap<String, String> getMetadata() {
        return metadata;
    }
}