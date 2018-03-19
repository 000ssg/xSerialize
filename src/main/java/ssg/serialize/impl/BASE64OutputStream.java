/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author 000ssg
 */
public class BASE64OutputStream extends FilterOutputStream {

    byte[] ibuf = new byte[BASE64Serializer.BUFFER_BASE_SIZE * 3];
    byte[] obuf = new byte[BASE64Serializer.BUFFER_BASE_SIZE * 4];
    int pos = 0;
    int wrapAt = 76;
    int wc = 0;

    public BASE64OutputStream(OutputStream out) {
        super(out);
    }

    public BASE64OutputStream(OutputStream out, int wrapAt) {
        super(out);
        setWrapAt(wrapAt);
    }

    public void setWrapAt(int wrapAt) {
        if (wrapAt > 3) {
            wrapAt = wrapAt - (wrapAt % 4);
        }
    }

    public int getWrapAt() {
        return wrapAt;
    }

    @Override
    public void close() throws IOException {
        flushBASE64(true);
        flush();
        super.close();
    }

    public void flushBASE64(boolean close) throws IOException {
        if (pos > 0) {
            int lc = (close) ? pos : pos - (pos % 3);
            int c = BASE64Serializer.write(ibuf, 0, lc, obuf, 0);
            if (wrapAt != 0) {
                if (wc + c > wrapAt) {
                    int cc = wrapAt - wc;
                    out.write(obuf, 0, cc);
                    out.write(BASE64Serializer.CRLF);
                    while (c - cc > wrapAt) {
                        out.write(obuf, cc, wrapAt);
                        out.write(BASE64Serializer.CRLF);
                        cc += wrapAt;
                    }
                    wc = c - cc;
                    out.write(obuf, cc, wc);
                } else {
                    out.write(obuf, 0, c);
                    wc += c;
                }
            } else {
                out.write(obuf, 0, c);
            }
            if (lc != pos) {
                // shift reminder data to start of buffer
                System.arraycopy(ibuf, pos - (pos - lc), ibuf, 0, pos - lc);
                pos = pos - lc;
            }else pos=0;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            int lc = Math.min(ibuf.length - pos, len);
            System.arraycopy(b, off, ibuf, pos, lc);
            off += lc;
            len -= lc;
            pos += lc;
            if (pos == ibuf.length) {
                flushBASE64(false);
            }
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(int b) throws IOException {
        ibuf[pos++] = (byte) b;
        if (pos == ibuf.length) {
            flushBASE64(false);
        }
    }

}
