/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.base.ObjectSerializerContext;
import ssg.serialize.ObjectSerializer.OSScanHandler;
import ssg.serialize.ObjectSerializer.OSStat;
import ssg.serialize.impl.BJSONSerializer.BJSONScanHandler;
import ssg.serialize.impl.BJSONSerializer.BJSONStat;
import ssg.serialize.tools.Decycle;
import ssg.serialize.utils.Dump;
import ssg.serialize.tools.Reflector;
import ssg.serialize.utils.DeepCompare;
import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import ssg.serialize.utils.TestPOJO.TestPOJO2;
import ssg.serialize.utils.TestPOJO.TestPOJO3;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static ssg.serialize.utils.TestPOJO.randomPOJO;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import ssg.serialize.base.ObjectSerializerContext.DecyclerListener;

/**
 *
 * @author 000ssg
 */
public class BJSONSerializerTest {

    public BJSONSerializerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of writeCardinal method, of class BJSONSerializer.
     */
    @Test
    public void testWrite_Object_OutputStream() throws Exception {
        System.out.println("write");
        int idx = 0;
        for (Object[] oo : new Object[][]{
            {null, new byte[]{BJSONSerializer.T_NULL}},
            {true, new byte[]{BJSONSerializer.T_TRUE}},
            {false, new byte[]{BJSONSerializer.T_FALSE}},
            {0, new byte[]{BJSONSerializer.T_ZERO}},
            {1, new byte[]{BJSONSerializer.T_ONE}},
            {"text", new byte[]{BJSONSerializer.T_STR8, 4, (byte) 't', (byte) 'e', (byte) 'x', (byte) 't'}},
            {new byte[]{1, 2, 3}, new byte[]{BJSONSerializer.T_BIN8, 3, 1, 2, 3}},
            {null, new byte[]{BJSONSerializer.T_NULL}},
            {null, new byte[]{BJSONSerializer.T_NULL}}
        }) {
            Object obj = oo[0];
            System.out.println("  [" + idx + "] " + obj);
            idx++;
            byte[] buf = (byte[]) oo[1];
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            BJSONSerializer instance = new BJSONSerializer();
            long expResult = buf.length;
            long result = instance.write(obj, os);
            assertEquals(expResult, result);
            assertArrayEquals(buf, os.toByteArray());

            InputStream is = new ByteArrayInputStream(os.toByteArray());
            Object obj2 = instance.read(is);
            if (obj instanceof byte[]) {
                assertArrayEquals((byte[]) obj, (byte[]) obj2);
            } else if (obj != null && obj.getClass().isArray()) {
                assertArrayEquals((Object[]) obj, (Object[]) obj2);
            } else {
                assertEquals(obj, obj2);
            }
        }
    }

    /**
     * Test of simplify method, of class BJSONSerializer.
     */
    @Test
    public void testSimplify() {
        System.out.println("simplify");
        Object obj = randomPOJO(TestPOJO1.class);
        BJSONSerializer instance = new BJSONSerializer();
        Object result = instance.simplify(obj);
        //System.out.println("Type: "+result.getClass().getName());
        assertEquals(Reflector.class, result.getClass());
        result = ((Reflector) result).asMap(obj);
        if (result instanceof Map) {
            Map m = (Map) result;
            for (Object key : m.keySet()) {
                Object val = m.get(key);
                Object sval = instance.simplify(val);
                if (val != null && !Decycle.isSimple(val)) {
                    if (val.getClass().isArray() || val instanceof Collection) {
                        assertNull(sval);
                    } else if (val instanceof Map) {
                        assertNull(sval);
                    } else if (val != null && val.getClass().isEnum()) {
                        assertNull(sval);
                    } else {
                        assertEquals(Reflector.class, sval.getClass());
                    }
                } else {
                    assertNull(sval);
                }
            }
        }
    }

