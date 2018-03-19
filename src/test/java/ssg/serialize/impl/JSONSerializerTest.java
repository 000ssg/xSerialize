/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.tools.Indent;
import ssg.serialize.ObjectSerializer.OSScanHandler;
import ssg.serialize.ObjectSerializer.OSStat;
import ssg.serialize.Serializer;
import ssg.serialize.impl.JSONSerializer.JOSStat;
import ssg.serialize.impl.JSONSerializer.JSONScanHandler;
import ssg.serialize.tools.Decycle;
import ssg.serialize.tools.Reflector;
import ssg.serialize.utils.DeepCompare;
import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import ssg.serialize.utils.TestPOJO.TestPOJO2;
import ssg.serialize.utils.TestPOJO.TestPOJO3;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static ssg.serialize.utils.TestPOJO.randomPOJO;

/**
 *
 * @author 000ssg
 */
public class JSONSerializerTest {

    public JSONSerializerTest() {
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
     * Test of writeCardinal method, of class JSONSerializer.
     */
    @Test
    public void testWrite_Object_OutputStream() throws Exception {
        System.out.println("write");
        int idx = 0;
        for (Object[] oo : new Object[][]{
            {null, "null"},
            {true, "true"},
            {false, "false"},
            {0L, "0"},
            {1L, "1"},
            {"text", "\"text\""},
            {new byte[]{1, 2, 3}, "\"AQID\""},
            {new int[]{1, 2, 3}, "[1,2,3]"},
            {new long[]{1, 2, 3}, "[1,2,3]"},
            {new short[]{1, 2, 3}, "[1,2,3]"},
            {new float[]{1, 2, 3}, "[1.0,2.0,3.0]"},
            {new double[]{1, 2, 3}, "[1.0,2.0,3.0]"},
            {new Integer[]{1, 2, 3}, "[1,2,3]"},
            {new Long[]{1L, 2L, 3L}, "[1,2,3]"},
            {new Short[]{1, 2, 3}, "[1,2,3]"},
            {new Float[]{1f, 2f, 3f}, "[1.0,2.0,3.0]"},
            {new Double[]{1.0, 2.0, 3.0}, "[1.0,2.0,3.0]"},
            {new LinkedHashMap() {
                {
                    put("A", "AA");
                    put("B", "bb");
                }
            }, "{\"A\":\"AA\",\"B\":\"bb\"}"},
            {new ArrayList() {
                {
                    add(1.0);
                    add(2L);
                    add("a");
                }
            }, "[1.0,2,\"a\"]"}
        }) {
            Object obj = oo[0];
            System.out.println("  [" + idx + "] " + obj);
            idx++;
            String exp = (String) oo[1];
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JSONSerializer instance = new JSONSerializer();
            long result = instance.write(obj, os);
            String s = os.toString("UTF-8");
            assertEquals(exp, s);

            InputStream is = new ByteArrayInputStream(os.toByteArray());
            Object obj2 = instance.read(is);
            if (obj instanceof byte[]) {
                assertArrayEquals((byte[]) obj, (byte[]) BASE64Serializer.decode((String) obj2));// Base64.decode((String) obj2));
            } else if (obj != null && obj.getClass().isArray()) {
                assertEquals(Array.getLength(obj), ((List) obj2).size());
                for (int i = 0; i < Array.getLength(obj); i++) {
                    if (Array.get(obj, i) instanceof Number) {
                        assertEquals(((Number) Array.get(obj, i)).longValue(), ((Number) ((List) obj2).get(i)).longValue());
                    } else {
                        assertEquals(Array.get(obj, i), ((List) obj2).get(i));
                    }
                }
            } else {
                assertEquals(obj, obj2);
            }
        }

        // test indentation
        Serializer ser = new JSONSerializer();
        Serializer seri = new JSONSerializer(true);
        Serializer serit = new JSONSerializer(new Indent("\t"));

        Map m = new LinkedHashMap();
        Map m1 = new LinkedHashMap();
        Map m2 = new LinkedHashMap();
        List l1 = new ArrayList();
        for (Object o : new Object[]{1.0, 2f, 3, (byte) 4, "str", new char[]{'c', 'h', 's'}}) {
            l1.add(o);
        }
        List l2 = new ArrayList();
        for (Object o : new Object[]{2.0, 3f, 4, (byte) 5, "str2", new char[]{'c', 'h', 's', '2'}}) {
            l2.add(o);
        }
        l2.add(m2);
        m.put("a", "a");
        m.put("b", "Bbb");
        m.put("m1", m1);
        m.put("l1", l1);
        m.put("l2", l2);
        m2.put("e", 2e57);
        m2.put("el", new ArrayList());
        m2.put("em", new LinkedHashMap());

        String sser = ser.toText(m);
        String sseri = seri.toText(m);
        String sserit = serit.toText(m);
//        System.out.println("SER  [" + sser.length() + "]: " + sser.replace("\n", "\n  "));
//        System.out.println("SERi [" + sseri.length() + "]: " + sseri.replace("\n", "\n  "));
//        System.out.println("SERit[" + sserit.length() + "]: " + sserit.replace("\n", "\n  "));
        assertEquals(120, sser.length());
        assertEquals(258, sseri.length());
        assertEquals(207, sserit.length());
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
        JSONSerializer instance = new JSONSerializer();
        byte[] ser = instance.toBytes(obj);
        Object res = instance.fromBytes(ser);
        //System.out.println("SRC: " + obj);
        //System.out.println("MAP: " + Dump.dumpMap((Map) res));
        Object result = instance.enrich(res, clazz);
        //System.out.println("DST: " + result);
        byte[] a = obj.toString().getBytes("UTF-8");
        byte[] b = result.toString().getBytes("UTF-8");
        assertArrayEquals(a, b);
        assertArrayEquals(obj.toString().getBytes("UTF-8"), result.toString().getBytes("UTF-8"));

        Object result2 = instance.enrich(res, TestPOJO2.class);
        assertArrayEquals(obj.toString().getBytes("UTF-8"), result2.toString().getBytes("UTF-8"));

        Object result3 = instance.enrich(res, TestPOJO3.class);
        assertArrayEquals(obj.toString().getBytes("UTF-8"), result3.toString().getBytes("UTF-8"));
    }

    @Test
    public void test0() throws Exception {
        JSONSerializer json = new JSONSerializer();
        for (File f : new File("./src/test/resources/json").listFiles()) {
            if (f.isFile() && f.getName().endsWith(".json")) {
                byte[] bb = new byte[(int) f.length()];
                long l = f.toURI().toURL().openStream().read(bb);
                try {
                    //Object o0 = new ObjectMapper().readValue(f, Map.class);
                    Object o1 = json.fromURL(f.toURI().toURL());
                    //assertTrue(o0 instanceof LinkedHashMap);
                    assertTrue(o1 instanceof LinkedHashMap);
                } catch (Throwable th) {
                    th.printStackTrace();
                    int a = 0;
                }
            }
        }
    }

    @Test
    public void testDecycling() throws Exception {
        System.out.println("test decycling");

        for (int max = 1; max < 22; max += 10) {
            Runtime.getRuntime().gc();
            Map root = new LinkedHashMap();

            if (max == 11) {
                for (int i = 0; i < 100; i++) {
                    root.put("o" + i, TestPOJO.randomPOJO(TestPOJO.TestPOJO1.class, 400));
//                    if (i == 0) {
//                        System.out.println(root.get("o" + i));
//                    }
                }
            }

            for (int j = 0; j < max; j++) {
                Map m = new LinkedHashMap();
                root.put("m" + j, m);
                for (int i = 0; i < 1000; i++) {
                    m.put("s" + j + "_" + i, "aaaaabbbbbbbbbbbbbbbbbbbbbbbbaaaaa");
                    m.put("2s", "aaaaabbbbbbbbbbbbbbbbbbbbbbbbaaaa2a");
                    m.put("3s_555555555555555555555", "aaaaabbbbbbbbbbbbbbbbbbbbbbbbaaaa3a");
                    m.put("4s" + j + "_" + i, "aaaaabbbbbbb078907yokuögkgiltiofgjkfjh,dfjhfjh,fhmfkgkjghjkghuilyklghjkbbbbbbbbbbbbbbbbbaaaaa");
                    m.put("bd" + j + "_" + i, BigDecimal.valueOf(Long.MAX_VALUE));
                    m.put("bi" + j + "_" + i, BigInteger.valueOf(Long.MAX_VALUE));
                    m.put("d" + j + "_" + i, 1.4675689795667e88);
                }
            }

            JSONSerializer bjson = new JSONSerializer();
            JSONSerializer bjson2 = new JSONSerializer();
            bjson.setResolveCyclicReferences(true);
            bjson2.setResolveCyclicReferences(true);
            //bjson.DEBUG=true;
            //bjson2.DEBUG=true;
            bjson2.decycleFlags
                    = JSONSerializer.DF_STRING
                    | JSONSerializer.DF_BIGDEC
                    | JSONSerializer.DF_BIGINT;

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

            System.out.println("\nCheck for " + max + " maps");
            System.out.println("  1 Written " + data.length + " bytes in " + (dur / 1000000f) + "ms.");
            System.out.println("  2 Written " + data2.length + " bytes in " + (dur2 / 1000000f) + "ms.");
            //System.out.println("  1 text: " + new String(data, "UTF-8"));
            //System.out.println("  2 text: " + new String(data2, "UTF-8"));

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

            }
            {
                if (!(new File("./target/JST_1.json")).exists()) {
                    // save these data for testScan to proceed
                    File f = Common.save(new File("./target/JST_1.json"), data);
                    if (f != null) {
                        System.out.println("Stored data as " + f);
                    }
                    f = Common.save(new File("./target/JST_2.json"), data2);
                    if (f != null) {
                        System.out.println("Stored data2 as " + f);
                    }
                }
                OSStat stat = bjson.scan(new ByteArrayInputStream(data));
                OSStat stat2 = bjson2.scan(new ByteArrayInputStream(data2));
                System.out.println("JSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
                System.out.println("JSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
            }
        }
    }

    @Test
    public void testScan() throws Exception {
        JSONSerializer bjson = new JSONSerializer();
        JSONSerializer bjson2 = new JSONSerializer();
        bjson.setResolveCyclicReferences(true);
        bjson2.setResolveCyclicReferences(true);
        //bjson.DEBUG=true;
        //bjson2.DEBUG=true;
        bjson2.decycleFlags
                = JSONSerializer.DF_STRING
                | JSONSerializer.DF_BIGDEC
                | JSONSerializer.DF_BIGINT;
        byte[] data = Common.load(new File("./target/JST_1.json"));
        byte[] data2 = Common.load(new File("./target/JST_2.json"));
        if (data != null) {
            //OSStat stat = bjson.scan(null, new ByteArrayInputStream(data));
            OSStat stat2 = bjson2.scan(new ByteArrayInputStream(data2));
            //System.out.println("JSON statistics 1:\n  " + stat.toString().replace("\n", "\n  "));
            System.out.println("JSON statistics 2:\n  " + stat2.toString().replace("\n", "\n  "));
        }

        if (data != null) {
            long l0 = System.nanoTime();
            Map m0 = (Map) bjson.fromBytes(data);
            float t0 = (System.nanoTime() - l0) / 1000000f;
            l0 = System.nanoTime();
            Map m01 = (Map) bjson2.fromBytes(data2);
            float t02 = (System.nanoTime() - l0) / 1000000f;

            JOSStat stat = new JOSStat();
            OSScanHandler<String, Integer> sh = new JSONScanHandler();
            bjson.scan(sh, stat, new ByteArrayInputStream(data));
            float t = (stat.completed - stat.started) / 1000000f;
            Map m = sh.root();
            bjson2.scan(sh, stat, new ByteArrayInputStream(data2));
            float t1 = (stat.completed - stat.started) / 1000000f;
            Map m2 = sh.root();

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
