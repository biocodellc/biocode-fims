package digester;

import settings.FimsPrinter;

/**
 * The column class stores attributes of each column that we know about on a spreadsheet.   These are typically
 * defined by the worksheet class.
 */
public class ColumnTrash {
    String uri;
    String name;

    public ColumnTrash() {

    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void print() {
        FimsPrinter.out.println("      name : " + name + ", uri : " + uri);
    }
}
