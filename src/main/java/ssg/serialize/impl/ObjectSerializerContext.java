/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.ObjectSerializer;
import static ssg.serialize.impl.BaseObjectSerializer.DF_BIGDEC;
import static ssg.serialize.impl.BaseObjectSerializer.DF_BIGINT;
import static ssg.serialize.impl.BaseObjectSerializer.DF_DEFAULT;
import static ssg.serialize.impl.BaseObjectSerializer.DF_EXTENSIONS;
import static ssg.serialize.impl.BaseObjectSerializer.DF_STRING;
import ssg.serialize.tools.Decycle;
import ssg.serialize.tools.Reflector;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ssg.serialize.tools.ClassCaster;
import ssg.serialize.tools.NumberTools;

/**
 *
 * @author 000ssg
 */
public class ObjectSerializerContext {

    // POJO cached for  simplify/enrich operations. One per thread
    //static Map<Class, POJO> cpojoCache = Collections.synchronizedMap(new HashMap<Class, POJO>());
    static Map<Class, Reflector> crefCache = Collections.synchronizedMap(new HashMap<Class, Reflector>());
    int pojoOptions = Reflector.O_SKIP_NULLS | Reflector.O_READERS | Reflector.O_WRITERS | Reflector.O_FULL_BEAN;

    // cross-references resolution for read/write operations support
    Decycler decyclerv;
    Decycle daDecycle = new Decycle();
    // flag to enable cyclic references resolution
    boolean resolveCyclicReferences = true;
    // decycling options
    int decycleFlags = DF_DEFAULT;
    protected DecyclerListener decyclerListener;
    // optional class casting
    protected ClassCaster classCaster;

    public ObjectSerializerContext() {
    }

    public ObjectSerializerContext(ObjectSerializer serializer) {
        if (serializer != null) {
            setResolveCyclicReferences(serializer.isResolveCyclicReferences());
        }
        if (serializer instanceof BaseObjectSerializer) {
            decycleFlags = ((BaseObjectSerializer) serializer).decycleFlags;
        }
        if (isResolveCyclicReferences()) {
            decycler(true);
        }
        if (serializer instanceof BaseObjectSerializer) {
            if (((BaseObjectSerializer) serializer).getClassCaster() != null) {
                setClassCaster(((BaseObjectSerializer) serializer).getClassCaster());
            }
        }
    }

    public boolean isResolveCyclicReferences() {
        return this.resolveCyclicReferences;
    }

    public void setResolveCyclicReferences(boolean resolve) {
        this.resolveCyclicReferences = resolve;
    }

    public Object simplify(Object obj) {
        if (obj != null) {
            if (Decycle.isSimple(obj)) {
                return null;
            }
            Class cl = obj.getClass();
            Reflector p = reflector(cl);
            if (p.isEmpty()) {
                // throw exception if nothing to writeCardinal?!
                return null;
            } else {
                return p;
            }
        }
        return null;
    }

