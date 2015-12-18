package bcid.Renderer;

import bcid.BCIDMetadataSchema;
import bcid.bcid;

/**
 * Abstract class Renderer implements the visitor methods
 * and controls all renderer subClasses for rendering bcids
 */
public abstract class Renderer extends BCIDMetadataSchema implements RendererInterface {
    protected StringBuilder outputSB;

    /**
     * render an Identifier
     *
     * @return
     */
    public String render(bcid bcid) {
        BCIDMetadataInit(bcid);
        outputSB = new StringBuilder();

        if (validIdentifier()) {
            enter();
            printMetadata();
            leave();
            return outputSB.toString();
        } else {
            return outputSB.toString();
        }
    }
}
