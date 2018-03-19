/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.utils.Stat;
import static ssg.serialize.impl.FunctionalJSON.size;
import ssg.serialize.utils.RW;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author 000ssg
 */
public class FunctionalBASE64 {

    public static void testSerializer() throws IOException {
        int writeIterations = 50;
        int readIterations = 50;

        byte[] r = new byte[400000];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) (0xFF & (i % 255));
        }

        // pre-init
        List<RW> rws = new ArrayList<RW>();
        System.out.println("Known RWs: " + RW.getRegisteredNames());
        for (String key : RW.getRegisteredNames()) {
            if (key.toLowerCase().contains("se64")) {
                RW rw = RW.getRW(key);
                if (rw.getType() == BASE64Serializer.class) {
                    if (rw.canWrite() || rw.canRead()) {
                        ((BASE64Serializer) rw.getInstance()).wrapAt = 0;
                        rw.title += "(q)";
                        rws.add(rw);
                        System.out.println("added " + rw);
                    }
                    rw = RW.getRW(key);
                }
                if (rw.canWrite() || rw.canRead()) {
                    rws.add(rw);
                    System.out.println("added " + rw);
                }
            }
        }

        RW[] oRW = rws.toArray(new RW[rws.size()]);
        String[] oOUT = new String[oRW.length];
        byte[][] oIN = new byte[oRW.length][];
        byte[][] rr = oIN;

        System.out.println("\nStart WRITE for " + oRW.length + " RW types:");
        List<Stat> stats = new ArrayList<Stat>();

        try {
            Object root = r;
            long start = System.nanoTime();
            // init WRITE
            for (int k = 0; k < oRW.length; k++) {
                RW rw = oRW[k];
                if (rw.canWrite()) {
                    System.out.println("Init " + rw);
                    stats.add(new Stat(
                            rw.title + " init", (start = System.nanoTime()) != 0,
                            size(oOUT[k] = (String) rw.write(root)),
                            (System.nanoTime() - start)));
                }
            }
            stats.add(null);

            for (int i = 0; i < writeIterations; i++) {
                int ii = -1;

                // re-init data
                for (int k = 0; k < r.length; k++) {
                    r[k] = (byte) (0xFF & (k % 255));
                }

                for (int k = 0; k < oRW.length; k++) {
                    RW rw = oRW[k];
                    if (rw.canWrite()) {
                        stats.add(new Stat(
                                rw.title + " " + ii, (start = System.nanoTime()) != 0,
                                size(oOUT[k] = (String) rw.write(root)),
                                (System.nanoTime() - start)));
                    }
                }
                stats.add(null);
            }
            stats.add(null);

            System.out.println("\nStart READ for " + oRW.length + " RW types:");
            // init READ
            for (int k = 0; k < oRW.length; k++) {
                RW rw = oRW[k];
                if (rw.canRead()) {
                    System.out.println("Init R " + rw);
                    stats.add(new Stat(
                            rw.title + " R init", (start = System.nanoTime()) != 0,
                            size(oIN[k] = (byte[]) rw.read((oOUT[k] != null) ? oOUT[k] : oOUT[0])),
                            (System.nanoTime() - start)));
                }
            }
            stats.add(null);

            for (int i = 0; i < readIterations; i++) {
                int ii = -2;
                for (int k = 0; k < oRW.length; k++) {
                    RW rw = oRW[k];
                    if (rw.canRead()) {
                        stats.add(new Stat(
                                rw.title + " R " + ii, (start = System.nanoTime()) != 0,
                                size(oIN[k] = (byte[]) rw.read((oOUT[k] != null) ? oOUT[k] : oOUT[0])),
                                (System.nanoTime() - start)));
                    }
                }
                stats.add(null);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        if (writeIterations > 10 || readIterations > 10) {
            // too many results - it is statistics...
        } else {
            System.out.println("\nWRITE/READ statistics for " + oRW.length + " RW types:");
            for (Stat stat : stats) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
        }

        Map<String, Stat.SStat> summary = new LinkedHashMap<String, Stat.SStat>();
        for (Stat st : stats) {
            if (st == null || st.getDescription() == null || st.getDescription().contains("init")) {
                continue;
            }
            Stat.SStat sst = summary.get(st.getDescription());
            if (sst == null) {
                summary.put(st.getDescription(), new Stat.SStat(st));
            } else {
                sst.add(st);
            }
        }

        // rate
        {
            List<Stat.SStat> ws = new ArrayList<Stat.SStat>();
            List<Stat.SStat> rs = new ArrayList<Stat.SStat>();
            for (Stat.SStat stat : summary.values()) {
                if (stat == null) {
                    continue;
                }
                if (stat.getDescription().contains(" R ")) {
                    rs.add(stat);
                } else {
                    ws.add(stat);
                }
            }
            if (!ws.isEmpty()) {
                ws.iterator().next().rate(ws);
            }
            if (!rs.isEmpty()) {
                rs.iterator().next().rate(rs);
            }

            System.out.println("\nWRITE/READ summary:");
            for (Stat.SStat stat : ws) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
            System.out.println();
            for (Stat.SStat stat : rs) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
        }

        for (int i = 0; i < rr[0].length; i++) {
            for (int j = 1; j < rr.length; j++) {
                if (rr[0][i] != rr[j][i]) {
                    System.out.println("Invalid byte at " + i + ", 0=" + rr[0][i] + ", j=" + j + " " + rr[j][i]);
                }
            }
        }
    }

    public static void testStreams() throws IOException {
        byte[] r = new byte[400000];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) (0xFF & (i % 255));
        }

        ByteArrayOutputStream baos0 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(r);

        BASE64Serializer base64 = new BASE64Serializer();

        for (int i = 16; i < 1000; i += 3) {
            byte[] rr=Arrays.copyOf(r, i);
            baos.reset();
            baos0.reset();
            bais = new ByteArrayInputStream(rr);

            // generate reference encoding and test decoded data (pipe)
            base64.pipe(bais, baos0, true);
            base64.pipe(new ByteArrayInputStream(baos0.toByteArray()), baos, false);
            
        }

    }

    public static void main(String[] args) throws Exception {
        testSerializer();
        testStreams();
    }
}
