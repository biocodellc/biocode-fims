package settings;


import digester.Rule;

/**
 * df
 * Special class to handle messages for reading Spreadsheet files
 */
public class Message {
    public String message;
    public Rule r;
    protected int row;

    public Integer getRow() {
        return this.row + 1;
    }

    private String getRowMessage() {
        String msg = "";
        if (this.row > 0) {
            Integer msgRow = this.row + 1;
            msg = "Row: " + (msgRow).toString() + ": ";
        }
        return msg;
    }

    /**
     * @return Message for this line
     */
    public String getLineMessage() {
        return getRowMessage() + " " + message;
    }
}

