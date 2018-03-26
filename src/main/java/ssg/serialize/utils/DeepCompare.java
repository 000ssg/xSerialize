/*
 * AS IS
 */
package ssg.serialize.utils;

import ssg.serialize.tools.IX;
import ssg.serialize.tools.Decycle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import ssg.serialize.impl.BASE64Serializer;
import ssg.serialize.tools.Reflector;

/**
 *
 * @author 000ssg
 */
public class DeepCompare {

    // enable POJO to compare 
    public static final long DCO_POJO = 0x0001;
    // enable POJO/Map compare
    public static final long DCO_MAP_POJO = 0x0002;
    // ignore key case for String-to-Object maps
    public static final long DCO_MAP_KEY_NO_CASE = 0x0002;
    // enable Array-to-Collection compare
    public static final long DCO_ARRCOL = 0x0002;
    // enable collection compare ignoring items order
    public static final long DCO_COLL_NO_ORDER = 0x0008;
    // enable compare bigint and number
    public static final long DCO_BIGINT_NUM = 0x0010;
    // enable compare bigint and byte[]
    public static final long DCO_BIGINT_BYTES = 0x0020;
    // enable compare bigint and byte[]
    public static final long DCO_BIGINT_STRING = 0x040;
    // enable compare bigdecimal and number
    public static final long DCO_BIGDEC_NUM = 0x0080;
    // enable compare bigdecimal and string
    public static final long DCO_BIGDEC_STRING = 0x0100;
    // enable compare enum value and and string
    public static final long DCO_ENUM_STRING = 0x0200;
    // enable float / double compare as float
    public static final long DCO_FLOAT_DOUBLE = 0x0400;
    // enable char[] compare with string
    public static final long DCO_CHARS_STRING = 0x0800;
    // enable byte[]with String as Base64
    public static final long DCO_BYTES_BASE64 = 0x1000;
    // ignore missing value in map if it is null
    public static final long DCO_IGNORE_MISSING_MAP_NULLS = 0x2000;
    // ignore nulls in collections
    public static final long DCO_IGNORE_COLLECTION_NULLS = 0x4000;
    // report duplicates...
    public static final long DCO_REPORT_DUPLICATES = 0x8000;

    // default options
    public static final long DCO_DEFAULT
            = DCO_POJO
            | DCO_MAP_POJO
            | DCO_ARRCOL
            | DCO_BIGINT_NUM
            | DCO_BIGINT_BYTES
            | DCO_BIGDEC_NUM
            | DCO_BIGDEC_STRING
            | DCO_BIGINT_STRING
            | DCO_ENUM_STRING
            | DCO_FLOAT_DOUBLE
            | DCO_CHARS_STRING
            | DCO_BYTES_BASE64;

    public static final long DCO_STRONGER
            = DCO_POJO
            | DCO_MAP_POJO
            | DCO_ARRCOL
            | DCO_BIGINT_NUM
            | DCO_BIGINT_BYTES
            | DCO_BIGDEC_NUM;

    public static final long DCO_STRICT
            = DCO_POJO;

    public static enum DIFF {
        same, value, size, name, type, missing, extra, duplicate
    }

    static ThreadLocal<Decycle[]> decycler = new ThreadLocal<Decycle[]>();
    static final Collection<DeepComparator> defaultComparators = new ArrayList<DeepComparator>() {
        {
            add(new DCCBigDecimal());
            add(new DCCBigInteger());
            add(new DCCEnum());
            add(new DCCNumber());
            add(new DCCCharsString());
            add(new DCCBytesBase64String());
        }
    };

    public static DC diff(Object a, Object b) {
        return diff(a, b, DCO_DEFAULT);
    }

    public static DC diff(Object a, Object b, long options) {
        return diff(a, b, options, null, defaultComparators);
    }

