/*
 * AS IS
 */
package ssg.serialize.impl;

import static ssg.serialize.impl.BaseObjectSerializer.DF_STRING;
import ssg.serialize.tools.Decycle;
import ssg.serialize.tools.Reflector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import ssg.serialize.tools.ClassCaster;
import ssg.serialize.tools.casters.Bytes2BigIntegerCC;
import ssg.serialize.tools.casters.Long2DateCC;
import ssg.serialize.tools.casters.String2BigDecimalCC;
import ssg.serialize.tools.casters.String2DateCC;

/**
 * BJSON specification implementation: http://bjson.org/
 *
 * @author 000ssg
 */
public class BJSONSerializer extends BaseObjectSerializer {

    // development optimization flag: use to modify code into variants and run testVariants test case to compare performance.
    public static int V = 1;
    // value constants
    public static final byte T_NULL = 0;
    public static final byte T_FALSE_OR_ZERO = 1;
    public static final byte T_EMPTY_STRING = 2;
    public static final byte T_TRUE_OR_ONE = 3;
    // positive cardinals
    public static final byte T_UINT8 = 4;
    public static final byte T_UINT16 = 5;
    public static final byte T_UINT32 = 6;
    public static final byte T_UINT64 = 7;
    // negative cardinals
    public static final byte T_NINT8 = 8;
    public static final byte T_NINT16 = 9;
    public static final byte T_NINT32 = 10;
    public static final byte T_NINT64 = 11;
    // reals
    public static final byte T_FLOAT_32 = 12;
    public static final byte T_DOUBLE_64 = 13;
    public static final byte T_FLOAT = 14;
    public static final byte T_DOUBLE = 15;
    // strings
    public static final byte T_STR8 = 16;
    public static final byte T_STR16 = 17;
    public static final byte T_STR32 = 18;
    public static final byte T_STR64 = 19;
    // bytes
    public static final byte T_BIN8 = 20;
    public static final byte T_BIN16 = 21;
    public static final byte T_BIN32 = 22;
    public static final byte T_BIN64 = 23;
    // strict? primitives
    public static final byte T_FALSE = 24;
    public static final byte T_TRUE = 25;
    public static final byte T_ZERO = 26;
    public static final byte T_ONE = 27;
    // arrays
    public static final byte T_ARR8 = 32;
    public static final byte T_ARR16 = 33;
    public static final byte T_ARR32 = 34;
    public static final byte T_ARR64 = 35;
    // maps
    public static final byte T_MAP8 = 36;
    public static final byte T_MAP16 = 37;
    public static final byte T_MAP32 = 38;
    public static final byte T_MAP64 = 39;
    // CUSTOM: meta info
    public static final byte T_REF = 111;

    public BJSONSerializer() {
        init();
    }

    public BJSONSerializer(boolean resolveCyclicReferences) {
        super(resolveCyclicReferences);
        init();
    }

    void init() {
        ClassCaster cc = this.getClassCaster();
        if (cc == null) {
            cc = new ClassCaster();
            this.setClassCaster(cc);
        }
        cc.addClassCasts(
                new String2BigDecimalCC(),
                new Bytes2BigIntegerCC(),
                new Long2DateCC(),
                new String2DateCC()
        );
    }

    @Override
    public long write(ObjectSerializerContext ctx, Object obj, OutputStream os) throws IOException {
        if (obj != null && obj.getClass().isEnum()) {
            return super.write(ctx, obj.toString(), os);
        } else {
            return super.write(ctx, obj, os);
        }
    }

