package geneious.plugin;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.ThreadUtilities;
import digester.*;
import jebl.util.ProgressListener;
import org.apache.commons.digester3.Digester;
import org.virion.jam.util.SimpleListener;
import org.xml.sax.SAXException;
import reader.ReaderManager;
import reader.plugins.TabularDataReader;
import renderers.Message;
import settings.CommandLineInputReader;
import settings.fimsPrinter;
import triplify.triplifier;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Matthew Cheung
 */
public class FIMSUploadOperation extends DocumentOperation {

    @Override
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("Upload to Biocode FIMS").setMainMenuLocation(GeneiousActionOptions.MainMenu.Tools);
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[0];
    }

    public static final String CODE = "projectCode";
    public static final String DATA = "sampleData";
    public static final String CONFIG = "configurationFile";
    public static final String TRIPLIFY = "triplify";
    public static final String UPLOAD = "upload";
    public static final String EXPORT = "export";

    @Override
    public Options getOptions(AnnotatedPluginDocument... annotatedPluginDocuments) throws DocumentOperationException {
        return new FIMSUploadOptions();
    }

    @Override
    public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] annotatedPluginDocuments, final ProgressListener progressListener, Options options) throws DocumentOperationException {

        fimsPrinter.out = new fimsPrinter() {

            private int linesToKeep = 80;
            LinkedList<String> message = new LinkedList<String>();

            @Override
            public void print(String content) {
                message.set(message.size()-1, message.getLast() + content);
                setMessage();
            }

            @Override
            public void println(String content) {
                if(message.size() < linesToKeep) {
                    message.addFirst(content);
                } else {
                    message.removeLast();
                    message.addFirst(content);
                }

                setMessage();
            }

            private void setMessage() {
                StringBuilder text = new StringBuilder();

                for (int i=message.size()-1; i>=0; i--) {
                    if(i != 0) {
                        text.append("<font color=\"gray\">");
                    }
                    text.append(message.get(i)).append("\n");
                    if(i != 0) {
                        text.append("</font>");
                    }
                }
                progressListener.setMessage("<html>" + text.toString() + "</html>");
            }
        };
        if(options instanceof FIMSUploadOptions) {
            FIMSUploadOptions uploadOptions = (FIMSUploadOptions)options;
            String project_code = uploadOptions.projectCodeOption.getValue();
            String outputFolder = uploadOptions.outputFolderOption.getValue();
            String configFile = uploadOptions.configOption.getValue();
            boolean triplify = uploadOptions.triplifyOption.getValue();

            boolean validationGood = true;
            boolean triplifyGood = true;
            boolean updateGood = true;
            Validation validation = null;
            try {
                // Read the input file & create the ReaderManager and load the plugins.
                ReaderManager rm = new ReaderManager();
                rm.loadReaders();
                TabularDataReader tdr = rm.openFile(uploadOptions.sampleDataOption.getValue());
                // TODO: find a way to set the active sheet programitcally, probably by reading the validation worksheet template and using that value, for now HARDCODING THIs value
                //tdr.setTable("Samples");

                // Validation
                validation = new Validation();
                addValidationRules(new Digester(), validation, configFile);
                validation.run(tdr, uploadOptions.projectCodeOption.getValue() + "_output", outputFolder);
//            validationGood = validation.printMessages();
                validationGood = checkValidation(validation);

                // Triplify if we validate
                if (triplify & validationGood) {
                    Mapping mapping = new Mapping();
                    addMappingRules(new Digester(), mapping, configFile);
                    triplifyGood = mapping.run(validation, new triplifier(project_code + "_output", outputFolder), project_code);
                    mapping.print();

                    // Upload after triplifying
                    if (uploadOptions.uploadOption.getValue() & triplifyGood) {
                        Fims fims = new Fims(mapping);
                        addFimsRules(new Digester(), fims, configFile);
                        fims.run();
                        fims.print();
                        if (uploadOptions.exportOption.getValue())
                            System.out.println("\tspreadsheet = " + fims.write());
                    }
                }

            } catch (Exception e) {
                throw new DocumentOperationException(e);
            } finally {
                if(validation != null)
                    validation.close();
            }
        } else {
            throw new IllegalStateException("Bad options");
        }
        progressListener.setIndeterminateProgress();
        fimsPrinter.out.println("Complete!");
        final AtomicBoolean keepProgressUp = new AtomicBoolean(true);
        progressListener.addFeedbackAction("Dismiss", new SimpleListener() {
            @Override
            public void objectChanged() {
                keepProgressUp.set(false);
            }
        });
        while(keepProgressUp.get()) {
            ThreadUtilities.sleep(1000);
        }
        return Collections.emptyList();
    }

    private boolean checkValidation(Validation validation) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        for (Iterator<Worksheet> w = validation.getWorksheets().iterator(); w.hasNext(); ) {
            Worksheet worksheet = w.next();
            System.out.println("\t" + worksheet.getSheetname() + " worksheet results");
            for (String msg : worksheet.getUniqueMessages(Message.ERROR)) {
                errors.append(msg).append("\n");
            }
            for (String msg : worksheet.getUniqueMessages(Message.WARNING)) {
                warnings.append(msg).append("\n");
            }
            // Worksheet has errors
            if (!worksheet.errorFree()) {
                Dialogs.showMessageDialog("Errors found on " + worksheet.getSheetname() + " worksheet.  Must fix to continue."
                , "Errors Found", null, Dialogs.DialogIcon.INFORMATION);
                return false;
            } else {
                // Worksheet has no errors but does have some warnings
                if (!worksheet.warningFree()) {
                    return Dialogs.showYesNoDialog("<html>Warnings found on " + worksheet.getSheetname() + " worksheet.\n" +
                            "\n" + warnings.toString().replace("Warning:", "<b>Warning</b>:") + "\n" +
                            "Do you wish to continue loading with warnings?</html>", "Warnings Found", null, Dialogs.DialogIcon.QUESTION);
                } else {
                    //Worksheet has no errors or warnings
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Process validation component rules
     *
     * @param d
     */
    private void addValidationRules(Digester d, Validation validation, String configFilename) throws IOException, SAXException {
        d.push(validation);

        // Create worksheet objects
        d.addObjectCreate("fims/validation/worksheet", Worksheet.class);
        d.addSetProperties("fims/validation/worksheet");
        d.addSetNext("fims/validation/worksheet", "addWorksheet");

        // Create rule objects
        d.addObjectCreate("fims/validation/worksheet/rule", Rule.class);
        d.addSetProperties("fims/validation/worksheet/rule");
        d.addSetNext("fims/validation/worksheet/rule", "addRule");
        d.addCallMethod("fims/validation/worksheet/rule/field", "addField", 0);

        // Create list objects
        d.addObjectCreate("fims/validation/lists/list", digester.List.class);
        d.addSetProperties("fims/validation/lists/list");
        d.addSetNext("fims/validation/lists/list", "addList");
        d.addCallMethod("fims/validation/lists/list/field", "addField", 0);

        // Create column objects
        d.addObjectCreate("fims/validation/worksheet/column", Column_trash.class);
        d.addSetProperties("fims/validation/worksheet/column");
        d.addSetNext("fims/validation/worksheet/column", "addColumn");

        d.parse(new File(configFilename));
    }

    /**
     * Process mapping component rules
     *
     * @param d
     */
    private void addMappingRules(Digester d, Mapping mapping, String configFilename) throws IOException, SAXException {
        d.push(mapping);

        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Add attributes associated with this entity
        d.addObjectCreate("fims/mapping/entity/attribute", Attribute.class);
        d.addSetProperties("fims/mapping/entity/attribute");
        d.addSetNext("fims/mapping/entity/attribute", "addAttribute");

        // Create relation objects
        d.addObjectCreate("fims/mapping/relation", Relation.class);
        d.addSetNext("fims/mapping/relation", "addRelation");
        d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);
        d.addCallMethod("fims/mapping/relation/predicate", "addPredicate", 0);
        d.addCallMethod("fims/mapping/relation/object", "addObject", 0);

        d.parse(new File(configFilename));
    }

    /**
     * Process metadata component rules
     *
     * @param d
     */
    private void addFimsRules(Digester d, Fims fims, String configFilename) throws IOException, SAXException {
        d.push(fims);
        d.addObjectCreate("fims/metadata", Metadata.class);
        d.addSetProperties("fims/metadata");
        d.addCallMethod("fims/metadata", "addText_abstract", 0);
        d.addSetNext("fims/metadata", "addMetadata");

        d.parse(new File(configFilename));
    }
}
