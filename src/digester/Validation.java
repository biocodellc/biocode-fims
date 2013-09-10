package digester;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Validation {
     private final LinkedList<Worksheet> worksheets = new LinkedList<Worksheet>();

    public void addWorksheet( Worksheet w )
    {
        worksheets.addLast( w );
    }

    public void print()
    {
        System.out.println( "Validation has " + worksheets.size() + " entries" );

        for ( Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); )
        {
            Worksheet w = i.next();
            w.print();
        }
    }
}