    /**
     * Test of enrich method, of class BJSONSerializer.
     */
    @Test
    public void testEnrich() throws Exception {
        System.out.println("enrich");
        Object obj = randomPOJO(TestPOJO1.class);
        TestPOJO1 obj01 = (TestPOJO1) randomPOJO(TestPOJO1.class);
        TestPOJO1 obj02 = (TestPOJO1) randomPOJO(TestPOJO1.class);
        obj01.self = null;
        obj01.aself = null;
        obj02.self = null;
        obj02.aself = null;
        ((TestPOJO1) obj).self = obj01;
        if (((TestPOJO1) obj).aself.length > 0) {
            ((TestPOJO1) obj).aself[0] = obj02;
        }
        if (((TestPOJO1) obj).aself.length > 1) {
            ((TestPOJO1) obj).aself[1] = obj01;
        }
        if (((TestPOJO1) obj).aself.length > 2) {
            ((TestPOJO1) obj).aself[1] = (TestPOJO1) obj;
        }

        Class clazz = obj.getClass();
        BJSONSerializer instance = new BJSONSerializer();
        byte[] ser = instance.toBytes(obj);
        Object res = instance.fromBytes(ser);
        Object result = instance.enrich(res, clazz);
        System.out.println("SRC: " + obj);
        System.out.println("MAP: " + Dump.dumpMap((Map) res, false, false));
        System.out.println("DST: " + result);

        DeepCompare.DC dc = DeepCompare.diff(obj, res);
        DeepCompare.DC dc2 = DeepCompare.diff(obj, result);
        DeepCompare.DC dc3 = DeepCompare.diff(res, result);
        System.out.println(""
                + "\nobj-to-map: " + dc.toString().replace("\n", "\n  ")
                + "\nobj-to-res: " + dc2.toString().replace("\n", "\n  ")
                + "\nmap-to-res: " + dc3.toString().replace("\n", "\n  ")
        );

        assertEquals(obj.toString(), result.toString());

        Object result2 = instance.enrich(res, TestPOJO2.class);
        assertEquals(obj.toString(), result2.toString());

        Object result3 = instance.enrich(res, TestPOJO3.class);
        assertEquals(obj.toString(), result3.toString());
    }

    @Test
    public void testVariants() throws Exception {
        System.out.println("test variants");

        // prepare data set
        Map root = new LinkedHashMap();
        for (int i = 0; i < 20; i++) {
            root.put("o" + i, TestPOJO.randomPOJO(TestPOJO.TestPOJO1.class, 400));
        }

        BJSONSerializer bjson = new BJSONSerializer();
        BJSONSerializer bjson2 = new BJSONSerializer();
        bjson.setResolveCyclicReferences(true);
        bjson2.setResolveCyclicReferences(true);
        //bjson.DEBUG=true;
        //bjson2.DEBUG=true;
        bjson2.setDecycleFlags(
                BJSONSerializer.DF_STRING
                | BJSONSerializer.DF_BIGDEC
                | BJSONSerializer.DF_BIGINT);

        float[][] vars = new float[2][2];
        int count = 100;

        OutputStream os = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
            }

