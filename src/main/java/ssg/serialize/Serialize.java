/*
 * AS IS
 */
package ssg.serialize;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

/**
 * Abstraction of storing data in serialized form (bytes) with utility target
 * variants.
 *
 * @author 000ssg
 */
public interface Serialize {

    /**
     * Serialize object to bytes sequence.
     *
     * @param obj
     * @return
     * @throws IOException
     */
    byte[] toBytes(Object obj) throws IOException;

    /**
     * Serialize object to output stream. Assuming as base method for any
     * implementation unless other approach gives better performance.
     *
     * @param obj
     * @param os
     * @return
     * @throws IOException
     */
    long toStream(Object obj, OutputStream os) throws IOException;

    /**
     * Serialize object to String (chars sequence).
     *
     * @param obj
     * @return
     * @throws IOException
     */
    String toText(Object obj) throws IOException;

    /**
     * Interprets URL as an output stream. If Complex actions are needed to
     * establish connection, the use toStream after all required actions are
     * done..
     *
     * @param obj
     * @param url
     * @return
     * @throws IOException
     */
    long toURL(Object obj, URL url) throws IOException;

    // low level tools
    /**
     * Implementation-specific writer to output stream.
     *
     * @param obj
     * @param os
     * @return
     * @throws IOException
     */
    long write(Object obj, OutputStream os) throws IOException;

}