    @Override
    public long writeScalar(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        long c = 0;
        if (obj instanceof Number) {
            if (obj instanceof BigDecimal) {
                c += writeString(((BigDecimal) obj).toString(), os, ctx);
            } else if (obj instanceof BigInteger) {
                byte[] bb = ((BigInteger) obj).toByteArray();
                c += writeBin(bb, os, ctx);
            } else if (obj instanceof Float || obj instanceof Double) {
                c += writeNum((Number) obj, os, ctx);
            } else if (((Number) obj).longValue() == 0) {
                os.write(T_ZERO);
                c++;
            } else if (((Number) obj).longValue() == 1) {
                os.write(T_ONE);
                c++;
            } else {
                c += writeNum((Number) obj, os, ctx);
            }
        } else if (obj instanceof String) {
            c += writeString((String) obj, os, ctx);
        } else if (obj instanceof Class) {
            c += writeString(((Class) obj).getName(), os, ctx);
        } else if (obj instanceof Boolean) {
            os.write(((Boolean) obj) ? T_TRUE : T_FALSE);
            c++;
        } else if (obj instanceof Character) {
            c += writeNum((Integer) (int) ((Character) obj).charValue(), os, ctx);
        }
        return c;
    }

    @Override
    public long writeCollection(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        if (obj instanceof byte[]) {
            return writeBin((byte[]) obj, os, ctx);
        } else {
            long c = 1;
            if (obj instanceof Collection) {
                Collection m = (Collection) obj;
                int ms = m.size();
                if ((decycleFlags & DF_IGNORE_COLLECTION_NULLS) != 0) {
                    for (Object o : m) {
                        if (o == null) {
                            ms--;
                        }
                    }
                }
                if (ms < 255) {
                    os.write(T_ARR8);
                    writeCardinal(ms, 1, os);
                    c++;
                } else if (ms < 65536) {
                    os.write(T_ARR16);
                    writeCardinal(ms, 2, os);
                    c += 2;
                } else if (ms < Integer.MAX_VALUE) {
                    os.write(T_ARR32);
                    writeCardinal(ms, 4, os);
                    c += 4;
                } else {
                    os.write(T_ARR64);
                    writeCardinal(ms, 8, os);
                    c += 8;
                }

                if ((decycleFlags & DF_IGNORE_COLLECTION_NULLS) != 0) {
                    for (Object val : m) {
                        if (val == null) {
                            continue;
                        }
                        c += write(ctx, val, os);
                    }
                } else {
                    for (Object val : m) {
                        c += write(ctx, val, os);
                    }
                }
            } else if (obj != null && obj.getClass().isArray()) {
                int ms = Array.getLength(obj);
                if ((decycleFlags & DF_IGNORE_COLLECTION_NULLS) != 0 && !obj.getClass().getComponentType().isPrimitive()) {
                    for (int i = 0; i < ms; i++) {
                        if (Array.get(obj, i) == null) {
                            ms--;
                        }
                    }
                }
                if (ms < 255) {
                    os.write(T_ARR8);
                    writeCardinal(ms, 1, os);
                    c++;
                } else if (ms < 65536) {
                    os.write(T_ARR16);
                    writeCardinal(ms, 2, os);
                    c += 2;
                } else if (ms < Integer.MAX_VALUE) {
                    os.write(T_ARR32);
                    writeCardinal(ms, 4, os);
                    c += 4;
                } else {
                    os.write(T_ARR64);
                    writeCardinal(ms, 8, os);
                    c += 8;
                }

                Class act = obj.getClass().getComponentType();
                if (act.isPrimitive()) {
                    if (boolean.class == act) {
                        boolean[] arr = (boolean[]) obj;
                        for (boolean v : arr) {
                            c += writeScalar(v, os, ctx);
                        }
                    } else if (byte.class == act) {
                        throw new IOException("byte[] must be handled separately, not as generic array");
                    } else if (short.class == act) {
                        short[] arr = (short[]) obj;
                        for (short v : arr) {
                            c += writeNum(v, os, ctx);
                        }
                    } else if (int.class == act) {
                        int[] arr = (int[]) obj;
                        for (int v : arr) {
                            c += writeNum(v, os, ctx);
                        }
                    } else if (long.class == act) {
                        long[] arr = (long[]) obj;
                        for (long v : arr) {
                            c += writeNum(v, os, ctx);
                        }
                    } else if (float.class == act) {
                        float[] arr = (float[]) obj;
                        for (float v : arr) {
                            c += writeNum(v, os, ctx);
                        }
                    } else if (double.class == act) {
                        double[] arr = (double[]) obj;
                        for (double v : arr) {
                            c += writeNum(v, os, ctx);
                        }
                    } else if (char.class == act) {
                        char[] arr = (char[]) obj;
                        for (char v : arr) {
                            c += write(ctx, v, os);
                        }
                    }
//                } else if (Number.class.isAssignableFrom(act)) {
//                    for (Object o : (Object[]) obj) {
//                        c += writeNum((Number) o, os, ctx);
//                    }
                } else if ((decycleFlags & DF_IGNORE_COLLECTION_NULLS) != 0 && !obj.getClass().getComponentType().isPrimitive()) {
                    for (Object o : (Object[]) obj) {
                        if (o == null) {
                            continue;
                        }
                        c += write(ctx, o, os);
                    }
                } else {
                    for (Object o : (Object[]) obj) {
                        c += write(ctx, o, os);
                    }
                }
            }
            return c;
        }
    }

