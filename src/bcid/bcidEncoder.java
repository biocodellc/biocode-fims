package bcid;

import com.ibm.icu.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * The purpsose of the bcidEncoder is to encode very short identifiers for EZID shoulders to represent
 * unique bcids.  These encoded numbers correspond to integers in the database and
 * conform to the EZID shoulder specification, namely letters up to and including the first
 * digit. E.g. aB1 or abcdefg1
 * <p/>
 * The system here uses successive combinations of 51 letters (ommitting O)
 * plus a spot for a digit at the end, currently only encoding the digit "2"
 * If we have 3 letters in the shoulder including 1 digit this gives 1,217,727 possible permutations.
 * 4 letters and 1 digit will give 74,549,800 possible permutations.
 */
public class bcidEncoder extends bcid implements encoder {
    private boolean debug = false;
    int[] endDigits = {1};

    private static Logger logger = LoggerFactory.getLogger(bcidEncoder.class);

    /**
     * Tell if DEBUG mode is on or off
     *
     * @return
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set DEBUG mode on or off, default is off
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Encode a bcid value
     *
     * @param i pass in a BigInteger
     * @return returns the encoded String
     */
    public String encode(BigInteger i) {

        // First spot is test bcid!
        if (i.intValue() == 1) {
            return "fk4";
        }

        String results = "";
        int largeNumber = i.intValue();
        // Calculate the number of characters are required to encode this number
        int numCharactersPositionstoEncode = numCharactersPositionstoEncode(largeNumber);

        if (debug) {
            int possAtCharPosition = optionsAtPosition(numCharactersPositionstoEncode);
            int sumPossAtPreviousCharPositions = optionsAtPreviousPositions(numCharactersPositionstoEncode);
            int totalPossibilities = sumPossAtPreviousCharPositions + possAtCharPosition;
            System.out.println("Integer to encode=" + i);
            System.out.println("# Character positions required to encode = " + numCharactersPositionstoEncode);
            System.out.println("# Possibilities for last set: " + possAtCharPosition);
            System.out.println("# Possibilities for 0 to (n-1) positions: " + sumPossAtPreviousCharPositions);
            System.out.println("# Total possibilities: " + totalPossibilities);
        }

        // The selector tells us where to start our counts for character insertions
        int selectorForCurrentPosition = largeNumber - optionsAtPreviousPositions(numCharactersPositionstoEncode);

        // Fill characters by looping each position
        // We run the loop from the last position going to the first position, calculating possibilities at each position
        // The characters are appended from first to last (opposite of our probability calculation)
        // This gives an encoding output of: aaa, aab, aac, aba, abb, abc, etc...
        for (int k = numCharactersPositionstoEncode; k > 0; k--) {
            // Calculate the number of possibilities in this position, which is
            Double possibilitiesPerIncrement = new Double(optionsAtPosition(k) / this.chars.length);
            // The position indicates how far down the character stack we search to fill in the appropriate character
            int position = ceilingAsArrayPosition(selectorForCurrentPosition / possibilitiesPerIncrement);
            // Start appending character to output
            try {
                results += this.chars[position];
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.warn("Increment = " + i + ", position = " + position + ", selector=" + selectorForCurrentPosition + ", possperincrement =" + possibilitiesPerIncrement);
            }

            // Update selector for current Position
            selectorForCurrentPosition = ((Double) (selectorForCurrentPosition - (possibilitiesPerIncrement * position))).intValue();
        }

        return results + "2";
    }


    /**
     * Decode a string for the shoulderEncoder
     *
     * @param entireString
     * @return BigInteger representation of this code
     */
    public BigInteger decode(String entireString) {
        String shoulder = null;
        String naan = null;
        String scheme = null;

        // Check to see if this is an entire ARK.  If so, we only want the second section, the shoulder itself
        if (entireString.contains("/")) {
            String[] strArray = entireString.split("/");
            if (strArray.length > 2) {
                scheme = strArray[0];
                naan = strArray[1];
                shoulder = strArray[2];

                // Look and see if this is the test bcid
                try {
                    if (naan.equals("99999") && shoulder.equals("fk4")) {
                        return new BigInteger("1");
                    }
                } catch (NullPointerException e) {
                    // do nothing we're OK
                }
            } else  {
               return null;
            }
        } else {
            shoulder = entireString;
        }

        // Convert the shoulder/text representation to a BigInteger
        int numCharactersPositionstoDecode = shoulder.length() - 1;
        Integer decodedInt = 0;
        int j = numCharactersPositionstoDecode;
        // Loop each position and sum the character position * possibilities at each increment
        for (int i = 0; i < numCharactersPositionstoDecode; i++) {
            // Calculate the number of possibilities in this position
            Double possibilitiesPerIncrement = new Double(optionsAtPosition(j) / this.chars.length);
            // Add to the decodedInt value:  numeric position of character * the number of possibilities at this increment
            decodedInt += this.codes[shoulder.charAt(i)] * possibilitiesPerIncrement.intValue();

            j--;
        }
        return new BigInteger(decodedInt.toString());
    }

