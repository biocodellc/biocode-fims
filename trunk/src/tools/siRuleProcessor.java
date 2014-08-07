package tools;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * Process JSON snippets under the Global Validation Rule column for SI columns
 * The purpose of this class is to convert the JSON to an appropriate rule like:
 * <p/>
 * <rule type='minimumMaximumNumberCheck' column='minimumDepthInMeters,maximumDepthInMeters'
 * level='error'></rule>
 */
public class siRuleProcessor {
    String jsonInput;
    String level = "warning";
    String type = null;
    String column = null;
    String list = null;
    String value = null;
    String otherColumn = null;
    LinkedList values = null;
    TreeMap treeMap = null;

    static ArrayList<String> ruleTypes = new ArrayList<String>();
    ArrayList<String> rules = new ArrayList<String>();

    public siRuleProcessor(String jsonInput, String column, TreeMap treeMap) throws Exception {
        ruleTypes.add("controlledVocabulary");
        ruleTypes.add("requiredValueFromOtherColumn");
        ruleTypes.add("minimumMaximumNumberCheck");
        ruleTypes.add("uniqueValue");
        ruleTypes.add("isNumber");
        ruleTypes.add("validateNumeric");

        this.jsonInput = jsonInput;
        this.column = column;
        this.treeMap = treeMap;

        // Parse the JSON
        JSONParser parser = new JSONParser();
        ContainerFactory containerFactory = new ContainerFactory() {
            public List creatArrayContainer() {
                return new LinkedList();
            }

            public Map createObjectContainer() {
                return new LinkedHashMap();
            }
        };

        try {
            Object obj = parser.parse(jsonInput, containerFactory);
            // If a linked list then we want to loop multiple rules that were passed in
            if (obj.getClass() == LinkedList.class) {
                LinkedList jsonList = (LinkedList) obj;
                Iterator it = jsonList.iterator();
                while (it.hasNext()) {
                    Map json = (Map) it.next();
                    try {
                        individualRuleProcessor(json);
                        resetVals();
                    } catch (Exception e) {
                        //e.printStackTrace();
                        System.err.println(e.getMessage());
                    }
                }
                // If it is not a linkedlist then it is just a single rule
            } else {
                Map json = (Map) obj;
                individualRuleProcessor(json);
                resetVals();
            }

        } catch (ParseException pe) {
            throw new Exception("unabled to parse JSON" + jsonInput);
        }
    }

    /**
     * Reset some global vars
     */
    private void resetVals() {
        this.list = null;
        this.level = "warning"; // reseting to default of warning
        this.otherColumn = null;
        this.value = null;
        this.values = null;
    }

    /**
     * Build the elements of the rule by looking at the JSON
     *
     * @param json
     *
     * @throws Exception
     */
    private void individualRuleProcessor(Map json) throws Exception {
        // Iterate the JSON responses
        Iterator iter = json.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();

            if (key.equalsIgnoreCase("type")) {
                if (!ruleTypes.contains(value)) {
                    throw new Exception("Unrecognized ruletype " + value);
                } else {
                    this.type = value;
                }
            } else if (key.equalsIgnoreCase("column")) {
                //System.out.println("compare this: " + column + ":" + value);
                this.column = value;
            } else if (key.equalsIgnoreCase("list")) {
                this.list = value;
            } else if (key.equalsIgnoreCase("level")) {
                this.level = value;
            } else if (key.equalsIgnoreCase("otherColumn")) {
                this.otherColumn = value;
            } else if (key.equalsIgnoreCase("value")) {
                this.value = value;
            } else if (key.equalsIgnoreCase("values")) {
                this.values = (LinkedList) entry.getValue();
            }


        }
        if (type == null || type.equalsIgnoreCase("null") || type.equalsIgnoreCase("")) {
            throw new Exception("No rule type specified");
        }
        constructRule();
    }

    /**
     * Construct the rule output
     *
     * @throws Exception
     */
    private void constructRule() throws Exception {
        StringBuilder sbOutput = new StringBuilder();
        // prevent returning null type rules
        if (type == null) {
            return;
        }
        sbOutput.append("\t\t<rule");
        // Convert the controlledVocabulary RuleType to FIMS syntax
        //if (type.equalsIgnoreCase("controlledVocabulary")) {
        //    type = "checkInXMLFields";
        //}
        sbOutput.append(" type='" + type + "'");
        if (column != null)
            sbOutput.append(" column='" + columnMapper(column) + "'");
        if (list != null) {
            //sbOutput.append(" list='" + columnMapper(list) + "'");
            sbOutput.append(" list='" + list + "'");
        }
        if (level != null)
            sbOutput.append(" level='" + level + "'");
        if (value != null)
            // Must encode the value field since it probably contains special characters
            sbOutput.append(" value='" + URLEncoder.encode(value, "utf-8") + "'");
        if (otherColumn != null)
            sbOutput.append(" otherColumn='" + columnMapper(otherColumn) + "'");

        sbOutput.append(">");
        if (values != null) {
            sbOutput.append("\n");
            Iterator it = values.iterator();
            while (it.hasNext()) {
                sbOutput.append("\t\t\t<field>" + it.next().toString() + "</field>\n");
            }
            sbOutput.append("\t\t");
        }

        sbOutput.append("</rule>\n");

        rules.add(sbOutput.toString());
    }

    /**
     * Map the Vernacular column to the primary field name
     *
     * @param value
     *
     * @return
     */
    private String columnMapper(String value) {

        String mappedVal = null;
        try {
            mappedVal = treeMap.get(value).toString();
        } catch (NullPointerException e) {
            return value;
        }
        if (mappedVal != null)
            return mappedVal;
        else
            return value;
    }


    /**
     * Print the results
     *
     * @return
     */
    public String print() {
        StringBuilder output = new StringBuilder();
        // Loop through the rules that we found
        Iterator it = rules.iterator();
        while (it.hasNext()) {
            output.append(it.next().toString());
        }
        return output.toString();
    }

    public static void main(String[] args) throws Exception {
        //siRuleProcessor test = new siRuleProcessor("{\"type\":\"controlledVocabulary\",\"list\":\"softParts\"}", "testColumn");
        //siRuleProcessor test = new siRuleProcessor("[{\"type\":\"controlledVocabulary\",\"list\":\"geneticSampleTypePrimary\"},{\"type\":\"requiredValueFromOtherColumn\",\"otherColumn\":\"basisOfRecord\",\"values\":[\"Genetic Sample\"]}]", "geneticSampleTypePrimary");
        //siRuleProcessor test = new siRuleProcessor("{\"type\":\"requiredValueFromOtherColumn\",\"otherColumn\":\"basisOfRecord\",\"values\":[\"Genetic Sample\"]}","geneticSampleTypePrimary");
        siRuleProcessor test = new siRuleProcessor("{\"type\":\"minimumMaximumNumberCheck\",\"column\":\"minimumDepthInMeters,maximumDepthInMeters\",\"level\":\"error\"}", "foo", null);

        System.out.println(test.print());

        //System.out.println(test.print());
    }
}

