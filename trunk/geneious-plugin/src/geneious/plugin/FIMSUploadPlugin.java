package geneious.plugin;

import com.biomatters.geneious.publicapi.plugin.DocumentOperation;
import com.biomatters.geneious.publicapi.plugin.GeneiousPlugin;

/**
 * @author Matthew Cheung
 */
public class FIMSUploadPlugin extends GeneiousPlugin {
    @Override
    public String getName() {
        return "FIMS Upload";
    }

    @Override
    public String getDescription() {
        return "Validate, Triplify and Upload Biocode FIMS data";
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getAuthors() {
        return "Mathew Cheung";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public String getMinimumApiVersion() {
        return "4.0";
    }

    @Override
    public int getMaximumApiVersion() {
        return 4;
    }

    @Override
    public DocumentOperation[] getDocumentOperations() {
        return new DocumentOperation[]{new FIMSUploadOperation()};
    }
}