    @Override
    public long writeNull(OutputStream os, ObjectSerializerContext ctx) throws IOException {
        os.write(T_NULL);
        return 1;
    }

    @Override
    public long writeMap(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        long c = 1;
        Map m = (Map) obj;
        int ms = m.size();

        if ((decycleFlags & DF_IGNORE_MAP_NULLS) != 0) {
            for (Object o : m.values()) {
                if (o == null) {
                    ms--;
                }
            }
        }

        if (ms < 255) {
            os.write(T_MAP8);
            writeCardinal(ms, 1, os);
            c++;
        } else if (ms < 65536) {
            os.write(T_MAP16);
            writeCardinal(ms, 2, os);
            c += 2;
        } else if (ms < Integer.MAX_VALUE) {
            os.write(T_MAP32);
            writeCardinal(ms, 4, os);
            c += 4;
        } else {
            os.write(T_MAP64);
            writeCardinal(ms, 8, os);
            c += 8;
        }

        if ((decycleFlags & DF_IGNORE_MAP_NULLS) != 0) {
            for (Object key : m.keySet()) {
                Object val = m.get(key);
                if (val == null) {
                    continue;
                }
                c += write(ctx, key, os);
                c += write(ctx, val, os);
            }
        } else {
            for (Object key : m.keySet()) {
                Object val = m.get(key);
                c += write(ctx, key, os);
                c += write(ctx, val, os);
            }
        }
        return c;
    }

    @Override
    public long writeObject(Object obj, Reflector rf, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        long c = 1;
        int ms = rf.size();

        if ((decycleFlags & DF_IGNORE_MAP_NULLS) != 0) {
            for (Object key : rf.keySet()) {
                if (rf.get(obj, key) == null) {
                    ms--;
                }
            }
        }

        if (ms < 255) {
            os.write(T_MAP8);
            writeCardinal(ms, 1, os);
            c++;
        } else if (ms < 65536) {
            os.write(T_MAP16);
            writeCardinal(ms, 2, os);
            c += 2;
        } else if (ms < Integer.MAX_VALUE) {
            os.write(T_MAP32);
            writeCardinal(ms, 4, os);
            c += 4;
        } else {
            os.write(T_MAP64);
            writeCardinal(ms, 8, os);
            c += 8;
        }

        if ((decycleFlags & DF_IGNORE_MAP_NULLS) != 0) {
            for (Object key : rf.keySet()) {
                Object val = rf.get(obj, key);
                if (val == null) {
                    continue;
                }
                c += write(ctx, key, os);
                c += write(ctx, val, os);
            }
        } else {
            for (Object key : rf.keySet()) {
                Object val = rf.get(obj, key);
                c += write(ctx, key, os);
                c += write(ctx, val, os);
            }
        }
        return c;
    }

