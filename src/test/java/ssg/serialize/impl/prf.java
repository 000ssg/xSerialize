/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.base.ObjectSerializerContext;
import ssg.serialize.utils.Stat;
import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import ssg.serialize.utils.TestPOJO.TestPOJO2;
import ssg.serialize.utils.TestPOJO.TestPOJO3;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 000ssg
 */
public class prf {

    public static class CountingOutputStream extends OutputStream {

        public int flushes = 0;
        public long count = 0;

        @Override
        public void write(int b) throws IOException {
            count++;
        }

        @Override
        public void flush() throws IOException {
            flushes++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            count += len;
        }

        @Override
        public void write(byte[] b) throws IOException {
            count += b.length;
        }

        public void reset() {
            flushes = 0;
            count = 0;
        }

        @Override
        public String toString() {
            return "COS{" + "flushes=" + flushes + ", count=" + count + '}';
        }

    }

    public static class WUTF {

        CountingOutputStream cos = new CountingOutputStream();
        OutputStream os;
        Writer wr;

        public WUTF() {
            try {
                wr = new OutputStreamWriter(cos, "UTF-8");
            } catch (Throwable th) {
            }
        }

        public WUTF(OutputStream os) {
            try {
                wr = new OutputStreamWriter(os, "UTF-8");
            } catch (Throwable th) {
            }
        }

        public long test(Object o) {
            try {
                wr.flush();
                cos.reset();
                int special = 0;
                if (o instanceof String) {
                    wr.write((String) o);
                    for (char ch : ((String) o).toCharArray()) {
                        switch (ch) {
                            case '\t':
                            case '\b':
                            case '\f':
                            case '\n':
                            case '\r':
                            case '\'':
                            case '"':
                            case '/':
                                special++;
                                break;
                        }
                    }
                } else if (o instanceof char[]) {
                    wr.write((char[]) o);
                    for (char ch : (char[]) o) {
                        switch (ch) {
                            case '\t':
                            case '\b':
                            case '\f':
                            case '\n':
                            case '\r':
                            case '\'':
                            case '"':
                            case '/':
                                special++;
                                break;
                        }
                    }
                } else if (o instanceof Character) {
                    wr.write((Integer) o);
                    switch ((char) ((Integer) o).intValue()) {
                        case '\t':
                        case '\b':
                        case '\f':
                        case '\n':
                        case '\r':
                        case '\'':
                        case '"':
                        case '/':
                            special++;
                            break;
                    }
                }
                wr.flush();

                return cos.count + 2 + special;
            } catch (Throwable th) {
                return 0;
            } finally {
                try {
                    wr.flush();
                } catch (Throwable th1) {
                }
                cos.reset();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        JSONSerializer js = new JSONSerializer();

        boolean doStr = false;
        boolean doS0 = false;
        boolean doWUTF = false;

        boolean doChs = false;

        boolean doObj = true;

        List<Stat> stats = new ArrayList<Stat>();
        long start = 0;
        CountingOutputStream cos = new CountingOutputStream();
        WUTF wutf = new WUTF();

        if (doStr) {
            ObjectSerializerContext ctx = new ObjectSerializerContext();
            String s = TestPOJO.generateItem(String.class, 5400000);
            System.out.println("\nWrite String (" + s.length() + ")");
            for (int i = 0; i < 5; i++) {
                stats.add(new Stat(
                        "writeString " + i, (start = System.nanoTime()) != 0,
                        js.writeString(s, false, cos, ctx),
                        (System.nanoTime() - start)));
                cos.reset();
                if (doS0) {
                    stats.add(new Stat(
                            "writeString0 " + i, (start = System.nanoTime()) != 0,
                            js.writeString0(s, false, cos, ctx),
                            (System.nanoTime() - start)));
                    cos.reset();
                }
                if (doWUTF) {
                    stats.add(new Stat(
                            "WUTF " + i, (start = System.nanoTime()) != 0,
                            wutf.test(s),
                            (System.nanoTime() - start)));
                    cos.reset();
                }
            }
            for (Stat stat : stats) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
        }

        if (doChs) {
            char[] chs = TestPOJO.generateItem(String.class, 800000).toCharArray();
            System.out.println("\nWrite char[" + chs.length + "]");
            stats.clear();
            for (int i = 0; i < 5; i++) {
                stats.add(new Stat(
                        "write (char[]) " + i, (start = System.nanoTime()) != 0,
                        js.write(chs, cos),
                        (System.nanoTime() - start)));
                cos.reset();
                stats.add(new Stat(
                        "WUTF " + i, (start = System.nanoTime()) != 0,
                        wutf.test(chs),
                        (System.nanoTime() - start)));
                cos.reset();
            }
            for (Stat stat : stats) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
        }

        if (doObj) {
            TestPOJO1 tp1 = TestPOJO.randomPOJO(TestPOJO1.class, 500);
            TestPOJO2 tp2 = TestPOJO.randomPOJO(TestPOJO2.class, 1500);
            TestPOJO3 tp3 = TestPOJO.randomPOJO(TestPOJO3.class, 15500);
            System.out.println("\nWrite TestPOJO1(2,3)");
            stats.clear();
            Object[] objs = new Object[]{tp1, tp2, tp3};
            for (int i = 0; i < 5; i++) {
                for (Object o : objs) {
//                    js.SA=false;
//                    stats.add(new Stat(
//                            "write !SA " + o.getClass().getSimpleName() + " " + i, (start = System.nanoTime()) != 0,
//                            js.write(o, cos),
//                            (System.nanoTime() - start)));
                    js.SA = true;
                    stats.add(new Stat(
                            "write SA " + o.getClass().getSimpleName() + " " + i, (start = System.nanoTime()) != 0,
                            js.write(o, cos),
                            (System.nanoTime() - start)));
                }
                cos.reset();
            }
            for (Stat stat : stats) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
        }
    }
}
