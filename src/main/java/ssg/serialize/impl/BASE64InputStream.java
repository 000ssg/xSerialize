/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Apply Base64 decoding to base input stream data. New lines (CR,LF) are
 * ignored.
 *
 * @author 000ssg
 */
public class BASE64InputStream extends FilterInputStream {

    byte[] ibuf = new byte[BASE64Serializer.BUFFER_BASE_SIZE * 4];
    byte[] obuf = new byte[BASE64Serializer.BUFFER_BASE_SIZE * 3];
    int ipos = 0;
    int opos = 0;
    int olen = 0;

    public BASE64InputStream(final InputStream is) {
        super(new InputStream() {
            @Override
            public int read() throws IOException {
                // filter to skip CR/LF
                int r = is.read();
                while (r == '\r' || r == '\n') {
                    r = is.read();
                }
                return r;
            }
        });
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        ipos = 0;
        opos = 0;
        olen = 0;
    }

    @Override
    public long skip(long n) throws IOException {
        ipos = 0;
        opos = 0;
        olen = 0;
        return super.skip(n);
    }

    /**
     * Prepare EMPTY output buffer.
     *
     * @return
     * @throws IOException
     */
    public int fetch() throws IOException {
        ipos = BaseDeserialize.readFull(ibuf, 0, super.in);
        if (ipos == -1 || ipos == 0) {
            return -1;
        }
        int[] offs = BASE64Serializer.read(ibuf, 0, ipos, obuf, 0);
        ipos = 0;
        opos = 0;
        olen = offs[1];
        return offs[1];
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (olen == -1) {
            return -1;
        }
        int c = 0;
        while (len > 0) {
            if (olen == -1) {
                return -1;
            } else if (olen == 0) {
                if (fetch() == -1) {
                    olen = -1;
                    break;
                }
            }
            int lc = Math.min(olen, len);
            System.arraycopy(obuf, 0, b, off, lc);
            off += lc;
            len -= lc;
            olen -= lc;
            c += lc;
        }
        return c;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read() throws IOException {
        if (olen == -1) {
            return -1;
        } else if (olen == 0) {
            if (fetch() == -1) {
                olen = -1;
                return -1;
            }
        }
        int r = obuf[opos++];
        olen--;
        return r;
    }
}
