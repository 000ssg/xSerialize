/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

/**
 *
 * @author 000ssg
 */
public class BASE64Serializer extends BaseStreamSerializer {

    static final private byte b64[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();
    static final private byte r64[] = new byte[128];
    static int BUFFER_BASE_SIZE = 256;//256*2;

    static {
        Arrays.fill(r64, 0, r64.length - 1, (byte) 0xFF);
        for (int i = 0; i < b64.length; i++) {
            r64[b64[i]] = (byte) i;
        }
    }

    static final byte PARTIAL = '=';
    static final byte CRLF[] = {'\r', '\n'};
    static BASE64Serializer shared = new BASE64Serializer();

    public int wrapAt = 76;

    public BASE64Serializer() {
        encoding = "ISO-8859-1";
    }

    /**
     * Usability method.
     *
     * @param buf
     * @return
     * @throws IOException
     */
    public static String encode(byte[] buf) throws IOException {
        return shared.toText(buf);
    }

    /**
     * Usability method.
     *
     * @param s
     * @return
     * @throws IOException
     */
    public static byte[] decode(String s) throws IOException {
        return shared.fromText(s);
    }

    @Override
    public byte[] fromBytes(byte[] data) throws IOException {
        byte[] buf = data;
        byte[] out = new byte[buf.length * 3 / 4];
        int[] r = read(buf, 0, buf.length, out, 0);
        int c = r[1];
        if (c < out.length) {
            return Arrays.copyOf(out, c);
        }
        return out;
    }

    @Override
    public byte[] toBytes(Object obj) throws IOException {
        if (obj instanceof byte[]) {
            byte[] buf = (byte[]) obj;
            byte[] out = new byte[(buf.length / 3 + ((buf.length % 3 > 0) ? 1 : 0)) * 4];
            int c = write(buf, 0, buf.length, out, 0);
            if (c < out.length) {
                return Arrays.copyOf(out, c);
            }
            return out;
        } else {
            return super.toBytes(obj);
        }
    }

    @Override
    public byte[] fromText(String text) throws IOException {
        return fromBytes(text.getBytes(encoding));
    }

    @Override
    public long write(Object obj, OutputStream os) throws IOException {
        // to string if known simple...
        if (obj instanceof File || obj instanceof URI || obj instanceof URL) {
            obj = obj.toString();
        }

        if (obj instanceof InputStream) {
            return write((InputStream) obj, os, wrapAt);
        } else {
            if (obj instanceof String) {
                obj = ((String) obj).getBytes(encoding);
            }
            if (obj instanceof byte[]) {
                byte[] buf = (byte[]) obj;
                byte[] out = new byte[Math.round(buf.length * 4 / 3f) + 2];
                int c = write(buf, 0, buf.length, out, 0);
                os.write(out, 0, c);
                return c;
            }
        }

        throw new IOException(getClass().getName() + ": Can write only byte arrays, got " + ((obj != null) ? obj.getClass().getName() : "null"));
    }

    @Override
    public byte[] read(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long c = read(is, baos);
        return baos.toByteArray();
    }

    /**
     * Reads len bytes from off in buf and writes to out.
     *
     * @param buf source Base64 data buffer
     * @param off offset in source buffer
     * @param len len to read from source buffer
     * @param out output (decoded) data buffer
     * @param ooff initial offset in output buffer
     * @return resultant offsets in in and out buffers
     */
    public static int[] read(byte[] buf, int off, int len, byte[] out, int ooff) throws IOException {
        int[] r = new int[]{off, ooff};
        int max = off + len;
        if (len == 0) {
            return r;
        }
        for (int i = 0; i < len; i += 4) {
            int j = off + i;
            // skip \r\n if detected
            if (buf[j] == '\r') {
                j++;
                i++;
            }
            if (buf[j] == '\n') {
                j++;
                i++;
            }
            if (max - j < 4) {
                // can't continue, return as is
                r[0] = j;
                break;
            }
            if (!testBASE64(buf[j])) {
                throw new IOException("Unexpected BASE64 symbol: " + buf[j] + "  " + (char) (0xff & buf[j]));
            }
            if (!testBASE64(buf[j + 1])) {
                throw new IOException("Unexpected BASE64 symbol: " + buf[j + 1] + "  " + (char) (0xff & buf[j + 1]));
            }
            r[0] = j + 4;
            byte o0 = r64[buf[j]];
            byte o1 = r64[buf[j + 1]];
            out[ooff++] = (byte) (((o0 << 2) | (o1 >> 4)) & 0xFF);
            if (buf[j + 2] != PARTIAL) {
                if (!testBASE64(buf[j + 2])) {
                    throw new IOException("Unexpected BASE64 symbol: " + buf[j + 2] + "  " + (char) (0xff & buf[j + 2]));
                }
                byte o2 = r64[buf[j + 2]];
                out[ooff++] = (byte) (((o1 << 4) | (o2 >> 2)) & 0xFF);
                if (buf[j + 3] != PARTIAL) {
                    if (!testBASE64(buf[j + 3])) {
                        throw new IOException("Unexpected BASE64 symbol: " + buf[j + 3] + "  " + (char) (0xff & buf[j + 3]));
                    }
                    byte o3 = r64[buf[j + 3]];
                    out[ooff++] = (byte) (((o2 << 6) | o3) & 0xFF);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        r[1] = ooff;
        return r;
    }

    /**
     * Stream encoding. Reads data in portions up to 76 bytes (19 groups of
     * 4-byte sequences) skipping optional CRLF (13,10) line break sequences.
     *
     * @param is
     * @param os
     * @return number of decoded bytes written to output stream
     * @throws IOException
     */
    public static long read(InputStream is, OutputStream os) throws IOException {
        long r = 0;
        byte[] buf = new byte[BUFFER_BASE_SIZE * 4];
        int rOff = 0;
        byte[] bufOut = new byte[buf.length / 4 * 3];
        int c = 0;
        while ((c = is.read(buf, rOff, buf.length - rOff)) != -1) {
            rOff = 0;
            while (c < buf.length) {
                int cc = is.read(buf, c, buf.length - c);
                if (cc != -1) {
                    c += cc;
                } else {
                    break;
                }
            }

            int[] oo = read(buf, 0, c, bufOut, 0);
            int bc = oo[1];
            if (bc > 0) {
                os.write(bufOut, 0, bc);
                r += bc;
            }
            if (oo[0] < c) {
                rOff = c - oo[0];
                System.arraycopy(buf, oo[0], buf, 0, rOff);
            }

            if (c < 4) {
                break;
            }
            c = 0;
        }
        return r;
    }

    /**
     * Reads len bytes from off in buf and writes to out.
     *
     * @param buf source (raw) data buffer
     * @param off offset in source buffer
     * @param len len to read from source buffer
     * @param out output (Base64) data buffer
     * @param ooff initial offset in output buffer
     * @return number of bytes written to output buffer
     */
    public static int write(byte[] buf, int off, int len, byte[] out, int ooff) {
        int ooff0 = ooff;
        for (int i = 0; i < len; i += 3) {
            int j = off + i;
            int c = len - i;
            // [0], 6 bits
            out[ooff++] = b64[0x3f & (buf[j] >> 2)];
            if (c > 1) {
                // [0] 2 bits (up) 0 + [1] 4 bits
                out[ooff++] = b64[0x3F & (((buf[j] & 0x3) << 4) | (buf[j + 1] >> 4) & 0x0f)];
                if (c > 2) {
                    // [1] 4 bits up + [2] 2 bits
                    out[ooff++] = b64[0x3F & (((buf[j + 1] & 0xf) << 2) | 0x3 & (0xFF & buf[j + 2] >> 6))];
                    out[ooff++] = b64[0x3F & buf[j + 2]];
                } else {
                    out[ooff++] = b64[0x3F & (((buf[j + 1] & 0xf) << 2))];
                    out[ooff++] = PARTIAL;
                }
            } else {
                out[ooff++] = b64[0x3F & (((buf[j] & 0x3) << 4))];
                out[ooff++] = PARTIAL;
                out[ooff++] = PARTIAL;
            }
        }
        return ooff - ooff0;
    }

    public static long write(InputStream is, OutputStream os, int wrapAt) throws IOException {
        long r = 0;
        int lc = 0;

        if (wrapAt != 0) {
            wrapAt = (wrapAt / 4) * 4;
        }

        byte[] bufOut = new byte[(wrapAt > 0) ? wrapAt : BUFFER_BASE_SIZE * 4];
        byte[] buf = new byte[bufOut.length / 4 * 3];

        int c = 0;
        boolean lastIsPartial = false;
        while ((c = is.read(buf)) != -1) {
            while (c < buf.length) {
                int cc = is.read(buf, c, buf.length - c);
                if (cc == -1) {
                    break;
                } else {
                    c += cc;
                }
            }

            if (wrapAt > 0) {
                if (r % wrapAt == 0) {
                    if (r > 0) {
                        os.write(CRLF);
                        lc += CRLF.length;
                    }
                }
            }

            int l = write(buf, 0, c, bufOut, 0);
            r += l;
            if (l > 0) {
                os.write(bufOut, 0, l);
                lastIsPartial = bufOut[l - 1] == PARTIAL;
                if (lastIsPartial) {
                    break;
                }
            }
        }
        if (wrapAt > 0 && r % wrapAt == 0 && !lastIsPartial) {
            os.write(CRLF);
            lc += CRLF.length;
        }
        return r + lc;
    }

    @Override
    public long[] pipe(InputStream is, OutputStream os, boolean encode) throws IOException {
        if (encode) {
            return BASE64Serializer.pipeEncode(is, os, wrapAt);
        } else {
            return BASE64Serializer.pipeDecode(is, os);
        }
    }

    /**
     * Stream encoder. Accepts input stream bytes and writes to output stream
     * Base64 encoded bytes with optional lines wrapping (if wrapAt = 0 - no
     * wrapping).
     *
     * @param is
     * @param os
     * @param wrapAt
     * @return
     * @throws IOException
     */
    public static long[] pipeEncode(InputStream is, OutputStream os, int wrapAt) throws IOException {
        long[] r = new long[2];

        byte[] bufIn = new byte[BUFFER_BASE_SIZE * 3];
        byte[] bufOut = new byte[((bufIn.length + 2) * 4) / 3];

        int c = 0;
        int off = 0;

        if (wrapAt > 0) {
            int wc = 0;
            while ((c = is.read(bufIn, off, bufIn.length - off)) != -1) {
                //System.out.println("    " + r[0] + "  " + r[1]);
                r[0] += c;
                c += off;

                int lc = c - (c % 3);
                int cc = BASE64Serializer.write(bufIn, 0, lc, bufOut, 0);

                int c0 = wc % wrapAt;
                if (c0 + cc > wrapAt) {
                    if (wc == wrapAt) {
                        os.write(CRLF);
                    }
                    int c1 = wrapAt - c0;
                    os.write(bufOut, 0, c1);
                    os.write(CRLF);
                    while (cc - c1 > wrapAt) {
                        os.write(bufOut, c1, wrapAt);
                        os.write(CRLF);
                        c1 += wrapAt;
                    }
                    os.write(bufOut, c1, cc - c1);
                    wc = cc - c1;
                } else {
                    if (wc == wrapAt) {
                        os.write(CRLF);
                        wc = 0;
                    }
                    os.write(bufOut, 0, cc);
                    wc += cc;
                }

                r[1] += cc;
                if (lc < c) {
                    // copy tail of buf to start and adjust off
                    for (int i = lc; i < c; i++) {
                        bufIn[i - lc] = bufIn[i];
                    }
                    off = c - lc;
                } else {
                    off = 0;
                }
            }

            if (off > 0) {
                if (wc == wrapAt) {
                    os.write(CRLF);
                    wc = 0;
                }
                int cc = BASE64Serializer.write(bufIn, 0, off, bufOut, 0);
                os.write(bufOut, 0, cc);
                wc += cc;
                r[1] += cc;
            }

        } else {
            while ((c = is.read(bufIn, off, bufIn.length - off)) != -1) {
                r[0] += c;
                c += off;

                int lc = c - (c % 3);
                int cc = BASE64Serializer.write(bufIn, 0, lc, bufOut, 0);

                os.write(bufOut, 0, cc);

                r[1] += cc;
                if (lc < c) {
                    // copy tail of buf to start and adjust off
                    for (int i = lc; i < c; i++) {
                        bufIn[i - lc] = bufIn[i];
                    }
                    off = c - lc;
                } else {
                    off = 0;
                }
            }
            if (off > 0) {
                int cc = BASE64Serializer.write(bufIn, 0, off, bufOut, 0);
                os.write(bufOut, 0, off);
                r[1] += off;
            }
        }

        return r;
    }

    /**
     * Stream decoder: accepts Base64-encoded content at input stream (with
     * ignoring line wraps) and wrties to output decoded bytes.
     *
     * @param is
     * @param os
     * @return
     * @throws IOException
     */
    public static long[] pipeDecode(InputStream is, OutputStream os) throws IOException {
        long[] r = new long[2];

        byte[] bufOut = new byte[BUFFER_BASE_SIZE * 3];
        byte[] bufIn = new byte[(bufOut.length * 4 + 2) / 3];

        final InputStream iis = is;
        is = new InputStream() {
            @Override
            public int read() throws IOException {
                // filter to skip CR/LF
                int r = iis.read();
                while (r == '\r' || r == '\n') {
                    r = iis.read();
                }
                return r;
            }
        };

        int c = 0;
        int off = 0;
        int wc = 0;
        while ((c = is.read(bufIn, off, bufIn.length - off)) != -1) {
            r[0] += c;
            c += off;
            int[] cc = BASE64Serializer.read(bufIn, 0, c, bufOut, 0);
            os.write(bufOut, 0, cc[1]);
            r[1] += cc[1];
            if (cc[0] < c) {
                // copy tail of buf to start and adjust off
                for (int i = cc[0]; i < c; i++) {
                    bufIn[i - cc[0]] = bufIn[i];
                }
                off = c - cc[0];
            } else {
                off = 0;
            }
        }

        return r;
    }

    /**
     * Returns true if b is valid BASE64 character (CR,LF are not valid!)
     *
     * @param b
     * @return
     */
    public static boolean testBASE64(byte b) {
        return (b & 0x80) == 0 && r64[b] != 0xFF;
    }
}