    @Override
    public Object read(InputStream is) throws IOException {
        ObjectSerializerContext ctx = createContext(is);
        try {
            return read(is, ctx);
        } finally {
            onCompletedContext("read", ctx);
        }
    }

    public Object read(InputStream is, ObjectSerializerContext ctx) throws IOException {
        int c = 0;
        while ((c = is.read()) != -1) {
            int vs = 0;
            switch (c) {
                case T_NULL:
                    return null;
                case T_FALSE_OR_ZERO:
                    return 0;
                case T_EMPTY_STRING:
                    return "";
                case T_TRUE_OR_ONE:
                    return 1;
                // strict? primitives
                case T_FALSE:
                    return false;
                case T_TRUE:
                    return true;
                case T_ZERO:
                    return 0;
                case T_ONE:
                    return 1;
                // positive cardinals
                case T_UINT8:
                    return readCardinal(1, is).intValue();
                case T_UINT16:
                    return readCardinal(2, is).intValue();
                case T_UINT32:
                    return readCardinal(4, is).intValue();
                case T_UINT64:
                    return readCardinal(8, is).longValue();
                // negative cardinals
                case T_NINT8:
                    return -readCardinal(1, is).intValue();
                case T_NINT16:
                    return -readCardinal(2, is).intValue();
                case T_NINT32:
                    return -readCardinal(4, is).intValue();
                case T_NINT64:
                    return -readCardinal(8, is).longValue();
                // reals
                case T_FLOAT_32:
                case T_FLOAT:
                    return Float.intBitsToFloat(readCardinal(4, is).intValue());
                case T_DOUBLE_64:
                case T_DOUBLE:
                    return Double.longBitsToDouble(readCardinal(8, is).longValue());
                // single-byte length
                case T_STR8:
                case T_BIN8:
                case T_ARR8:
                case T_MAP8:
                    vs = 1;
                    break;
                // 2-byte length
                case T_STR16:
                case T_BIN16:
                case T_ARR16:
                case T_MAP16:
                    vs = 2;
                    break;
                // 4-byte length
                case T_STR32:
                case T_BIN32:
                case T_ARR32:
                case T_MAP32:
                    vs = 4;
                    break;
                // 4-byte length
                case T_STR64:
                case T_BIN64:
                case T_ARR64:
                case T_MAP64:
                    vs = 8;
                    break;
                // custom: cross-references
                case T_REF:
                    vs = 4;
                    break;
            }

            // here we must load the vs-bytes len
            long len = readCardinal(vs, is).longValue();

            switch (c) {
                // strings
                case T_STR8:
                case T_STR16:
                case T_STR32:
                case T_STR64: {
                    byte[] buf = new byte[(int) len];
                    readFull(buf, buf.length, is);
                    if ((decycleFlags & DF_STRING) != 0) {
                        String s = new String(buf, "UTF-8");
                        String s1 = ctx.resolveRef(s, null);
                        return (s1 != null) ? s1 : s;
                    } else {
                        return new String(buf, "UTF-8");
                    }
                }
                // bytes
                case T_BIN8:
                case T_BIN16:
                case T_BIN32:
                case T_BIN64: {
                    byte[] buf = new byte[(int) len];
                    readFull(buf, buf.length, is);
                    return ctx.resolveRef(buf, null);
                }
                // arrays
                case T_ARR8:
                case T_ARR16:
                case T_ARR32:
                case T_ARR64: {
                    Object[] oo = new Object[(int) len];
                    ctx.resolveRef(oo, null);
                    for (int i = 0; i < (int) len; i++) {
                        oo[i] = read(is, ctx);
                    }
                    return oo;
                }
                // maps
                case T_MAP8:
                case T_MAP16:
                case T_MAP32:
                case T_MAP64: {
//                    Map m = new LinkedHashMap();
                    Map m = (!isResolveCyclicReferences())
                            ? new LinkedHashMap()
                            : new Decycle.DecycledMap();

                    ctx.resolveRef(m, null);
                    for (int i = 0; i < (int) len; i++) {
                        Object key = read(is, ctx);
                        //System.out.println("BJSON.read.map: key=" + key);
                        if (key instanceof String) {
                        } else {
                            int a = 0;
                        }
                        Object val = read(is, ctx);
                        m.put(key, val);
                    }
                    return m;
                }
                // custom: cross-references
                case T_REF:
                    Object o = ctx.resolveRef(null, (long) len);
                    //System.out.println("REF: " + len + " -> " + o);
                    return o;
            }
        }
        throw new IOException("Unrecognized data type: " + c);
    }

