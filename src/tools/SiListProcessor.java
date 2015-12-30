package tools;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;


/**
 * Parse the SI generated LIST elements which are stored as a triple, like:
 * {listName}|{department}|{value}
 */
public class SiListProcessor {
    private File outputDirectory, listsFile;
    private String delimiter = "\\|";
    private HashMap<String, String> departmentLookup = new HashMap<String, String>();

    private static Logger logger = LoggerFactory.getLogger(SiListProcessor.class);

    public SiListProcessor() {
        outputDirectory = new File(System.getProperty("user.dir") + System.getProperty("file.separator") +
                "Documents" + System.getProperty("file.separator") +
                "Smithsonian" + System.getProperty("file.separator"));

        listsFile = new File(outputDirectory.getAbsolutePath() + System.getProperty("file.separator") + "si_lookups.txt");

        departmentLookup.put("Mammals", "SIVZM");
        departmentLookup.put("Fishes", "SIVZF");
        departmentLookup.put("Mineral Sciences", "SIMIN");
        departmentLookup.put("Birds", "SIVZB");
        departmentLookup.put("Invertebrate Zoology", "SIINV");
        departmentLookup.put("Amphibians & Reptiles", "SIVZA");
        departmentLookup.put("Entomology", "SIENT");
        departmentLookup.put("Botany", "SIBOT");
    }

    /**
     * Return an iterator over distinct departments
     *
     * @return
     */
    public Iterator distinctDepartment() {
        Set<String> department = new HashSet<String>();

        String line;
        FileReader fr;

       /* try {
            fr = new FileReader(listsFile);
        } catch (FileNotFoundException e) {
            throw new FimsRuntimeException(500, e);
        } */

        // Use encoding
        BufferedReader br = encodedBR(listsFile, "UTF8");

        try {
            while ((line = br.readLine()) != null) {
                // Create a list element holding each of the values
                listElement elem = new listElement(line);
                department.add(elem.getDepartment());
            }
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.warn("IOException", e);
            }
        }
        return department.iterator();
    }

    /**
     * Method for reading files which skips Byte Order Mark (BOM). This shows up in some files and screws some
     * things up so we generally don't want it.
     *
     * @param file
     * @param encoding
     *
     * @return
     */
    public BufferedReader encodedBR(File file, String encoding) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new FimsRuntimeException(500, e);
        }
        // Skip BOM indicated by false in constructor
        BOMInputStream bomIn = new BOMInputStream(is, false);
        return new BufferedReader(new InputStreamReader(bomIn));
    }

    /**
     * Accept either the list specific department name or the abbreviation code for the department
     * and return the department name
     *
     * @return
     */
    private String mapDepartment(String name) {
        Iterator it = departmentLookup.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (name.trim().equalsIgnoreCase(pairs.getKey().toString()) ||
                    name.trim().equalsIgnoreCase(pairs.getValue().toString())) {
                return pairs.getKey().toString();
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return null;
    }

    /**
     * Loop each Department for the input file
     *
     * @param department
     *
     * @return
     */
    public MultiHashMap loopDepartment(String department) {
        department = mapDepartment(department);

        MultiMap departmentMap = new MultiHashMap();
        String line;

        // Loop the Lists file
        FileReader fr = null;
        /*(try {
            fr = new FileReader(listsFile);
        } catch (FileNotFoundException e) {
            throw new FimsRuntimeException(500, e);
        }
        BufferedReader br = new BufferedReader(fr);
        */
        // Use encoding
        BufferedReader br = encodedBR(listsFile, "UTF8");


        try {
            while ((line = br.readLine()) != null) {
                // Create a list element holding each of the values
                listElement elem = new listElement(line);
                // Search for our department or the default ALL departments
                if (elem.getDepartment().equals(department) ||
                        elem.getDepartment().equals("")) {
                    departmentMap.put(elem.getListName(), elem.getValue());
                }
            }
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.warn("IOException", e);
            }
        }

        // Return the lists for this department
        return (MultiHashMap) departmentMap;
    }

    /**
     * Print a multiHashMap Departement list
     *
     * @param departmentMap
     *
     * @return
     */
    public StringBuilder printList(MultiHashMap departmentMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("\t<lists>\n");

        Set set = departmentMap.entrySet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            sb.append("\t\t<list alias='" + entry.getKey() + "' caseInsensitive='true'>\n");
            List list = (java.util.List) entry.getValue();
            Iterator listIt = list.iterator();
            while (listIt.hasNext()) {
                sb.append("\t\t\t<field><![CDATA[" + listIt.next() + "]]></field>\n");
            }
            sb.append("\t\t</list>\n");
        }
        sb.append("\t</lists>\n");

        return sb;
    }

    public static void main(String[] args) {
        SiListProcessor t = new SiListProcessor();
        StringBuilder sb = t.printList(t.loopDepartment("Mineral Sciences"));
        System.out.println(sb.toString());

        //System.out.println(t.mapDepartment("Entomology"));
    }

    /**
     * Class to hold the listElement values (each of the triples)
     */
    class listElement {
        private String listName;
        private String department;
        private String value;

        /**
         * Take in a line argument and populate class variables
         *
         * @param line
         */
        public listElement(String line) {
            String[] elements = line.split(delimiter);
            this.listName = elements[0];
            this.department = elements[1];
            // 3rd element often empty, just call it an empty value
            try {
                this.value = elements[2];
            } catch (Exception e2) {
                this.value = "";
                //System.out.println(line);
            }
        }

        public String getListName() {
            return listName;
        }

        public String getDepartment() {
            return department;
        }

        public String getValue() {
            return value;
        }
    }
}
