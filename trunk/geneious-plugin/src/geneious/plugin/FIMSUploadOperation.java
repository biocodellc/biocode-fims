package geneious.plugin;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.ThreadUtilities;
import jebl.util.ProgressListener;
import org.virion.jam.util.SimpleListener;
import run.process;
import settings.fimsInputter;
import settings.fimsPrinter;

import java.util.Collections;
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

        fimsInputter.in = new fimsInputter() {
            @Override
            public boolean continueOperation(String question) {
                question = "<html>" + question.replace("Warning:", "<b>Warning</b>:") + "</html>";
                return Dialogs.showYesNoDialog(question + "\nContinue?", "Continue?", null, Dialogs.DialogIcon.QUESTION);
            }
        };

        if(options instanceof FIMSUploadOptions) {
            FIMSUploadOptions uploadOptions = (FIMSUploadOptions)options;
            String project_code = uploadOptions.projectCodeOption.getValue();
            String sampleDataFile = uploadOptions.sampleDataOption.getValue();
            String outputFolder = uploadOptions.outputFolderOption.getValue();
            String configFile = uploadOptions.configOption.getValue();
            boolean upload = uploadOptions.uploadOption.getValue();
            boolean export = uploadOptions.exportOption.getValue();
            boolean triplify = uploadOptions.triplifyOption.getValue();
            process process = new process(configFile, sampleDataFile, outputFolder, project_code, export, triplify, upload);
            try {
                process.runAll();
            } catch (Exception e) {
                throw new DocumentOperationException("FIMS operation failed", e);
            }

        } else {
            throw new IllegalStateException("Bad options");
        }
        progressListener.setProgress(0.99);
        fimsPrinter.out.println("Complete!");
        final AtomicBoolean keepProgressUp = new AtomicBoolean(true);
        progressListener.addFeedbackAction("Dismiss", new SimpleListener() {
            @Override
            public void objectChanged() {
                keepProgressUp.set(false);
            }
        });
        while(keepProgressUp.get() || progressListener.isCanceled()) {
            ThreadUtilities.sleep(1000);
        }
        return Collections.emptyList();
    }


}
