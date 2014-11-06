package run;

import unit_tests.SIConfigurationFileTester;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Manage configuration file messages, so we can display them easily
 */
public class configurationFileErrorMessager extends ArrayList {
    ArrayList<message> messages = new ArrayList<message>();

    public void add(SIConfigurationFileTester tester, String message, String validationName) {
        messages.add(new message(tester, message, validationName));
    }

    /**
     * print all the messages
     * @return
     */
    public String printMessages() {
        Iterator it  = messages.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            message m = (message) it.next();
            sb.append(m.getFullMessage());
        }
        return sb.toString();
    }
    /**
     * Hold message contents
      */
    class message {
        SIConfigurationFileTester tester;
        String message;
        String validationName;

        message(SIConfigurationFileTester tester, String message, String validationName) {
            this.tester = tester;
            this.message = message;
            this.validationName = validationName;
        }

        public String getFile() {
            return tester.fileToTest.getName();
        }
        public String getValidationName() {
             return validationName;
        }
        public String getMessage() {
            return message;
        }
        public String getFullMessage() {
            return getFile() + ";" + getValidationName() + ";message = " + getMessage() + "\n";
        }
    }
}
