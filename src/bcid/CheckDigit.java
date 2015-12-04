package bcid;

/**
 * The Check Digit algorithm is adapted from http://en.wikipedia.org/wiki/Luhn_mod_N_algorithm
 * <p>
 * This Class assumes use of Base64 encoding values
 */
public class CheckDigit {

    // Replicate base64 encoding Characters
    static protected char[] chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_="
                    .toCharArray();

    // Lookup table for converting base64 characters to value in range 0..63
    static protected byte[] codes = new byte[256];
    static {
        for (int i = 0; i < 256; i++) codes[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++) codes[i] = (byte) (i - 'A');
        for (int i = 'a'; i <= 'z'; i++) codes[i] = (byte) (26 + i - 'a');
        for (int i = '0'; i <= '9'; i++) codes[i] = (byte) (52 + i - '0');
        codes['-'] = 62;
        codes['_'] = 63;
    }
    private int NumberOfValidInputCharacters(String input) {
        return input.length();
    }

    private int CodePointFromCharacter(char character) {
        return codes[character];
    }

    private char CharacterFromCodePoint(int codePoint) {
        return chars[codePoint];
    }

    /**
     * Generate the Check Character given a string
     * @param input
     * @return
     */
    private char GenerateCheckCharacter(String input) {
        int factor = 2;
        int sum = 0;
        int n = NumberOfValidInputCharacters(input);

        // Starting from the right and working leftwards is easier since
        // the initial "factor" will always be "2"
        for (int i = input.length() - 1; i >= 0; i--) {
            int codePoint = CodePointFromCharacter(input.charAt(i));
            int addend = factor * codePoint;

            // Alternate the "factor" that each "codePoint" is multiplied by
            factor = (factor == 2) ? 1 : 2;

            // Sum the digits of the "addend" as expressed in base "n"
            addend = (addend / n) + (addend % n);
            sum += addend;
        }

        // Calculate the number that must be added to the "sum"
        // to make it divisible by "n"
        int remainder = sum % n;
        int checkCodePoint = (n - remainder) % n;

        return CharacterFromCodePoint(checkCodePoint);
    }

    /**
     * Pass in a string and return the string with a check character on the end.
     * @param input
     * @return A String with the check character on the end.
     */
    public String generate(String input) {
        return input + GenerateCheckCharacter(input);
    }

    /**
     * Examine the string and see if it validates
     * @param input
     * @return true/false if this verifies
     */
    public boolean verify(String input) {
        if (input.length() > 30)  {
            return false;
            //throw new Exception("Unable to decode string due to length, possibly this is a UUID?  Unable to validate");
        }

        int factor = 1;
        int sum = 0;
        int n = NumberOfValidInputCharacters(input) - 1;

        // Starting from the right, work leftwards
        // Now, the initial "factor" will always be "1"
        // since the last character is the check character
        for (int i = input.length() - 1; i >= 0; i--) {
            int codePoint = CodePointFromCharacter(input.charAt(i));
            int addend = factor * codePoint;

            // Alternate the "factor" that each "codePoint" is multiplied by
            factor = (factor == 2) ? 1 : 2;

            // Sum the digits of the "addend" as expressed in base "n"
            addend = (addend / n) + (addend % n);
            sum += addend;
        }

        int remainder = sum % n;

        return (remainder == 0);
    }

    /**
     * Strip the last character from the String, which is the checkDigit
     * @param digits
     * @return a String without the last character (the check digit)
     */
    public String getCheckDigit(String digits) {
        return digits.substring(0, digits.length() -1);
    }

}
