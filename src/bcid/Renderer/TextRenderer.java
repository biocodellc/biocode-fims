package bcid.Renderer;

import java.util.Iterator;
import java.util.Map;

/**
 * textRenderer renders object results as Text
 */
public class TextRenderer extends Renderer {


    public void enter() {
        outputSB.append("***" + this.bcid.getClass().getSimpleName() + "***\n");
    }

    public void printMetadata() {
        Iterator iterator = this.bcid.getMetadata().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pairs = (Map.Entry) iterator.next();
            outputSB.append(pairs.getKey() + "=" + pairs.getValue() + "\n");
        }
    }

    public void leave() {
    }

    public boolean validIdentifier() {
        if (this.bcid == null) {
            outputSB.append("bcid is null");
            return false;
        } else {
            return true;
        }

    }
}
