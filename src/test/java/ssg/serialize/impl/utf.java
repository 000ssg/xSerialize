/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 *
 * @author 000ssg
 */
public class utf {
    public static int utf8(char ch, byte[] buf, int off) throws IOException {
        int c = off;
        if (ch < 128) {
            buf[c++] = (byte) ch;
        } else if (ch < 2048) {
            buf[c++] = (byte) (0xc0 | (ch >> 6) & 0x1f);
            buf[c++] = (byte) (0x80 | (ch & 0x3f));
        } else if (ch == 0xd800) {
        } else if (ch == 0xdc00) {
            buf[c++] = (byte) 244;
            buf[c++] = (byte) 143;
            buf[c++] = (byte) 176;
            buf[c++] = (byte) 128;
        } else if (ch >= 0xd800 && ch <= 0xdfff) {
            buf[c++] = 63;
        } else {
            buf[c++] = (byte) (0xe0 | (ch >> 12) & 0xf);
            buf[c++] = (byte) (0x80 | (ch >> 6) & 0x3f);
            buf[c++] = (byte) (0x80 | (ch & 0x3f));
        }
        return c - off;
    }

    public static int utf8(char ch, OutputStream os) throws IOException {
        if (ch < 128) {
            os.write((byte) ch);
            return 1;
        } else if (ch < 2048) {
            os.write(0xc0 | (ch >> 6) & 0x1f);
            os.write(0x80 | (ch & 0x3f));
            return 2;
        } else if (ch == 0xd800) {
            return 0;
        } else if (ch == 0xdc00) {
            os.write(new byte[]{(byte) 244, (byte) 143, (byte) 176, (byte) 128});
            return 4;
        } else if (ch >= 0xd800 && ch <= 0xdfff) {
            os.write(63);
            return 1;
        } else {
            os.write(0xe0 | (ch >> 12) & 0xf);
            os.write(0x80 | (ch >> 6) & 0x3f);
            os.write(0x80 | (ch & 0x3f));
            return 3;
        }
    }
    
    public static int utf8_(char ch, OutputStream os) throws IOException {
        if (ch < 128) {
            os.write((byte) ch);
            return 1;
        } else if (ch < 2048) {
            os.write(0xc0 | (ch >> 6) & 0x1f);
            os.write(0x80 | (ch & 0x3f));
            return 2;
        } else {
            if (ch >= 0xd800 && ch <= 0xdfff) {
                return 0;
            }
            os.write(0xe0 | (ch >> 12) & 0xf);
            os.write(0x80 | (ch >> 6) & 0x3f);
            os.write(0x80 | (ch & 0x3f));
            return 3;
        }
    }

    public static void main(String[] args) throws IOException {
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        Writer wr = new OutputStreamWriter(baos1, "UTF-8");

        int last = 0;
        for (int i = 0xCFF0; i < 65536; i++) {
            wr.write(new char[]{(char) i});
            wr.flush();
            utf8((char) i, baos2);
            int pos = baos1.size();
            if (pos == last) {
                continue;
            }
            System.out.print(i + "\t" + (pos - last) + "\t" + baos1.size() + " " + baos2.size());
            byte[] b1 = baos1.toByteArray();
            byte[] b2 = baos2.toByteArray();
            for (int k = last; k < pos; k++) {
                System.out.print(" [" + (b1[k] & 0xff) + " " + ((k<b2.length) ? (b2[k] & 0xff) :"?") + "]");
//                if (b1[k] != b2[k]) {
//                    int a = 0;
//                }
            }
            System.out.println();
            last = pos;
        }

        int a = 0;
    }
}
