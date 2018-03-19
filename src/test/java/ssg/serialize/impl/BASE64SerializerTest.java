/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.utils.RW;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
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
public class BASE64SerializerTest {

    public BASE64SerializerTest() {
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
     * Test of write method, of class BASE64Serializer.
     */
    @Test
    public void testWrite_InputStream_OutputStream() throws Exception {
        System.out.println("write");

        int check = 0;
        for (int i = 0; i < 100; i++) {
            //System.out.println("  max size: "+i);
            byte[][] test = testBytes(i);
            //InputStream is = new ByteArrayInputStream(test[0]);
            ByteArrayOutputStream os1 = new ByteArrayOutputStream();
            ByteArrayOutputStream os2 = new ByteArrayOutputStream();
            long result1 = BASE64Serializer.write(new ByteArrayInputStream(test[0]), os1, 0);
            long result2 = BASE64Serializer.write(new ByteArrayInputStream(test[0]), os2, 76);
            if (i == check) {
                String s0 = (test[1] != null) ? new String(test[1]) : null;
                String s1 = (test[2] != null) ? new String(test[2]) : null;
                String s20 = new String(os1.toByteArray());
                String s21 = new String(os2.toByteArray());
                System.out.println(
                        "For buf len " + i
                        + "\nR0: " + s0
                        + "\nB0: " + s20
                        + "\nR1: " + s1
                        + "\nB1: " + s21
                );
            }
            if (test[1] != null) {
                assertEquals(test[1].length, result1);
            }
            if (test[2] != null) {
                assertEquals(test[2].length, result2);
            }
            if (test[1] != null) {
                assertArrayEquals(test[1], os1.toByteArray());
            }
            if (test[2] != null) {
                assertArrayEquals(test[2], os2.toByteArray());
            }

            ByteArrayOutputStream os21 = new ByteArrayOutputStream();
            ByteArrayOutputStream os22 = new ByteArrayOutputStream();
            long result21 = BASE64Serializer.read(new ByteArrayInputStream(os1.toByteArray()), os21);
            long result22 = BASE64Serializer.read(new ByteArrayInputStream(os2.toByteArray()), os22);
            if (i == check) {
                String s0 = new String(test[0]);
                String s21 = new String(os21.toByteArray());
                String s22 = new String(os22.toByteArray());
                System.out.println(
                        "For buf len " + i
                        + "\nR0: " + s0
                        + "\nB0: " + s21
                        + "\nB1: " + s22
                );
            }
            assertEquals(test[0].length, result21);
            assertEquals(test[0].length, result22);
            assertArrayEquals(test[0], os21.toByteArray());
            assertArrayEquals(test[0], os22.toByteArray());

        }
    }

    @Test
    public void testPipes() throws IOException {
        byte[] r = new byte[400000];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) (0xFF & (i % 255));
        }

        ByteArrayOutputStream baos0 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(r);

        BASE64Serializer base64 = new BASE64Serializer();

        for (int i = 16; i < 1000; i += 3) {
            byte[] rr = Arrays.copyOf(r, i);
            baos.reset();
            baos0.reset();
            bais = new ByteArrayInputStream(rr);

            // generate reference encoding and test decoded data (pipe)
            base64.pipe(bais, baos0, true);
            base64.pipe(new ByteArrayInputStream(baos0.toByteArray()), baos, false);
            //System.out.print(i+" ");
//            System.out.println("[" + i + "]"
//                    + "\n-------------- base64=" + baos0.size() + "\n" + baos0.toString()
//                    + "\n-------------- bytes =" + baos.size() + " = " + rr.length + "  " + (baos.size() == rr.length)
//            );
            assertArrayEquals(rr, baos.toByteArray());
        }
        //System.out.println();
    }

    @Test
    public void testStreams() throws Throwable {
        byte[] r = new byte[400000];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) (0xFF & (i * 15 - i % 255));
        }

        ByteArrayOutputStream baos0 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(r);

        BASE64Serializer base64 = new BASE64Serializer();

        try {
            for (int i = 16; i < r.length/100; i += 3) {//1000; i += 3) {
                byte[] rr = Arrays.copyOf(r, i);
                baos.reset();
                baos0.reset();
                bais = new ByteArrayInputStream(rr);
//                System.out.println("[" + i + "]");
                // generate reference encoding and test decoded data (pipe)
                base64.pipe(bais, baos0, true);
//                if (i == 14653) {
//                    System.out.println("[" + i + "]"
//                            + "\n-------------- ref=" + baos0.size() + "\n" + baos0.toString()
//                    //                            + "\n-------------- str=" + baos.size() + "\n" + baos.toString()
//                    );
//                }

                base64.pipe(new ByteArrayInputStream(baos0.toByteArray()), baos, false);
                //System.out.print(i+" ");
                assertArrayEquals(rr, baos.toByteArray());

                // encode
                baos.reset();
                BASE64OutputStream o1 = new BASE64OutputStream(baos);
                o1.write(rr);
                o1.close();

                if (1 == 0) {
                    System.out.println("[" + i + "]"
                            + "\n-------------- ref=" + baos0.size() + "\n" + baos0.toString()
                            + "\n-------------- str=" + baos.size() + "\n" + baos.toString()
                    );
                }

                assertArrayEquals(baos0.toByteArray(), baos.toByteArray());

                byte[] rrr = baos.toByteArray();
                baos.reset();
                BASE64InputStream i1 = new BASE64InputStream(new ByteArrayInputStream(rrr));
                byte[] rrrr = new byte[rrr.length];
                int lc = i1.read(rrrr);
                assertEquals(rr.length, lc);
                assertArrayEquals(rr, Arrays.copyOf(rrrr, lc));
            }
        } catch (Throwable th) {
            throw th;
        }
        //System.out.println();
    }

    public byte[][] testBytes(int max) throws Exception {
        String s = "ewthw6ju4w6h 4t2  3r_2 3уыеко75ул53оäråyketletLYOIUTÖI/Ra2-----------------------------------------------------------------------------------------------------------------------------1";
        byte[][] r = new byte[3][];
        try {
            r[0] = s.getBytes("ISO-8859-1");
        } catch (Throwable th) {
            r[0] = "aSW2FR3TNTNMW5Y7KE5".getBytes();
        }
        //r[0]="1234".getBytes();
        //r[0] = " 3уыекол53оäråy".getBytes("ISO-8859-1");
        if (max > 0 && r[0].length > max) {
            r[0] = Arrays.copyOf(r[0], max);
        }

        RW rw0 = RW.getRW("Base64");
        RW rw1 = RW.getRW("BASE64Encoder");
        r[1] = (rw0 != null && rw0.canWrite()) ? ((String) rw0.write(r[0])).getBytes() : null;
        r[2] = (rw1 != null && rw1.canWrite()) ? ((String) rw1.write(r[0])).getBytes() : null;

//        BASE64Encoder enc = new BASE64Encoder();
//        r[1] = Base64.getEncoder().encodeToString(r[0]).getBytes();
//        r[2] = enc.encode(r[0]).getBytes();
        return r;
    }

    String dumpB64(String s) {
        StringBuilder sb = new StringBuilder();
        for (String s0 : s.split("\n")) {
            for (int i = 0; i < s0.length() / 4; i++) {
                sb.append(s0, i, Math.min(s0.length(), i + 4));
                sb.append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