    public static DC diff(Object a, Object b, long options, DC dc, Collection<DeepComparator> comparators) {
        Decycle[] d = decycler.get();
        boolean dn = (d == null);
        if (d == null) {
            d = new Decycle[]{
                new Decycle(),
                new Decycle()
            };
            decycler.set(d);
        }

        try {
            Decycle.DTYPE dta = null;
            Decycle.DTYPE dtb = null;
            if (a != null) {
                if (d[0].isDecycleable(a)) {
                    dta = d[0].check(a);
                }
            }
            if (b != null) {
                if (d[1].isDecycleable(b)) {
                    dtb = d[1].check(b);
                }
            }
            if (dc == null) {
                dc = new DC(a, b, DIFF.same);
                if (a == b || a != null && a.equals(b)) {
                    return dc;
                }
            }
            if (Decycle.DTYPE.DUP.equals(dta) && dta.equals(dtb)) {
                if ((options & DCO_REPORT_DUPLICATES) != 0) {
                    dc.state = DIFF.duplicate;
                }
                return dc;
            }
            if (a == null && b != null || b == null && a != null) {
                dc.state = DIFF.type;
                return dc;
            }

            // try comparator-based checks
            if (comparators != null) {
                for (DeepComparator cmp : comparators) {
                    if (cmp != null && cmp.test(options, a, b)) {
                        DC r = cmp.compare(options, a, b, dc);
                        if (r != null) {
                            return r;
                        }
                    }
                }
            }

            // prepare/try structures
            if (a instanceof Collection || a != null && a.getClass().isArray()) {
                if ((options & DCO_ARRCOL) == DCO_ARRCOL) {
                    a = new IX(a);
                    IX bix = new IX(b);
                    if (bix.size() == -1) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    b = bix;
                } else if (a instanceof Collection && b instanceof Collection
                        || a.getClass().isArray() && b.getClass().isArray()) {
                    a = new IX(a);
                    b = new IX(b);
                } else {
                    dc.state = DIFF.type;
                    return dc;
                }
            } else if (a instanceof Map && b instanceof Map) {
                // already comparable!
            } else if (a instanceof Map && !(b instanceof Map)) {
                // b -> POJO
                if ((options & (DCO_POJO | DCO_MAP_POJO)) == (DCO_POJO | DCO_MAP_POJO)) {
                    Map m = Reflector.reflect(b, false);// new POJO(b);
                    if (m == null) {
                        dc.state = DIFF.type;
                        dc.diff = "b is non-reflective";
                        return dc;
                    } else {
                        b = m;
                    }
                }
            } else if (b instanceof Map && !(a instanceof Map)) {
                // a -> POJO
                if ((options & (DCO_POJO | DCO_MAP_POJO)) == (DCO_POJO | DCO_MAP_POJO)) {
                    Map m = Reflector.reflect(a, false);// new POJO(b);
                    if (m == null) {
                        dc.state = DIFF.type;
                        dc.diff = "a is non-reflective";
                        return dc;
                    } else {
                        a = m;
                    }
                }
            } else if ((options & DCO_POJO) == DCO_POJO) {
                Map ma = Reflector.reflect(a, false);// new POJO(a);
                Map mb = Reflector.reflect(b, false);// new POJO(b);
                if (ma == null && mb == null) {
                    // scalars?
                } else if (ma == null || mb == null) {
                    dc.state = DIFF.type;
                    dc.diff = (ma == null) ? "a is non-reflective" : "b is non-reflective";
                    return dc;
                } else {
                    a = ma;
                    b = mb;
                }
            }

            if (a instanceof IX) {
                IX ma = (IX) a;
                IX mb = (IX) b;
                if ((options & DCO_IGNORE_COLLECTION_NULLS) != 0) {
                    int amax = ma.size();
                    int bmax = mb.size();
                    int ac = 0;
                    int bc = 0;
                    for (int i = 0; i < Math.min(ma.size(), mb.size()); i++) {
                        Object va = (ac < amax) ? ma.item(ac) : null;
                        Object vb = (bc < bmax) ? mb.item(bc) : null;
                        if (va == null || vb == null) {
                            while (va == null && ac < amax) {
                                ac++;
                                if (ac >= amax) {
                                    break;
                                }
                                va = ma.item(ac);
                            }
                            while (vb == null && bc < bmax) {
                                bc++;
                                if (bc >= bmax) {
                                    break;
                                }
                                vb = mb.item(bc);
                            }
                        }
                        if (va == null && vb == null || ac >= amax && bc >= bmax) {
                            break;
                        }
                        DC vd = diff(va, vb, options, null, comparators);
                        if (vd == null || DIFF.same == vd.state) {
                        } else {
                            dc.add(i, vd);
                        }
                        if (ac < amax) {
                            ac++;
                        }
                        if (bc < bmax) {
                            bc++;
                        }
                    }
                    if (ac == amax && bc == bmax) {
                    } else {
                        dc.state = DIFF.size;
                        dc.diff = "(" + ma.size() + "/" + ac + " != " + mb.size() + "/" + bc + ")";
                    }
                } else if (ma.size() != mb.size()) {
                    dc.state = DIFF.size;
                    dc.diff = "(" + ma.size() + " != " + mb.size() + ")";
                } else {
                    for (int i = 0; i < ma.size(); i++) {
                        dc.count++;
                        DC vd = diff(ma.item(i), mb.item(i), options, null, comparators);
                        if (vd == null || DIFF.same == vd.state) {
                            continue;
                        }
                        dc.add(i, vd);
                    }
                }
            } else if (a instanceof Map) {
                Map ma = (Map) a;
                Map mb = (Map) b;
                boolean ignoreNulls = (options & DCO_IGNORE_MISSING_MAP_NULLS) != 0;
                Collection akeys = new HashSet();
                for (Object key : ma.keySet()) {
                    dc.count++;
                    akeys.add(key);
                    Object va = ma.get(key);
                    Object vb = mb.get(key);
                    DC vd = diff(va, vb, options, null, comparators);
                    if (va == null && vb == null && !ignoreNulls && !mb.containsKey(key)) {
                        vd.state = DIFF.missing;
                        vd.diff = "b.key: '" + key + "'";
                    } else if (vd == null || DIFF.same == vd.state) {
                        continue;
                    } else if (va != null && !mb.containsKey(key)) {
                        vd.state = DIFF.missing;
                    }
                    dc.add(key, vd);
                }
                for (Object key : mb.keySet()) {
                    if (akeys.contains(key)) {
                        continue;
                    }
                    dc.count++;
                    Object va = null;
                    Object vb = mb.get(key);
                    if (vb == null && ignoreNulls) {
                        continue;
                    }
                    DC vd = new DC(va, vb, DIFF.extra);
                    vd.diff = "a.key: '" + key + "'";
                    dc.add(key, vd);
                }
            }

            return dc;
        } finally {
            if (dn) {
                decycler.set(null);
            }
        }
    }

