/*
 * AS IS
 */
package ssg.serialize.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author 000ssg
 */
public class Dump {

    public static String dumpMap(Object obj) {
        StringBuilder sb = new StringBuilder();
        if (obj instanceof Map) {
            Map map = (Map) obj;
            sb.append("{");
            if (!map.isEmpty()) {
                for (Object key : map.keySet()) {
                    sb.append("\n  " + key + ": ");
                    Object val = map.get(key);
                    if (val == null) {
                        sb.append("null");
                    } else if (val.getClass().isArray()) {
                        sb.append(dumpArray(val).replace("\n", "\n    "));
                    } else {
                        sb.append(val.toString().replace("\n", "\n    "));
                    }
                }
            }
            sb.append("}");
        }
        return sb.toString();
    }

    public static String dumpArray(Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (obj != null && obj.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(obj); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(Array.get(obj, i));
            }
        } else if (obj instanceof Collection) {
            boolean first = true;
            for (Object val : (Collection) obj) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(val);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
