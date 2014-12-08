package settings;


/**
 * Send output to fimsPrinter.out (for command-line applications)
 */
public class standardPrinter extends fimsPrinter {
    public void print(String content) {
        System.out.print(content);
    }

    public void println(String content) {
        System.out.println(content);
    }
}
