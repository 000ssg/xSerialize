/*
 * AS IS
 */
package ssg.serialize.base;

import ssg.serialize.Serialize;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Basic implementation of serializer interface with abstract read/write methods
 * left for actual implementation.
 *
 * @author 000ssg
 */
public abstract class BaseSerialize implements Serialize {

    // string to/from bytes default encoding
    String encoding = "UTF-8";

    public BaseSerialize() {
    }

    @Override
    public String toText(Object obj) throws IOException {
        byte[] buf = toBytes(obj);
        if (buf != null) {
            return new String(buf, encoding);
        } else {
            return null;
        }
    }

    @Override
    public byte[] toBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long c = BaseSerialize.this.toStream(obj, baos);
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
}
