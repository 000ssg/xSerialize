/*
 * AS IS
 */
package ssg.serialize.impl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author 000ssg
 */
public class Functional implements Runnable {

    static AtomicInteger counter = new AtomicInteger();

    int maxRuns=1;
    int maxL = 5;
    int scaleM = 200;

    @Override
    public void run() {
        counter.getAndIncrement();
        System.out.println("START in thread " + Thread.currentThread().getName());
        try {
            for (int iii = 0; iii < maxRuns; iii++) {
                FunctionalBASE64.main(null);
                Thread.sleep(1);
                FunctionalJSON.main(null);
                Thread.sleep(1);
                FunctionalJSON.main(new String[]{"a"});
                FunctionalPipe.main(null);
                Thread.sleep(1);
                for (int i = 0; i < maxL; i++) {
                    ProfileJSON.main(new String[]{"" + (i * scaleM + 1)});
                    Thread.sleep(1);
                }
                ProfileBJSON.main(null);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            counter.getAndDecrement();
            System.out.println("STOP  in thread " + Thread.currentThread().getName());
        }
    }

    public static void main(String args[]) throws Exception {
        Functional f = new Functional();
        f.run();

        if (1 == 1) {
            return;
        }

        f.maxRuns=3;
        f.maxL = 3;
        f.scaleM = 100;

        Thread[] ths = new Thread[10];
        for (int i = 0; i < ths.length; i++) {
            ths[i] = new Thread(f);
            ths[i].setName("TH_" + i);
            ths[i].setDaemon(true);
        }

        for (Thread th : ths) {
            th.start();
        }

        while (counter.get() > 0) {
            Thread.sleep(1);
        }
    }
}
