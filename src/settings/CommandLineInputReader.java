package settings;

import java.io.*;

/**
 * Convenience class to read input during program execution.  Useful only for command-prompt application scenarios
 */
public class CommandLineInputReader {

    public String getResponse() throws IOException {
        //  open up standard input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        //  read the response from the command-line; need to use try/catch with the
        return br.readLine();
    }

    public static void main(String[] args) {
        //  prompt the user to enter their name
        fimsPrinter.out.print("Enter your name: ");
        try {
            fimsPrinter.out.println("Thanks for the name, " + new CommandLineInputReader().getResponse());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

