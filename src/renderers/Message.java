package renderers;

import java.util.List;

/**
 * Generic class to handle messages
 */
public class Message {
    protected String message;
    protected Integer level;
    protected java.util.List list;
    public static final Integer WARNING = 0;
    public static final Integer ERROR = 1;

    public String getLevelAsString() {
        if (level == 0) return "Warning";
        else return "Error";
    }

    public Integer getLevel() {
        return level;
    }

    public Message() {
    }

    public Message(String message, Integer level, List list) {
        this.message = message;
        this.level = level;
        this.list = list;
    }

    public List getList() {
        return list;
    }

    public String getMessage() {
        return message;
    }

    /**
     * @return Message for this line
     */
    public String print() {

        // Check that there is stuff in this list
        String listString = "";
        if (list != null)
            listString = " " + list.toString();

        return getLevelAsString() + ": " + message + listString;
    }
}
