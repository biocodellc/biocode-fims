package geneious.plugin;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.DocumentUtilities;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.FileUtilities;
import jebl.util.ProgressListener;
import run.process;
import settings.FIMSException;
import settings.fimsInputter;
import settings.fimsPrinter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

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
        final StringBuilder log = new StringBuilder();

        fimsPrinter.out = new fimsPrinter() {

            private int linesToKeep = 20;
            LinkedList<String> message = new LinkedList<String>();

            @Override
            public void print(String content) {
                log.append(content);
                message.set(message.size() - 1, message.getLast() + content);
                setMessage();
            }

            @Override
            public void println(String content) {
                log.append(content).append("\n");
                if (message.size() < linesToKeep) {
                    message.addFirst(content);
                } else {
                    message.removeLast();
                    message.addFirst(content);
                }

                setMessage();
            }

            private void setMessage() {
                StringBuilder text = new StringBuilder();

                for (int i = message.size() - 1; i >= 0; i--) {
                    if (i != 0) {
                        text.append("<font color=\"gray\">");
                    }
                    text.append(message.get(i)).append("\n");
                    if (i != 0) {
                        text.append("</font>");
                    }
                }
                progressListener.setMessage("<html>" + text.toString() + "</html>");
            }
        };

        fimsInputter.in = new fimsInputter() {
            @Override
            public boolean continueOperation(String question) {
                question = question.replace("Warning:", "<b>Warning</b>:");
                log.append("\n\n").append(question).append("\n\n");
                return Dialogs.showYesNoDialog("<html>" + question + "\nContinue?</html>", "Continue?", null, Dialogs.DialogIcon.QUESTION);
            }
        };

        if (options instanceof FIMSUploadOptions) {
            FIMSUploadOptions uploadOptions = (FIMSUploadOptions) options;
            String project_code = uploadOptions.projectCodeOption.getValue();
            String sampleDataFile = uploadOptions.sampleDataOption.getValue();
            String username = uploadOptions.usernameOption.getValue();
            String password = uploadOptions.passwordOption.getValue();
            boolean upload = uploadOptions.uploadOption.getValue();
            // For the plugin we probably never need to write directly back out to a spreadsheet, this is used for testing
            boolean export = false;

            // We always want to triplify if we upload.  By the same token we don't need to triplify if we're not uploading
            boolean triplify = upload;

            File tempDir = null;
            try {
                tempDir = FileUtilities.createTempDir(true);
            } catch (IOException e) {
                throw new DocumentOperationException("Failed to create temp output directory: " + e.getMessage(), e);
            }
            String outputFolder = tempDir.getAbsolutePath();

             // Structure fileName for the sample data
            String fileName = sampleDataFile;
            if (fileName.endsWith(File.separator)) {
                fileName = fileName.substring(0, fileName.length() - 2);
            }
            if (fileName.contains(File.separator)) {
                fileName = sampleDataFile.substring(sampleDataFile.lastIndexOf(File.separator) + 1);
            }

            // Run the process
            try {
                process process = new process(sampleDataFile, outputFolder, project_code, export, triplify, upload, username, password);
                process.runAll();
            } catch (FIMSException e) {
                //Dialogs.showTextFieldDialog("Error Message", e.getMessage().toString(), null, null);
                StringWriter stacktrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stacktrace));
                String logText = "Initialization Error: " + e.getMessage();
                logText += "<br><br><strong>Details</strong>:<br>" + stacktrace;
                return DocumentUtilities.createAnnotatedPluginDocuments(new LogDocument("FIMS Upload of " + fileName, logText));
            }

            return DocumentUtilities.createAnnotatedPluginDocuments(new LogDocument("FIMS Upload of " + fileName, log.toString()));
        } else {
            throw new IllegalStateException("Bad options");
        }
    }

    public static void displayExceptionDialog(Exception exception) {
        displayExceptionDialog("Error", exception.getMessage(), exception, null);
    }

    public static void displayExceptionDialog(String title, String message, Exception exception, Component owner) {
        StringWriter stacktrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stacktrace));
        Dialogs.DialogOptions dialogOptions = new Dialogs.DialogOptions(Dialogs.OK_ONLY, title, owner, Dialogs.DialogIcon.WARNING);
        dialogOptions.setMoreOptionsButtonText("Show details...", "Hide details...");
        Dialogs.showMoreOptionsDialog(dialogOptions, message, stacktrace.toString());
    }
}