    ////////////////////////////////////////////////////////////////////////////
    //////////////// utilities
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Performs stream scan and returns type-based summary
     *
     * @param is
     * @return
     * @throws IOException
     */
    @Override
    public OSStat scan(OSScanHandler handler, OSStat stat, InputStream is) throws IOException {
        if (stat == null) {
            stat = new BJSONStat();
            ((BOSStat) stat).ctx = createContext(is);
        } else if (stat instanceof BOSStat) {
            if (((BOSStat) stat).ctx == null) {
                ((BOSStat) stat).ctx = createContext(is);
            }
        }

        // init and stop handler if present
        if (handler != null) {
            handler.onStart();
        }
        try {
            return scan(stat, handler, is);
        } finally {
            if (handler != null) {
                handler.onEnd(null);
            }
        }
    }

    OSStat scan(OSStat stat, OSScanHandler handler, InputStream is) throws IOException {
        int c = 0;

        while ((c = is.read()) != -1) {
            stat.addType((byte) c);
            int vs = 0;
            switch (c) {
                case T_NULL:
                    if (handler != null) {
                        handler.onScalar((byte) c, null, false);
                    }
                    return stat;
                case T_FALSE_OR_ZERO:
                    if (handler != null) {
                        handler.onScalar((byte) c, false, false);
                    }
                    return stat;
                case T_EMPTY_STRING:
                    if (handler != null) {
                        handler.onScalar((byte) c, "", false);
                    }
                    return stat;
                case T_TRUE_OR_ONE:
                    if (handler != null) {
                        handler.onScalar((byte) c, true, false);
                    }
                    return stat;
                // strict? primitives
                case T_FALSE:
                    if (handler != null) {
                        handler.onScalar((byte) c, false, false);
                    }
                    return stat;
                case T_TRUE:
                    if (handler != null) {
                        handler.onScalar((byte) c, true, false);
                    }
                    return stat;
                case T_ZERO:
                    if (handler != null) {
                        handler.onScalar((byte) c, 0, false);
                    }
                    return stat;
                case T_ONE:
                    if (handler != null) {
                        handler.onScalar((byte) c, 1, false);
                    }
                    return stat;
                // positive cardinals
                case T_UINT8:
                    if (handler != null) {
                        handler.onScalar((byte) c, (0xFF & readCardinal(1, is).byteValue()), false);
                    } else {
                        is.skip(1);
                    }
                    return stat;
                case T_UINT16:
                    if (handler != null) {
                        handler.onScalar((byte) c, (0xFFFF & readCardinal(2, is).shortValue()), false);
                    } else {
                        is.skip(2);
                    }
                    return stat;
                case T_UINT32:
                    if (handler != null) {
                        handler.onScalar((byte) c, (0xFFFFFFFF & readCardinal(4, is).longValue()), false);
                    } else {
                        is.skip(4);
                    }
                    return stat;
                case T_UINT64:
                    if (handler != null) {
                        handler.onScalar((byte) c, readCardinal(8, is).longValue(), false);
                    } else {
                        is.skip(8);
                    }
                    return stat;
                // negative cardinals
                case T_NINT8:
                    if (handler != null) {
                        handler.onScalar((byte) c, -(0xFF & readCardinal(1, is).byteValue()), false);
                    } else {
                        is.skip(1);
                    }
                    return stat;
                case T_NINT16:
                    if (handler != null) {
                        handler.onScalar((byte) c, -(0xFFFF & readCardinal(2, is).shortValue()), false);
                    } else {
                        is.skip(2);
                    }
                    return stat;
                case T_NINT32:
                    if (handler != null) {
                        handler.onScalar((byte) c, -(0xFFFFFFFF & readCardinal(4, is).longValue()), false);
                    } else {
                        is.skip(4);
                    }
                    return stat;
                case T_NINT64:
                    if (handler != null) {
                        handler.onScalar((byte) c, -readCardinal(8, is).longValue(), false);
                    } else {
                        is.skip(8);
                    }
                    return stat;
                // reals
                case T_FLOAT_32:
                case T_FLOAT:
                    if (handler != null) {
                        handler.onScalar((byte) c, Float.intBitsToFloat(readCardinal(4, is).intValue()), false);
                    } else {
                        is.skip(4);
                    }
                    return stat;
                case T_DOUBLE_64:
                case T_DOUBLE:
                    if (handler != null) {
                        handler.onScalar((byte) c, Double.longBitsToDouble(readCardinal(8, is).longValue()), false);
                    } else {
                        is.skip(8);
                    }
                    return stat;
                // single-byte length
                case T_STR8:
                case T_BIN8:
                case T_ARR8:
                case T_MAP8:
                    vs = 1;
                    break;
                // 2-byte length
                case T_STR16:
                case T_BIN16:
                case T_ARR16:
                case T_MAP16:
                    vs = 2;
                    break;
                // 4-byte length
                case T_STR32:
                case T_BIN32:
                case T_ARR32:
                case T_MAP32:
                    vs = 4;
                    break;
                // 4-byte length
                case T_STR64:
                case T_BIN64:
                case T_ARR64:
                case T_MAP64:
                    vs = 8;
                    break;
                // custom: cross-references
                case T_REF:
                    vs = 4;
                    break;
            }

            // here we must load the vs-bytes len
            long len = readCardinal(vs, is).longValue();

            switch (c) {
                // strings
                case T_STR8:
                case T_STR16:
                case T_STR32:
                case T_STR64:
                    if (handler != null) {
                        byte[] buf = new byte[(int) len];
                        readFull(buf, buf.length, is);
                        handler.onScalar((byte) c, new String(buf, encoding), (decycleFlags & DF_STRING) != 0);
                    } else {
                        is.skip(len);
                    }
                    return stat;
                // bytes
                case T_BIN8:
                case T_BIN16:
                case T_BIN32:
                case T_BIN64:
                    if (handler != null) {
                        byte[] buf = new byte[(int) len];
                        readFull(buf, buf.length, is);
                        handler.onOpen((byte) c, true);
                        handler.pushByte(buf);
                        handler.onClose((byte) c);
                    } else {
                        is.skip(len);
                    }
                    return stat;
                // arrays
                case T_ARR8:
                case T_ARR16:
                case T_ARR32:
                case T_ARR64: {
                    if (handler != null) {
                        handler.onOpen((byte) c, true);
                    }
                    for (int i = 0; i < (int) len; i++) {
                        scan(stat, handler, is);
                    }
                    if (handler != null) {
                        handler.onClose((byte) c);
                    }
                    return stat;
                }
                // maps
                case T_MAP8:
                case T_MAP16:
                case T_MAP32:
                case T_MAP64: {
                    if (handler != null) {
                        handler.onOpen((byte) c, true);
                    }
                    for (int i = 0; i < (int) len; i++) {
                        //System.out.println("MapKey[" + i + "]: ");
                        scan(stat, handler, is);
                        //System.out.println("MapValue[" + i + "]: ");
                        scan(stat, handler, is);
                    }
                    if (handler != null) {
                        handler.onClose((byte) c);
                    }
                    return stat;
                }
                // custom: cross-references
                case T_REF:
                    //Object o = resolveRef(null, (long) len);
                    //System.out.println("REF: " + len + " -> " + o);
                    stat.addRef((int) len);
                    if (handler != null) {
                        handler.onOpen((byte) c, true);
                        handler.onClose((byte) c, (int) len);
                    }
                    return stat;
            }
        }
        throw new IOException("Unrecognized data type: " + c);
    }

