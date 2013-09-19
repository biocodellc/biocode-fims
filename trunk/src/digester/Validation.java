package digester;

import reader.plugins.TabularDataReader;
import renderers.Message;
import renderers.RendererInterface;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Validation implements RendererInterface {
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

    /**
     * Loop through worksheets and print out object data
     */
    public void printObject() {
        System.out.println("Validate");

        for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
            Worksheet w = i.next();
            w.print();
        }
    }

    /**
     * Print output for the commandline
     */
    public void print() {
        System.out.println("Validate ...");

        for (Iterator<Worksheet> w = worksheets.iterator(); w.hasNext(); ) {
            Worksheet worksheet = w.next();
            System.out.println("\t" + worksheet.getSheetname() + " worksheet");
            for (Iterator<Message> m = worksheet.getMessages().iterator(); m.hasNext(); ) {
                Message message = m.next();
                System.out.println("\t\t" + message.print());
            }
            if (!worksheet.errorFree())  {
                System.out.println("\tErrors found on " + worksheet.getSheetname()+ " worksheet.  Must fix to continue.");
            }
        }

    }

    public boolean run() throws Exception {
        boolean errorFree = true;
        for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
            digester.Worksheet w = i.next();
            boolean thisError = w.run(this);
            if (errorFree)
                errorFree = thisError;
        }
        return errorFree;
    }
}
