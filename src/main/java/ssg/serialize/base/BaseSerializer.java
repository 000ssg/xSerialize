/*
 * AS IS
 */
package ssg.serialize.base;

import ssg.serialize.Serializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Basic implementation of serializer interface with abstract read/write methods
 * left for actual implementation.
 *
 * @author 000ssg
 */
public abstract class BaseSerializer implements Serializer {

    // string to/from bytes default encoding
    protected String encoding = "UTF-8";

    public BaseSerializer() {
    }

    @Override
    public String toText(Object obj) throws IOException {
        byte[] buf = toBytes(obj);
        if (buf != null) {
            return new String(buf, getEncoding());
        } else {
            return null;
        }
    }

    @Override
    public byte[] toBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long c = BaseSerializer.this.toStream(obj, baos);
        baos.close();
        return baos.toByteArray();
    }

    @Override
    public long toStream(Object obj, OutputStream os) throws IOException {
        return write(obj, os);
    }

    @Override
    public long toURL(Object obj, URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setDoInput(false);
        conn.setDoOutput(true);
        try {
            return write(obj, conn.getOutputStream());
        } finally {
            try {
                conn.getOutputStream().close();
            } catch (Throwable th) {
            }
        }
    }

    @Override
    public Object fromText(String text) throws IOException {
        if (text != null) {
            return fromBytes(text.getBytes(getEncoding()));
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
    public int readFull(byte[] buf, int bytes, InputStream is) throws IOException {
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

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    
}
