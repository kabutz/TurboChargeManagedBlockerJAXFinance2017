package eu.javaspecialists.performance.managedblocker;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;

// demo 1 - 46370
public class Fibonacci {
    public BigInteger f(int n) {
        Map<Integer, BigInteger> cache =
            new ConcurrentHashMap<>();
        cache.put(0, BigInteger.ZERO);
        cache.put(1, BigInteger.ONE);
        return f(n, cache);
    }

    private final BigInteger RESERVED =
        BigInteger.valueOf(-1000);

    private class FibonacciReservedBlocker implements
        ForkJoinPool.ManagedBlocker {
        BigInteger result;
        private final int n;
        private final Map<Integer, BigInteger> cache;

        public FibonacciReservedBlocker(int n, Map<Integer, BigInteger> cache) {
            this.n = n;
            this.cache = cache;
        }

        public boolean isReleasable() {
            return (result = cache.get(n)) != RESERVED;
        }

        public boolean block() throws InterruptedException {
            synchronized (RESERVED) {
                while (!isReleasable()) {
                    RESERVED.wait();
                }
            }
            return true;
        }
    }

    public BigInteger f(int n,
                        Map<Integer, BigInteger> cache) {
        BigInteger result = cache.putIfAbsent(n, RESERVED);
        if (result == null) {
            int half = (n + 1) / 2;

            RecursiveTask<BigInteger> f0_task = new RecursiveTask<BigInteger>() {
                protected BigInteger compute() {
                    return f(half - 1, cache);
                }
            };
            f0_task.fork();

            BigInteger f1 = f(half, cache);
            BigInteger f0 = f0_task.join();

            long time = n < 100_000 ? 0 : System.currentTimeMillis();
            try {
                if (n % 2 == 1) {
                    result = f0.multiply(f0)
                        .add(f1.multiply(f1));
                } else {
                    result = f0.shiftLeft(1).add(f1)
                        .multiply(f1);
                }
            } finally {
                time = n < 100_000 ? 0 : (System.currentTimeMillis() - time);
                if (time > 50)
                    System.out.println("fib(" + n + ") time = " + time + "ms");
            }
            synchronized (RESERVED) {
                cache.put(n, result);
                RESERVED.notifyAll();
            }
        } else if (result == RESERVED) {
            try {
                FibonacciReservedBlocker blocker =
                    new FibonacciReservedBlocker(n, cache);
                ForkJoinPool.managedBlock(blocker);
                result = blocker.result;
            } catch (InterruptedException e) {
                throw new CancellationException("interrupted");
            }
        }
        return result;
    }
}
