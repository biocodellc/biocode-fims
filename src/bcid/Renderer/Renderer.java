package bcid.Renderer;

import bcid.BCIDMetadataSchema;
import bcid.GenericIdentifier;

/**
 * Abstract class Renderer implements the visitor methods
 * and controls all renderer subClasses for rendering bcids
 */
public abstract class Renderer extends BCIDMetadataSchema implements RendererInterface {
    protected StringBuilder outputSB;

    GenericIdentifier identifier = null;

    /**
     * render an Identifier
     *
     * @return
     */
    public String render(GenericIdentifier identifier) {
        this.identifier = identifier;

        BCIDMetadataInit(identifier);
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
