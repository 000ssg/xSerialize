/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.Deserialize;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Basic implementation of serializer interface with abstract read/write methods
 * left for actual implementation.
 *
 * @author 000ssg
 */
public abstract class BaseDeserialize implements Deserialize {

    // string to/from bytes default encoding
    String encoding = "UTF-8";

    public BaseDeserialize() {
    }

    @Override
    public Object fromText(String text) throws IOException {
        if (text != null) {
            return fromBytes(text.getBytes(encoding));
        } else {
            return null;
        }
    }

    @Override
    public Object fromBytes(byte[] data) throws IOException {
        if (data != null) {
            return fromStream(new ByteArrayInputStream(data));
        } else {
            return null;
        }
    }

    @Override
    public Object fromStream(InputStream is) throws IOException {
        return read(is);
    }

    @Override
    public Object fromURL(URL url) throws IOException {
        return read(url.openStream());
    }
    /**
     * Tries to read from input stream as many bytes as requested.
     *
     * @param buf
     * @param bytes
     * @param is
     * @return
     * @throws IOException
     */
    public static int readFull(byte[] buf, int bytes, InputStream is) throws IOException {
        int c = is.read(buf);
        if (c == -1) {
            return -1;
        }
        while (c < bytes) {
            int c1 = is.read(buf, c, buf.length - c);
            if (c1 == -1) {
                throw new IOException("Failed to read cardinal value of " + bytes + " len, got " + c + " only.");
            }
            c += c1;
        }
        return c;
    }

}
