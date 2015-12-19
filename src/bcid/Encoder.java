package bcid;

import fimsExceptions.FimsException;

import java.math.BigInteger;

/**
 * Encoders interface defines the methods and fields that the various encoders need to implement
 */
public interface Encoder {
    static char[] chars = null;
    static  byte[] codes = new byte[256];
    public String encode(BigInteger identifier);
    public BigInteger decode(String identifier) throws FimsException;
}
