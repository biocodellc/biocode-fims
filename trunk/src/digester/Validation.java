package digester;

import reader.plugins.TabularDataReader;
import renderers.Message;
import renderers.RendererInterface;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Validation implements ValidationInterface, RendererInterface {
    private final LinkedList<Worksheet> worksheets = new LinkedList<Worksheet>();
    private final LinkedList<List> lists = new LinkedList<List>();
    private TabularDataReader tabularDataReader = null;

    public Validation(TabularDataReader tabularDataReader) {
        this.tabularDataReader = tabularDataReader;
    }

    public TabularDataReader getTabularDataReader() {
        return tabularDataReader;
    }

    /**
     * Add a worksheet to the validation component
     *
     * @param w
     */
    public void addWorksheet(Worksheet w) {
        worksheets.addLast(w);
    }

    /**
     * Add a list to the validation component
     *
     * @param l
     */
    public void addList(List l) {
        lists.addLast(l);
    }

    /**
     * Get the set of lists defined by this validation object
     *
     * @return
     */
    public LinkedList<List> getLists() {
        return lists;
    }

    /**
     * Lookup a list by its alias
     *
     * @param alias
     * @return
     */
    public List findList(String alias) {
        for (Iterator<List> i = lists.iterator(); i.hasNext(); ) {
            List l = i.next();
            if (l.getAlias().equals(alias))
                return l;
        }
        return null;
    }

    public void print() {
        for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
            Worksheet w = i.next();
            w.print();
        }
    }


    /**
     * Print Command lists all messages
     */
    public void printCommand() {
        for (Iterator<Worksheet> w = worksheets.iterator(); w.hasNext(); ) {
            Worksheet worksheet = w.next();
            System.out.println("\tProcessing sheet = " + worksheet.getSheetname());
            for (Iterator<Message> m = worksheet.getMessages().iterator(); m.hasNext(); ) {
                Message message = m.next();
                System.out.println("\t\t" + message.print());
            }
        }
    }

    public void run(Object parent) {
        for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
            Worksheet w = i.next();
            w.run(this);
        }
    }
}