    @Override
    public long writeRef(Object obj, Object oref, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        Long ref = (Long) oref;//checkRef(obj);
        if (ref != null) {
            os.write(T_REF);
            writeCardinal(ref.longValue(), 4, os);
            return 5;
        } else {
            return 0;
        }
    }

    int writeNum(Number n, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        if (n instanceof Float) {
            os.write(T_FLOAT);
            writeCardinal(Float.floatToRawIntBits(n.floatValue()), 4, os);
            return 4 + 1;
        } else if (n instanceof Double) {
            os.write(T_DOUBLE);
            writeCardinal(Double.doubleToRawLongBits(n.doubleValue()), 8, os);
            return 8 + 1;
        } else if (n.longValue() < 0) {
            long ms = -n.longValue();
            int c = 1;
            if (ms < 255) {
                os.write(T_NINT8);
                writeCardinal(ms, 1, os);
                c++;
            } else if (ms < 65536) {
                os.write(T_NINT16);
                writeCardinal(ms, 2, os);
                c += 2;
            } else if (ms < Integer.MAX_VALUE) {
                os.write(T_NINT32);
                writeCardinal(ms, 4, os);
                c += 4;
            } else {
                os.write(T_NINT64);
                writeCardinal(ms, 8, os);
                c += 8;
            }
            return c;
        } else {
            long ms = n.longValue();
            int c = 1;
            if (ms < 255) {
                os.write(T_UINT8);
                writeCardinal(ms, 1, os);
                c++;
            } else if (ms < 65536) {
                os.write(T_UINT16);
                writeCardinal(ms, 2, os);
                c += 2;
            } else if (ms < Integer.MAX_VALUE) {
                os.write(T_UINT32);
                writeCardinal(ms, 4, os);
                c += 4;
            } else {
                os.write(T_UINT64);
                writeCardinal(ms, 8, os);
                c += 8;
            }
            return c;
        }
    }