            @Override
            public void write(byte[] b) throws IOException {
            }
        };

        for (int i = 0; i < vars.length; i++) {
            BJSONSerializer.V = i;
            for (int j = 0; j < count; j++) {
                long st = System.nanoTime();
                bjson.toStream(root, os);
                vars[i][0] += (System.nanoTime() - st) / 1000000f;
                st = System.nanoTime();
                bjson2.toStream(root, os);
                vars[i][1] += (System.nanoTime() - st) / 1000000f;
            }
        }

        System.out.println("V0  : " + vars[0][0] / count);
        System.out.println("V0 q: " + vars[0][1] / count);
        System.out.println("V1  : " + vars[1][0] / count);
        System.out.println("V1 q: " + vars[1][1] / count);

    }

    @Test
    public void testDecycling() throws Exception {
        System.out.println("test decycling");

        for (int max = 1; max < 102; max += 50) {
            Runtime.getRuntime().gc();
            Map root = new LinkedHashMap();

            final boolean specialCase = max == 51;

            if (specialCase) {
                for (int i = 0; i < 2; i++) {//100; i++) {
                    root.put("o" + i, TestPOJO.randomPOJO(TestPOJO.TestPOJO1.class, 400));
//                    if (i == 0) {
//                        System.out.println(root.get("o" + i));
//                    }
                }
            }

            for (int j = 0; j < max; j++) {
                Map m = new LinkedHashMap();
                root.put("m" + j, m);
                for (int i = 0; i < ((specialCase) ? 10 : 1000); i++) {
                    m.put("s" + j + "_" + i, "aaaaabbbbbbbbbbbbbbbbbbbbbbbbaaaaa");
                    m.put("2s", "aaaaabbbbbbbbbbbbbbbbbbbbbbbbaaaa2a");
                    m.put("3s_555555555555555555555", "aaaaabbbbbbbbbbbbbbbbbbbbbbbbaaaa3a");
                    m.put("4s" + j + "_" + i, "aaaaabbbbbbb078907yokuögkgiltiofgjkfjh,dfjhfjh,fhmfkgkjghjkghuilyklghjkbbbbbbbbbbbbbbbbbaaaaa");
                    m.put("bd" + j + "_" + i, BigDecimal.valueOf(Long.MAX_VALUE));
                    m.put("bi" + j + "_" + i, BigInteger.valueOf(Long.MAX_VALUE));
                    m.put("d" + j + "_" + i, 1.4675689795667e88);
                }
            }

            BJSONSerializer bjson = new BJSONSerializer();
            BJSONSerializer bjson2 = new BJSONSerializer() {
                @Override
                public ObjectSerializerContext createContext(Object obj) {
                    ObjectSerializerContext ctx = super.createContext(obj);
                    if (specialCase && 1 == 0) {
                        ctx.setDecyclerListener(new DecyclerListener() {
                            @Override
                            public void onRegisterRef(Object obj, Integer ref, Long hash) {
                                if (obj == null) {
                                    int a = 0;
                                }
                                System.out.println("REGISTER(NEW):\t" + ref + "\t" + hash + "\t" + obj.getClass().getSimpleName());
                            }

                            @Override
                            public void onRegisterDuplicate(Object obj, Integer ref, Long hash) {
                                if (obj == null) {
                                    int a = 0;
                                }
                                System.out.println("REGISTER(DUP):\t" + ref + "\t" + hash + "\t" + obj.getClass().getSimpleName());
                            }

                            @Override
                            public void onResolveRef(Long ref, Object obj) {
                                if (obj == null) {
                                    int a = 0;
                                }
                                System.out.println("RESOLVE      :\t" + ref + "\t\t" + obj.getClass().getSimpleName());
                            }
                        });
                    }
                    return ctx;
                }

            };
            bjson.setResolveCyclicReferences(true);
            bjson2.setResolveCyclicReferences(true);
            //bjson.DEBUG=true;
            //bjson2.DEBUG=true;
            bjson2.setDecycleFlags(
                    BJSONSerializer.DF_STRING
                    | BJSONSerializer.DF_BIGDEC
                    | BJSONSerializer.DF_BIGINT);

            byte[] data = null;
            byte[] data2 = null;
            long dur = 0;
            long dur2 = 0;
            for (int i = 0; i < ((specialCase) ? 1 : 10); i++) {
                if (specialCase) {
                    System.out.println("WRITE 1:");
                }
                dur = System.nanoTime();
                data = bjson.toBytes(root);
                dur = System.nanoTime() - dur;

                if (specialCase) {
                    System.out.println("WRITE 2:");
                }
                dur2 = System.nanoTime();
                data2 = bjson2.toBytes(root);
                dur2 = System.nanoTime() - dur2;
            }

            System.out.println("\nCheck for " + max + " maps");
            System.out.println("  1 Written " + data.length + " bytes in " + (dur / 1000000f) + "ms.");
            System.out.println("  2 Written " + data2.length + " bytes in " + (dur2 / 1000000f) + "ms.");

            if (specialCase) {
                System.out.println("READ 1:");
            }
            Object rroot = bjson.fromBytes(data, root.getClass());
            long rdur = System.nanoTime();
            if (specialCase) {
                System.out.println("READ 1(2):");
            }
            rroot = bjson.fromBytes(data, root.getClass());
            rdur = System.nanoTime() - rdur;

            System.out.println("  1 Restored " + data.length + " bytes in " + (rdur / 1000000f) + "ms.");

            if (specialCase) {
                System.out.println("READ 2:");
            }
            Object rroot2 = bjson2.fromBytes(data2, root.getClass());
            long rdur2 = System.nanoTime();
            rroot2 = bjson2.fromBytes(data2, root.getClass());
            rdur2 = System.nanoTime() - rdur2;

            System.out.println("  2 Restored " + data2.length + " bytes in " + (rdur2 / 1000000f) + "ms.");

            System.out.println("1 vs 2 metrics for " + max + " maps");
            System.out.println("  size    : " + data2.length * 1f / data.length + "\t\t" + data2.length + "\t" + data.length);
            System.out.println("  time (W): " + (dur2 * 1f / dur) + "\t\t" + dur2 / 1000000f + "\t" + dur / 1000000f);
            System.out.println("  time (R): " + (rdur2 * 1f / rdur) + "\t\t" + rdur2 / 1000000f + "\t" + rdur / 1000000f);
            System.out.println("  time (F): " + ((rdur2 + dur2) * 1f / (rdur + dur)) + "\t\t" + (rdur2 + dur2) / 1000000f + "\t" + (rdur + dur) / 1000000f);
            System.out.println("  HASHES:");
            System.out.println(""
                    + "    #ORIG: " + root.toString().hashCode()
                    + "\n    #R1  : " + rroot.toString().hashCode()
                    + "\n    #R2  : " + rroot2.toString().hashCode()
            );

            if (max == 1) {
                System.out.println(""
                        + "\nORIG: " + root
                        + "\nR1  : " + rroot
                        + "\nR2  : " + rroot2
                );

                DeepCompare.DC dc = DeepCompare.diff(root, rroot);
                DeepCompare.DC dc2 = DeepCompare.diff(rroot, rroot2);
                System.out.println(""
                        + "\nORIG-to-R1: " + dc.toString().replace("\n", "\n  ")
                        + "\nR1-to-R2  : " + dc2.toString().replace("\n", "\n  ")
                );
                assertEquals(DeepCompare.DIFF.same, dc.getState());
                assertEquals(DeepCompare.DIFF.same, dc2.getState());
            }
            {
                if (max == 51 && !(new File("./target/JST_1.bjson")).exists()) {
                    // save these data for testScan to proceed
                    File f = Common.save(new File("./target/JST_1.bjson"), data);
                    if (f != null) {
                        System.out.println("Stored data as " + f);
                    }

                    f = Common.save(new File("./target/JST_2.bjson"), data2);
                    if (f != null) {
                        System.out.println("Stored data2 as " + f);
                    }
                }

                OSScanHandler<Byte, Integer> sh = new BJSONScanHandler();
                OSScanHandler<Byte, Integer> sh2 = new BJSONScanHandler();
                OSStat stat = bjson.scan(sh, null, new ByteArrayInputStream(data));
                OSStat stat2 = bjson2.scan(sh2, null, new ByteArrayInputStream(data2));
                System.out.println("BJSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
                System.out.println("BJSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));

                DeepCompare.DC dc = DeepCompare.diff(root, sh.root());
                DeepCompare.DC dc2 = DeepCompare.diff(sh.root(), sh2.root());
                System.out.println(""
                        + "\nORIG-to-R1: " + dc.toString().replace("\n", "\n  ")
                        + "\nR1-to-R2  : " + dc2.toString().replace("\n", "\n  ")
                );
                assertEquals(DeepCompare.DIFF.same, dc.getState());
                assertEquals(DeepCompare.DIFF.same, dc2.getState());

            }
        }
    }

    @Test
    public void testScan() throws Exception {
        BJSONSerializer bjson = new BJSONSerializer();
        BJSONSerializer bjson2 = new BJSONSerializer();
        bjson.setResolveCyclicReferences(true);
        bjson2.setResolveCyclicReferences(true);
        //bjson.DEBUG=true;
        //bjson2.DEBUG=true;
        bjson2.setDecycleFlags(
                BJSONSerializer.DF_STRING
                | BJSONSerializer.DF_BIGDEC
                | BJSONSerializer.DF_BIGINT);
        byte[] data = Common.load(new File("./target/JST_1.bjson"));
        byte[] data2 = Common.load(new File("./target/JST_2.bjson"));
        if (data != null) {
            //OSStat stat = bjson.scan(null, new ByteArrayInputStream(data));
            OSStat stat2 = bjson2.scan(new ByteArrayInputStream(data2));
            //System.out.println("JSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
            System.out.println("BJSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
        }

        if (data != null) {
            long l0 = System.nanoTime();
            Map m0 = (Map) bjson.fromBytes(data);
            float t0 = (System.nanoTime() - l0) / 1000000f;
            l0 = System.nanoTime();
            Map m01 = (Map) bjson2.fromBytes(data2);
            float t02 = (System.nanoTime() - l0) / 1000000f;

            BJSONStat stat = new BJSONStat();
            OSScanHandler<Byte, Integer> sh = new BJSONScanHandler();
            bjson.scan(sh, stat, new ByteArrayInputStream(data));
            float t = (stat.getCompleted() - stat.getStarted()) / 1000000f;
            Map m = sh.root();
            bjson2.scan(sh, stat, new ByteArrayInputStream(data2));
            float t1 = (stat.getCompleted() - stat.getStarted()) / 1000000f;
            Map m2 = sh.root();

            System.out.println("\nm0 in\t" + t0 + "ms.");
            System.out.println("m02 in\t" + t02 + "ms.");
            System.out.println("m in\t" + t + "ms.");
            System.out.println("m2 in\t" + t1 + "ms.");
            DeepCompare.DC dc0_01 = DeepCompare.diff(m0, m01);
            DeepCompare.DC dc0_ = DeepCompare.diff(m0, m);
            DeepCompare.DC dc0_2 = DeepCompare.diff(m0, m2);
            System.out.println(""
                    + "\nm0-to-m01: " + dc0_01.toString().replace("\n", "\n  ")
                    + "\nm0-to-m  : " + dc0_.toString().replace("\n", "\n  ")
                    + "\nm0-to-m2 : " + dc0_2.toString().replace("\n", "\n  ")
            );

            int a = 0;
        }
    }

}
