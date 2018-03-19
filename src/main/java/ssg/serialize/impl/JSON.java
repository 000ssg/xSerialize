/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * JSON central: static methods for JSON/BJSON mostly used conversions.
 *
 * @author 000ssg
 */
public class JSON {

    // default JSONs
    private static final JSONSerializer standardJSON = new JSONSerializer();
    private static final JSONSerializer standardPrettyJSON = new JSONSerializer(true);
    private static final JSONSerializer extendedJSON = new JSONSerializer() {
        {
            setResolveCyclicReferences(true);
        }
    };
    private static final JSONSerializer extendedPrettyJSON = new JSONSerializer(true) {
        {
            setResolveCyclicReferences(true);
        }
    };
    private static final BJSONSerializer standardBJSON = new BJSONSerializer();
    private static final BJSONSerializer extendedBJSON = new BJSONSerializer() {
        {
            setResolveCyclicReferences(true);
        }
    };

    ////////////////////////////////////////////////////////////////////////////
    //////////////// JSON
    ////////////////////////////////////////////////////////////////////////////
    public static long toJSON(Object obj, OutputStream os) throws IOException {
        return standardJSON.toStream(obj, os);
    }

    public static String toJSON(Object obj) throws IOException {
        return standardJSON.toText(obj);
    }

    public static long toPrettyJSON(Object obj, OutputStream os) throws IOException {
        return standardPrettyJSON.toStream(obj, os);
    }

    public static String toPrettyJSON(Object obj) throws IOException {
        return standardPrettyJSON.toText(obj);
    }

    public static long toJSONX(Object obj, OutputStream os) throws IOException {
        return extendedJSON.toStream(obj, os);
    }

    public static String toJSONX(Object obj) throws IOException {
        return extendedJSON.toText(obj);
    }

    public static long toPrettyJSONX(Object obj, OutputStream os) throws IOException {
        return extendedPrettyJSON.toStream(obj, os);
    }

    public static String toPrettyJSONX(Object obj) throws IOException {
        return extendedPrettyJSON.toText(obj);
    }

    public static <T> T fromJSON(InputStream is, Class<T> type) throws IOException {
        if (type != null) {
            return standardJSON.fromStream(is, type);
        } else {
            return (T) standardJSON.fromStream(is);
        }
    }

    public static <T> T fromJSON(String text, Class<T> type) throws IOException {
        if (type != null) {
            return standardJSON.fromText(text, type);
        } else {
            return (T) standardJSON.fromText(text);
        }
    }

    public static <T> T fromJSONX(InputStream is, Class<T> type) throws IOException {
        if (type != null) {
            return extendedJSON.fromStream(is, type);
        } else {
            return (T) extendedJSON.fromStream(is);
        }
    }

    public static <T> T fromJSONX(String text, Class<T> type) throws IOException {
        if (type != null) {
            return extendedJSON.fromText(text, type);
        } else {
            return (T) extendedJSON.fromText(text);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //////////////// JSON
    ////////////////////////////////////////////////////////////////////////////
    public static long toBJSON(Object obj, OutputStream os) throws IOException {
        return standardBJSON.toStream(obj, os);
    }

    public static byte[] toBJSON(Object obj) throws IOException {
        return standardBJSON.toBytes(obj);
    }

    public static long toBJSONX(Object obj, OutputStream os) throws IOException {
        return extendedBJSON.toStream(obj, os);
    }

    public static byte[] toBJSONX(Object obj) throws IOException {
        return extendedBJSON.toBytes(obj);
    }

    public static <T> T fromBJSON(InputStream is, Class<T> type) throws IOException {
        if (type != null) {
            return standardBJSON.fromStream(is, type);
        } else {
            return (T) standardBJSON.fromStream(is);
        }
    }

    public static <T> T fromBJSON(byte[] buf, Class<T> type) throws IOException {
        if (type != null) {
            return standardBJSON.fromBytes(buf, type);
        } else {
            return (T) standardBJSON.fromBytes(buf);
        }
    }

    public static <T> T fromBJSONX(InputStream is, Class<T> type) throws IOException {
        if (type != null) {
            return extendedBJSON.fromStream(is, type);
        } else {
            return (T) extendedBJSON.fromStream(is);
        }
    }

    public static <T> T fromBJSONX(byte[] buf, Class<T> type) throws IOException {
        if (type != null) {
            return extendedBJSON.fromBytes(buf, type);
        } else {
            return (T) extendedBJSON.fromBytes(buf);
        }
    }
}