    int writeString(String n, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        byte[] data = n.getBytes("UTF-8");
        long ms = data.length;
        int c = 1;
        if (ms < 255) {
            os.write(T_STR8);
            writeCardinal(ms, 1, os);
            c++;
        } else if (ms < 65536) {
            os.write(T_STR16);
            writeCardinal(ms, 2, os);
            c += 2;
        } else if (ms < Integer.MAX_VALUE) {
            os.write(T_STR32);
            writeCardinal(ms, 4, os);
            c += 4;
        } else {
            os.write(T_STR64);
            writeCardinal(ms, 8, os);
            c += 8;
        }
        os.write(data);
        c += ms;
        return c;
    }

    int writeBin(byte[] data, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        long ms = data.length;
        int c = 1;
        if (ms < 255) {
            os.write(T_BIN8);
            writeCardinal(ms, 1, os);
            c++;
        } else if (ms < 65536) {
            os.write(T_BIN16);
            writeCardinal(ms, 2, os);
            c += 2;
        } else if (ms < Integer.MAX_VALUE) {
            os.write(T_BIN32);
            writeCardinal(ms, 4, os);
            c += 4;
        } else {
            os.write(T_BIN64);
            writeCardinal(ms, 8, os);
            c += 8;
        }
        os.write(data);
        c += ms;
        return c;
    }

    void writeCardinal(long l, int bytes, OutputStream os) throws IOException {
        long mask = 0xFF;
        int shift = 0;
        bytes--;
        os.write((byte) (l & mask));
        while (bytes > 0) {
            shift++;
            mask <<= 8;
            os.write((byte) ((l & mask) >> shift * 8));
            bytes--;
        }
    }

