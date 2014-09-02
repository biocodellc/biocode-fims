package renderers;

import ch.lambdaj.group.Group;

import java.util.List;

/**
 * Generic class to handle messages
 */
public class Message {
    protected String message;
    protected Integer level;
    protected   String groupMessage;
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

    public Message(String message, Integer level, String groupMessage) {
        this.message = message;
        this.level = level;
        this.groupMessage = groupMessage;
    }

    public   String getGroupMessage() {
        return groupMessage;
    }

    public String getMessage() {
        return message;
    }

    /**
     * @return Message for this line
     */
   public String print() {

        // Check that there is stuff in this list
       /* String listString = "";
        if (list != null)
            listString = " " + list.toString();
        */
        return getLevelAsString() + ": " + message + " ("+groupMessage + ")";

    }
}
