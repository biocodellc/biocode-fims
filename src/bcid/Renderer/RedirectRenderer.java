package bcid.Renderer;

import bcid.Bcid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * jsonRenderer renders objects as JSON
 */
public class RedirectRenderer extends TextRenderer {

    private static Logger logger = LoggerFactory.getLogger(RedirectRenderer.class);

    public void enter(Bcid bcid) {
    }

    public void printMetadata(Bcid bcid) {
        Iterator iterator = bcid.getMetadata().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            outputSB.append("\"" + pairs.getKey() + "\":\"" + pairs.getValue() + "\"");
            if (iterator.hasNext()) {
                outputSB.append(",");
            }
        }
//        try {
            outputSB.append(bcid.getWebAddress());
//        } catch (URISyntaxException e) {
//            TODO should we silence this exception?
//            logger.warn("URISyntaxException thrown", e);
//        }
    }

    public void leave(Bcid bcid) {
    }

    public boolean validIdentifier(Bcid bcid)  {
        return true;
    }

}
