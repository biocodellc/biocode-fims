package renderers;

/**
 * Handle messaging for
 */
public class RowMessage extends Message {
    private Integer row;

    public RowMessage(String message, Integer level) {
        this(message, level, null);
    }

    public RowMessage(String message, Integer level, Integer row) {
        this(message, null, level, row);
    }

    public RowMessage(String message, java.util.List list, Integer level, Integer row) {
        this.message = message;
        this.row = row;
        this.list = list;
        this.level = level;
    }

    /**
     * @return Message for this line
     */
    public String print() {

        // Check that there is stuff in this list
        String listString = "";
        if (list != null)
            listString = list.toString();
        Integer msgRow = this.row + 1;
        return getLevelAsString() + ": Row " + (msgRow).toString() + ": " + message + " " + listString;
    }


    public void setRow(Integer row) {
        this.row = row;
    }
}

