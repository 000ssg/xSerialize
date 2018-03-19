/*
 * AS IS
 */
package ssg.serialize.tools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map-like representation of object.
 */
public class Reflector {

    public static final int O_SKIP_NULLS = 0x0001;
    public static final int O_READERS = 0x0002;
    public static final int O_WRITERS = 0x0004;
    public static final int O_PROTECTED = 0x0008;
    public static final int O_PRIVATE = 0x0010;
    public static final int O_STATIC = 0x0020;
    public static final int O_CASCADED = 0x0040;
    public static final int O_FULL_BEAN = 0x0080;

    private final static Map<Class, Reflector> defaultReflectors = new HashMap<Class, Reflector>();
    private final static Map<Class, Method> enumValueOf = new HashMap<Class, Method>();
    private final static Set<Class> defaultNonReflectables = new HashSet<Class>() {
        {
            add(String.class);
            add(File.class);
            add(URL.class);
            add(URI.class);
            add(Class.class);
            add(Number.class);
            add(BigDecimal.class);
            add(BigInteger.class);
            add(Byte.class);
            add(Short.class);
            add(Integer.class);
            add(Long.class);
            add(Float.class);
            add(Double.class);
            add(Character.class);
            add(Date.class);
        }
    };

    Class clazz;
    int options;
    Map<String, Object> accessors = new LinkedHashMap<String, Object>();

    public Reflector(Object obj) {
        clazz = (obj instanceof Class) ? (Class) obj : (obj != null) ? obj.getClass() : null;
        init(O_SKIP_NULLS | O_READERS | O_WRITERS | O_FULL_BEAN);
    }

    public Reflector(Object obj, int options) {
        clazz = (obj instanceof Class) ? (Class) obj : (obj != null) ? obj.getClass() : null;
        init(options);
    }

    public Reflector(Class clazz) {
        this.clazz = clazz;
        init(O_SKIP_NULLS | O_READERS | O_WRITERS | O_FULL_BEAN);
    }

    /**
     * Default reflector mapper interface. Returns Map representation of object
     * or null if obj is null or class is not supported by reflector.
     *
     * @param obj
     * @return
     */
    public static Map<String, Object> reflect(Object obj) {
        return reflect(obj, false);
    }

    /**
     * Default reflector mapper interface. Returns Map representation of object
     * If obj is null or class is not supported by reflector returns either null
     * (allowEmpty=false) or empty map (allowEmpty=true).
     *
     * @param obj
     * @param allowEmpty
     * @return
     */
    public static Map<String, Object> reflect(Object obj, boolean allowEmpty) {
        Reflector r = reflector(obj);
        return (r != null) ? r.asMap(obj) : (allowEmpty) ? Collections.emptyMap() : null;
    }

    public static Reflector reflector(Object obj) {
        if (obj == null
                || obj.getClass().isPrimitive()
                || obj.getClass().isArray()
                || obj.getClass().isEnum()
                || defaultNonReflectables.contains(obj.getClass())) {
            return null;
        }
        Class cl = obj.getClass();
        Reflector r = defaultReflectors.get(cl);
        if (r == null) {
            synchronized (defaultReflectors) {
                r = new Reflector(cl);
                defaultReflectors.put(cl, r);
            }
        }
        return r;
    }

    public boolean isEmpty() {
        return accessors.isEmpty();
    }

    public int size() {
        return accessors.size();
    }

    /**
     * Returns new instance for POJO-associated object. POJO is not changed. To
     * make use of POJO, use p=p.setObject(newObj);
     *
     * @param <T>
     * @return
     */
    public <T> T newInstance() {
        try {
            Object r = clazz.newInstance();
            return (T) r;
        } catch (Throwable th) {
            throw new RuntimeException("Failed to create new object instance: " + clazz, th);
        }
    }

