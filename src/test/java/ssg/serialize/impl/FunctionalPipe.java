/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import ssg.serialize.StreamSerializer;

/**
 *
 * @author 000ssg
 */
public class FunctionalPipe {

    static TestPOJO1 testPOJO = TestPOJO.randomPOJO(TestPOJO1.class, 100);
    static String testString = TestPOJO.generateItem(String.class, 50000);
    static byte[] testBytes = TestPOJO.generateItem(byte[].class, 50000);

    static StreamSerializer ser1 = new BASE64Serializer();
    static StreamSerializer ser2 = new BASE64Serializer();
    static StreamSerializer ser3 = new UTF8Serializer();

    public static void main(String[] args) throws Exception {

        ByteArrayOutputStream baos0 = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos0);
        oos.writeObject(testPOJO);

        // bytes
        byte[] b00 = baos0.toByteArray();// ser1.toBytes(testPOJO);
        byte[] b01 = ser1.toBytes(testString);
        byte[] b02 = ser1.toBytes(testBytes);
        byte[] b11 = ser2.toBytes(testString);
        byte[] b12 = ser2.toBytes(testBytes);
        byte[] b21 = ser3.toBytes(testString);
        byte[] b22 = ser3.toBytes(testBytes);

        // pipe in/out
        int bIdx = 0;
        for (Object[] bbo : new Object[][]{
            {"POJO (Obj)", b00},
            {"String (Obj)", b01},
            {"bytes (Obj)", b02},
            {"String (B64)", b11},
            {"bytes (B64)", b12},
            {"String (UTF8)", b21},
            {"bytes (UTF8)", b22}}) {
            String title = (String) bbo[0];
            byte[] bb = (byte[]) bbo[1];
            InputStream bais = new ByteArrayInputStream(bb);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            baos.reset();
            try {
                bais = new ByteArrayInputStream(bb);
                ser1.pipe(bais, baos, false);
                System.out.println("Obj [" + bIdx + "] -> " + title + "\t" + bb.length + " -> " + baos.size());
                bais = new ByteArrayInputStream(baos.toByteArray());
                baos.reset();
                ser1.pipe(bais, baos, true);
                System.out.println("Obj [" + bIdx + "] <- " + baos.size());
            } catch (IOException ioex) {
                System.out.println("Obj [" + bIdx + "] " + title + "\t" + ioex);
            } catch (Throwable th) {
                System.out.println("Obj [" + bIdx + "] " + title + "\t" + th);
            }

            baos.reset();
            try {
                bais = new ByteArrayInputStream(bb);
                ser2.pipe(bais, baos, false);
                System.out.println("B64 [" + bIdx + "] " + title + "\t" + bb.length + " -> " + baos.size());
                bais = new ByteArrayInputStream(baos.toByteArray());
                baos.reset();
                ser2.pipe(bais, baos, true);
                System.out.println("B64 [" + bIdx + "] <- " + baos.size());
            } catch (IOException ioex) {
                System.out.println("B64 [" + bIdx + "] " + title + "\t" + ioex);
            } catch (Throwable th) {
                System.out.println("B64 [" + bIdx + "] " + title + "\t" + th);
            }

            baos.reset();
            try {
                bais = new ByteArrayInputStream(bb);
                ser3.pipe(bais, baos, false);
                System.out.println("UTF8[" + bIdx + "] " + title + "\t" + bb.length + " -> " + baos.size());
                bais = new ByteArrayInputStream(baos.toByteArray());
                baos.reset();
                ser3.pipe(bais, baos, true);
                System.out.println("UTF8[" + bIdx + "] <- " + baos.size());
            } catch (IOException ioex) {
                System.out.println("UTF8[" + bIdx + "] " + title + "\t" + ioex);
            } catch (Throwable th) {
                System.out.println("UTF8[" + bIdx + "] " + title + "\t" + th);
            }
            bIdx++;
        }
    }
}
