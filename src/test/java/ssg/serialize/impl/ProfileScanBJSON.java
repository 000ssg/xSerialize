/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.base.ObjectSerializerContext;
import ssg.serialize.ObjectSerializer;
import ssg.serialize.ObjectSerializer.OSStat;
import ssg.serialize.impl.BJSONSerializer.BJSONStat;
import ssg.serialize.utils.DeepCompare;
import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Map;
import ssg.serialize.ObjectSerializer.DummyScanStatistics;
import ssg.serialize.ObjectSerializer.OSScanHandler;

/**
 * Perform multiple scan code executions to evaluate most critical code paths
 * while scanning and ensure data consistency is not broken.
 *
 * @author 000ssg
 */
public class ProfileScanBJSON {

    public static void main(String[] args) throws Exception {
        System.out.println("ProfileScanBJSON.main");
        Object root = TestPOJO.randomPOJO(TestPOJO1.class);
        int sz = 1000;
        if (root instanceof TestPOJO1) {
            if (args != null && args.length > 0) {
                try {
                    sz = Integer.parseInt(args[0]);
                } catch (Throwable th) {
                }
                if (sz < 1 || sz > 10000) {
                    sz = 100;
                }
            }

            TestPOJO1 pojo = (TestPOJO1) root;
            pojo.aself = new TestPOJO1[sz];
            for (int i = 0; i < pojo.aself.length; i++) {
                pojo.aself[i] = TestPOJO.randomPOJO(TestPOJO1.class);
            }
            for (TestPOJO1 pi : pojo.aself) {
                pi.abd = new BigDecimal[pojo.abd.length];
                for (int i = 0; i < pi.abd.length; i++) {
                    pi.abd[i] = pojo.abd[i];
                }
            }
        }

        System.out.println("ProfileBJSON: size=" + sz);

        BJSONSerializer bjson = new BJSONSerializer() {
            @Override
            public void onCompletedContext(String info, ObjectSerializerContext ctx) {
                boolean wr = "write".equals(info) || "enrich".equals(info);
                //System.out.println(info+"\n  "+ctx.decyclerv.dumpRegistered(wr, false,true).replace("\n", "\n  "));
            }
        };
        BJSONSerializer bjson2 = new BJSONSerializer() {
            @Override
            public void onCompletedContext(String info, ObjectSerializerContext ctx) {
                boolean wr = "write".equals(info) || "enrich".equals(info);
                //System.out.println(info+"\n  "+ctx.decyclerv.dumpRegistered(wr, false,true).replace("\n", "\n  "));
            }
        };
        bjson2.setDecycleFlags(
                BJSONSerializer.DF_STRING
                | BJSONSerializer.DF_BIGDEC
                | BJSONSerializer.DF_BIGINT);
        byte[] data = null;
        byte[] data2 = null;
        Map datam = null;
        long dur = 0;
        long dur2 = 0;
        long durr = 0;
        long durr2 = 0;
        { //for (int i = 0; i < 10; i++) {
            dur = System.nanoTime();
            data = bjson.toBytes(root);
            dur = System.nanoTime() - dur;

            durr = System.nanoTime();
            bjson.fromBytes(data);
            durr = System.nanoTime() - durr;

            dur2 = System.nanoTime();
            data2 = bjson2.toBytes(root);
            dur2 = System.nanoTime() - dur2;

            durr2 = System.nanoTime();
            bjson2.fromBytes(data2);
            durr2 = System.nanoTime() - durr2;
        }

        System.out.println("  1 Written " + data.length + " bytes in " + (dur / 1000000f) + "ms.");
        System.out.println("  2 Written " + data2.length + " bytes in " + (dur2 / 1000000f) + "ms.");

        datam = (Map) bjson.fromBytes(data);
        Object rroot = bjson.fromBytes(data, root.getClass());
        long rdur = System.nanoTime();
        rroot = bjson.fromBytes(data, root.getClass());
        rdur = System.nanoTime() - rdur;

        System.out.println("  1 Restored " + data.length + " bytes in " + (rdur / 1000000f) + "ms.");

        Object rroot2 = bjson2.fromBytes(data2, root.getClass());
        long rdur2 = System.nanoTime();
        rroot2 = bjson2.fromBytes(data2, root.getClass());
        rdur2 = System.nanoTime() - rdur2;

        System.out.println("  2 Restored " + data2.length + " bytes in " + (rdur2 / 1000000f) + "ms.");

        {
            DeepCompare.DC dc = DeepCompare.diff(root, rroot);
            DeepCompare.DC dc2 = DeepCompare.diff(rroot, rroot2);
            System.out.println(""
                    + "\nORIG-to-R1: " + dc.toString().replace("\n", "\n  ")
                    + "\nR1-to-R2  : " + dc2.toString().replace("\n", "\n  ")
            );
        }
        {
            OSStat stat = bjson.scan(new ByteArrayInputStream(data));
            OSStat stat2 = bjson2.scan(new ByteArrayInputStream(data2));
            System.out.println("BJSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
            System.out.println("BJSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
        }
        {
            System.out.println();
            OSScanHandler sh = new DummyScanStatistics();
            OSScanHandler sh2 = new DummyScanStatistics();
            OSStat stat = bjson.scan(sh, null, new ByteArrayInputStream(data));
            OSStat stat2 = bjson2.scan(sh2, null, new ByteArrayInputStream(data2));
            System.out.println("BJSON dummy statistics 1:\n  " + sh.toString().replace("\n", "\n  ") + "\n  " + stat.toString().replace("\n", "\n  "));
            System.out.println("BJSON dummy statistics 2:\n  " + sh2.toString().replace("\n", "\n  ") + "\n  " + stat2.toString().replace("\n", "\n  "));
        }

        // now do multiple restoring scans
        if (1 == 1) {
            int count = 0;
            float times = 0;
            float etimes = 0;
            Object mrr = null;
            final boolean debug = false;
            ObjectSerializer.OSScanHandler<Byte, Integer> sh = new BJSONSerializer.BJSONScanHandler() {
                @Override
                public void onClose(Byte type, Integer ref, Object value) {
                    if (debug) {
                        System.out.println("onClose(" + BJSONSerializer.type2name(type) + "" + ((ref != null) ? " (R=" + ref + ")" : "") + ((value != null) ? "<R>" : ""));
                    }
                    super.onClose(type, ref, value);
                }

                @Override
                public void onClose(Byte type, Integer ref) {
                    if (debug) {
                        System.out.println("onClose(" + BJSONSerializer.type2name(type) + "" + ((ref != null) ? " (R=" + ref + ")" : ""));
                    }
                    super.onClose(type, ref);
                }

                @Override
                public void onOpen(Byte type, boolean referrable) {
                    if (debug) {
                        System.out.println("onOpen(" + BJSONSerializer.type2name(type) + "" + ((referrable) ? " (R)" : ""));
                    }
                    super.onOpen(type, referrable);
                }

                @Override
                public void onScalar(Byte type, Object value, boolean referrable) {
                    if (debug) {
                        System.out.println("onScalar(" + BJSONSerializer.type2name(type) + "" + ((referrable) ? " (R)" : "") + ": " + value);
                    }
                    super.onScalar(type, value, referrable);
                }

            };
            for (int i = 0; i < 100; i++) {
                BJSONStat stat = new BJSONStat();
                bjson.scan(sh, stat, new ByteArrayInputStream(data));
                float t = (stat.getCompleted() - stat.getStarted()) / 1000000f;
                times += t;
                count++;

                Object mr = sh.root();
                long mrrStart = System.nanoTime();
                mrr = bjson2.enrich(mr, root.getClass());
                float mrrt = (System.nanoTime() - mrrStart) / 1000000f;
                etimes += mrrt;

                //System.out.println("BJSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
            }
            System.out.println("\nBJSON: " + (times / count) + " (" + ((times + etimes) / count) + ")" + "ms, " + count + " times, ref=" + durr / 1000000f + " / " + rdur / 1000000f + "ms.");
            Object mr = sh.root();
            DeepCompare.DC dc = DeepCompare.diff(root, mr);
            DeepCompare.DC dc2 = DeepCompare.diff(root, mrr);
            System.out.println(""
                    + "\nORIG-to-R : " + dc.toString().replace("\n", "\n  ")
                    + "\nORIG-to-RR: " + dc2.toString().replace("\n", "\n  ")
            );
        }
        if (1 == 1) {
            int count = 0;
            float times = 0;
            float etimes = 0;
            Object mrr = null;
            ObjectSerializer.OSScanHandler<Byte, Integer> sh = new BJSONSerializer.BJSONScanHandler();
            for (int i = 0; i < 100; i++) {
                BJSONStat stat = new BJSONStat();
                bjson2.scan(sh, stat, new ByteArrayInputStream(data2));
                float t = (stat.getCompleted() - stat.getStarted()) / 1000000f;
                times += t;
                count++;

                Object mr = sh.root();
                long mrrStart = System.nanoTime();
                mrr = bjson2.enrich(mr, root.getClass());
                float mrrt = (System.nanoTime() - mrrStart) / 1000000f;
                etimes += mrrt;

                //System.out.println("BJSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
            }
            System.out.println("\nBJSON2: " + (times / count) + " (" + ((times + etimes) / count) + ")" + "ms, " + count + " times, ref=" + durr2 / 1000000f + " / " + rdur2 / 1000000f + "ms.");
            Object mr = sh.root();
            DeepCompare.DC dc = DeepCompare.diff(root, mr);
            DeepCompare.DC dc2 = DeepCompare.diff(root, mrr);
            System.out.println(""
                    + "\nORIG-to-R : " + dc.toString().replace("\n", "\n  ")
                    + "\nORIG-to-RR: " + dc2.toString().replace("\n", "\n  ")
            );
        }

    }
}
