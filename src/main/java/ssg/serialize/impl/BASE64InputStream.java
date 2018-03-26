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

    /**
     * Prepares BASE64 decoder.
     *
     * @param is
     */
    public BASE64InputStream(final InputStream is) {
        super(new InputStream() {
            @Override
            public int read() throws IOException {
                // filter to skip CR/LF
                int c = is.read();
                while (c == '\r' || c == '\n') {
                    c = is.read();
                }
                return c;
            }
        });
    }

    /**
     * Section-aware input stream handling support.
     *
     * If sectionOnly=true, then input stream content is skipped up to "\r\n"
     * and is forced to EOF once "\r\n-" is detected simulating section content
     * within section boundaries like:
     *
     * ----- header\r\n
     *
     * content
     *
     * \r\n------ end of section
     *
     * @param is
     * @param sectionOnly
     */
    public BASE64InputStream(final InputStream is, boolean sectionOnly) {
        super((sectionOnly)
                ? new InputStream() {
            int prelast = 0;
            int last = 0;
            boolean EOF = false;
            long pos = 0;

            {
                try {
                    int c = 0;
                    while ((c = is.read()) != -1) {
                        if (c == '\n' && last == '\r') {
                            break;
                        }
                        last = c;
                        pos++;
                        //System.out.println("["+pos+"]   "+(""+(char)c).replace("\n", "\\n").replace("\r", "\\r"));
                    }
                    //System.out.println("["+pos+"]*  "+(""+(char)c).replace("\n", "\\n").replace("\r", "\\r"));
                    last = 0;
                    pos = 0;
                } catch (IOException ioex) {
                    int a = 0;
                }
            }

            @Override
            public int read() throws IOException {
                if (EOF) {
                    return -1;
                }
                prelast = last;

                int c = is.read();
                if (c == -1) {
                    EOF = true;
                    return c;
                }
                //System.out.println("["+pos+"] "+((c>' ') ? (char)c : "#"+c));
                pos++;
                //System.out.println("["+pos+"].  "+(""+(char)c).replace("\n", "\\n").replace("\r", "\\r"));

                if (c == '-' && last == '\n' && prelast == '\r') {
                    EOF = true;
                    return -1;
                }
                last = c;

                // filter to skip CR/LF
                while (c == '\r' || c == '\n') {
                    c = read();
                }

                return c;
            }

        }
                : new InputStream() {
            @Override
            public int read() throws IOException {
                // filter to skip CR/LF
                int c = is.read();
                while (c == '\r' || c == '\n') {
                    c = is.read();
                }
                return c;
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
        int r = 0xFF & obuf[opos++];
        olen--;
        return r;
    }
}