    /**
     * Sum the total of all possible options at previous positions
     *
     * @param numCharactersPositionstoEncode
     * @return
     */
    private int optionsAtPreviousPositions(int numCharactersPositionstoEncode) {
        int optionsAtPreviousPositions = 0;
        for (int i = numCharactersPositionstoEncode - 1; i > 0; i--) {
            optionsAtPreviousPositions += Double.valueOf(Math.pow(chars.length, i)).intValue();
        }
        return optionsAtPreviousPositions;
    }

    /**
     * Calculate the number of possibilities at a particular position
     *
     * @param numCharactersPositionstoEncode
     * @return
     */
    private int optionsAtPosition(int numCharactersPositionstoEncode) {
        return Double.valueOf(Math.pow(chars.length, numCharactersPositionstoEncode)).intValue();
    }

    /**
     * Calculate the number of character positions needed
     *
     * @param i
     * @return
     */
    private int numCharactersPositionstoEncode(int i) {
        // Fetch number of character Positions needed for this encoding
        int numCharactersPositionstoEncode = 0;
        int selector = 1;
        int sumPreviousPossibilities = 0;
        while (numCharactersPositionstoEncode < selector) {
            int possibilities = optionsAtPosition(selector);
            // Add in # possibilities from previous position
            sumPreviousPossibilities += possibilities;
            if (i <= sumPreviousPossibilities) {
                numCharactersPositionstoEncode = selector;
            } else {
                selector++;
            }
        }
        return numCharactersPositionstoEncode;

    }

    /**
     * An Excel-like Ceiling function, returning integer versions of double values attached to the next highest number,
     * so 1.11, 1.2, 1.5, 1.6 all become 2
     * 1.0 stays as 1
     * This is returned as array position so we can use directly with mapping encoded characters to their position in the array
     *
     * @param position
     * @return
     */
    private int ceilingAsArrayPosition(Double position) {
        BigDecimal bd = new BigDecimal(Double.toString(position));
        return bd.setScale(0, BigDecimal.ROUND_CEILING).intValue() - 1;
    }

    /** No digits used and eliminate uppercase O, not to confuse with Zero (0), this scheme used to construct shoulders for the EZID system, characters used for encoding are ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz **/
    static public char[] chars =
            "ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                    .toCharArray();


    /** Lookup table for converting shoulder characters to values **/
    static public byte[] codes = new byte[256];

    static {
        int i = 1;
        codes['A'] = (byte) (i++);
        codes['B'] = (byte) (i++);
        codes['C'] = (byte) (i++);
        codes['D'] = (byte) (i++);
        codes['E'] = (byte) (i++);
        codes['F'] = (byte) (i++);
        codes['G'] = (byte) (i++);
        codes['H'] = (byte) (i++);
        codes['I'] = (byte) (i++);
        codes['J'] = (byte) (i++);
        codes['K'] = (byte) (i++);
        codes['L'] = (byte) (i++);
        codes['M'] = (byte) (i++);
        codes['N'] = (byte) (i++);
        codes['P'] = (byte) (i++);
        codes['Q'] = (byte) (i++);
        codes['R'] = (byte) (i++);
        codes['S'] = (byte) (i++);
        codes['T'] = (byte) (i++);
        codes['U'] = (byte) (i++);
        codes['V'] = (byte) (i++);
        codes['W'] = (byte) (i++);
        codes['X'] = (byte) (i++);
        codes['Y'] = (byte) (i++);
        codes['Z'] = (byte) (i++);
        codes['a'] = (byte) (i++);
        codes['b'] = (byte) (i++);
        codes['c'] = (byte) (i++);
        codes['d'] = (byte) (i++);
        codes['e'] = (byte) (i++);
        codes['f'] = (byte) (i++);
        codes['g'] = (byte) (i++);
        codes['h'] = (byte) (i++);
        codes['i'] = (byte) (i++);
        codes['j'] = (byte) (i++);
        codes['k'] = (byte) (i++);
        codes['l'] = (byte) (i++);
        codes['m'] = (byte) (i++);
        codes['n'] = (byte) (i++);
        codes['o'] = (byte) (i++);
        codes['p'] = (byte) (i++);
        codes['q'] = (byte) (i++);
        codes['r'] = (byte) (i++);
        codes['s'] = (byte) (i++);
        codes['t'] = (byte) (i++);
        codes['u'] = (byte) (i++);
        codes['v'] = (byte) (i++);
        codes['w'] = (byte) (i++);
        codes['x'] = (byte) (i++);
        codes['y'] = (byte) (i++);
        codes['z'] = (byte) (i++);
    }

    /**
     * Demonstrate encoding/decoding in this class
     *
     * @param args
     */
    public static void main(String args[]) {

        bcidEncoder shoulderEncoder = new bcidEncoder();
        shoulderEncoder.setDebug(true);

        BigInteger i = new BigInteger("10000000");
        String result = shoulderEncoder.encode(i);
        System.out.println("Encode/Decode Unique Value and Back");
        System.out.println("  Encoding " + i + " = " + result);
        System.out.println("  Decoding " + result + " = " + shoulderEncoder.decode(result));

        String ark = "ark:/87286/zzqF2/foodad";
        System.out.println("Decode ARK = " + ark);
        System.out.println("  Result = " + shoulderEncoder.decode(ark));

        ark = "ark:/87286/zzqF2";
        System.out.println("Decode ARK = " + ark);
        System.out.println("  Result = " + shoulderEncoder.decode(ark));


    }

    public HashMap<String, String> getMetadata() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