    public <T> T enrich(Object obj, Class<T> clazz) throws IOException {
        if (obj == null) {
            return null;
        } else if (clazz == null) {
            return (T) obj;
        } else if (Map.class.isAssignableFrom(clazz)) {
            if (obj instanceof Map) {
                return (T) obj;
            }
        } else if (List.class.isAssignableFrom(clazz)) {
            if (obj instanceof List) {
                return (T) obj;
            } else if (obj.getClass().isArray()) {
                return (T) Arrays.asList((Object[]) obj);
            } else if (obj instanceof Collection) {
                List lst = new ArrayList(((Collection) obj).size());
                lst.addAll((Collection) obj);
                return (T) lst;
            }
            throw new IOException("Incompatible type: requested " + clazz + ", got " + obj);
        }
        {
            Object enriched = findEnriched(obj);
            if (enriched != null) {
                return (T) enriched;
            }
        }

        if (clazz.isEnum() && obj instanceof String) {
            return Reflector.getEnumValue(clazz, (String) obj);
        }

        // check if custom class cast
        if (getClassCaster() != null) {
            T r = getClassCaster().cast(obj, clazz);
            if (r != null) {
                return r;
            }
        }

        // check if primitive/scalar
        if (NumberTools.isNumeric(clazz)) {
            // TODO: enrich number (non-primitives)
            if (obj instanceof String) {
                return (T) NumberTools.parse((String) obj, null, clazz);
            } else if (obj instanceof Number) {
                return (T) NumberTools.cast((Number) obj, clazz);
            }
        } else if (clazz.isPrimitive()) {
            // TODO: enrich absolute primitives
            if (boolean.class == clazz) {
                if (obj instanceof Boolean) {
                    return (T) obj;
                } else if (obj instanceof String) {
                    return (T) (Boolean) Boolean.parseBoolean((String) obj);
                }
            } else if (char.class == clazz) {
                if (obj instanceof Character) {
                    return (T) obj;
                } else if (obj instanceof Number) {
                    return (T) (Character) (char) ((Number) obj).intValue();
                }
            }
        } else if (Boolean.class.equals(clazz)) {
            if (obj instanceof Boolean) {
                return (T) obj;
            } else if (obj instanceof String) {
                return (T) (Boolean) Boolean.parseBoolean((String) obj);
            }
        } else if (Character.class.equals(clazz)) {
            if (obj instanceof Character) {
                return (T) obj;
            } else if (obj instanceof String) {
                return (T) (Character) ((String) obj).charAt(0);
            } else if (obj instanceof Number) {
                return (T) (Character) (char) ((Number) obj).intValue();
            }
        } else if (clazz.equals(String.class)) {
            // TODO: enrich string
            return (T) obj.toString();
        } else if (clazz.isArray()) {
            if (clazz == char[].class && obj instanceof String) {
                return (T) ((String) obj).toCharArray();
            }
            Class ct = clazz.getComponentType();
            if (obj.getClass().isArray()) {
                Class oclass = obj.getClass();
                if (oclass == clazz) {
                    return (T) obj;
                }
                Object arr = Array.newInstance(ct, Array.getLength(obj));
                registerEnriched(obj, arr);
                Object item = null;
                for (int idx = 0; idx < Array.getLength(obj); idx++) {
                    try {
                        item = enrich(Array.get(obj, idx), ct);
                        Array.set(arr, idx, item);
                    } catch (Throwable rtex) {
                        throw new IOException("Failed to restore collection item " + idx + " as " + ct, rtex);
                    }
                }
                return (T) arr;
            } else if (obj instanceof Collection) {
                Collection coll = (Collection) obj;
                Object arr = Array.newInstance(ct, coll.size());
                registerEnriched(obj, arr);
                int idx = 0;
                for (Object ar : coll) {
                    Array.set(arr, idx++, enrich(ar, ct));
                }
                return (T) arr;
            }
        }

        {
            // do POJO/Reflector...
            Reflector p = reflector(clazz);
            Object r = p.newInstance();
            Object r2 = registerEnriched(obj, r);
            if (r2 != r) {
                return (T) r2;
            }

            Map m = (Map) obj;

            for (Object n : m.keySet()) {
                try {
                    p.put(r, n, enrich(m.get(n), p.typeOf(n)));
                } catch (RuntimeException rtex) {
                    throw new IOException("failed to enrich property '" + n + "' as " + p.typeOf(n), rtex);
                }
            }

            return (T) r;
        }
    }

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

    public Decycler decycler(boolean createMissing) {
        if (!isResolveCyclicReferences()) {
            return null;
        }
        if (decyclerv == null) {
            decyclerv = createDecycler();
        }
        return decyclerv;
    }

    public Decycler createDecycler() {
        if ((decycleFlags & DF_EXTENSIONS) != 0) {
            return new Decycler() {
                long xx = 10000000000L;
                boolean decycleStrings = (decycleFlags & DF_STRING) != 0;
                boolean decycleBI = (decycleFlags & DF_BIGINT) != 0;
                boolean decycleBD = (decycleFlags & DF_BIGDEC) != 0;

                @Override
                public boolean forceDecycle(Object obj) {
                    //System.out.println("  yyyyy " + ((obj!=null) ? obj.hashCode() : "<null>") + "\t" + obj);
                    return super.forceDecycle(obj)
                            || decycleStrings && obj instanceof String
                            || decycleBI && obj instanceof BigInteger
                            || decycleBD && obj instanceof BigDecimal;
                }

                @Override
                public long identity(Object obj) {
                    if (forceDecycle(obj)) {
                        return ((long) ((long) System.identityHashCode(obj.getClass()) << 32)) + obj.hashCode();
                    }
                    return super.identity(obj);
                }
            };
        } else {
            return new Decycler();
        }
    }

    public Long checkRef(Object obj) throws IOException {
        Decycler decycler = decycler(false);
        if (decycler == null) {
            return null;
        }
        return decycler.onWrite(obj);
    }

    public <T> T resolveRef(Object obj, Long ref) throws IOException {
        Decycler decycler = decycler(false);
        if (decycler == null) {
            return (T) obj;
        }
        return (T) decycler.onRead(obj, ref);
    }

    public <T> T findEnriched(Object obj) {
        Decycler decycler = decycler(false);
        if (decycler == null) {
            return (T) obj;
        }
        return decycler.findEnriched(obj);
    }

