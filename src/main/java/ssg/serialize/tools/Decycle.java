/*
 * AS IS
 */
package ssg.serialize.tools;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * Decycle is thread-safe objects registration tool to avoid cyclic loops (e.g.
 * in scan or toString()).
 *
 * @author 000ssg
 */
public class Decycle {

    public static enum DTYPE {
        NEW, // decycle initialized!
        FIRST, // 1st appearance
        DUP // duplicated
    }
    private ThreadLocal<Decycle> decycle = new ThreadLocal<Decycle>();
    private Collection cache = new HashSet();
    private boolean reset = true;

    public void reset() {
        Decycle d = decycle.get();
        if (d == null) {
            d = new Decycle();
        } else {
            d.cache.clear();
        }
        d.reset = true;
    }

    public DTYPE check(Object obj) {
        Decycle d = decycle.get();

        if (d == null || d.reset) {
            if (d == null) {
                d = new Decycle();
                decycle.set(d);
            }
            d.reset = false;
            if (d.isDecycleable(obj)) {
                d.cache.add(System.identityHashCode(obj));
            }
            return DTYPE.NEW;
        }
        if (!d.isDecycleable(obj)) {
            return DTYPE.FIRST;
        }
        int hash = System.identityHashCode(obj);
        if (d.cache.contains(hash)) {
            return DTYPE.DUP;
        } else {
            d.cache.add(hash);
            return DTYPE.FIRST;
        }
    }

    public boolean isDecycleable(Object obj) {
        return !isSimple(obj);
    }

    public static boolean isSimple(Object obj) {
        if (obj == null) {
            return false;
        }
        Class cl = obj.getClass();
        if (cl.isPrimitive()
                || obj instanceof Number
                || cl == Boolean.class
                || cl == Character.class
                || cl == String.class
                || cl == String.class
                || cl == Byte.class
                || cl == Short.class
                || cl == Integer.class
                || cl == Long.class
                || cl == Float.class
                || cl == Double.class
                || cl == File.class
                || cl == URL.class
                || cl == URI.class) {
            return true;
        }
        return false;
    }

    static Decycle daDecycle = new Decycle();

    public static class DecycledMap<A, B> extends LinkedHashMap<A, B> {

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object o) {
            return hashCode() == System.identityHashCode(o);
        }

        @Override
        public String toString() {
            Decycle.DTYPE _dt = daDecycle.check(this);
            try {
                if (Decycle.DTYPE.DUP.equals(_dt)) {
                    return "#DUP";
                }
                return super.toString();
            } finally {
                if (Decycle.DTYPE.NEW.equals(_dt)) {
                    daDecycle.reset();
                }
            }
        }
    }

    public static class DecycledList<A> extends ArrayList<A> {

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object o) {
            return hashCode() == System.identityHashCode(o);
        }

        @Override
        public String toString() {
            Decycle.DTYPE _dt = daDecycle.check(this);
            try {
                if (Decycle.DTYPE.DUP.equals(_dt)) {
                    return "#DUP";
                }
                return super.toString();
            } finally {
                if (Decycle.DTYPE.NEW.equals(_dt)) {
                    daDecycle.reset();
                }
            }
        }
    }
}
