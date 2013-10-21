package digester;

import java.util.Iterator;

/**
 * The column class stores attributes of each column that we know about on a spreadsheet.   These are typically
 * defined by the worksheet class.
 */
public class Column_trash {
    String uri;
    String name;

    public Column_trash() {

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
        System.out.println("      name : " + name + ", uri : " + uri);
    }
}
