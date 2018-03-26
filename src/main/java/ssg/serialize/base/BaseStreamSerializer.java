/*
 * AS IS
 */
package ssg.serialize.base;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import ssg.serialize.StreamSerializer;

/**
 *
 * @author 000ssg
 */
public abstract class BaseStreamSerializer extends BaseSerializer implements StreamSerializer {

    @Override
    public long[] pipe(InputStream is, OutputStream os, boolean encode) throws IOException {
        final long[] r = new long[2];
        r[1] = write(read(new FilterInputStream(is) {
            long pos = 0;
            long marked = 0;
            int marklimit = 0;

            @Override
            public synchronized void reset() throws IOException {
                super.reset();
                pos = Math.max(marked, pos - marklimit);
            }

            @Override
            public synchronized void mark(int readlimit) {
                super.mark(readlimit);
                marked = pos;
                marklimit = readlimit;
            }

            @Override
            public long skip(long n) throws IOException {
                long l = super.skip(n);
                pos += l;
                return l;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int r = super.read(b, off, len);
                if (r > 0) {
                    pos += r;
                }
                return r;
            }

            @Override
            public int read(byte[] b) throws IOException {
                int r = super.read(b);
                if (r > 0) {
                    pos += r;
                }
                return r;
            }

            @Override
            public int read() throws IOException {
                int r = super.read();
                if (r != -1) {
                    pos++;
                }
                return r;
            }
        }), os);
        return r;
    }

}
