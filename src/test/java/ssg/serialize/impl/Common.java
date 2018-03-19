/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author 000ssg
 */
public class Common {

    public static File save(File f, byte[] data) {
        try {
            FileOutputStream os = new FileOutputStream(f);
            os.write(data);
            os.close();
            return f;
        } catch (IOException ioex) {
            return null;
        }
    }

    public static byte[] load(File f) {
        if (f == null || !f.exists() || !f.isFile()) {
            return null;
        }
        try {
            byte[] buf = new byte[(int) f.length()];
            FileInputStream is = new FileInputStream(f);
            is.read(buf);
            return buf;
        } catch (IOException ioex) {
            return null;
        }
    }
    
}
