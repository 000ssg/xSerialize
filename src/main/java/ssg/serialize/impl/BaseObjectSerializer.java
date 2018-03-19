/*
 * AS IS
 */
package ssg.serialize.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import ssg.serialize.ObjectSerializer;
import ssg.serialize.tools.Reflector;
import java.util.LinkedHashMap;
import ssg.serialize.tools.ClassCaster;

/**
 *
 * @author 000ssg
 */
public abstract class BaseObjectSerializer extends BaseSerializer implements ObjectSerializer {

    // decycler extension
    public static final int DF_DEFAULT = 0x0000;
    public static final int DF_STRING = 0x0001;
    public static final int DF_BIGINT = 0x0002;
    public static final int DF_BIGDEC = 0x0004;
    public static int DF_EXTENSIONS
            = DF_STRING
            | DF_BIGINT
            | DF_BIGDEC;
    public static final int DF_IGNORE_MAP_NULLS = 0x1000;
    //public static final int DF_IGNORE_EMPTY_MAPS = 0x2000;
    public static final int DF_IGNORE_COLLECTION_NULLS = 0x4000;
    //public static final int DF_IGNORE_EMPTY_COLLECTIONS = 0x8000;

    // flag to enable cyclic references resolution
    boolean resolveCyclicReferences = true;
    // decycling options
    int decycleFlags = DF_DEFAULT;
    // optional class casting
    protected ClassCaster classCaster;

    public BaseObjectSerializer() {
    }

    public BaseObjectSerializer(boolean resolveCyclicReferences) {
        this.resolveCyclicReferences = resolveCyclicReferences;
    }

    public ObjectSerializerContext createContext(Object obj) {
        return new ObjectSerializerContext(this);
    }

    public void onCompletedContext(String info, ObjectSerializerContext ctx) {
    }

    ////////////////////////////////////////////////////////////////////////////
    /////////////////  object write
    ////////////////////////////////////////////////////////////////////////////
    public abstract long writeNull(OutputStream os, ObjectSerializerContext ctx) throws IOException;

