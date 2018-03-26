/*
 * AS IS
 */
package ssg.serialize.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import ssg.serialize.tools.Decycle;

/**
 *
 * @author 000ssg
 */
public class Dump {

    static Decycle daDecycle = new Decycle();

    static Decycle decycle() {
        return daDecycle;
    }

    static void release(Decycle dd) {
        daDecycle.reset();
    }

    public static String dump(Object obj, boolean indented, boolean counted) {
        if (obj instanceof Map) {
            return dumpMap(obj, indented, counted);
        } else if (obj instanceof byte[]) {
            StringBuilder sb = new StringBuilder();
            byte[] bb = (byte[]) obj;
            if (counted) {
                sb.append("(" + bb.length + ") 0x");
            } else {
                sb.append("0x");
            }
            for (int i = 0; i < bb.length; i++) {
                if (i > 0) {
                    sb.append("");
                }
                sb.append(Integer.toHexString(0xFF & bb[i]));
            }
            sb.append("");
            return sb.toString();
        } else if (obj instanceof Collection || obj != null && obj.getClass().isArray()) {
            return dumpArray(obj, indented, counted);
        } else {
            return "" + obj;
        }
    }

    public static String dumpMap(Object obj, boolean indented, boolean counted) {
        Decycle d = decycle();
        Decycle.DTYPE dt = (d.isDecycleable(obj)) ? d.check(obj) : Decycle.DTYPE.FIRST;
        if (dt == Decycle.DTYPE.DUP) {
            return "#DUP";
        }

        try {
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
                        } else if (val instanceof Collection || val.getClass().isArray()) {
                            sb.append(dump(val, indented & !(val instanceof byte[]), counted).replace("\n", "\n  "));
                        } else {
                            sb.append(val.toString().replace("\n", "\n  "));
                        }
                    }
                }
                sb.append("}");
            }
            return sb.toString();
        } finally {
            if (dt == Decycle.DTYPE.NEW) {
                release(d);
            }
        }
    }

    public static String dumpArray(Object obj, boolean indented, boolean counted) {
        Decycle d = decycle();
        Decycle.DTYPE dt = (d.isDecycleable(obj)) ? d.check(obj) : Decycle.DTYPE.FIRST;
        if (dt == Decycle.DTYPE.DUP) {
            return "#DUP";
        }

        try {
            StringBuilder sb = new StringBuilder();
            if (counted) {
                if (obj != null && obj.getClass().isArray()) {
                    sb.append("(" + Array.getLength(obj) + ") ");
                } else if (obj instanceof Collection) {
                    sb.append("(" + ((Collection) obj).size() + ") ");
                }
            }
            sb.append("[");
            if (obj != null && obj.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(obj); i++) {
                    if (i > 0) {
                        if (indented) {
                            sb.append(",\n  ");
                        } else {
                            sb.append(", ");
                        }
                    } else if (indented) {
                        sb.append("\n  ");
                    }
                    Object val = Array.get(obj, i);
                    sb.append(dump(val, indented & !(val instanceof byte[]), counted).replace("\n", "\n  "));
                }
            } else if (obj instanceof Collection) {
                boolean first = true;
                for (Object val : (Collection) obj) {
                    if (first) {
                        first = false;
                        if (indented) {
                            sb.append("\n  ");
                        }
                    } else if (indented) {
                        sb.append(",\n  ");
                    } else {
                        sb.append(", ");
                    }
                    sb.append(dump(val, indented & !(val instanceof byte[]), counted).replace("\n", "\n  "));
                }
            }
            if (indented) {
                sb.append("\n]");
            } else {
                sb.append("]");
            }
            return sb.toString();
        } finally {
            if (dt == Decycle.DTYPE.NEW) {
                release(d);
            }
        }
    }
}
