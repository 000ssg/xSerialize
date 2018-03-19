/*
 * AS IS
 */
package ssg.serialize.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import ssg.serialize.ObjectSerializer;
import ssg.serialize.impl.BJSONSerializer;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author 000ssg
 */
public class DeepCompareTest {

    public DeepCompareTest() {
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
     * Test of diff method, of class DeepCompare.
     */
    @Test
    public void testDiff() throws Exception {
        System.out.println("diff");
        ObjectSerializer objser = new BJSONSerializer();
        TestPOJO.TestPOJO1 a = TestPOJO.randomPOJO(TestPOJO.TestPOJO1.class);
        TestPOJO.TestPOJO1 aa = a;
        TestPOJO.TestPOJO1 b = objser.fromBytes(objser.toBytes(a), TestPOJO1.class);
        Object b2 = objser.fromBytes(objser.toBytes(a));
        TestPOJO.TestPOJO1 c = TestPOJO.randomPOJO(TestPOJO.TestPOJO1.class);
        long options = DeepCompare.DCO_STRONGER;
        long xoptions = DeepCompare.DCO_DEFAULT;

        // identity compare
        DeepCompare.DC result = DeepCompare.diff(a, aa, options);
        System.out.println("Identity: " + result.toString(true));
        assertEquals(DeepCompare.DIFF.same, result.state);

        // fully restored item (via serialization) compare
        result = DeepCompare.diff(a, b, options);
        System.out.println("Full (ser/deser): " + result.toString(true));
        assertEquals(DeepCompare.DIFF.same, result.state);

        // partially restored item (via serialization, not type match) compare
        result = DeepCompare.diff(a, b2, options);
        System.out.println("Partial (ser/deser): " + result.toString(true));
        assertEquals(DeepCompare.DIFF.value, result.state);
        result = DeepCompare.diff(a, b2, xoptions);
        System.out.println("PartialX(ser/deser): " + result.toString(true));
        assertEquals(DeepCompare.DIFF.same, result.state);

        // different items compare
        result = DeepCompare.diff(a, c, options);
        System.out.println("Dirfferent: " + result.toString(true));
        assertEquals(DeepCompare.DIFF.value, result.state);
    }

    /**
     * Test of diff method, of class DeepCompare.
     */
    @Test
    public void testDiffOptions() throws Exception {
        System.out.println("diff options");
        long options = DeepCompare.DCO_STRONGER;
        long xoptions = DeepCompare.DCO_DEFAULT;
        long xxoptions = DeepCompare.DCO_DEFAULT
                | DeepCompare.DCO_IGNORE_COLLECTION_NULLS
                | DeepCompare.DCO_IGNORE_MISSING_MAP_NULLS;

        Map a = new LinkedHashMap();
        Map b = new LinkedHashMap();

        DeepCompare.DC dc = DeepCompare.diff(a, b, options);
        DeepCompare.DC dcx = DeepCompare.diff(a, b, xoptions);
        DeepCompare.DC dcxx = DeepCompare.diff(a, b, xxoptions);

        System.out.println("\n1: "+show(diff(a, b, options, xoptions, xxoptions), "DC", "DCX", "DCXX"));
        a.put("Empty", null);
        System.out.println("\n2: "+show(diff(a, b, options, xoptions, xxoptions), "DC", "DCX", "DCXX"));
        b.put("Empty", null);
        a.put("Text A", "A");
        System.out.println("\n3: "+show(diff(a, b, options, xoptions, xxoptions), "DC", "DCX", "DCXX"));
        b.put("Text A", "A");
        System.out.println("\n4: "+show(diff(a, b, options, xoptions, xxoptions), "DC", "DCX", "DCXX"));
        a.put("Byte1", new Byte[]{0,null,1});
        b.put("Byte1", new Byte[]{0,1});
        System.out.println("\n5: "+show(diff(a, b, options, xoptions, xxoptions), "DC", "DCX", "DCXX"));
        a.put("Byte2", new Byte[]{0,null,1});
        b.put("Byte2", new Byte[]{0,1,null,null});
        System.out.println("\n6: "+show(diff(a, b, options, xoptions, xxoptions), "DC", "DCX", "DCXX"));
    }

    public static DeepCompare.DC[] diff(Object a, Object b, long... options) {
        DeepCompare.DC[] r = new DeepCompare.DC[options.length];
        for (int i = 0; i < options.length; i++) {
            r[i] = DeepCompare.diff(a, b, options[i]);
        }
        return r;
    }

    public static String show(DeepCompare.DC[] dcs, String... titles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dcs.length; i++) {
            String t = (titles != null && i < titles.length) ? titles[i] : "[" + i + "]";
            sb.append("\n" + t + "\t");
            if (dcs[i] != null) {
                sb.append(dcs[i].toString(false).replace("\n", "\n  \t"));
            }
        }
        return sb.toString();
    }

}