    public abstract long writeScalar(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException;

    public abstract long writeCollection(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException;

    public abstract long writeMap(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException;

    public abstract long writeObject(Object obj, Reflector rf, OutputStream os, ObjectSerializerContext ctx) throws IOException;

    public abstract long writeRef(Object obj, Object ref, OutputStream os, ObjectSerializerContext ctx) throws IOException;

    public boolean isCollection(Object obj, ObjectSerializerContext ctx) {
        return obj instanceof Collection || obj != null && obj.getClass().isArray();
    }

    public boolean isMap(Object obj, ObjectSerializerContext ctx) {
        return obj instanceof Map;
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////// override/implement
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean isResolveCyclicReferences() {
        return this.resolveCyclicReferences;
    }

    @Override
    public void setResolveCyclicReferences(boolean resolve) {
        this.resolveCyclicReferences = resolve;
    }

    @Override
    public OSStat scan(InputStream is) throws IOException {
        return scan(null, null, is);
    }

    @Override
    public long write(Object obj, OutputStream os) throws IOException {
        ObjectSerializerContext ctx = createContext(obj);
        try {
            return write(ctx, obj, os);
        } finally {
            onCompletedContext("write", ctx);
        }
    }

    public long write(ObjectSerializerContext ctx, Object obj, OutputStream os) throws IOException {
        long c = 0;
        if (obj == null) {
            c += writeNull(os, ctx);
        } else if (ctx.isScalar(obj)) {
            Long ref = ((decycleFlags & DF_EXTENSIONS) != 0) ? ctx.checkRef(obj) : null;
            if (ref != null) {
                c += writeRef(obj, ref, os, ctx);
            } else {
                c += writeScalar(obj, os, ctx);
            }
        } else {
            Long ref = ctx.checkRef(obj);
            if (ref != null) {
                c += writeRef(obj, ref, os, ctx);
            } else if (isCollection(obj, ctx)) {
                c += writeCollection(obj, os, ctx);
            } else if (isMap(obj, ctx)) {
                c += writeMap(obj, os, ctx);
            } else {
                Object obj2 = simplify(ctx, obj);
                if (obj2 instanceof Reflector) {
                    c += writeObject(obj, ((Reflector) obj2), os, ctx);
                } else {
                    c += write(ctx, obj2, os);
                }
            }
        }
        return c;
    }

    @Override
    public <T> T fromText(String text, Class<T> type) throws IOException {
        return enrich(fromText(text), type);
    }

    @Override
    public <T> T fromBytes(byte[] data, Class<T> type) throws IOException {
        return enrich(fromBytes(data), type);
    }

    @Override
    public <T> T fromStream(InputStream is, Class<T> type) throws IOException {
        return enrich(fromStream(is), type);
    }

    @Override
    public <T> T fromURL(URL url, Class<T> type) throws IOException {
        return enrich(fromURL(url), type);
    }

    @Override
    public Object simplify(Object obj) {
        ObjectSerializerContext ctx = createContext(obj);
        try {
            return simplify(ctx, obj);
        } finally {
            onCompletedContext("simplify", ctx);
        }
    }

    @Override
    public <T> T enrich(Object obj, Class<T> clazz) throws IOException {
        ObjectSerializerContext ctx = createContext(obj);
        try {
            return enrich(ctx, obj, clazz);
        } finally {
            onCompletedContext("enrich", ctx);
        }
    }

    public Object simplify(ObjectSerializerContext ctx, Object obj) {
        return ctx.simplify(obj);
//        if (obj != null) {
//            if (Decycle.isSimple(obj)) {
//                return null;
//            }
//            Class cl = obj.getClass();
//            Reflector p = ctx.reflector(cl);
//            if (p.isEmpty()) {
//                // throw exception if nothing to writeCardinal?!
//                return null;
//            } else {
//                return p;
//            }
//        }
//        return null;
    }

    public <T> T enrich(ObjectSerializerContext ctx, Object obj, Class<T> clazz) throws IOException {
        return ctx.enrich(obj, clazz);
//        if (obj == null) {
//            return null;
//        } else if (clazz == null) {
//            return (T) obj;
//        } else if (Map.class.isAssignableFrom(clazz)) {
//            if (obj instanceof Map) {
//                return (T) obj;
//            }
//        } else if (List.class.isAssignableFrom(clazz)) {
//            if (obj instanceof List) {
//                return (T) obj;
//            } else if (obj.getClass().isArray()) {
//                return (T) Arrays.asList((Object[]) obj);
//            } else if (obj instanceof Collection) {
//                List lst = new ArrayList(((Collection) obj).size());
//                lst.addAll((Collection) obj);
//                return (T) lst;
//            }
//            throw new IOException("Incompatible type: requested " + clazz + ", got " + obj);
//        }
//        {
//            Object enriched = ctx.findEnriched(obj);
//            if (enriched != null) {
//                return (T) enriched;
//            }
//        }
//
//        if (clazz.isEnum() && obj instanceof String) {
//            return Reflector.getEnumValue(clazz, (String) obj);
//        }
//
//        // check if custom class cast
//        if (getClassCaster() != null) {
//            T r = getClassCaster().cast(obj, clazz);
//            if (r != null) {
//                return r;
//            }
//        }
//
//        // check if primitive/scalar
//        if (NumberTools.isNumeric(clazz)) {
//            // TODO: enrich number (non-primitives)
//            if (obj instanceof String) {
//                return (T) NumberTools.parse((String) obj, null, clazz);
//            } else if (obj instanceof Number) {
//                return (T) NumberTools.cast((Number) obj, clazz);
//            }
//        } else if (clazz.isPrimitive()) {
//            // TODO: enrich absolute primitives
//            if (boolean.class == clazz) {
//                if (obj instanceof Boolean) {
//                    return (T) obj;
//                } else if (obj instanceof String) {
//                    return (T) (Boolean) Boolean.parseBoolean((String) obj);
//                }
//            } else if (char.class == clazz) {
//                if (obj instanceof Character) {
//                    return (T) obj;
//                } else if (obj instanceof Number) {
//                    return (T) (Character) (char) ((Number) obj).intValue();
//                }
//            }
//        } else if (Boolean.class.equals(clazz)) {
//            if (obj instanceof Boolean) {
//                return (T) obj;
//            } else if (obj instanceof String) {
//                return (T) (Boolean) Boolean.parseBoolean((String) obj);
//            }
//        } else if (Character.class.equals(clazz)) {
//            if (obj instanceof Character) {
//                return (T) obj;
//            } else if (obj instanceof String) {
//                return (T) (Character) ((String) obj).charAt(0);
//            } else if (obj instanceof Number) {
//                return (T) (Character) (char) ((Number) obj).intValue();
//            }
//        } else if (clazz.equals(String.class)) {
//            // TODO: enrich string
//            return (T) obj.toString();
//        } else if (clazz.isArray()) {
//            if (clazz == char[].class && obj instanceof String) {
//                return (T) ((String) obj).toCharArray();
//            }
//            Class ct = clazz.getComponentType();
//            if (obj.getClass().isArray()) {
//                Class oclass = obj.getClass();
//                if (oclass == clazz) {
//                    return (T) obj;
//                }
//                Object arr = Array.newInstance(ct, Array.getLength(obj));
//                ctx.registerEnriched(obj, arr);
//                Object item = null;
//                for (int idx = 0; idx < Array.getLength(obj); idx++) {
//                    try {
//                        item = enrich(ctx, Array.get(obj, idx), ct);
//                        Array.set(arr, idx, item);
//                    } catch (Throwable rtex) {
//                        throw new IOException("Failed to restore collection item " + idx + " as " + ct, rtex);
//                    }
//                }
//                return (T) arr;
//            } else if (obj instanceof Collection) {
//                Collection coll = (Collection) obj;
//                Object arr = Array.newInstance(ct, coll.size());
//                ctx.registerEnriched(obj, arr);
//                int idx = 0;
//                for (Object ar : coll) {
//                    Array.set(arr, idx++, enrich(ctx, ar, ct));
//                }
//                return (T) arr;
//            }
//        }
//
//        {
//            // do POJO/Reflector...
//            Reflector p = ctx.reflector(clazz);
//            Object r = p.newInstance();
//            Object r2 = ctx.registerEnriched(obj, r);
//            if (r2 != r) {
//                return (T) r2;
//            }
//
//            Map m = (Map) obj;
//
//            for (Object n : m.keySet()) {
//                try {
//                    p.put(r, n, enrich(ctx, m.get(n), p.typeOf(n)));
//                } catch (RuntimeException rtex) {
//                    throw new IOException("failed to enrich property '" + n + "' as " + p.typeOf(n), rtex);
//                }
//            }
//
//            return (T) r;
//        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //////////////// utility methods
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @return the classCaster
     */
    public ClassCaster getClassCaster() {
        return classCaster;
    }

    /**
     * @param classCaster the classCaster to set
     */
    public void setClassCaster(ClassCaster classCaster) {
        this.classCaster = classCaster;
    }

    public static abstract class BOSStat<T, R extends Number> implements OSStat<T, R> {

        Map<T, int[]> types = new LinkedHashMap<T, int[]>();
        Map<R, int[]> refs = new LinkedHashMap<R, int[]>();
        List<T> refTypes = new ArrayList<T>();
        long started = System.nanoTime();
        long completed = started;
        long pos;
        ObjectSerializerContext ctx;

        @Override
        public long pos() {
            return pos;
        }

        @Override
        public void pos(long pos) {
            this.pos = pos;
        }

        @Override
        public void addType(T type) {
            int[] ii = types.get(type);
            if (ii == null) {
                ii = new int[1];
                types.put(type, ii);
            }
            ii[0]++;
            completed = System.nanoTime();
            if (isRefType(type, ctx)) {
                refTypes.add(type);
            }
        }

        @Override
        public void addRef(R ref) {
            int[] ii = refs.get(ref);
            if (ii == null) {
                ii = new int[1];
                refs.put(ref, ii);
            }
            ii[0]++;
            completed = System.nanoTime();
        }

        public abstract boolean isRefType(T type, ObjectSerializerContext ctx);

        public abstract boolean isScalarType(T type, ObjectSerializerContext ctx);

        public abstract T refType(R ref);

        public abstract String type2name(T type);

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName() + "{" + "types=" + types.size() + ", refs=" + refs.size() + ", evaluated in " + (completed - started) / 1000000f + "ms.");
            if (!types.isEmpty() || !refs.isEmpty()) {
                int tcount = 0;
                int rcount = 0;
                if (!types.isEmpty()) {
                    sb.append("\n  TYPES:");
                    int at = sb.length() - 1;
                    for (T type : types.keySet()) {
                        int[] ii = types.get(type);
                        tcount += ii[0];
                        sb.append("\n    " + type2name(type) + "\t" + ii[0]);
                    }
                    sb.insert(at, " (" + tcount + ")");
                }
                if (!refs.isEmpty()) {
                    sb.append("\n  REFERENCES:");
                    int at = sb.length() - 1;
                    for (R ref : refs.keySet()) {
                        int[] ii = refs.get(ref);
                        rcount += ii[0];
                        sb.append("\n    " + ref + "\t" + ii[0]);
                        T rt = refType(ref);
                        if (rt != null) {
                            sb.append("\t" + type2name(rt));
                        }
                    }
                    sb.insert(at, " (" + rcount + ")");
                }
                sb.append("\n  TYPES+REFS=" + (tcount + rcount));
                sb.append("\n");
            }
            sb.append('}');
            return sb.toString();
        }

    }
}
