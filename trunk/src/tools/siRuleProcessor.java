package tools;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    String otherColumn = null;
    LinkedList values = null;

    static ArrayList<String> ruleTypes = new ArrayList<String>();
    ArrayList<String> rules = new ArrayList<String>();

    public siRuleProcessor(String jsonInput, String column) throws Exception {
        ruleTypes.add("controlledVocabulary");
        ruleTypes.add("requiredValueFromOtherColumn");
        ruleTypes.add("minimumMaximumNumberCheck");
        ruleTypes.add("uniqueValue");
        ruleTypes.add("isNumber");


        this.jsonInput = jsonInput;
        this.column = column;

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
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
                // If it is not a linkedlist then it is just a single rule
            } else {
                Map json = (Map) obj;
                individualRuleProcessor(json);
            }

        } catch (ParseException pe) {
            throw new Exception("unabled to parse JSON" + jsonInput);
        }
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
                this.column = value;
            } else if (key.equalsIgnoreCase("list")) {
                this.list = value;
            } else if (key.equalsIgnoreCase("level")) {
                this.level = value;
            } else if (key.equalsIgnoreCase("otherColumn")) {
                this.otherColumn = value;
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
        sbOutput.append(" type='" + type + "'");
        if (column != null)
            sbOutput.append(" column='" + column + "'");
        if (list != null)
            sbOutput.append(" list='" + list + "'");
        if (level != null)
            sbOutput.append(" level='" + level + "'");
        if (otherColumn != null)
            sbOutput.append(" otherColumn='" + otherColumn + "'");

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
     * Print the results
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
        siRuleProcessor test = new siRuleProcessor("{\"type\":\"minimumMaximumNumberCheck\",\"column\":\"minimumDepthInMeters,maximumDepthInMeters\",\"level\":\"error\"}","foo");

        System.out.println(test.print());

        //System.out.println(test.print());
    }
}
