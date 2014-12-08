package settings;

import org.json.simple.JSONObject;

/**
 * an exception class to wrap exceptions thrown from the bcid system
 */
public class BCIDConnectorException  extends FIMSRuntimeException {

    public BCIDConnectorException(JSONObject response) {
        super(response);
    }

    public BCIDConnectorException(Integer httpStatusCode, Throwable cause) {
        super(httpStatusCode, cause);
    }
}
