package digester;

import java.util.ArrayList;

/**
 * A list of data to use in the validator.  We store data in lists because we find that different rules can refer
 * to the same list, and so we need only define them once.
 */
public class List {
    private String alias;
    private java.util.List fields = new ArrayList();

    /**
     * return the alias for which this list is known
     *
     * @return
     */
    public String getAlias() {
        return alias;
    }

    /**
     * set the alias by which this list is known
     *
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Add a field that belongs to this list
     *
     * @param field
     */
    public void addField(String field) {
        fields.add(field);
    }

    /**
     * Get the set of fields that belong to this list
     *
     * @return
     */
    public java.util.List getFields() {
        return fields;
    }

    public void print() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void run(Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