    public Map asMap(final Object obj) {
        return new Map() {
            @Override
            public int size() {
                return accessors.size();
            }

            @Override
            public boolean isEmpty() {
                return accessors.isEmpty();
            }

            @Override
            public boolean containsKey(Object key) {
                return accessors.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Object get(Object key) {
                Object o = accessors.get(key);
                if (o instanceof Field) {
                    try {
                        return ((Field) o).get(obj);
                    } catch (Throwable th) {
                        throw new RuntimeException("READ: failed to read field " + key + " for " + obj, th);
                    }
                } else if (o instanceof Method[]) {
                    Method[] mm = (Method[]) o;
                    if (mm[0] == null) {
                        throw new RuntimeException("READ: no accessor: " + key + " for " + obj);
                    }
                    try {
                        return mm[0].invoke(obj);
                    } catch (Throwable th) {
                        throw new RuntimeException("READ: failed to execute reader " + key + " for " + obj, th);
                    }
                } else {
                    // throw exception? no accessor
                    throw new RuntimeException("READ: No such property: " + key + " for " + obj);
                }
            }

            @Override
            public Object put(Object key, Object value) {
                Object o = accessors.get(key);
                if (o instanceof Field) {
                    try {
                        ((Field) o).set(obj, value);
                    } catch (Throwable th) {
                        throw new RuntimeException("WRITE: failed to write field " + key + " for " + obj, th);
                    }
                } else if (o instanceof Method[]) {
                    Method[] mm = (Method[]) o;
                    if (mm[1] == null) {
                        throw new RuntimeException("WRITE: no accessor: " + key + " for " + obj);
                    }
                    try {
                        return mm[1].invoke(obj, value);
                    } catch (Throwable th) {
                        throw new RuntimeException("WRITE: failed to execute writer " + key + " for " + obj, th);
                    }
                } else {
                    // throw exception? no accessor
                    throw new RuntimeException("WRITE: No such property: " + key + " for " + obj);
                }
                return value;
            }

            @Override
            public Object remove(Object key) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void putAll(Map m) {
                if (m != null) {
                    for (Object key : m.keySet()) {
                        try {
                            put(key, m.get(key));
                        } catch (Throwable th) {
                            throw new RuntimeException("WRITE: failed to put multiple properties to " + obj, th);
                        }
                    }
                }
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set keySet() {
                return accessors.keySet();
            }

            @Override
            public Collection values() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set entrySet() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

    public Class typeOf(Object key) {
        Object o = accessors.get(key);
        if (o instanceof Field) {
            return ((Field) o).getType();
        } else if (o instanceof Method[]) {
            Method[] mm = (Method[]) o;
            if (mm[0] != null) {
                return mm[0].getReturnType();
            } else {
                return mm[1].getParameterTypes()[0];
            }
        }
        return null;
    }

    public void init(int options) {
        this.options = options;
        if (clazz != null) {
            Field[] fs = clazz.getFields();
            Method[] ms = clazz.getMethods();
            Field[] fsp = null;
            Field[] fspp = null;
            Method[] msp = null;
            Method[] mspp = null;
            // pre-sort fields/methods within groups by names
            for (Field[] ff : new Field[][]{fs, fsp, fspp}) {
                if (ff != null && ff.length > 0) {
                    Arrays.sort(ff, new Comparator<Field>() {
                        @Override
                        public int compare(Field o1, Field o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
                }
            }
            for (Method[] mm : new Method[][]{ms, msp, mspp}) {
                if (mm != null && mm.length > 0) {
                    Arrays.sort(mm, new Comparator<Method>() {
                        @Override
                        public int compare(Method o1, Method o2) {
                            int c = o1.getName().compareTo(o2.getName());
                            if (c == 0) {
                                c = ((Integer) o1.getParameterCount()).compareTo(o2.getParameterCount());
                            }
                            if (c == 0) {
                                c = o1.toGenericString().compareTo(o2.toGenericString());
                            }
                            return c;
                        }
                    });
                }
            }
            for (Field[] ff : new Field[][]{fs, fsp, fspp}) {
                if (ff == null || ff.length == 0) {
                    continue;
                }
                for (Field f : ff) {
                    if (Modifier.isTransient(f.getModifiers())) {
                        continue;
                    }
                    if (Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    String n = f.getName();
                    if (accessors.containsKey(n)) {
                        continue;
                    }
                    accessors.put(n, f);
                    f.setAccessible(true);
                }
            }
            for (Method[] mm : new Method[][]{ms, msp, mspp}) {
                if (mm == null || mm.length == 0) {
                    continue;
                }
                for (Method m : mm) {
                    String ns = toSetterName(m);
                    String ng = toGetterName(m);
                    if (ns != null) {
                        Object ao = accessors.get(ns);
                        if (ao instanceof Field) {
                            continue;
                        }
                        if (ao instanceof Method[]) {
                            if (((Method[]) ao)[1] == null) {
                                ((Method[]) ao)[1] = m;
                            }
                        } else if (ao == null) {
                            accessors.put(ns, new Method[]{null, m});
                        }
                    } else if (ng != null) {
                        if ("class".equals(ng)) {
                            continue;
                        }
                        Object ao = accessors.get(ng);
                        if (ao instanceof Field) {
                            continue;
                        }
                        if (ao instanceof Method[]) {
                            if (((Method[]) ao)[0] == null) {
                                ((Method[]) ao)[0] = m;
                            }
                        } else if (ao == null) {
                            accessors.put(ng, new Method[]{m, null});
                        }
                    }
                }
            }
            if ((options & O_FULL_BEAN) == O_FULL_BEAN) {
                // remove incomplete bean elements (i.e. is only getter or setter is present
                for (Object key : accessors.keySet().toArray()) {
                    Object val = accessors.get(key);
                    if (val instanceof Method[]) {
                        Method[] mm = (Method[]) val;
                        if (mm.length < 2 || mm[0] == null || mm[1] == null) {
                            accessors.remove(key);
                        }
                    }
                }
            }
        }
    }

    String toGetterName(Method m) {
        if (m == null) {
            return null;
        }
        Class[] pts = m.getParameterTypes();
        if (pts == null || pts.length == 0) {
            String n = m.getName();
            if (n.startsWith("is")) {
                n = n.substring(2);
            } else if (n.startsWith("get")) {
                n = n.substring(3);
            } else {
                return null;
            }
            if (n.isEmpty()) {
                return null;
            }
            if (n.length() > 1) {
                return Character.toLowerCase(n.charAt(0)) + n.substring(1);
            } else {
                return n.toLowerCase();
            }
        }
        return null;
    }

    String toSetterName(Method m) {
        if (m == null) {
            return null;
        }
        Class[] pts = m.getParameterTypes();
        if (pts != null && pts.length == 1) {
            String n = m.getName();
            if (n.startsWith("set")) {
                n = n.substring(3);
            } else {
                return null;
            }
            if (n.isEmpty()) {
                return null;
            }
            if (n.length() > 1) {
                return Character.toLowerCase(n.charAt(0)) + n.substring(1);
            } else {
                return n.toLowerCase();
            }
        }
        return null;
    }

    public Object get(Object obj, Object key) {
        Object o = accessors.get(key);
        if (o instanceof Field) {
            try {
                return ((Field) o).get(obj);
            } catch (Throwable th) {
                throw new RuntimeException("READ: failed to read field " + key + " for " + obj, th);
            }
        } else if (o instanceof Method[]) {
            Method[] mm = (Method[]) o;
            if (mm[0] == null) {
                throw new RuntimeException("READ: no accessor: " + key + " for " + obj);
            }
            try {
                return mm[0].invoke(obj);
            } catch (Throwable th) {
                throw new RuntimeException("READ: failed to execute reader " + key + " for " + obj, th);
            }
        } else {
            // throw exception? no accessor
            throw new RuntimeException("READ: No such property: " + key + " for " + obj);
        }
    }

    public Object put(Object obj, Object key, Object value) {
        Object o = accessors.get(key);
        if (o instanceof Field) {
            try {
                ((Field) o).set(obj, value);
            } catch (Throwable th) {
                throw new RuntimeException("WRITE: failed to write field " + key + " for " + obj, th);
            }
        } else if (o instanceof Method[]) {
            Method[] mm = (Method[]) o;
            if (mm[1] == null) {
                throw new RuntimeException("WRITE: no accessor: " + key + " for " + obj);
            }
            try {
                return mm[1].invoke(obj, value);
            } catch (Throwable th) {
                throw new RuntimeException("WRITE: failed to execute writer " + key + " for " + obj, th);
            }
        } else {
            // throw exception? no accessor
            throw new RuntimeException("WRITE: No such property: " + key + " for " + obj);
        }
        return value;
    }

    public Set keySet() {
        return accessors.keySet();
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////// utilities
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Converts valueName to enumeration value of given class. If no class or
     * class is not enum - null is returned. If invalid value - IOException is
     * thrown.
     *
     * @param <T>
     * @param cl
     * @param valueName
     * @return
     * @throws IOException
     */
    public static <T> T getEnumValue(Class cl, String valueName) throws IOException {
        if (cl == null || !cl.isEnum() || valueName == null) {
            return null;
        }
        Method m = enumValueOf.get(cl);
        if (m == null) {
            try {
                m = cl.getMethod("valueOf", String.class);
                enumValueOf.put(cl, m);
            } catch (Throwable th) {
            }
        }
        try {
            return (T) m.invoke(null, valueName);
        } catch (Throwable th) {
            throw new IOException("Failed to restore enumeration " + cl.getName() + " for value '" + valueName + "'", th);
        }
    }

    public static Class[] generics(Class clazz, Type type) {
        //System.out.println("G: "+type+", cl="+type);
        Type t = type;
        if (Collection.class.isAssignableFrom(clazz)) {
            //t=type.getComponentType().getGenericSuperclass();
            int a = 0;
        }
        if (!t.equals(clazz)) {
            ParameterizedType pt = (t instanceof ParameterizedType) ? (ParameterizedType) t : null;
            if (pt == null) {
                return null;
            }
            Type[] rpt = ((Class) pt.getRawType()).getGenericInterfaces();
            for (int i = 0; i < rpt.length; i++) {
                if (rpt[i] instanceof ParameterizedType) {
                    rpt = ((ParameterizedType) rpt[i]).getActualTypeArguments();
                    break;
                }
            }

            Type[] apt = pt.getActualTypeArguments();
            Class[] result = new Class[apt.length];
            if (apt != null) {
                for (int i = 0; i < apt.length; i++) {
                    Type at = apt[i];
                    Type ar = (rpt != null && rpt.length > i) ? rpt[i] : null;
                    try {
                        if (at instanceof ParameterizedType) {
                            // TODO: consider Type[], not Class[] for result...
                            result[i] = (Class) ((ParameterizedType) at).getRawType();
                        } else {
                            Class ac = Reflector.class.getClassLoader().loadClass(at.getTypeName());
                            result[i] = ac;
                        }
                    } catch (Throwable th) {
                        if (at instanceof TypeVariable) {
                            // TODO: check if should recursively go up to Object or whatever exists...
                            if (at.getTypeName().indexOf('.') == 0) {
                                TypeVariable tv = (TypeVariable) at;
                                Type[] tbs = tv.getBounds();
                                result[i] = (tbs != null && tbs.length > 0) ? (Class) tbs[0] : Object.class;
                            }
                        } else {
                            // th.printStackTrace();
                        }
                    }
                }
            }
            int a = 0;
            return result;
        } else {
            return null;
        }
    }
    
    public static Type[] generics(Object obj) {
        Type t = null;
        if (obj instanceof Field) {
            t = ((Field) obj).getGenericType();
        } else if (obj instanceof Method) {
            Method m = (Method) obj;
            if (m.getParameterTypes() == null || m.getParameterTypes().length == 0) {
                t = m.getGenericReturnType();
            } else if (m.getParameterTypes() != null && m.getParameterTypes().length == 1 && m.getReturnType().equals(Void.class)) {
                t = m.getGenericParameterTypes()[0];
            }
        } else if (obj instanceof ParameterizedType) {
            t = (Type) obj;
        } else if (obj instanceof Class) {
            t = (Class) obj;
        } else if (obj != null) {
            t = obj.getClass();
        }
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            return pt.getActualTypeArguments();
        } else if (t != null) {
            return new Type[]{t};
        } else {
            return null;
        }
    }

}
