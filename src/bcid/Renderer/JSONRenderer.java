package bcid.Renderer;

import java.util.Iterator;
import java.util.Map;

/**
 * jsonRenderer renders objects as JSON
 */
public class JSONRenderer extends TextRenderer {

    public void enter() {
        outputSB.append("{");
        outputSB.append("\"" + this.bcid.getClass().getSimpleName() + "\":");
        outputSB.append("{");
    }

    public void printMetadata() {
        Iterator iterator = this.bcid.getMetadata().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            outputSB.append("\"" + pairs.getKey() + "\":\"" + pairs.getValue() + "\"");
            if (iterator.hasNext()) {
                outputSB.append(",");
            }
        }
    }

    public void leave() {
        outputSB.append("}}");
    }

    public boolean validIdentifier()  {
        if (this.bcid == null) {
            outputSB.append("{\"Identifier\":{\"status\":\"not found\"}}");
            return false;
        } else {
            return true;
        }
    }

}
