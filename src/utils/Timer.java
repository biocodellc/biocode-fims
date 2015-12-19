package utils;

/**
* Utility class to help time events easily
 */
public class Timer {
    long begin;

    public Timer() {
        begin = System.currentTimeMillis();
    }

    public void lap(String message) {
        long end = System.currentTimeMillis();
        long executionTime = end - begin;
        System.out.println("" + executionTime + " ms : " + message);
    }


}
