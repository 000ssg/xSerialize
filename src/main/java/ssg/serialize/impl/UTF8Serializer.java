/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author 000ssg
 */
public class UTF8Serializer extends BaseStreamSerializer {

    public static final byte bit8 = (byte) 0x80;
    public static final byte bit87 = (byte) 0xC0;
    public static final byte bit876 = (byte) 0xE0;
    public static final byte bit8765 = (byte) 0xF0;
    public static final byte bit87654 = (byte) 0xF8;
    public static final byte mask3 = (byte) 0x07;
    public static final byte mask4 = (byte) 0x0F;
    public static final byte mask5 = (byte) 0x1F;
    public static final byte mask6 = (byte) 0x3F;
    public static final byte mask7 = (byte) 0x7F;

    ThreadLocal<char[]> charsVar = new ThreadLocal<char[]>();
    ThreadLocal<byte[]> bytesVar = new ThreadLocal<byte[]>();

    @Override
    public long write(Object obj, OutputStream os) throws IOException {
        // to string if known simple...
        if (obj instanceof File) {
            obj = ((File) obj).toURI().toURL().openStream();
        } else if (obj instanceof URI) {
            obj = ((URI) obj).toURL().openStream();
        } else if (obj instanceof URL) {
            obj = ((URL) obj).openStream();
        }

        if (obj instanceof InputStream) {
            return write((InputStream) obj, os);
        } else {
            if (obj instanceof byte[]) {
                obj = new String((byte[]) obj, encoding);
            }
            if (obj instanceof String) {
                obj = ((String) obj).toCharArray();
            }
            if (obj instanceof char[]) {
                byte[] buf = new byte[1024];
                char[] cbuf = (char[]) obj;
                int c = cbuf.length;
                int pos = 0;
                for (char ch : cbuf) {
                    pos += toUTF8(ch, buf, pos);
                    if (pos + 3 >= buf.length) {
                        // flush
                        os.write(buf, 0, pos);
                        c += pos;
                        pos = 0;
                    }
                }
                if (pos > 0) {
                    os.write(buf, 0, pos);
                    c += pos;
                    pos = 0;
                }

                return c;
            }
        }
        throw new IOException(getClass().getName() + ": Can write only byte arrays, strings, or input streams (or openable as input stream File, URI, URL), got " + obj);
    }

    @Override
    public Object read(InputStream is) throws IOException {
        int len = 0;
        List<char[]> chss = new ArrayList<char[]>();
        byte[] buf = getBBuffer();
        char[] cbuf = getCBuffer();
        int posB = 0;
        int posC = 0;
        int max = cbuf.length;
        int c = 0;
        while ((c = is.read(buf, posB, buf.length - posB)) != -1) {
            int[] wr = UTF8Serializer.fromUTF8(buf, 0, c, cbuf, posC);
            if (wr[0] != c) {
                posB = wr[0];
            } else {
                posB = 0;
            }
            if (wr[1] >= max) {
                chss.add(Arrays.copyOf(cbuf, wr[1]));
                len += wr[1];
                posC = 0;
            } else {
                posC = wr[1];
            }
        }

        char[] r = new char[len];
        int pos = 0;
        for (char[] chs : chss) {
            System.arraycopy(chs, 0, r, pos, chs.length);
            pos += chs.length;
        }
        if (posC > 0) {
            System.arraycopy(cbuf, 0, r, pos, posC);
        }
        return r;
    }

    byte[] getBBuffer() {
        byte[] bs = bytesVar.get();
        if (bs == null) {
            bs = new byte[1024 * 5];
            bytesVar.set(bs);
        }
        return bs;
    }

    char[] getCBuffer() {
        char[] chs = charsVar.get();
        if (chs == null) {
            chs = new char[1024];
            charsVar.set(chs);
        }
        return chs;
    }

