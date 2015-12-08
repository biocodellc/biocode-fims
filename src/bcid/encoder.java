package bcid;

import fimsExceptions.FIMSException;

import java.math.BigInteger;

/**
 * Encoders interface defines the methods and fields that the various encoders need to implement
 */
public interface encoder {
    static char[] chars = null;
    static  byte[] codes = new byte[256];
    public String encode(BigInteger identifier);
    public BigInteger decode(String identifier) throws FIMSException;
}
