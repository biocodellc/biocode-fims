package geneious.plugin;

import com.biomatters.geneious.publicapi.plugin.Options;

public class FIMSUploadOptions extends Options {

    StringOption projectCodeOption;
    FileSelectionOption sampleDataOption;
    FileSelectionOption configOption;
    FileSelectionOption outputFolderOption;
    BooleanOption triplifyOption;
    BooleanOption uploadOption;
    BooleanOption exportOption;

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);
        projectCodeOption = addStringOption("projectCode", "Project Code:", "DEMOH");
        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");
        triplifyOption = addBooleanOption("triplify", "Triplify", true);
        uploadOption = addBooleanOption("upload", "Upload", false);
        exportOption = addBooleanOption("export", "Export to spreadsheet", false);

        configOption = addFileSelectionOption("configFile", "Configuration File:", "../../sampledata/indoPacificConfiguration_v2.xml");
        configOption.setAdvanced(true);

        outputFolderOption = addFileSelectionOption("outputFolder", "Output Folder:", "../../tripleOutput");
        outputFolderOption.setAdvanced(true);
    }
}