    public static int toUTF8(char ch, byte[] buf, int off) throws IOException {
        int c = off;
        if (ch < 128) {
            buf[c++] = (byte) ch;
        } else if (ch < 2048) {
            buf[c++] = (byte) (bit87 | (ch >> 6) & mask5);
            buf[c++] = (byte) (bit8 | (ch & mask6));
        } else if (ch == 0xd800) {
        } else if (ch == 0xdc00) {
            buf[c++] = (byte) 244;
            buf[c++] = (byte) 143;
            buf[c++] = (byte) 176;
            buf[c++] = (byte) 128;
        } else if (ch >= 0xd800 && ch <= 0xdfff) {
            buf[c++] = 63;
        } else {
            buf[c++] = (byte) (bit876 | (ch >> 12) & mask4);
            buf[c++] = (byte) (bit8 | (ch >> 6) & mask6);
            buf[c++] = (byte) (bit8 | (ch & mask6));
        }
        return c - off;
    }

    public static int[] fromUTF8(byte[] buf, int off, int len, char[] out, int ooff) throws IOException {
        int[] r = new int[]{off, ooff};
        int max = off + len;
        int maxOut = out.length;
        for (int i = off; i < max; i++) {
            byte b = buf[i];
            if (b == '\r' || b == '\n') {
                r[0]++;
                continue;
            }
            if (ooff >= maxOut) {
                return r;
            }
            if ((b & bit87654) == bit8765) {
                // 4-byte
                if (i + 3 < max) {
                    return r;
                }
                int c = (b & mask3) << 18
                        | (buf[i++] & mask6) << 12
                        | (buf[i++] & mask6) << 6
                        | buf[i++] & mask6;
                r[0] += 4;
                throw new IOException("Unesupported 4-byte in BASE64 encoding: " + c);
                //out[ooff++] = (char) c;
            } else if ((b & bit8765) == bit876) {
                // 3-byte
                if (i + 2 < max) {
                    return r;
                }
                int c = (b & mask4) << 12
                        | (buf[i++] & mask6) << 6
                        | buf[i++] & mask6;
                out[ooff++] = (char) c;
                r[0] += 3;
                r[1]++;
            } else if ((b & bit876) == bit87) {
                // 2-byte
                if (i + 1 < max) {
                    return r;
                }
                int c = (b & mask5) << 6
                        | buf[i++] & mask6;
                out[ooff++] = (char) c;
                r[0] += 2;
                r[1]++;
            } else if ((b & bit8) == 0) {
                out[ooff++] = (char) (mask7 & b);
                r[0]++;
                r[1]++;
            } else {
                throw new IOException("Unexpected byte in UTF-8 encoding: " + b);
            }
        }
        return r;
    }

    @Override
    public long[] pipe(InputStream is, OutputStream os, boolean encode) throws IOException {
        long[] r = new long[2];
        byte[] buf = new byte[1024];
        char[] cbuf = new char[1024];
        if (encode) {
            Reader rdr = new InputStreamReader(is, encoding);
            int c = 0;
            int pos = 0;
            while ((c = rdr.read(cbuf)) != -1) {
                r[0] += c;
                for (int i = 0; i < c; i++) {
                    pos += toUTF8(cbuf[i], buf, pos);
                    if (pos + 3 >= buf.length) {
                        // flush
                        os.write(buf, 0, pos);
                        r[1] += pos;
                        pos = 0;
                    }
                }
            }
            if (pos > 0) {
                os.write(buf, 0, pos);
                r[1] += pos;
                pos = 0;
            }
        } else {
            Writer wr = new OutputStreamWriter(os);
            int c = 0;
            int bpos = 0;
            int pos = 0;
            while ((c = is.read(buf, bpos, buf.length - bpos)) != -1) {
                r[0] += c;
                int[] cc = fromUTF8(buf, 0, bpos, cbuf, pos);
                if (cc[0] != c) {
                    bpos = cc[0];
                } else {
                    bpos = 0;
                }
                if (cc[1] >= cbuf.length) {
                    wr.write(cbuf, 0, pos);
                    r[1] += pos;
                } else {
                    pos = cc[1];
                }

            }
            if (pos > 0) {
                wr.write(cbuf, 0, pos);
                r[1] += pos;
                pos = 0;
            }
            wr.flush();
        }
        return r;
    }

}