    public <T> T registerEnriched(Object obj, Object enriched) {
        Decycler decycler = decycler(false);
        if (decycler == null) {
            return (T) enriched;
        }
        //System.out.println("-> " + System.identityHashCode(obj) + " (" + obj.getClass() + ") -> " + System.identityHashCode(enriched) + " (" + enriched.getClass() + ")");
        return decycler.onEnrich(obj, enriched);
    }

    public Map<Class, Reflector> cache() {
        return crefCache;
    }

    public Reflector reflector(Class cl) {
        Reflector p = crefCache.get(cl);
        if (p == null) {
            p = new Reflector(cl, pojoOptions);
            crefCache.put(cl, p);
        }
        return p;
    }

    public boolean isScalar(Object obj) {
        if (obj == null) {
            return false;
        }

        if (Decycle.isSimple(obj)) {
            return true;
        }

        if (obj instanceof String
                || obj != null && obj.getClass().isEnum()
                || obj instanceof Number
                || obj instanceof Boolean
                || obj instanceof Character) {
            return true;
        }

        Class cl = (obj instanceof Class) ? (Class) obj : obj.getClass();
        if (cl.isPrimitive()) {
            return true;
        }

        return false;
    }

    /**
     * @return the decyclerListener
     */
    public DecyclerListener getDecyclerListener() {
        return decyclerListener;
    }

    /**
     * @param decyclerListener the decyclerListener to set
     */
    public void setDecyclerListener(DecyclerListener decyclerListener) {
        this.decyclerListener = decyclerListener;
    }

    /**
     * Cyclic references resolver.
     *
     * objects - all processed objects by identity hash
     *
     * offsets - relative offsets of objects (object as identity hash)
     *
     * cycled - cycled objects identity hashes
     *
     * In WRITE mode offsets are stored as (hash,offset)
     *
     * In READ mode offsets are stored as (offset, hash)
     *
     * The offsets are stored as "table" property in (offset,hash) sequences
     * into bytes array.
     *
     * Offset is long, hash is int.
     *
     * To enable more flexibility, the identy evaluation is delegated to
     * BaseObjectSerializer just allowing overriding.
     *
     */
    public class Decycler {

        public transient Map<Long, Object> objects = new HashMap<Long, Object>();
        public transient Collection<Long> cycled = new HashSet<Long>();
        public transient Map<Long, Long> offsets = new LinkedHashMap<Long, Long>();
        public transient int count = 0;

        public void reset() {
            count = 0;
            objects.clear();
            cycled.clear();
            offsets.clear();
        }

        @Override
        public String toString() {
            return "Decycler{" + "objects=" + objects.size() + ", cycled=" + cycled.size() + ", offsets=" + offsets.size() + ", count=" + count + '}';
        }

        /**
         * Checks if this object is already stored and returns original's save
         * order. If this is 1st time, the object is stored and null is
         * returned.
         *
         * @param offset
         * @param hash
         * @param obj
         * @return
         */
        public Long onWrite(Object obj) {
            if (obj != null) {
                if (isScalar(obj)) { // || obj instanceof POJO) {
                    if (!forceDecycle(obj)) {
                        return null;
                    }
                }

                count++;
                long hash = identity(obj);
                Object obj2 = objects.get(hash);//offsets.get(hash);
                if (obj2 != null && obj2 == obj) { //j==obj2) {// offsets.containsKey(hash)) {
                    cycled.add(hash);
                    //System.out.println("WR:REF:" + count + ":" + obj.getClass().getSimpleName());
                    if (decyclerListener != null) {
                        decyclerListener.onRegisterDuplicate(obj, count, hash);
                    }
                    return offsets.get(hash);
                } else {
                    offsets.put(hash, (long) count);
                    objects.put(hash, obj);
                    if (decyclerListener != null) {
                        decyclerListener.onRegisterRef(obj, count, hash);
                    }
                    //System.out.println("WR:NEW:" + count + ":" + obj.getClass().getSimpleName() + "  " + ((obj instanceof char[]) ? new String((char[]) obj) : ""));
                }
            }
            return null;
        }

