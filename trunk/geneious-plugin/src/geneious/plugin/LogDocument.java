package geneious.plugin;

import com.biomatters.geneious.publicapi.documents.*;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holds the log file for an upload attempt
 *
 * @author Matthew Cheung
 */
public class LogDocument implements PluginDocument {
    String name;
    String logText;

    public LogDocument(String name, String logText) {
        this.name = name;
        this.logText = logText;
    }

    public LogDocument() {
        // For de-serialization by Geneious
    }

    @Override
    public List<DocumentField> getDisplayableFields() {
        return null;
    }

    @Override
    public Object getFieldValue(String s) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URN getURN() {
        return null;
    }

    @Override
    public Date getCreationDate() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String toHTML() {
        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<h2>").append(name).append("</h2>");
        // Initialization errors have to do with expedition codes, login names, and general environment setup
        if (logText.contains("Initialization Error:")) {
            builder.append("<b><font color=\"red\">Unable to initialize FIMS, see below for messages.</font></b><br><br>");
            builder.append(logText);
        // Validation errors have to do with errors on the spreadsheet itself
        } else if (logText.contains("Error:")) {
            builder.append("<b><font color=\"red\">Validation failed, see below for errors.</font></b><br><br>");
            builder.append(logText.replace("\n", "<br>").replace("Error:", "<b><font color=\"red\">Error</font></b>:"));
        } else {
            builder.append("<b><font color=\"green\">Validation succeeded.</font></b><br><br>");
            builder.append(logText.replace("\n", "<br>").replace("Warning:", "<b><font color=\"#FFA500\">Warning</font></b>:"));
        }
        builder.append("</html>");
        return builder.toString();
    }

    private static final String NAME_KEY = "name";
    private static final String LOG_KEY = "logText";

    @Override
    public Element toXML() {
        Element element = new Element(XMLSerializable.ROOT_ELEMENT_NAME);
        element.addContent(new Element(NAME_KEY).setText(name));
        element.addContent(new Element(LOG_KEY).setText(logText));
        return element;
    }

    @Override
    public void fromXML(Element element) throws XMLSerializationException {
        name = element.getChildText(NAME_KEY);
        logText = element.getChildText(LOG_KEY);
    }
}
