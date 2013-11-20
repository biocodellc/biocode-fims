package geneious.plugin;

import com.biomatters.geneious.publicapi.plugin.Options;

import java.io.File;
import java.net.URL;

public class FIMSUploadOptions extends Options {

    StringOption projectCodeOption;
    FileSelectionOption sampleDataOption;
    FileSelectionOption configOption;
    //BooleanOption triplifyOption;
    BooleanOption uploadOption;
    //BooleanOption exportOption;

    public FIMSUploadOptions() {
        super(FIMSUploadOptions.class);
        projectCodeOption = addStringOption("projectCode", "Biocode FIMS Project Code:", "DEMOH");
        sampleDataOption = addFileSelectionOption("sampleData", "Sample Data:", "");
        //triplifyOption = addBooleanOption("triplify", "Triplify", true);
        uploadOption = addBooleanOption("upload", "Upload", false);
        //exportOption = addBooleanOption("export", "Export to spreadsheet", false);

        /*
        String defaultConfigPath = "sampledata/indoPacificConfiguration_v2.xml";
        URL resource = getClass().getResource("indoPacificConfiguration_v2.xml");
        if(resource != null) {
            File configFile = new File(resource.getFile().replace("%20", " "));
            if(configFile.exists()) {
                defaultConfigPath = configFile.getAbsolutePath();
            }
        }
        configOption = addFileSelectionOption("configFile", "Configuration File:", defaultConfigPath);
        */
        //configOption.setAdvanced(true);
    }
}
