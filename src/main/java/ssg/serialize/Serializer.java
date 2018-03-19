/*
 * AS IS
 */
package ssg.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serializer abstracts conversion of objects to Stream, String, byte[] and
 * back.
 *
 * Its main purpose is to provide utility methods for encoding (serializing) and
 * decoding (deserializing) for a number of formats encapsulating implementation
 * of similar actions.
 *
 * Base implementation uses abstract read/write methods defined in this
 * interface to delegate all format specifics handling so to minimize adaptation
 * efforts.
 *
 * @author 000ssg
 */
public interface Serializer extends Deserialize, Serialize {

}