    /**
     * Deep compare result
     */
    public static class DC {

        DC parent;
        String key;
        Object a;
        Object b;
        DIFF state = DIFF.same;
        String diff;
        Map<String, DC> dcs;
        int count = 0;

        public DC(Object a, Object b, DIFF diff) {
            this.a = a;
            this.b = b;
            this.state = diff;
        }

        public DIFF getState() {
            return state;
        }

        public void add(Object key, DC dc) {
            if (key != null && dc != null) {
                if (dcs == null) {
                    dcs = new LinkedHashMap<String, DC>() {
                        @Override
                        public DC remove(Object key) {
                            DC dc = super.remove(key);
                            if (dc != null) {
                                dc.parent = null;
                                dc.key = null;
                            }
                            return dc;
                        }

                        @Override
                        public DC put(String key, DC value) {
                            DC dc = super.put(key, value);
                            if (dc == null) {
                                dc = value;
                            }
                            if (dc != null) {
                                dc.parent = DC.this;
                                dc.key = key;
                            }
                            return dc;
                        }
                    };
                }
            }
            dcs.put((key instanceof Number) ? "[" + key + "]" : "" + key, dc);
            if (this.state == DIFF.same && dc != null) {
                this.state = DIFF.value;
            }
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean dumpValues) {
            StringBuilder sb = new StringBuilder();
            sb.append("DC{"
                    + state
                    + ((diff != null) ? " " + diff : "")
                    + ", count=" + count
                    + ", a=" + ((a != null) ? a.getClass().getSimpleName() : "null")
                    + ", b=" + ((b != null) ? b.getClass().getSimpleName() : "null")
                    + ", dcs=" + ((dcs != null) ? dcs.size() : "<none>"));
            String p = path();
            if (parent != null && dumpValues && DIFF.same != state) {
                if (p != null) {
                    sb.append("\n    Path: " + p);
                }
                if (a instanceof Collection || a != null && a.getClass().isArray()) {
                    sb.append("\n    A: " + ("" + Dump.dump(a, false, false)).replace("\n", "\n    "));
                } else {
                    sb.append("\n    A: " + ("" + a).replace("\n", "\n    "));
                }
                if (b instanceof Collection || b != null && b.getClass().isArray()) {
                    sb.append("\n    B: " + ("" + Dump.dump(b, false, false)).replace("\n", "\n    "));
                } else {
                    sb.append("\n    B: " + ("" + b).replace("\n", "\n    "));
                }
            }
            if (dcs != null) {
                for (Entry<String, DC> dce : dcs.entrySet()) {
                    sb.append("\n  " + dce.getKey() + ": " + dce.getValue().toString(dumpValues).replace("\n", "\n    "));
                }
                sb.append("\n");
            }
            sb.append('}');
            return sb.toString();
        }

