/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.ObjectSerializer;
import ssg.serialize.ObjectSerializer.OSStat;
import ssg.serialize.utils.DeepCompare;
import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;

/**
 *
 * @author 000ssg
 */
public class ProfileJSON {

    public static void main(String[] args) throws Exception {
        if (1 == 0) {
            Object root = TestPOJO.randomPOJO(TestPOJO1.class);
            int sz = 100;
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
            }

            System.out.println("ProfileJSON: size=" + sz);

            JSONSerializer json = new JSONSerializer();
            byte[] data = null;
            long dur = 0;
            for (int i = 0; i < 10; i++) {
                dur = System.nanoTime();
                data = json.toBytes(root);
                dur = System.nanoTime() - dur;
            }

            System.out.println("  Written " + data.length + " bytes in " + (dur / 1000000f) + "ms.");

            try {
                Object root2 = json.fromBytes(data, root.getClass());
                long dur2 = System.nanoTime();
                root2 = json.fromBytes(data, root.getClass());
                dur2 = System.nanoTime() - dur2;

                System.out.println("  Restored " + data.length + " bytes in " + (dur2 / 1000000f) + "ms.");

                {
                    ObjectSerializer.OSStat stat = json.scan(new ByteArrayInputStream(data));
                    System.out.println("JSON statistics:\n  " + stat.toString().replace("\n", "\n  "));
                }

            } catch (Throwable th) {
                th.printStackTrace();
                File f = new File("./src/test/resources/json/0.json");
                OutputStream os = new FileOutputStream(f);
                os.write(data);
                os.close();
                System.out.println(new String(data, "UTF-8"));
                throw new Exception(th);
            }
        } else {
            Object root = TestPOJO.randomPOJO(TestPOJO1.class);
            int sz = 2;
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

            System.out.println("ProfileJSON: size=" + sz);

            JSONSerializer json = new JSONSerializer(true) {
                @Override
                public void onCompletedContext(String info, ObjectSerializerContext ctx) {
                    boolean wr = "write".equals(info) || "enrich".equals(info);
                    //System.out.println(info+"\n  "+ctx.decyclerv.dumpRegistered(wr, false,true).replace("\n", "\n  "));
                }
            };
            JSONSerializer json2 = new JSONSerializer(true) {
                @Override
                public void onCompletedContext(String info, ObjectSerializerContext ctx) {
                    boolean wr = "write".equals(info) || "enrich".equals(info);
                    //System.out.println(info+"\n  "+ctx.decyclerv.dumpRegistered(wr, false,true).replace("\n", "\n  "));
                }
            };
            json2.decycleFlags
                    = JSONSerializer.DF_STRING
                    | JSONSerializer.DF_BIGDEC
                    | JSONSerializer.DF_BIGINT;
            byte[] data = null;
            byte[] data2 = null;
            long dur = 0;
            long dur2 = 0;
            for (int i = 0; i < 10; i++) {
                //System.out.println("Write JSON:");
                dur = System.nanoTime();
                data = json.toBytes(root);
                dur = System.nanoTime() - dur;

                //System.out.println("Write JSON2:");
                dur2 = System.nanoTime();
                data2 = json2.toBytes(root);
                dur2 = System.nanoTime() - dur2;
            }

            System.out.println("  1 Written " + data.length + " bytes in " + (dur / 1000000f) + "ms.");
            System.out.println("  2 Written " + data2.length + " bytes in " + (dur2 / 1000000f) + "ms.");

            {
                Object rroot = json.fromBytes(data);
                DeepCompare.DC dc = DeepCompare.diff(root, rroot);
                DeepCompare.DC dc2 = DeepCompare.diff(rroot, json2.fromBytes(data2));
                System.out.println("Check written structures:"
                        + "\nORIG-to-R1: " + dc.toString().replace("\n", "\n  ")
                        + "\nR1-to-R2  : " + dc2.toString().replace("\n", "\n  ")
                );
                //System.out.println("V1: "+new String(data,"UTF-8"));
                //System.out.println("V2: "+new String(data2,"UTF-8"));
                {
                    OSStat stat = json.scan(new ByteArrayInputStream(data));
                    OSStat stat2 = json2.scan(new ByteArrayInputStream(data2));
                    System.out.println("JSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
                    System.out.println("JSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
                }
            }

            Object rroot = json.fromBytes(data, root.getClass());
            long rdur = System.nanoTime();
            rroot = json.fromBytes(data, root.getClass());
            rdur = System.nanoTime() - rdur;

            System.out.println("  1 Restored " + data.length + " bytes in " + (rdur / 1000000f) + "ms.");
            {
                DeepCompare.DC dc = DeepCompare.diff(root, rroot);
                System.out.println("Check written structures:"
                        + "\nORIG-to-R1: " + dc.toString().replace("\n", "\n  ")
                );
            }

            Object rroot2 = json2.fromBytes(data2, root.getClass());
            long rdur2 = System.nanoTime();
            rroot2 = json2.fromBytes(data2, root.getClass());
            rdur2 = System.nanoTime() - rdur2;

            System.out.println("  2 Restored " + data2.length + " bytes in " + (rdur2 / 1000000f) + "ms.");

            DeepCompare.DC dc = DeepCompare.diff(root, rroot);
            DeepCompare.DC dc2 = DeepCompare.diff(rroot, rroot2);
            System.out.println(""
                    + "\nORIG-to-R1: " + dc.toString().replace("\n", "\n  ")
                    + "\nR1-to-R2  : " + dc2.toString().replace("\n", "\n  ")
            );
            {
                OSStat stat = json.scan(new ByteArrayInputStream(data));
                OSStat stat2 = json2.scan(new ByteArrayInputStream(data2));
                System.out.println("JSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
                System.out.println("JSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
            }

        }
    }

}