        /**
         * Increments objects count and returns
         *
         * @param offset
         * @return
         */
        public <T> T onRead(Object obj, Long ref) {
//            if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj instanceof Character) {
            if (ref == null && isScalar(obj)) {
                if (!forceDecycle(obj)) {
                    return null;
                }
            }

            count++;

            Long i = (ref != null) ? offsets.get(ref) : null;//  (obj instanceof Number) ? offsets.get(((Number) obj).longValue()) : null;
            if (i != null) {
                //System.out.println("RD:REF:" + count + ":" + (objects.get(i)).getClass().getSimpleName());
                T r = (T) objects.get(i);
                if (decyclerListener != null) {
                    decyclerListener.onResolveRef(ref, r);
                }
                return r;
            } else {
                long hash = identity(obj);
                offsets.put((long) count, hash);
                objects.put(hash, obj);
                //System.out.println("RD:NEW:" + count + ":" + obj.getClass().getSimpleName() + "  " + obj);
                if (decyclerListener != null) {
                    decyclerListener.onRegisterRef(obj, count, hash);
                }
                return (T) obj;
            }
        }

        /**
         * Replaces old item (if exists) with enriched value. Returns the
         * enriched value.
         *
         * @param <T>
         * @param src
         * @param ehriched
         * @return
         */
        public <T> T onEnrich(Object src, Object enriched) {
            long hash = identity(src);
            if (objects.containsKey(hash)) {
                Object o = objects.get(hash);
                if (identity(o) == hash) {
                    objects.put(hash, enriched);
                } else {
                    enriched = o;
                }
            } else {
                objects.put(hash, enriched);
            }
            return (T) enriched;
        }

        /**
         * Returns enriched variant if exists and changed, otherwise - null.
         *
         * @param <T>
         * @param src
         * @return
         */
        public <T> T findEnriched(Object src) {
            Long hash = identity(src);
            Object enriched = objects.get(src);
            if (enriched == null || enriched == src) {
                return null;
            }
            return (T) enriched;
        }

        public boolean hasReferences() {
            return !cycled.isEmpty();
        }

        public String dumpCycled() {
            StringBuilder sb = new StringBuilder();
            sb.append("cycled: " + cycled.size());
            for (Long hash : cycled) {
                sb.append("\n  " + hash + " -> " + offsets.get(hash) + " -> " + ("" + objects.get(hash)).replace("\n", "\\n"));
            }

            return sb.toString();
        }

        public String dumpRegistered(boolean forWrite, boolean withHashes, boolean typesOnly) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            if (!offsets.isEmpty()) {
                Map.Entry<Long, Long>[] ee = offsets.entrySet().toArray(new Map.Entry[offsets.size()]);
                if (forWrite) {
                    Arrays.sort(ee, new Comparator<Map.Entry<Long, Long>>() {
                        @Override
                        public int compare(Map.Entry<Long, Long> o1, Map.Entry<Long, Long> o2) {
                            return o1.getValue().compareTo(o2.getValue());
                        }
                    });
                }
                for (Map.Entry<Long, Long> entry : ee) {
                    Object hash = entry.getKey();
                    Object key = entry.getValue();
                    if (!forWrite) {
                        key = entry.getKey();
                        hash = entry.getValue();
                    }
                    sb.append("\n  " + key + "(");
                    if (withHashes) {
                        sb.append(hash);
                        if (cycled.contains(hash)) {
                            sb.append(", cycled");
                        }
                    } else if (cycled.contains(hash)) {
                        sb.append("cycled");
                    }
                    sb.append("): ");
                    Object val = objects.get(hash);
                    if (val == null) {
                        sb.append("null");
                    } else if (val.getClass().isArray()) {
                        sb.append("[");
                        if (typesOnly) {
                            sb.append(Array.getLength(val) + ", " + val.getClass().getComponentType().getSimpleName());
                        } else {
                            for (int i = 0; i < Array.getLength(val); i++) {
                                if (i > 0) {
                                    sb.append(", ");
                                }
                                sb.append(Array.get(val, i));
                            }
                        }
                        sb.append("]");
                    } else if (typesOnly) {
                        sb.append(val.getClass().getSimpleName());
                        if (val instanceof String && ((String) val).length() < 20) {
                            sb.append("  " + val);
                        }
                    } else {
                        sb.append(val.toString().replace("\n", "\\n"));
                    }
                }
            }
            sb.append("}");
            return sb.toString();
        }

        /**
         * Object identity evaluation for duplicates and cyclic references
         * detection/resolution support.
         *
         * @param obj
         * @return
         */
        public long identity(Object obj) {
            if (obj == null) {
                return System.identityHashCode(null);
            } else {
                return (((long) System.identityHashCode(obj.getClass())) << 32) | System.identityHashCode(obj);
            }
        }

        public boolean forceDecycle(Object obj) {
            return false;
        }

    }

    /**
     * Decycler follow-up listener
     */
    public static interface DecyclerListener {

        void onRegisterRef(Object obj, Integer ref, Long hash);

        void onRegisterDuplicate(Object obj, Integer ref, Long hash);

        void onResolveRef(Long ref, Object obj);
    }
}
