/*
 * AS IS
 */
package ssg.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author 000ssg
 */
public interface StreamSerializer extends Serializer {

    /**
     * Applies write (encode=true) or read (encode=false) data transformation
     * between input and output streams.
     *
     * @param is
     * @param os
     * @param encode
     * @return
     * @throws IOException
     */
    long[] pipe(InputStream is, OutputStream os, boolean encode) throws IOException;
}
