/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.ObjectSerializer.OSStat;
import ssg.serialize.utils.DeepCompare;
import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author 000ssg
 */
public class ProfileBJSON {

    public static void main(String[] args) throws Exception {
        Object root = TestPOJO.randomPOJO(TestPOJO1.class);
        int sz = 1000;
        String cs = "dfghjkrykyryjkryklrylrylörylrylr";
        byte[] cbb = new byte[]{2, 3, 4, 6, 7, 89, 9, 56, 34, 5};
        BigInteger cbi = new BigInteger(cbb);
        BigDecimal cbd = new BigDecimal("1233554748568967967967.3456");

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
                if (pojo.aself[i].as != null && pojo.aself[i].as.length > 0) {
                    //pojo.aself[i].as[0] = cs;
                }
                if (pojo.abi != null && pojo.abi.length > 0) {
                    //pojo.abi[0] = cbi;
                }
                if (pojo.abd != null && pojo.abd.length > 0) {
                    pojo.abd[0] = cbd;
                }
                if (pojo.ab != null && pojo.ab.length > 0) {
                    //pojo.ab = cbb;
                }
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
        bjson2.decycleFlags
                = BJSONSerializer.DF_STRING
                | BJSONSerializer.DF_BIGDEC
                | BJSONSerializer.DF_BIGINT;
        byte[] data = null;
        byte[] data2 = null;
        long dur = 0;
        long dur2 = 0;
        for (int i = 0; i < 10; i++) {
            dur = System.nanoTime();
            data = bjson.toBytes(root);
            dur = System.nanoTime() - dur;

            dur2 = System.nanoTime();
            data2 = bjson2.toBytes(root);
            dur2 = System.nanoTime() - dur2;
        }

        System.out.println("  1 Written " + data.length + " bytes in " + (dur / 1000000f) + "ms.");
        System.out.println("  2 Written " + data2.length + " bytes in " + (dur2 / 1000000f) + "ms.");

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

        DeepCompare.DC dc = DeepCompare.diff(root, rroot);
        DeepCompare.DC dc2 = DeepCompare.diff(rroot, rroot2);
        System.out.println(""
                + "\nORIG-to-R1: " + dc.toString().replace("\n", "\n  ")
                + "\nR1-to-R2  : " + dc2.toString().replace("\n", "\n  ")
        );
        {
            OSStat stat = bjson.scan(new ByteArrayInputStream(data));
            OSStat stat2 = bjson2.scan(new ByteArrayInputStream(data2));
            System.out.println("BJSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
            System.out.println("BJSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
        }

    }
}
