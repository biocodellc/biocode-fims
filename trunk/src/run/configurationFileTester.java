package run;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test the Configuration File... these tests can be Extremely important and ensure that all things work
 * correctly downstream, especially during the triplification step.
 */
public class configurationFileTester {
    StringBuilder errorMessages = null;

    /**
     * Constructor for class, storing messages in the errorMessages class variable
     */
    public configurationFileTester() {
        errorMessages = new StringBuilder();
    }

    /**
     * Test that the configuration file is OK!
     *
     * @return
     */
    public boolean testConfigFile(File fileToTest) throws configurationFileError {

        DocumentBuilder builder = null;

        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            builder = factory.newDocumentBuilder();

            builder.setErrorHandler(new configurationFileErrorHandler());

        } catch (ParserConfigurationException e) {
            throw new configurationFileError(e.getMessage() + " for " + fileToTest.getAbsoluteFile());
        }

        // A simple first check that the document is valid
        try {
            document = builder.parse(new InputSource(fileToTest.getAbsoluteFile().toString()));
        } catch (IOException e) {
            throw new configurationFileError(e.getMessage() + " for " + fileToTest.getAbsoluteFile());
        } catch (SAXException e) {
            throw new configurationFileError("Bad formatting for " + fileToTest.getAbsoluteFile());
        }

        // Loop Rules
        NodeList rules = document.getElementsByTagName("rule");
        ArrayList<String> uniqueKeys = getUniqueValueRules(rules);

        // Loop Entities
        NodeList entities = document.getElementsByTagName("entity");
        // atLeastOneUniqueKey
        boolean atLeastOneUniqueKeyFound = false;
        // Loop Entities
        for (int i = 0; i < entities.getLength(); i++) {
            NamedNodeMap entityAttributes = entities.item(i).getAttributes();

            // Check worksheetUniqueKeys
            String worksheetUniqueKey = entityAttributes.getNamedItem("worksheetUniqueKey").getNodeValue();

            if (uniqueKeys.contains(worksheetUniqueKey)) {
                atLeastOneUniqueKeyFound = true;
            }

            // construct a List to hold URI values to check they are unique within the node
            List<String> uriList = new ArrayList<String>();

            // Check the attributes for this entity
            List entityChildren = elements(entities.item(i));
            for (int j = 0; j < entityChildren.size(); j++) {
                Element attributeElement = (Element) entityChildren.get(j);

                if (attributeElement != null) { //&& node.getNodeName().equals("attribute")) {

                    NamedNodeMap attributes = attributeElement.getAttributes();//.getNamedItem("uri");

                    // Check the URI field by populating the uriList with values
                    String uri = null;
                    try {
                        uri = attributes.getNamedItem("uri").getNodeValue().toString();
                    } catch (NullPointerException e) {
                    }
                    if (uri != null) {
                        uriList.add(uri);
                    }

                    String column = null;
                    try {
                        column = attributes.getNamedItem("column").getNodeValue();
                    } catch (NullPointerException e) {
                    }
                    if (column != null) {
                        checkSpecialCharacters(column);
                    }
                }

            }
            // Run the URI list unique Value check for each entity
            checkUniqueValuesInList("URI attribute value", uriList);
        }

        // Tell is if atLeastOneUniqueKey is not found.
        if (!atLeastOneUniqueKeyFound) {
            errorMessages.append("\tMust define a at least one Entity worksheetUniqueKey that has a uniqueValue rule\n");
        }


        // Send out errorMessages
        if (errorMessages.length() > 0) {
            throw new configurationFileError(errorMessages.toString());
        }
        return true;
    }

    /**
     * Construct a list of elements given a parent node, ensuring that only element children are returned.
     *
     * @param parent
     *
     * @return
     */
    private List<Element> elements(Node parent) {
        List<Element> result = new LinkedList<Element>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
                result.add((Element) nl.item(i));
        }
        return result;
    }

    /**
     * Check that a particular list has unique values
     *
     * @param message
     * @param list
     */
    private void checkUniqueValuesInList(String message, List<String> list) {
        Set<String> uniqueSet = new HashSet<String>(list);
        for (String temp : uniqueSet) {
            Integer count = Collections.frequency(list, temp);
            if (count > 1) {
                errorMessages.append("\t" + message + " " + temp + " used more than once in list\n");
            }
        }
    }

    /**
     * Check for special characters in a string
     *
     * @param stringToCheck
     */
    private void checkSpecialCharacters(String stringToCheck) {
        // Check worksheetUniqueKeys
        //String column = attributeAttributes.getNamedItem("column").getNodeValue();
        Pattern p = Pattern.compile("[^a-z0-9 \\(\\)\\/\\_-]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(stringToCheck);
        if (m.find()) {
            errorMessages.append("\tColumn attribute value " + stringToCheck + " contains an invalid character\n");
        }
    }

    /**
     * Return an ArrayList of rules that have unique values
     *
     * @param rules
     *
     * @return
     */
    private ArrayList<String> getUniqueValueRules(NodeList rules) {
        ArrayList<String> keys = new ArrayList<String>();
        int length = rules.getLength();
        Node[] copy = new Node[length];

        for (int n = 0; n < length; ++n) {
            NamedNodeMap attributes = rules.item(n).getAttributes();
            // Search all rules type=uniqueValue
            if (attributes.getNamedItem("type").getNodeValue().equals("uniqueValue")) {
                // Get the column name on this key
                String columnName = attributes.getNamedItem("column").getNodeValue();
                keys.add(columnName);
            }
        }

        return keys;
    }

    public static void main(String[] args) {
        String output_directory = System.getProperty("user.dir") + File.separator + "sampledata" + File.separator;

        // Check ACTIVE Project configuration files
        Integer projects[] = {1,3,4,5,8,9,10,11,12,22};

        for (int i = 0; i < projects.length; i++) {
            int project_id = projects[i];
            System.out.println("Configuration File Testing For Project = " + project_id);
            try {
                configurationFileTester cFT = new configurationFileTester();
                File file = new configurationFileFetcher(project_id, output_directory, true).getOutputFile();
                cFT.testConfigFile(file);
            } catch (configurationFileError e) {
                System.out.println("Configuration File Construction Error Messages: \n" + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        // Check for well-formedness -- this one passes
        /*
        configurationFileTester cFT = new configurationFileTester();

        try {
            cFT.testConfigFile(new File(output_directory + "testConfiguration1.xml"));
        } catch (configurationFileError e) {
            System.out.println("Configuration File Construction Error Messages: \n" + e.getMessage());
        }

        // Check for unique keys
        try {
            cFT.testConfigFile(new File(output_directory + "testConfiguration2.xml"));
        } catch (configurationFileError e) {
            System.out.println("Configuration File Construction Error Messages: \n" + e.getMessage());
        }
        */


    }
}