        public String path() {
            if (parent == null) {
                return "";
            } else {
                String s = parent.path();
                if (key != null) {
                    if (key.startsWith("[")) {
                        return s + key;
                    } else {
                        return s + "/" + key;
                    }
                } else {
                    return s + "?";
                }
            }
        }
    }

    /**
     * Represents handler of a/b comparator.
     *
     * Execute test() to check if comparator is applicable to a/b with given
     * options...
     *
     * Execute compare() to evaluate DIFF. If null is returned -> use another
     * comparator
     */
    public static interface DeepComparator {

        boolean test(long options, Object a, Object b);

        DC compare(long options, Object a, Object b, DC dc);
    }

    public static class DCCBigDecimal implements DeepComparator {

        @Override
        public boolean test(long options, Object a, Object b) {
            return a instanceof BigDecimal || b instanceof BigDecimal;
        }

        @Override
        public DC compare(long options, Object a, Object b, DC dc) {
            if (a instanceof BigDecimal) {
                BigDecimal ad = (BigDecimal) a;
                if (b instanceof Number) {
                    if (ad.doubleValue() != ((Number) b).doubleValue()) {
                        dc.state = DIFF.type;
                        dc.diff = "BigDec(a).double!=Number(b).double";
                        return dc;
                    } else {
                        return dc;
                    }
                } else if (b instanceof String) {
                    if ((options & DCO_BIGINT_STRING) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    if (!ad.toString().equals(b)) {
                        dc.state = DIFF.type;
                        dc.diff = "BigDec(a).string!=string(b)";
                        return dc;
                    } else {
                        return dc;
                    }
                }
            } else {
                BigDecimal bd = (BigDecimal) b;
                if (a instanceof Number) {
                    if (bd.doubleValue() != ((Number) a).doubleValue()) {
                        dc.state = DIFF.type;
                        dc.diff = "BigDec(b).double!=Number(a).double";
                        return dc;
                    } else {
                        return dc;
                    }
                } else if (a instanceof String) {
                    if ((options & DCO_BIGINT_STRING) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    if (!bd.toString().equals(a)) {
                        dc.state = DIFF.type;
                        dc.diff = "BigDec(b).string!=string(a)";
                        return dc;
                    } else {
                        return dc;
                    }
                }
            }
            dc.state = DIFF.type;
            return dc;
        }
    }

    public static class DCCBigInteger implements DeepComparator {

        @Override
        public boolean test(long options, Object a, Object b) {
            return a instanceof BigInteger || b instanceof BigInteger;
        }

        @Override
        public DC compare(long options, Object a, Object b, DC dc) {
            if (a instanceof BigInteger) {
                BigInteger ai = (BigInteger) a;
                if (b instanceof Number) {
                    if ((options & DCO_BIGINT_NUM) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    if (ai.longValue() != ((Number) b).longValue()) {
                        dc.state = DIFF.type;
                        dc.diff = "BigInt(a).long!=Number(b).long";
                        return dc;
                    } else {
                        return dc;
                    }
                } else if (b instanceof byte[]) {
                    if ((options & DCO_BIGINT_BYTES) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    byte[] ab = ai.toByteArray();
                    byte[] bb = (byte[]) b;
                    if (ab.length != bb.length) {
                        dc.state = DIFF.type;
                        dc.diff = "BigInt(a).byte[].length!=(b)byte[].length";
                        return dc;
                    } else {
                        for (int i = 0; i < ab.length; i++) {
                            if (ab[i] != bb[i]) {
                                dc.state = DIFF.type;
                                dc.diff = "BigInt(a).byte[" + i + "]!=(b)byte[" + i + "]";
                                return dc;
                            }
                        }
                    }
                    return dc;
                } else if (b instanceof String) {
                    if ((options & DCO_BIGINT_STRING) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    if (!ai.toString().equals(b)) {
                        dc.state = DIFF.type;
                        dc.diff = "BigInt(a).string!=string(b)";
                        return dc;
                    }
                }
            } else {
                BigInteger bi = (BigInteger) b;
                if (a instanceof Number) {
                    if ((options & DCO_BIGINT_NUM) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    if (bi.longValue() != ((Number) a).longValue()) {
                        dc.state = DIFF.type;
                        dc.diff = "BigInt(a).long!=Number(b).long";
                        return dc;
                    } else {
                        return dc;
                    }
                } else if (a instanceof byte[]) {
                    if ((options & DCO_BIGINT_BYTES) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    byte[] bb = bi.toByteArray();
                    byte[] ab = (byte[]) a;
                    if (ab.length != bb.length) {
                        dc.state = DIFF.type;
                        dc.diff = "BigInt(b).byte[].length!=(a)byte[].length";
                        return dc;
                    } else {
                        for (int i = 0; i < ab.length; i++) {
                            if (ab[i] != bb[i]) {
                                dc.state = DIFF.type;
                                dc.diff = "BigInt(b).byte[" + i + "]!=(a)byte[" + i + "]";
                                return dc;
                            }
                        }
                    }
                    return dc;
                } else if (a instanceof String) {
                    if ((options & DCO_BIGINT_STRING) == 0) {
                        dc.state = DIFF.type;
                        return dc;
                    }
                    if (!bi.toString().equals(a)) {
                        dc.state = DIFF.type;
                        dc.diff = "BigInt(b).string!=string(a)";
                        return dc;
                    }
                }
            }
            dc.state = DIFF.type;
            return dc;
        }

    }

    public static class DCCEnum implements DeepComparator {

        @Override
        public boolean test(long options, Object a, Object b) {
            return a != null && a.getClass().isEnum() || b != null && b.getClass().isEnum();
        }

        @Override
        public DC compare(long options, Object a, Object b, DC dc) {
            if ((options & DCO_ENUM_STRING) == 0) {
                dc.state = DIFF.type;
                return dc;
            }
            if (a.getClass().isEnum()) {
                if (b instanceof String) {
                    if (a.toString().equals(b)) {
                        return dc;
                    }
                }
            } else if (a instanceof String) {
                if (b.toString().equals(a)) {
                    return dc;
                }
            }
            dc.state = DIFF.type;
            return dc;
        }

    }

    public static class DCCNumber implements DeepComparator {

        @Override
        public boolean test(long options, Object a, Object b) {
            return a instanceof Number && b instanceof Number;
        }

        @Override
        public DC compare(long options, Object a, Object b, DC dc) {
            if ((options & DCO_FLOAT_DOUBLE) != 0 && (a instanceof Float || b instanceof Float)) {
                if (((Number) a).floatValue() != ((Number) b).floatValue()) {
                    dc.state = DIFF.value;
                    return dc;
                } else {
                    return dc;
                }
            } else {
                long la = (a instanceof Float)
                        ? Float.floatToIntBits((Float) a)
                        : (a instanceof Double)
                                ? Double.doubleToLongBits((Double) a)
                                : ((Number) a).longValue();
                long lb = (b instanceof Float)
                        ? Float.floatToIntBits((Float) b)
                        : (b instanceof Double)
                                ? Double.doubleToLongBits((Double) b)
                                : ((Number) b).longValue();
                if (la != lb) {
                    dc.state = DIFF.value;
                    return dc;
                }
            }
            return null;
        }

    }

    public static class DCCCharsString implements DeepComparator {

        @Override
        public boolean test(long options, Object a, Object b) {
            return (options & DCO_CHARS_STRING) != 0 && (a instanceof char[] && b instanceof String
                    || a instanceof String && b instanceof char[]);
        }

        @Override
        public DC compare(long options, Object a, Object b, DC dc) {
            if (a instanceof char[]) {
                if (!new String((char[]) a).equals(b)) {
                    dc.state = DIFF.type;
                    dc.diff = "(a)char[] != String(b)";
                    return dc;
                }
            } else if (!new String((char[]) b).equals(a)) {
                dc.state = DIFF.type;
                dc.diff = "(b)char[] != String(a)";
                return dc;
            }
            return dc;
        }

    }

    public static class DCCBytesBase64String implements DeepComparator {

        @Override
        public boolean test(long options, Object a, Object b) {
            return (options & DCO_BYTES_BASE64) != 0 && (a instanceof byte[] && b instanceof String
                    || a instanceof String && b instanceof byte[]);
        }

        @Override
        public DC compare(long options, Object a, Object b, DC dc) {
            try {
                if (a instanceof byte[]) {
                    byte[] ab = (byte[]) a;
                    byte[] bb = BASE64Serializer.decode((String) b);
                    if (ab.length != bb.length) {
                        dc.state = DIFF.type;
                        dc.diff = "base64(b).byte[].length!=(a)byte[].length";
                        return dc;
                    } else {
                        for (int i = 0; i < ab.length; i++) {
                            if (ab[i] != bb[i]) {
                                dc.state = DIFF.type;
                                dc.diff = "base64(b).byte[" + i + "]!=(a)byte[" + i + "]";
                                return dc;
                            }
                        }
                    }
                } else {
                    byte[] ab = BASE64Serializer.decode((String) a);
                    byte[] bb = (byte[]) b;
                    if (ab.length != bb.length) {
                        dc.state = DIFF.type;
                        dc.diff = "base64(a).byte[].length!=(b)byte[].length";
                        return dc;
                    } else {
                        for (int i = 0; i < ab.length; i++) {
                            if (ab[i] != bb[i]) {
                                dc.state = DIFF.type;
                                dc.diff = "base64(a).byte[" + i + "]!=(b)byte[" + i + "]";
                                return dc;
                            }
                        }
                    }
                }
                return dc;
            } catch (Throwable th) {
            }
            dc.state = DIFF.type;
            return dc;
        }

    }

}