    Number readCardinal(int bytes, InputStream is) throws IOException {
        byte[] buf = new byte[bytes];
        int c = readFull(buf, bytes, is);
        long l = 0;
        int shift = 0;
        for (int i = 0; i < bytes; i++) {
            l |= ((long) (0xFF & buf[i])) << shift;
            shift += 8;
        }
        return l;
    }

    static Map<Byte, Field> typeNames;

    public static String type2name(byte type) {
        if (typeNames == null) {
            typeNames = new LinkedHashMap<Byte, Field>();
            Field[] ff = BJSONSerializer.class.getFields();
            for (Field f : ff) {
                //System.out.println("Field: " + f.getName() + ":" + f.getType() + "   " + f);
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
                    if (f.getName().startsWith("T_")) {
                        if (f.getType() == byte.class) {
                            try {
                                typeNames.put(f.getByte(null), f);
                                //System.out.println("  added for " + f.getByte(null));
                            } catch (Throwable th) {
                            }
                        }
                    }
                }
            }
        }
        Field f = typeNames.get(type);
        return (f != null) ? f.getName() : null;
    }

    public static class BJSONStat extends BOSStat<Byte, Integer> {

        @Override
        public String type2name(Byte type) {
            return BJSONSerializer.type2name(type);
        }

        @Override
        public boolean isRefType(Byte type, ObjectSerializerContext ctx) {
            if (type == null) {
                return false;
            }
            switch (type) {
                // strings
                case T_STR8:
                case T_STR16:
                case T_STR32:
                case T_STR64: {
                    if ((ctx.decycleFlags & DF_STRING) != 0) {
                        return true;
                    } else {
                        return false;
                    }
                }
                // bytes
                case T_BIN8:
                case T_BIN16:
                case T_BIN32:
                case T_BIN64: {
                    return true;
                }
                // arrays
                case T_ARR8:
                case T_ARR16:
                case T_ARR32:
                case T_ARR64: {
                    return true;
                }
                // maps
                case T_MAP8:
                case T_MAP16:
                case T_MAP32:
                case T_MAP64: {
                    return true;
                }
                default:
                    return false;
            }
        }

        @Override
        public boolean isScalarType(Byte type, ObjectSerializerContext ctx) {
            if (type == null) {
                return false;
            }
            switch (type) {
                case T_NULL:
                case T_FALSE_OR_ZERO:
                case T_EMPTY_STRING:
                case T_TRUE_OR_ONE:
                // strict? primitives
                case T_FALSE:
                case T_TRUE:
                case T_ZERO:
                case T_ONE:
                // positive cardinals
                case T_UINT8:
                case T_UINT16:
                case T_UINT32:
                case T_UINT64:
                // negative cardinals
                case T_NINT8:
                case T_NINT16:
                case T_NINT32:
                case T_NINT64:
                // reals
                case T_FLOAT_32:
                case T_FLOAT:
                case T_DOUBLE_64:
                case T_DOUBLE:
                // single-byte length
                case T_STR8:
                case T_STR16:
                case T_STR32:
                case T_STR64:
                    return true;
            }
            return false;
        }

        @Override
        public Byte refType(Integer ref) {
            return (ref < refTypes.size()) ? refTypes.get(ref) : null;
        }
    }

    public static class BJSONScanHandler extends BaseScanHandler<Byte, Integer> {

        int nextRef = 1;

        @Override
        public void onStart() {
            super.onStart();
            nextRef = 1;
        }

        @Override
        public boolean isObject(Byte type) {
            switch (type) {
                // maps
                case T_MAP8:
                case T_MAP16:
                case T_MAP32:
                case T_MAP64: {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isCollection(Byte type) {
            switch (type) {
                // arrays
                case T_ARR8:
                case T_ARR16:
                case T_ARR32:
                case T_ARR64: {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object adjust(Object value, Byte type) {
            if (T_REF == type) {
                return "#REF" + value;
            } else {
                return value;
            }
        }

        @Override
        public Integer createReference(Byte type, Object value) {
            if (type == null || value == null) {
                return null;
            } else {
                return nextRef++;
            }
        }

    }

}
