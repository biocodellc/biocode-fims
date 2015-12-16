package bcid.Renderer;

import bcid.GenericIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

/**
 * jsonRenderer renders objects as JSON
 */
public class RedirectRenderer extends TextRenderer {

    private static Logger logger = LoggerFactory.getLogger(RedirectRenderer.class);

    public void enter(GenericIdentifier identifier) {
    }

    public void printMetadata(GenericIdentifier identifier) {
        Iterator iterator = identifier.getMetadata().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            outputSB.append("\"" + pairs.getKey() + "\":\"" + pairs.getValue() + "\"");
            if (iterator.hasNext()) {
                outputSB.append(",");
            }
        }
        try {
            outputSB.append(identifier.getWebAddress());
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }
    }

    public void leave(GenericIdentifier identifier) {
    }

    public boolean validIdentifier(GenericIdentifier identifier)  {
        return true;
    }

}
