/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ssg.serialize.ObjectSerializer;
import ssg.serialize.utils.TestPOJO;

/**
 *
 * @author 000ssg
 */
public class JSONTest {

    public JSONTest() {
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
     * Test of toJSON method, of class JSON.
     */
    @Test
    public void testToJSON_Object_OutputStream() throws Exception {
        System.out.println("toJSON");

        ObjectSerializer json = new JSONSerializer();
        ObjectSerializer jsonx = new JSONSerializer();
        ObjectSerializer bjson = new BJSONSerializer();
        ObjectSerializer bjsonx = new BJSONSerializer();
        jsonx.setResolveCyclicReferences(true);
        bjsonx.setResolveCyclicReferences(true);

        List titles = new ArrayList();
        List result = new ArrayList();
        List outputs = new ArrayList();

        Object obj = TestPOJO.randomPOJO(CompactPOJO.class);
        ((CompactPOJO) obj).cp = TestPOJO.randomPOJO(CompactPOJO.class);
        Object objx = TestPOJO.randomPOJO(CompactPOJO.class);
        ((CompactPOJO) objx).cp = (CompactPOJO) objx;
        byte[] data = bjson.toBytes(obj);
        byte[] datax = bjsonx.toBytes(objx);
        String text = json.toText(obj);
        String textx = jsonx.toText(objx);

        long l = 0;
        String s = null;
        Object o = null;
        byte[] bb = null;
        titles.add("JSON.toJSON(obj, getNOS()))");
        result.add(l = JSON.toJSON(obj, getNOS())); // long
        outputs.add(l);
        titles.add("JSON.toJSON(obj))");
        result.add(s = JSON.toJSON(obj)); // String
        outputs.add(s);
        titles.add("JSON.toPrettyJSON(obj, getNOS()))");
        result.add(l = JSON.toPrettyJSON(obj, getNOS())); // long
        outputs.add(l);
        titles.add("JSON.toPrettyJSON(obj))");
        result.add(s = JSON.toPrettyJSON(obj)); // string
        outputs.add(s);
        titles.add("JSON.toJSONX(obj, getNOS()))");
        result.add(l = JSON.toJSONX(obj, getNOS())); // long
        outputs.add(l);
        titles.add("JSON.toJSONX(objx))");
        result.add(s = JSON.toJSONX(objx)); // string
        outputs.add(s);
        titles.add("JSON.toPrettyJSONX(objx, getNOS()))");
        result.add(l = JSON.toPrettyJSONX(objx, getNOS())); // long
        outputs.add(l);
        titles.add("JSON.toPrettyJSONX(objx))");
        result.add(s = JSON.toPrettyJSONX(objx)); // string
        outputs.add(s);
        titles.add("JSON.fromJSON(getIS(text),null))");
        result.add(o = JSON.fromJSON(getIS(text), null)); // object
        outputs.add(o);
        titles.add("JSON.fromJSON(text,null))");
        result.add(o = JSON.fromJSON(text, null)); // object
        outputs.add(o);
        titles.add("JSON.fromJSONX(getIS(textx),null))");
        result.add(o = JSON.fromJSONX(getIS(textx), null)); // object
        outputs.add(o);
        titles.add("JSON.fromJSONX(textx,null))");
        result.add(o = JSON.fromJSONX(textx, null)); // object
        outputs.add(o);
        titles.add("JSON.toBJSON(obj,getNOS()))");
        result.add(l = JSON.toBJSON(obj, getNOS())); // long
        outputs.add(l);
        titles.add("JSON.toBJSON(obj))");
        result.add(bb = JSON.toBJSON(obj)); // byte[]
        outputs.add(bb);
        titles.add("JSON.toBJSONX(objx,getNOS()))");
        result.add(l = JSON.toBJSONX(objx, getNOS())); // long
        outputs.add(l);
        titles.add("JSON.toBJSONX(objx))");
        result.add(bb = JSON.toBJSONX(objx)); // byte[]
        outputs.add(bb);
        titles.add("JSON.fromBJSON(getIS(data),null))");
        result.add(o = JSON.fromBJSON(getIS(data), null)); // object
        outputs.add(o);
        titles.add("JSON.fromBJSON(data,null))");
        result.add(o = JSON.fromBJSON(data, null)); // object
        outputs.add(o);
        titles.add("JSON.fromBJSONX(getIS(datax),null))");
        result.add(o = JSON.fromBJSONX(getIS(datax), null)); // object
        outputs.add(o);
        titles.add("JSON.fromBJSONX(datax,null))");
        result.add(o = JSON.fromBJSONX(datax, null)); // object
        outputs.add(o);

        for (int i = 0; i < titles.size(); i++) {
            System.out.println(titles.get(i));
            System.out.println("  result: " + (""+result.get(i)).replace("\n", "\n    "));
            //System.out.println("  output: " + outputs.get(i));
        }

    }

    public static class CompactPOJO {

        public byte b;
        public short sh;
        public int i;
        public long l;
        public float f;
        public double d;
        public String s;
        public CompactPOJO cp;
    }

    public static OutputStream getNOS() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }
        };
    }

    public static InputStream getIS(byte[] bb) {
        return new ByteArrayInputStream(bb);
    }

    public static InputStream getIS(String s) {
        try {
            return new ByteArrayInputStream(s.getBytes("UTF-8"));
        } catch (Throwable th) {
            return null;
        }
    }
}
