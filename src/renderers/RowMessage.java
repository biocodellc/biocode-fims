package renderers;

/**
 * Handle messaging for
 */
public class RowMessage extends Message implements Comparable {
    private Integer row;

    public RowMessage(String message, String groupMessage, Integer level) {
        this(message, groupMessage, level, null);
    }

    /*public RowMessage(String message, Integer level, Integer row) {
        this(message, null, level, row);
    } */

    public RowMessage(String message, String groupMessage, Integer level, Integer row) {
        this.message = message;
        this.row = row;
        if (groupMessage == null) {
            this.groupMessage = this.level + ": Unclassified message";
        } else {
            this.groupMessage = groupMessage;
        }

        this.level = level;
    }

    public String getGroupMessageAsString() {
        return "<div id='groupMessage' class='" + getLevelAsString()+ "'>" + this.getLevelAsString() + ": " + groupMessage + "</div>";

    }

    /**
     * @return Message for this line
     */
    public String print() {
        if (this.row != null) {
            return "Row " + this.row + ": " + message;
        } else {
            return message;
        }

    }


    public void setRow(Integer row) {
        this.row = row;
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

