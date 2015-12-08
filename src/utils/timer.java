package utils;

/**
* Utility class to help time events easily
 */
public class timer {
    long begin;

    public timer() {
        begin = System.currentTimeMillis();
    }

    public void lap(String message) {
        long end = System.currentTimeMillis();
        long executionTime = end - begin;
        System.out.println("" + executionTime + " ms : " + message);
    }


}
