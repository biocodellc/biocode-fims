package bcid;

import bcidExceptions.BCIDException;
import org.apache.commons.codec.binary.Base64;
import util.SettingsManager;

import java.math.BigInteger;

/**
 * The element encoder operates by equating encoded Strings directly to BigIntegers in the database.
 * The BigIntegers in the database are used for joining and linking on the back-end.
 * This class implements the encoders interface, taking a BigInteger and turning
 * it into an encoded bcid String.   Conversely, it can
 * attempt taking a String and decoding it into an BigInteger.
 */
public class elementEncoder implements encoder {
    // Make all base64 encoding URL safe -- this constructor will remove equals ("=") as padding
    private Base64 base64 = new Base64(true);

    /**
     * Characters used for encoding elements, when requested are ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_=  *
     */
    static public char[] chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-="
                    .toCharArray();

    /**
     * Lookup table for converting base64 characters to value in range 0..63 *
     */
    static public byte[] codes = new byte[256];

    static {
        for (int i = 0; i < 256; i++) codes[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++) codes[i] = (byte) (i - 'A');
        for (int i = 'a'; i <= 'z'; i++) codes[i] = (byte) (26 + i - 'a');
        for (int i = '0'; i <= '9'; i++) codes[i] = (byte) (52 + i - '0');
        codes['-'] = 62;
        //codes['_'] = 63;
    }

    String prefix = null;

    static SettingsManager sm;
    static {
        sm = SettingsManager.getInstance();
        sm.loadProperties();
    }

    /**
     * Instantiate the encoderBCID class by passing in a prefix to work with
     *
     * @param prefix
     */
    public elementEncoder(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Use Base64 encoding to turn BigIntegers numbers into Strings
     * We do this both to obfuscate integers used as identifiers and also to save some space.
     * Base64 is not necessarily the best for compressing numbers but it is well-known
     * The encoding presented here adds a check digit at the end of the string
     *
     * @param big
     * @return A String representation of this BigInteger
     */
    public String encode(BigInteger big) {
        CheckDigit checkDigit = new CheckDigit();
        String strVal = prefix + sm.retrieveValue("divider") + new String(base64.encode(big.toByteArray()));
        strVal = strVal.replace("\r\n", "");
        return checkDigit.generate(strVal);
    }

    /**
     * Base64 decode identifiers into integer representations.
     * 1. verify that the entire string is good w/ check digit
     * 2. Then base64 decode just the encoded piece of the string
     *
     * @param entireString
     * @return a BigIntgeger representation of this BCID
     */
    public BigInteger decode(String entireString) throws BCIDException {
        CheckDigit checkDigit = new CheckDigit();

        // Pull off potential last piece of string which would represent the local Identifier
        // The piece to decode is ark:/NAAN/shoulder_elementIdentifer (anything else after a last trailing "/" not decoded)
        StringBuilder sbEntireString = new StringBuilder();

        // Break the identifier down into component parts
        String bits[] = entireString.split("/");
        String scheme = bits[0];
        String naan = bits[1];
        String datasetPlusSuffix[] = bits[2].split(sm.retrieveValue("divider"));
        String dataset = datasetPlusSuffix[0];

        sbEntireString.append(scheme + "/" + naan + "/" + dataset);

        if (datasetPlusSuffix.length > 1) {
            sbEntireString.append(sm.retrieveValue("divider") + datasetPlusSuffix[1]);
        }
        String encodedString = sbEntireString.toString();

        // Validate using CheckDigit
        if (!checkDigit.verify(encodedString)) {
            throw new BCIDException(entireString + " does not verify");
        }
        // Get just the encoded portion of the string minus the prefix
        String encodedPiece = encodedString.replaceFirst(prefix, "").replaceFirst(sm.retrieveValue("divider"), "");

        // Now check the Actual String, minus check Character
        String actualString = checkDigit.getCheckDigit(encodedPiece);

        // Now return the integer that was encoded here.
        return new BigInteger(base64.decode(actualString));
    }
}
