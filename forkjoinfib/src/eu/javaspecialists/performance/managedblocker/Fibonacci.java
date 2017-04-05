package eu.javaspecialists.performance.managedblocker;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;
// demo 1 - 46370
public class Fibonacci {
    public BigInteger f(int n) {
        if (n == 0) return BigInteger.ZERO;
        if (n == 1) return BigInteger.ONE;

        int half = (n + 1) / 2;

        BigInteger f0 = f(half-1);
        BigInteger f1 = f(half);

        if (n % 2 == 1) {
            return f0.multiply(f0)
                .add(f1.multiply(f1));
        } else {
            return f0.shiftLeft(1).add(f1)
                .multiply(f1);
        }
    }
}
