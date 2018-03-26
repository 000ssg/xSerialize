/*
 * AS IS
 */
package ssg.serialize.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import ssg.serialize.tools.Decycle;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ssg.serialize.tools.Reflector;

/**
 * TestPOJO is set of similar "bean" classes with public fileds, methods, or
 * mixed bean properties implementation.
 *
 * The classes represent all java primitives and their arrays as well as
 * objectized versions of both.
 *
 * Additionally they have self single and array references, allowing testing of
 * cyclic references.
 *
 * @author 000ssg
 */
public class TestPOJO {

    public static enum POJOEnum1 {
        aaa, bbb, ccc;
    }

    public static <T> T randomPOJO(Class<T> clazz) {
        return randomPOJO(clazz, 0);
    }

    public static <T> T randomPOJO(Class<T> clazz, int len) {
        Object o = null;
        try {
            o = clazz.newInstance();
            Reflector r = new Reflector(o);// new POJO(o);
            for (Object key : r.keySet()) {
                r.put(o, key, generateItem(r.typeOf(key), len));
            }
        } catch (Throwable th) {
        }
        return (T) o;
    }

    public static <T> T generateItem(Class<T> clazz) {
        return generateItem(clazz, 0);
    }

    public static <T> T generateItem(Class<T> clazz, int len) {
        if (clazz.isArray()) {
            Object o = Array.newInstance(clazz.getComponentType(), (len != 0) ? len : 5 + (int) (Math.random() * 35));
            for (int i = 0; i < Array.getLength(o); i++) {
                Array.set(o, i, generateItem(clazz.getComponentType()));
            }
            return (T) o;
        } else {
            if (clazz.isEnum()) {
                try {
                    Method m = clazz.getMethod("values");
                    Object[] vs = (Object[]) m.invoke(null);
                    int idx = (int) (Math.random() * vs.length);
                    if (idx < 0) {
                        idx = 0;
                    }
                    if (idx >= vs.length) {
                        idx = vs.length - 1;
                    }
                    return (T) vs[idx];
                } catch (Throwable th) {
                    return null;
                }
            }
            if (clazz == byte.class) {
                return (T) (Byte) (byte) (Math.random() * Byte.MAX_VALUE - Byte.MAX_VALUE / 2);
            }
            if (clazz == Byte.class) {
                return (T) (Byte) (byte) (Math.random() * Byte.MAX_VALUE - Byte.MAX_VALUE / 2);
            }
            if (clazz == short.class) {
                return (T) (Short) (short) (Math.random() * Short.MAX_VALUE - Short.MAX_VALUE / 2);
            }
            if (clazz == Short.class) {
                return (T) (Short) (short) (Math.random() * Short.MAX_VALUE - Short.MAX_VALUE / 2);
            }
            if (clazz == int.class) {
                return (T) (Integer) (int) (Math.random() * Integer.MAX_VALUE - Integer.MAX_VALUE / 2);
            }
            if (clazz == Integer.class) {
                return (T) (Integer) (int) (Math.random() * Integer.MAX_VALUE - Integer.MAX_VALUE / 2);
            }
            if (clazz == long.class) {
                return (T) (Long) (long) (Math.random() * Long.MAX_VALUE - Long.MAX_VALUE / 2);
            }
            if (clazz == Long.class) {
                return (T) (Long) (long) (Math.random() * Long.MAX_VALUE - Long.MAX_VALUE / 2);
            }
            if (clazz == float.class) {
                return (T) (Float) (float) (Math.random() * Float.MAX_VALUE - Float.MAX_VALUE / 2);
            }
            if (clazz == Float.class) {
                return (T) (Float) (float) (Math.random() * Float.MAX_VALUE - Float.MAX_VALUE / 2);
            }
            if (clazz == double.class) {
                return (T) (Double) (double) (Math.random() * Double.MAX_VALUE - Double.MAX_VALUE / 2);
            }
            if (clazz == Double.class) {
                return (T) (Double) (double) (Math.random() * Double.MAX_VALUE - Double.MAX_VALUE / 2);
            }
            if (clazz == BigInteger.class) {
                return (T) BigInteger.valueOf((long) (Math.random() * Long.MAX_VALUE - Long.MAX_VALUE / 2));
            }
            if (clazz == BigDecimal.class) {
                return (T) BigDecimal.valueOf((double) (Math.random() * Double.MAX_VALUE - Double.MAX_VALUE / 2));
            }
            if (clazz == boolean.class) {
                return (T) (Boolean) ((Math.random() > 0.5) ? true : false);
            }
            if (clazz == Boolean.class) {
                return (T) (Boolean) ((Math.random() > 0.5) ? true : false);
            }
            if (clazz == char.class) {
                int c = (int) Math.abs((Math.random() * Short.MAX_VALUE - Short.MAX_VALUE / 2));
                while (c >= 0xD800 && c < 0xDE00) {
                    c = (int) Math.abs((Math.random() * Short.MAX_VALUE - Short.MAX_VALUE / 2));
                }
                return (T) (Character) (char) (c);
            }
            if (clazz == Character.class) {
                //return (T) (Character) (char) (Math.random() * Short.MAX_VALUE - Short.MAX_VALUE / 2);
                int c = (int) Math.abs((Math.random() * Short.MAX_VALUE - Short.MAX_VALUE / 2));
                while (c >= 0xD800 && c < 0xDE00) {
                    c = (int) Math.abs((Math.random() * Short.MAX_VALUE - Short.MAX_VALUE / 2));
                }
                return (T) (Character) (char) (c);
            }
            if (clazz == String.class) {
                StringBuilder sb = new StringBuilder();
                int sz = (len != 0) ? len / 2 : 10 + (int) (Math.random() * 20);
                int sz2 = (len != 0) ? len / 2 : 10 + (int) (Math.random() * 20);
                if (len != 0 && sz + sz2 < len) {
                    sz2 = len - sz2;
                }

                for (int i = 0; i < sz; i++) {
                    sb.append((char) ('A' + i));
                }

                for (int i = 0; i < sz2; i++) {
                    sb.append((char) ('Б' + i));
                }
                return (T) sb.toString();
            }

//    public TestPOJO1 self;
//    public TestPOJO1[] aself;
            if (clazz == Map.class) {
                Map m = new LinkedHashMap();
                for (int i = 0; i < 5; i++) {
                    m.put("_" + i, "__" + i * 4);
                }
                return (T) m;
            }
            if (clazz == List.class) {
                List l = new ArrayList();
                for (int i = 0; i < 5; i++) {
                    l.add("_" + i);
                }
                return (T) l;
            }
        }

        return null;
    }

    /**
     * Convert object to byte[] with java serialization
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public static byte[] javaSerialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        return baos.toByteArray();
    }

    /**
     * Restore object from byte[] of java serialized content.
     *
     * @param <T>
     * @param buf
     * @return
     * @throws IOException
     */
    public static <T> T javaDeserialize(byte[] buf) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
            Object obj = ois.readObject();
            return (T) obj;
        } catch (Throwable th) {
            if (th instanceof IOException) {
                throw (IOException) th;
            } else {
                throw new IOException(th);
            }
        }
    }

    private static Decycle daDecycle = new Decycle();

    public static class TestPOJO1 implements Cloneable, Serializable {

        public byte b;
        public Byte bb;
        public short sh;
        public Short shsh;
        public int i;
        public Integer ii;
        public long l;
        public Long ll;
        public float f;
        public Float ff;
        public double d;
        public Double dd;
        public BigInteger bi;
        public BigDecimal bd;
        public boolean bo;
        public Boolean bobo;
        public char ch;
        public Character chch;
        public String s;
        public POJOEnum1 en;

        public byte[] ab;
        public Byte[] abb;
        public short[] ash;
        public Short[] ashsh;
        public int[] ai;
        public Integer[] aii;
        public long[] al;
        public Long[] all;
        public float[] af;
        public Float[] aff;
        public double[] ad;
        public Double[] add;
        public BigInteger[] abi;
        public BigDecimal[] abd;
        public boolean[] abo;
        public Boolean[] abobo;
        public char[] ach;
        public Character[] achch;
        public String[] as;
        public POJOEnum1[] aen;

        public TestPOJO1 self;
        public TestPOJO1[] aself;
        public Map<String, byte[]> amb;
        public List<char[]> alch;

        @Override
        public String toString() {
            Decycle.DTYPE _dt = daDecycle.check(this);
            try {
                if (Decycle.DTYPE.DUP.equals(_dt)) {
                    return "#DUP";
                }
                return "{"
                        + "\n  b=" + b
                        + "\n  bb=" + bb
                        + "\n  sh=" + sh
                        + "\n  shsh=" + shsh
                        + "\n  i=" + i
                        + "\n  ii=" + ii
                        + "\n  l=" + l
                        + "\n  ll=" + ll
                        + "\n  f=" + f
                        + "\n  ff=" + ff
                        + "\n  d=" + d
                        + "\n  dd=" + dd
                        + "\n  bi=" + bi
                        + "\n  bd=" + bd
                        + "\n  bo=" + bo
                        + "\n  bobo=" + bobo
                        + "\n  ch=" + ch
                        + "\n  chch=" + chch
                        + "\n  s=" + s
                        + "\n  en=" + en
                        + "\n  ab=" + Dump.dumpArray(ab, false, false)
                        + "\n  abb=" + Dump.dumpArray(abb, false, false)
                        + "\n  ash=" + Dump.dumpArray(ash, false, false)
                        + "\n  ashsh=" + Dump.dumpArray(ashsh, false, false)
                        + "\n  ai=" + Dump.dumpArray(ai, false, false)
                        + "\n  aii=" + Dump.dumpArray(aii, false, false)
                        + "\n  al=" + Dump.dumpArray(al, false, false)
                        + "\n  all=" + Dump.dumpArray(all, false, false)
                        + "\n  af=" + Dump.dumpArray(af, false, false)
                        + "\n  aff=" + Dump.dumpArray(aff, false, false)
                        + "\n  ad=" + Dump.dumpArray(ad, false, false)
                        + "\n  add=" + Dump.dumpArray(add, false, false)
                        + "\n  abi=" + Dump.dumpArray(abi, false, false)
                        + "\n  abd=" + Dump.dumpArray(abd, false, false)
                        + "\n  abo=" + Dump.dumpArray(abo, false, false)
                        + "\n  abobo=" + Dump.dumpArray(abobo, false, false)
                        + "\n  ach=" + Dump.dumpArray(ach, false, false)
                        + "\n  achch=" + Dump.dumpArray(achch, false, false)
                        + "\n  as=" + Dump.dumpArray(as, false, false)
                        + "\n  aen=" + Dump.dumpArray(aen, false, false)
                        + "\n  self=" + ("" + self).replace("\n", "\n  ")
                        + "\n  aself=" + Dump.dumpArray(aself, false, false).replace("\n", "\n  ")
                        + "\n  amb=" + Dump.dumpArray(amb, false, false)
                        + "\n  alch=" + Dump.dumpArray(alch, false, false)
                        + "\n}";
            } finally {
                if (Decycle.DTYPE.NEW.equals(_dt)) {
                    daDecycle.reset();
                }
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 61 * hash + this.b;
            hash = 61 * hash + (this.bb != null ? this.bb.hashCode() : 0);
            hash = 61 * hash + this.sh;
            hash = 61 * hash + (this.shsh != null ? this.shsh.hashCode() : 0);
            hash = 61 * hash + this.i;
            hash = 61 * hash + (this.ii != null ? this.ii.hashCode() : 0);
            hash = 61 * hash + (int) (this.l ^ (this.l >>> 32));
            hash = 61 * hash + (this.ll != null ? this.ll.hashCode() : 0);
            hash = 61 * hash + Float.floatToIntBits(this.f);
            hash = 61 * hash + (this.ff != null ? this.ff.hashCode() : 0);
            hash = 61 * hash + (int) (Double.doubleToLongBits(this.d) ^ (Double.doubleToLongBits(this.d) >>> 32));
            hash = 61 * hash + (this.dd != null ? this.dd.hashCode() : 0);
            hash = 61 * hash + (this.bi != null ? this.bi.hashCode() : 0);
            hash = 61 * hash + (this.bd != null ? this.bd.hashCode() : 0);
            hash = 61 * hash + (this.bo ? 1 : 0);
            hash = 61 * hash + (this.bobo != null ? this.bobo.hashCode() : 0);
            hash = 61 * hash + this.ch;
            hash = 61 * hash + (this.chch != null ? this.chch.hashCode() : 0);
            hash = 61 * hash + (this.s != null ? this.s.hashCode() : 0);
            hash = 61 * hash + Arrays.hashCode(this.ab);
            hash = 61 * hash + Arrays.deepHashCode(this.abb);
            hash = 61 * hash + Arrays.hashCode(this.ash);
            hash = 61 * hash + Arrays.deepHashCode(this.ashsh);
            hash = 61 * hash + Arrays.hashCode(this.ai);
            hash = 61 * hash + Arrays.deepHashCode(this.aii);
            hash = 61 * hash + Arrays.hashCode(this.al);
            hash = 61 * hash + Arrays.deepHashCode(this.all);
            hash = 61 * hash + Arrays.hashCode(this.af);
            hash = 61 * hash + Arrays.deepHashCode(this.aff);
            hash = 61 * hash + Arrays.hashCode(this.ad);
            hash = 61 * hash + Arrays.deepHashCode(this.add);
            hash = 61 * hash + Arrays.deepHashCode(this.abi);
            hash = 61 * hash + Arrays.deepHashCode(this.abd);
            hash = 61 * hash + Arrays.hashCode(this.abo);
            hash = 61 * hash + Arrays.deepHashCode(this.abobo);
            hash = 61 * hash + Arrays.hashCode(this.ach);
            hash = 61 * hash + Arrays.deepHashCode(this.achch);
            hash = 61 * hash + Arrays.deepHashCode(this.as);
            hash = 61 * hash + System.identityHashCode(this.self);//this.self != null ? this.self.hashCode() : 0);
            hash = 61 * hash + System.identityHashCode(this.aself); // Arrays.deepHashCode(this.aself);
            hash = 61 * hash + (this.amb != null ? this.amb.hashCode() : 0);
            hash = 61 * hash + (this.alch != null ? this.alch.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestPOJO1 other = (TestPOJO1) obj;
            if (this.b != other.b) {
                return false;
            }
            if (this.sh != other.sh) {
                return false;
            }
            if (this.i != other.i) {
                return false;
            }
            if (this.l != other.l) {
                return false;
            }
            if (Float.floatToIntBits(this.f) != Float.floatToIntBits(other.f)) {
                return false;
            }
            if (Double.doubleToLongBits(this.d) != Double.doubleToLongBits(other.d)) {
                return false;
            }
            if (this.bo != other.bo) {
                return false;
            }
            if (this.ch != other.ch) {
                return false;
            }
            if ((this.s == null) ? (other.s != null) : !this.s.equals(other.s)) {
                return false;
            }
            if (this.bb != other.bb && (this.bb == null || !this.bb.equals(other.bb))) {
                return false;
            }
            if (this.shsh != other.shsh && (this.shsh == null || !this.shsh.equals(other.shsh))) {
                return false;
            }
            if (this.ii != other.ii && (this.ii == null || !this.ii.equals(other.ii))) {
                return false;
            }
            if (this.ll != other.ll && (this.ll == null || !this.ll.equals(other.ll))) {
                return false;
            }
            if (this.ff != other.ff && (this.ff == null || !this.ff.equals(other.ff))) {
                return false;
            }
            if (this.dd != other.dd && (this.dd == null || !this.dd.equals(other.dd))) {
                return false;
            }
            if (this.bi != other.bi && (this.bi == null || !this.bi.equals(other.bi))) {
                return false;
            }
            if (this.bd != other.bd && (this.bd == null || !this.bd.equals(other.bd))) {
                return false;
            }
            if (this.bobo != other.bobo && (this.bobo == null || !this.bobo.equals(other.bobo))) {
                return false;
            }
            if (this.chch != other.chch && (this.chch == null || !this.chch.equals(other.chch))) {
                return false;
            }
            if (!Arrays.equals(this.ab, other.ab)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abb, other.abb)) {
                return false;
            }
            if (!Arrays.equals(this.ash, other.ash)) {
                return false;
            }
            if (!Arrays.deepEquals(this.ashsh, other.ashsh)) {
                return false;
            }
            if (!Arrays.equals(this.ai, other.ai)) {
                return false;
            }
            if (!Arrays.deepEquals(this.aii, other.aii)) {
                return false;
            }
            if (!Arrays.equals(this.al, other.al)) {
                return false;
            }
            if (!Arrays.deepEquals(this.all, other.all)) {
                return false;
            }
            if (!Arrays.equals(this.af, other.af)) {
                return false;
            }
            if (!Arrays.deepEquals(this.aff, other.aff)) {
                return false;
            }
            if (!Arrays.equals(this.ad, other.ad)) {
                return false;
            }
            if (!Arrays.deepEquals(this.add, other.add)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abi, other.abi)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abd, other.abd)) {
                return false;
            }
            if (!Arrays.equals(this.abo, other.abo)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abobo, other.abobo)) {
                return false;
            }
            if (!Arrays.equals(this.ach, other.ach)) {
                return false;
            }
            if (!Arrays.deepEquals(this.achch, other.achch)) {
                return false;
            }
            if (!Arrays.deepEquals(this.as, other.as)) {
                return false;
            }
            if (this.self != other.self && (this.self == null || !this.self.equals(other.self))) {
                return false;
            }
            if (this.aself == null && other.aself != null || this.aself != null && other.aself == null) {
                return false;
            }
            if (this.aself != null) {
                if (this.aself.length != other.aself.length) {
                    return false;
                }
                for (int i = 0; i < this.aself.length; i++) {
                    if (this.aself[i] != other.aself[i]) {
                        return false;
                    }
                }
            }
//            if (!Arrays.deepEquals(this.aself, other.aself)) {
//                return false;
//            }
            if (this.amb != other.amb && (this.amb == null || !this.amb.equals(other.amb))) {
                return false;
            }
            if (this.alch != other.alch && (this.alch == null || !this.alch.equals(other.alch))) {
                return false;
            }
            return true;
        }

    }

    public static class TestPOJO2 implements Cloneable, Serializable {

        protected byte b;
        protected Byte bb;
        protected short sh;
        protected Short shsh;
        protected int i;
        protected Integer ii;
        protected long l;
        protected Long ll;
        protected float f;
        protected Float ff;
        protected double d;
        protected Double dd;
        protected BigInteger bi;
        protected BigDecimal bd;
        protected boolean bo;
        protected Boolean bobo;
        protected char ch;
        protected Character chch;
        protected String s;
        protected POJOEnum1 en;

        protected byte[] ab;
        protected Byte[] abb;
        protected short[] ash;
        protected Short[] ashsh;
        protected int[] ai;
        protected Integer[] aii;
        protected long[] al;
        protected Long[] all;
        protected float[] af;
        protected Float[] aff;
        protected double[] ad;
        protected Double[] add;
        protected BigInteger[] abi;
        protected BigDecimal[] abd;
        protected boolean[] abo;
        protected Boolean[] abobo;
        protected char[] ach;
        protected Character[] achch;
        protected String[] as;
        protected POJOEnum1[] aen;

        protected TestPOJO2 self;
        protected TestPOJO2[] aself;
        protected Map<String, byte[]> amb;
        protected List<char[]> alch;

        @Override
        public String toString() {
            Decycle.DTYPE _dt = daDecycle.check(this);
            try {
                if (Decycle.DTYPE.DUP.equals(_dt)) {
                    return "#DUP";
                }
                return "{"
                        + "\n  b=" + b
                        + "\n  bb=" + bb
                        + "\n  sh=" + sh
                        + "\n  shsh=" + shsh
                        + "\n  i=" + i
                        + "\n  ii=" + ii
                        + "\n  l=" + l
                        + "\n  ll=" + ll
                        + "\n  f=" + f
                        + "\n  ff=" + ff
                        + "\n  d=" + d
                        + "\n  dd=" + dd
                        + "\n  bi=" + bi
                        + "\n  bd=" + bd
                        + "\n  bo=" + bo
                        + "\n  bobo=" + bobo
                        + "\n  ch=" + ch
                        + "\n  chch=" + chch
                        + "\n  s=" + s
                        + "\n  en=" + en
                        + "\n  ab=" + Dump.dumpArray(ab, false, false)
                        + "\n  abb=" + Dump.dumpArray(abb, false, false)
                        + "\n  ash=" + Dump.dumpArray(ash, false, false)
                        + "\n  ashsh=" + Dump.dumpArray(ashsh, false, false)
                        + "\n  ai=" + Dump.dumpArray(ai, false, false)
                        + "\n  aii=" + Dump.dumpArray(aii, false, false)
                        + "\n  al=" + Dump.dumpArray(al, false, false)
                        + "\n  all=" + Dump.dumpArray(all, false, false)
                        + "\n  af=" + Dump.dumpArray(af, false, false)
                        + "\n  aff=" + Dump.dumpArray(aff, false, false)
                        + "\n  ad=" + Dump.dumpArray(ad, false, false)
                        + "\n  add=" + Dump.dumpArray(add, false, false)
                        + "\n  abi=" + Dump.dumpArray(abi, false, false)
                        + "\n  abd=" + Dump.dumpArray(abd, false, false)
                        + "\n  abo=" + Dump.dumpArray(abo, false, false)
                        + "\n  abobo=" + Dump.dumpArray(abobo, false, false)
                        + "\n  ach=" + Dump.dumpArray(ach, false, false)
                        + "\n  achch=" + Dump.dumpArray(achch, false, false)
                        + "\n  as=" + Dump.dumpArray(as, false, false)
                        + "\n  aen=" + Dump.dumpArray(aen, false, false)
                        + "\n  self=" + ("" + self).replace("\n", "\n  ")
                        + "\n  aself=" + Dump.dumpArray(aself, false, false).replace("\n", "\n  ")
                        + "\n  amb=" + Dump.dumpArray(amb, false, false)
                        + "\n  alch=" + Dump.dumpArray(alch, false, false)
                        + "\n}";
            } finally {
                if (Decycle.DTYPE.NEW.equals(_dt)) {
                    daDecycle.reset();
                }
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + this.getB();
            hash = 47 * hash + (this.getBb() != null ? this.getBb().hashCode() : 0);
            hash = 47 * hash + this.getSh();
            hash = 47 * hash + (this.getShsh() != null ? this.getShsh().hashCode() : 0);
            hash = 47 * hash + this.getI();
            hash = 47 * hash + (this.getIi() != null ? this.getIi().hashCode() : 0);
            hash = 47 * hash + (int) (this.getL() ^ (this.getL() >>> 32));
            hash = 47 * hash + (this.getLl() != null ? this.getLl().hashCode() : 0);
            hash = 47 * hash + Float.floatToIntBits(this.getF());
            hash = 47 * hash + (this.getFf() != null ? this.getFf().hashCode() : 0);
            hash = 47 * hash + (int) (Double.doubleToLongBits(this.getD()) ^ (Double.doubleToLongBits(this.getD()) >>> 32));
            hash = 47 * hash + (this.getDd() != null ? this.getDd().hashCode() : 0);
            hash = 47 * hash + (this.getBi() != null ? this.getBi().hashCode() : 0);
            hash = 47 * hash + (this.getBd() != null ? this.getBd().hashCode() : 0);
            hash = 47 * hash + (this.isBo() ? 1 : 0);
            hash = 47 * hash + (this.getBobo() != null ? this.getBobo().hashCode() : 0);
            hash = 47 * hash + this.getCh();
            hash = 47 * hash + (this.getChch() != null ? this.getChch().hashCode() : 0);
            hash = 47 * hash + (this.getS() != null ? this.getS().hashCode() : 0);
            hash = 47 * hash + Arrays.hashCode(this.getAb());
            hash = 47 * hash + Arrays.deepHashCode(this.getAbb());
            hash = 47 * hash + Arrays.hashCode(this.getAsh());
            hash = 47 * hash + Arrays.deepHashCode(this.getAshsh());
            hash = 47 * hash + Arrays.hashCode(this.getAi());
            hash = 47 * hash + Arrays.deepHashCode(this.getAii());
            hash = 47 * hash + Arrays.hashCode(this.getAl());
            hash = 47 * hash + Arrays.deepHashCode(this.getAll());
            hash = 47 * hash + Arrays.hashCode(this.getAf());
            hash = 47 * hash + Arrays.deepHashCode(this.getAff());
            hash = 47 * hash + Arrays.hashCode(this.getAd());
            hash = 47 * hash + Arrays.deepHashCode(this.getAdd());
            hash = 47 * hash + Arrays.deepHashCode(this.getAbi());
            hash = 47 * hash + Arrays.deepHashCode(this.getAbd());
            hash = 47 * hash + Arrays.hashCode(this.getAbo());
            hash = 47 * hash + Arrays.deepHashCode(this.getAbobo());
            hash = 47 * hash + Arrays.hashCode(this.getAch());
            hash = 47 * hash + Arrays.deepHashCode(this.getAchch());
            hash = 47 * hash + Arrays.deepHashCode(this.getAs());
            hash = 61 * hash + System.identityHashCode(this.getSelf());//this.self != null ? this.self.hashCode() : 0);
            hash = 61 * hash + System.identityHashCode(this.getAself()); // Arrays.deepHashCode(this.aself);
            hash = 47 * hash + (this.getAmb() != null ? this.getAmb().hashCode() : 0);
            hash = 47 * hash + (this.getAlch() != null ? this.getAlch().hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestPOJO2 other = (TestPOJO2) obj;
            if (this.getB() != other.getB()) {
                return false;
            }
            if (this.getSh() != other.getSh()) {
                return false;
            }
            if (this.getI() != other.getI()) {
                return false;
            }
            if (this.getL() != other.getL()) {
                return false;
            }
            if (Float.floatToIntBits(this.getF()) != Float.floatToIntBits(other.getF())) {
                return false;
            }
            if (Double.doubleToLongBits(this.getD()) != Double.doubleToLongBits(other.getD())) {
                return false;
            }
            if (this.isBo() != other.isBo()) {
                return false;
            }
            if (this.getCh() != other.getCh()) {
                return false;
            }
            if ((this.getS() == null) ? (other.getS() != null) : !this.s.equals(other.s)) {
                return false;
            }
            if (this.getBb() != other.getBb() && (this.getBb() == null || !this.bb.equals(other.bb))) {
                return false;
            }
            if (this.getShsh() != other.getShsh() && (this.getShsh() == null || !this.shsh.equals(other.shsh))) {
                return false;
            }
            if (this.getIi() != other.getIi() && (this.getIi() == null || !this.ii.equals(other.ii))) {
                return false;
            }
            if (this.getLl() != other.getLl() && (this.getLl() == null || !this.ll.equals(other.ll))) {
                return false;
            }
            if (this.getFf() != other.getFf() && (this.getFf() == null || !this.ff.equals(other.ff))) {
                return false;
            }
            if (this.getDd() != other.getDd() && (this.getDd() == null || !this.dd.equals(other.dd))) {
                return false;
            }
            if (this.getBi() != other.getBi() && (this.getBi() == null || !this.bi.equals(other.bi))) {
                return false;
            }
            if (this.getBd() != other.getBd() && (this.getBd() == null || !this.bd.equals(other.bd))) {
                return false;
            }
            if (this.getBobo() != other.getBobo() && (this.getBobo() == null || !this.bobo.equals(other.bobo))) {
                return false;
            }
            if (this.getChch() != other.getChch() && (this.getChch() == null || !this.chch.equals(other.chch))) {
                return false;
            }
            if (!Arrays.equals(this.ab, other.ab)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abb, other.abb)) {
                return false;
            }
            if (!Arrays.equals(this.ash, other.ash)) {
                return false;
            }
            if (!Arrays.deepEquals(this.ashsh, other.ashsh)) {
                return false;
            }
            if (!Arrays.equals(this.ai, other.ai)) {
                return false;
            }
            if (!Arrays.deepEquals(this.aii, other.aii)) {
                return false;
            }
            if (!Arrays.equals(this.al, other.al)) {
                return false;
            }
            if (!Arrays.deepEquals(this.all, other.all)) {
                return false;
            }
            if (!Arrays.equals(this.af, other.af)) {
                return false;
            }
            if (!Arrays.deepEquals(this.aff, other.aff)) {
                return false;
            }
            if (!Arrays.equals(this.ad, other.ad)) {
                return false;
            }
            if (!Arrays.deepEquals(this.add, other.add)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abi, other.abi)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abd, other.abd)) {
                return false;
            }
            if (!Arrays.equals(this.abo, other.abo)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abobo, other.abobo)) {
                return false;
            }
            if (!Arrays.equals(this.ach, other.ach)) {
                return false;
            }
            if (!Arrays.deepEquals(this.achch, other.achch)) {
                return false;
            }
            if (!Arrays.deepEquals(this.as, other.as)) {
                return false;
            }
            if (this.getSelf() != other.getSelf() && (this.getSelf() == null || !this.self.equals(other.self))) {
                return false;
            }
            if (this.getAself() == null && other.getAself() != null || this.getAself() != null && other.getAself() == null) {
                return false;
            }
            if (this.getAself() != null) {
                if (this.getAself().length != other.getAself().length) {
                    return false;
                }
                for (int i = 0; i < this.getAself().length; i++) {
                    if (this.getAself()[i] != other.getAself()[i]) {
                        return false;
                    }
                }
            }
//            if (!Arrays.deepEquals(this.aself, other.aself)) {
//                return false;
//            }
            if (this.getAmb() != other.getAmb() && (this.getAmb() == null || !this.amb.equals(other.amb))) {
                return false;
            }
            if (this.getAlch() != other.getAlch() && (this.getAlch() == null || !this.alch.equals(other.alch))) {
                return false;
            }
            return true;
        }

        /**
         * @return the b
         */
        public byte getB() {
            return b;
        }

        /**
         * @param b the b to set
         */
        public void setB(byte b) {
            this.b = b;
        }

        /**
         * @return the bb
         */
        public Byte getBb() {
            return bb;
        }

        /**
         * @param bb the bb to set
         */
        public void setBb(Byte bb) {
            this.bb = bb;
        }

        /**
         * @return the sh
         */
        public short getSh() {
            return sh;
        }

        /**
         * @param sh the sh to set
         */
        public void setSh(short sh) {
            this.sh = sh;
        }

        /**
         * @return the shsh
         */
        public Short getShsh() {
            return shsh;
        }

        /**
         * @param shsh the shsh to set
         */
        public void setShsh(Short shsh) {
            this.shsh = shsh;
        }

        /**
         * @return the i
         */
        public int getI() {
            return i;
        }

        /**
         * @param i the i to set
         */
        public void setI(int i) {
            this.i = i;
        }

        /**
         * @return the ii
         */
        public Integer getIi() {
            return ii;
        }

        /**
         * @param ii the ii to set
         */
        public void setIi(Integer ii) {
            this.ii = ii;
        }

        /**
         * @return the l
         */
        public long getL() {
            return l;
        }

        /**
         * @param l the l to set
         */
        public void setL(long l) {
            this.l = l;
        }

        /**
         * @return the ll
         */
        public Long getLl() {
            return ll;
        }

        /**
         * @param ll the ll to set
         */
        public void setLl(Long ll) {
            this.ll = ll;
        }

        /**
         * @return the f
         */
        public float getF() {
            return f;
        }

        /**
         * @param f the f to set
         */
        public void setF(float f) {
            this.f = f;
        }

        /**
         * @return the ff
         */
        public Float getFf() {
            return ff;
        }

        /**
         * @param ff the ff to set
         */
        public void setFf(Float ff) {
            this.ff = ff;
        }

        /**
         * @return the d
         */
        public double getD() {
            return d;
        }

        /**
         * @param d the d to set
         */
        public void setD(double d) {
            this.d = d;
        }

        /**
         * @return the dd
         */
        public Double getDd() {
            return dd;
        }

        /**
         * @param dd the dd to set
         */
        public void setDd(Double dd) {
            this.dd = dd;
        }

        /**
         * @return the bi
         */
        public BigInteger getBi() {
            return bi;
        }

        /**
         * @param bi the bi to set
         */
        public void setBi(BigInteger bi) {
            this.bi = bi;
        }

        /**
         * @return the bd
         */
        public BigDecimal getBd() {
            return bd;
        }

        /**
         * @param bd the bd to set
         */
        public void setBd(BigDecimal bd) {
            this.bd = bd;
        }

        /**
         * @return the bo
         */
        public boolean isBo() {
            return bo;
        }

        /**
         * @param bo the bo to set
         */
        public void setBo(boolean bo) {
            this.bo = bo;
        }

        /**
         * @return the bobo
         */
        public Boolean getBobo() {
            return bobo;
        }

        /**
         * @param bobo the bobo to set
         */
        public void setBobo(Boolean bobo) {
            this.bobo = bobo;
        }

        /**
         * @return the ch
         */
        public char getCh() {
            return ch;
        }

        /**
         * @param ch the ch to set
         */
        public void setCh(char ch) {
            this.ch = ch;
        }

        /**
         * @return the chch
         */
        public Character getChch() {
            return chch;
        }

        /**
         * @param chch the chch to set
         */
        public void setChch(Character chch) {
            this.chch = chch;
        }

        /**
         * @return the s
         */
        public String getS() {
            return s;
        }

        /**
         * @param s the s to set
         */
        public void setS(String s) {
            this.s = s;
        }

        /**
         * @return the en
         */
        public POJOEnum1 getEn() {
            return en;
        }

        /**
         * @param en the en to set
         */
        public void setEn(POJOEnum1 en) {
            this.en = en;
        }

        /**
         * @return the ab
         */
        public byte[] getAb() {
            return ab;
        }

        /**
         * @param ab the ab to set
         */
        public void setAb(byte[] ab) {
            this.ab = ab;
        }

        /**
         * @return the abb
         */
        public Byte[] getAbb() {
            return abb;
        }

        /**
         * @param abb the abb to set
         */
        public void setAbb(Byte[] abb) {
            this.abb = abb;
        }

        /**
         * @return the ash
         */
        public short[] getAsh() {
            return ash;
        }

        /**
         * @param ash the ash to set
         */
        public void setAsh(short[] ash) {
            this.ash = ash;
        }

        /**
         * @return the ashsh
         */
        public Short[] getAshsh() {
            return ashsh;
        }

        /**
         * @param ashsh the ashsh to set
         */
        public void setAshsh(Short[] ashsh) {
            this.ashsh = ashsh;
        }

        /**
         * @return the ai
         */
        public int[] getAi() {
            return ai;
        }

        /**
         * @param ai the ai to set
         */
        public void setAi(int[] ai) {
            this.ai = ai;
        }

        /**
         * @return the aii
         */
        public Integer[] getAii() {
            return aii;
        }

        /**
         * @param aii the aii to set
         */
        public void setAii(Integer[] aii) {
            this.aii = aii;
        }

        /**
         * @return the al
         */
        public long[] getAl() {
            return al;
        }

        /**
         * @param al the al to set
         */
        public void setAl(long[] al) {
            this.al = al;
        }

        /**
         * @return the all
         */
        public Long[] getAll() {
            return all;
        }

        /**
         * @param all the all to set
         */
        public void setAll(Long[] all) {
            this.all = all;
        }

        /**
         * @return the af
         */
        public float[] getAf() {
            return af;
        }

        /**
         * @param af the af to set
         */
        public void setAf(float[] af) {
            this.af = af;
        }

        /**
         * @return the aff
         */
        public Float[] getAff() {
            return aff;
        }

        /**
         * @param aff the aff to set
         */
        public void setAff(Float[] aff) {
            this.aff = aff;
        }

        /**
         * @return the ad
         */
        public double[] getAd() {
            return ad;
        }

        /**
         * @param ad the ad to set
         */
        public void setAd(double[] ad) {
            this.ad = ad;
        }

        /**
         * @return the add
         */
        public Double[] getAdd() {
            return add;
        }

        /**
         * @param add the add to set
         */
        public void setAdd(Double[] add) {
            this.add = add;
        }

        /**
         * @return the abi
         */
        public BigInteger[] getAbi() {
            return abi;
        }

        /**
         * @param abi the abi to set
         */
        public void setAbi(BigInteger[] abi) {
            this.abi = abi;
        }

        /**
         * @return the abd
         */
        public BigDecimal[] getAbd() {
            return abd;
        }

        /**
         * @param abd the abd to set
         */
        public void setAbd(BigDecimal[] abd) {
            this.abd = abd;
        }

        /**
         * @return the abo
         */
        public boolean[] getAbo() {
            return abo;
        }

        /**
         * @param abo the abo to set
         */
        public void setAbo(boolean[] abo) {
            this.abo = abo;
        }

        /**
         * @return the abobo
         */
        public Boolean[] getAbobo() {
            return abobo;
        }

        /**
         * @param abobo the abobo to set
         */
        public void setAbobo(Boolean[] abobo) {
            this.abobo = abobo;
        }

        /**
         * @return the ach
         */
        public char[] getAch() {
            return ach;
        }

        /**
         * @param ach the ach to set
         */
        public void setAch(char[] ach) {
            this.ach = ach;
        }

        /**
         * @return the achch
         */
        public Character[] getAchch() {
            return achch;
        }

        /**
         * @param achch the achch to set
         */
        public void setAchch(Character[] achch) {
            this.achch = achch;
        }

        /**
         * @return the as
         */
        public String[] getAs() {
            return as;
        }

        /**
         * @param as the as to set
         */
        public void setAs(String[] as) {
            this.as = as;
        }

        /**
         * @return the aen
         */
        public POJOEnum1[] getAen() {
            return aen;
        }

        /**
         * @param aen the aen to set
         */
        public void setAen(POJOEnum1[] aen) {
            this.aen = aen;
        }

        /**
         * @return the self
         */
        public TestPOJO2 getSelf() {
            return self;
        }

        /**
         * @param self the self to set
         */
        public void setSelf(TestPOJO2 self) {
            this.self = self;
        }

        /**
         * @return the aself
         */
        public TestPOJO2[] getAself() {
            return aself;
        }

        /**
         * @param aself the aself to set
         */
        public void setAself(TestPOJO2[] aself) {
            this.aself = aself;
        }

        /**
         * @return the amb
         */
        public Map<String, byte[]> getAmb() {
            return amb;
        }

        /**
         * @param amb the amb to set
         */
        public void setAmb(Map<String, byte[]> amb) {
            this.amb = amb;
        }

        /**
         * @return the alch
         */
        public List<char[]> getAlch() {
            return alch;
        }

        /**
         * @param alch the alch to set
         */
        public void setAlch(List<char[]> alch) {
            this.alch = alch;
        }

    }

    public static class TestPOJO3 implements Cloneable, Serializable {

        public byte b;
        public Byte bb;
        protected short sh;
        protected Short shsh;
        protected int i;
        protected Integer ii;
        protected long l;
        protected Long ll;
        protected float f;
        protected Float ff;
        protected double d;
        protected Double dd;
        protected BigInteger bi;
        protected BigDecimal bd;
        protected boolean bo;
        protected Boolean bobo;
        protected char ch;
        protected Character chch;
        protected String s;
        protected POJOEnum1 en;

        protected byte[] ab;
        protected Byte[] abb;
        protected short[] ash;
        protected Short[] ashsh;
        public int[] ai;
        public Integer[] aii;
        protected long[] al;
        protected Long[] all;
        protected float[] af;
        protected Float[] aff;
        protected double[] ad;
        protected Double[] add;
        protected BigInteger[] abi;
        protected BigDecimal[] abd;
        protected boolean[] abo;
        protected Boolean[] abobo;
        protected char[] ach;
        protected Character[] achch;
        protected String[] as;
        protected POJOEnum1[] aen;

        protected TestPOJO3 self;
        protected TestPOJO3[] aself;
        protected Map<String, byte[]> amb;
        protected List<char[]> alch;

        @Override
        public String toString() {
            Decycle.DTYPE _dt = daDecycle.check(this);
            try {
                if (Decycle.DTYPE.DUP.equals(_dt)) {
                    return "#DUP";
                }
                return "{"
                        + "\n  b=" + b
                        + "\n  bb=" + bb
                        + "\n  sh=" + sh
                        + "\n  shsh=" + shsh
                        + "\n  i=" + i
                        + "\n  ii=" + ii
                        + "\n  l=" + l
                        + "\n  ll=" + ll
                        + "\n  f=" + f
                        + "\n  ff=" + ff
                        + "\n  d=" + d
                        + "\n  dd=" + dd
                        + "\n  bi=" + bi
                        + "\n  bd=" + bd
                        + "\n  bo=" + bo
                        + "\n  bobo=" + bobo
                        + "\n  ch=" + ch
                        + "\n  chch=" + chch
                        + "\n  s=" + s
                        + "\n  en=" + en
                        + "\n  ab=" + Dump.dumpArray(ab, false, false)
                        + "\n  abb=" + Dump.dumpArray(abb, false, false)
                        + "\n  ash=" + Dump.dumpArray(ash, false, false)
                        + "\n  ashsh=" + Dump.dumpArray(ashsh, false, false)
                        + "\n  ai=" + Dump.dumpArray(ai, false, false)
                        + "\n  aii=" + Dump.dumpArray(aii, false, false)
                        + "\n  al=" + Dump.dumpArray(al, false, false)
                        + "\n  all=" + Dump.dumpArray(all, false, false)
                        + "\n  af=" + Dump.dumpArray(af, false, false)
                        + "\n  aff=" + Dump.dumpArray(aff, false, false)
                        + "\n  ad=" + Dump.dumpArray(ad, false, false)
                        + "\n  add=" + Dump.dumpArray(add, false, false)
                        + "\n  abi=" + Dump.dumpArray(abi, false, false)
                        + "\n  abd=" + Dump.dumpArray(abd, false, false)
                        + "\n  abo=" + Dump.dumpArray(abo, false, false)
                        + "\n  abobo=" + Dump.dumpArray(abobo, false, false)
                        + "\n  ach=" + Dump.dumpArray(ach, false, false)
                        + "\n  achch=" + Dump.dumpArray(achch, false, false)
                        + "\n  as=" + Dump.dumpArray(as, false, false)
                        + "\n  aen=" + Dump.dumpArray(aen, false, false)
                        + "\n  self=" + ("" + self).replace("\n", "\n  ")
                        + "\n  aself=" + Dump.dumpArray(aself, false, false).replace("\n", "\n  ")
                        + "\n  amb=" + Dump.dumpArray(amb, false, false)
                        + "\n  alch=" + Dump.dumpArray(alch, false, false)
                        + "\n}";
            } finally {
                if (Decycle.DTYPE.NEW.equals(_dt)) {
                    daDecycle.reset();
                }
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + this.b;
            hash = 47 * hash + (this.bb != null ? this.bb.hashCode() : 0);
            hash = 47 * hash + this.sh;
            hash = 47 * hash + (this.shsh != null ? this.shsh.hashCode() : 0);
            hash = 47 * hash + this.i;
            hash = 47 * hash + (this.ii != null ? this.ii.hashCode() : 0);
            hash = 47 * hash + (int) (this.l ^ (this.l >>> 32));
            hash = 47 * hash + (this.ll != null ? this.ll.hashCode() : 0);
            hash = 47 * hash + Float.floatToIntBits(this.f);
            hash = 47 * hash + (this.ff != null ? this.ff.hashCode() : 0);
            hash = 47 * hash + (int) (Double.doubleToLongBits(this.d) ^ (Double.doubleToLongBits(this.d) >>> 32));
            hash = 47 * hash + (this.dd != null ? this.dd.hashCode() : 0);
            hash = 47 * hash + (this.bi != null ? this.bi.hashCode() : 0);
            hash = 47 * hash + (this.bd != null ? this.bd.hashCode() : 0);
            hash = 47 * hash + (this.bo ? 1 : 0);
            hash = 47 * hash + (this.bobo != null ? this.bobo.hashCode() : 0);
            hash = 47 * hash + this.ch;
            hash = 47 * hash + (this.chch != null ? this.chch.hashCode() : 0);
            hash = 47 * hash + (this.s != null ? this.s.hashCode() : 0);
            hash = 47 * hash + Arrays.hashCode(this.ab);
            hash = 47 * hash + Arrays.deepHashCode(this.abb);
            hash = 47 * hash + Arrays.hashCode(this.ash);
            hash = 47 * hash + Arrays.deepHashCode(this.ashsh);
            hash = 47 * hash + Arrays.hashCode(this.ai);
            hash = 47 * hash + Arrays.deepHashCode(this.aii);
            hash = 47 * hash + Arrays.hashCode(this.al);
            hash = 47 * hash + Arrays.deepHashCode(this.all);
            hash = 47 * hash + Arrays.hashCode(this.af);
            hash = 47 * hash + Arrays.deepHashCode(this.aff);
            hash = 47 * hash + Arrays.hashCode(this.ad);
            hash = 47 * hash + Arrays.deepHashCode(this.add);
            hash = 47 * hash + Arrays.deepHashCode(this.abi);
            hash = 47 * hash + Arrays.deepHashCode(this.abd);
            hash = 47 * hash + Arrays.hashCode(this.abo);
            hash = 47 * hash + Arrays.deepHashCode(this.abobo);
            hash = 47 * hash + Arrays.hashCode(this.ach);
            hash = 47 * hash + Arrays.deepHashCode(this.achch);
            hash = 47 * hash + Arrays.deepHashCode(this.as);
            hash = 61 * hash + System.identityHashCode(this.self);//this.self != null ? this.self.hashCode() : 0);
            hash = 61 * hash + System.identityHashCode(this.aself); // Arrays.deepHashCode(this.aself);
            hash = 47 * hash + (this.amb != null ? this.amb.hashCode() : 0);
            hash = 47 * hash + (this.alch != null ? this.alch.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestPOJO3 other = (TestPOJO3) obj;
            if (this.b != other.b) {
                return false;
            }
            if (this.sh != other.sh) {
                return false;
            }
            if (this.i != other.i) {
                return false;
            }
            if (this.l != other.l) {
                return false;
            }
            if (Float.floatToIntBits(this.f) != Float.floatToIntBits(other.f)) {
                return false;
            }
            if (Double.doubleToLongBits(this.d) != Double.doubleToLongBits(other.d)) {
                return false;
            }
            if (this.bo != other.bo) {
                return false;
            }
            if (this.ch != other.ch) {
                return false;
            }
            if ((this.s == null) ? (other.s != null) : !this.s.equals(other.s)) {
                return false;
            }
            if (this.bb != other.bb && (this.bb == null || !this.bb.equals(other.bb))) {
                return false;
            }
            if (this.shsh != other.shsh && (this.shsh == null || !this.shsh.equals(other.shsh))) {
                return false;
            }
            if (this.ii != other.ii && (this.ii == null || !this.ii.equals(other.ii))) {
                return false;
            }
            if (this.ll != other.ll && (this.ll == null || !this.ll.equals(other.ll))) {
                return false;
            }
            if (this.ff != other.ff && (this.ff == null || !this.ff.equals(other.ff))) {
                return false;
            }
            if (this.dd != other.dd && (this.dd == null || !this.dd.equals(other.dd))) {
                return false;
            }
            if (this.bi != other.bi && (this.bi == null || !this.bi.equals(other.bi))) {
                return false;
            }
            if (this.bd != other.bd && (this.bd == null || !this.bd.equals(other.bd))) {
                return false;
            }
            if (this.bobo != other.bobo && (this.bobo == null || !this.bobo.equals(other.bobo))) {
                return false;
            }
            if (this.chch != other.chch && (this.chch == null || !this.chch.equals(other.chch))) {
                return false;
            }
            if (!Arrays.equals(this.ab, other.ab)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abb, other.abb)) {
                return false;
            }
            if (!Arrays.equals(this.ash, other.ash)) {
                return false;
            }
            if (!Arrays.deepEquals(this.ashsh, other.ashsh)) {
                return false;
            }
            if (!Arrays.equals(this.ai, other.ai)) {
                return false;
            }
            if (!Arrays.deepEquals(this.aii, other.aii)) {
                return false;
            }
            if (!Arrays.equals(this.al, other.al)) {
                return false;
            }
            if (!Arrays.deepEquals(this.all, other.all)) {
                return false;
            }
            if (!Arrays.equals(this.af, other.af)) {
                return false;
            }
            if (!Arrays.deepEquals(this.aff, other.aff)) {
                return false;
            }
            if (!Arrays.equals(this.ad, other.ad)) {
                return false;
            }
            if (!Arrays.deepEquals(this.add, other.add)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abi, other.abi)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abd, other.abd)) {
                return false;
            }
            if (!Arrays.equals(this.abo, other.abo)) {
                return false;
            }
            if (!Arrays.deepEquals(this.abobo, other.abobo)) {
                return false;
            }
            if (!Arrays.equals(this.ach, other.ach)) {
                return false;
            }
            if (!Arrays.deepEquals(this.achch, other.achch)) {
                return false;
            }
            if (!Arrays.deepEquals(this.as, other.as)) {
                return false;
            }
            if (this.self != other.self && (this.self == null || !this.self.equals(other.self))) {
                return false;
            }
            if (this.aself == null && other.aself != null || this.aself != null && other.aself == null) {
                return false;
            }
            if (this.aself != null) {
                if (this.aself.length != other.aself.length) {
                    return false;
                }
                for (int i = 0; i < this.aself.length; i++) {
                    if (this.aself[i] != other.aself[i]) {
                        return false;
                    }
                }
            }
//            if (!Arrays.deepEquals(this.aself, other.aself)) {
//                return false;
//            }
            if (this.amb != other.amb && (this.amb == null || !this.amb.equals(other.amb))) {
                return false;
            }
            if (this.alch != other.alch && (this.alch == null || !this.alch.equals(other.alch))) {
                return false;
            }
            return true;
        }

        /**
         * @return the sh
         */
        public short getSh() {
            return sh;
        }

        /**
         * @param sh the sh to set
         */
        public void setSh(short sh) {
            this.sh = sh;
        }

        /**
         * @return the shsh
         */
        public Short getShsh() {
            return shsh;
        }

        /**
         * @param shsh the shsh to set
         */
        public void setShsh(Short shsh) {
            this.shsh = shsh;
        }

        /**
         * @return the i
         */
        public int getI() {
            return i;
        }

        /**
         * @param i the i to set
         */
        public void setI(int i) {
            this.i = i;
        }

        /**
         * @return the ii
         */
        public Integer getIi() {
            return ii;
        }

        /**
         * @param ii the ii to set
         */
        public void setIi(Integer ii) {
            this.ii = ii;
        }

        /**
         * @return the l
         */
        public long getL() {
            return l;
        }

        /**
         * @param l the l to set
         */
        public void setL(long l) {
            this.l = l;
        }

        /**
         * @return the ll
         */
        public Long getLl() {
            return ll;
        }

        /**
         * @param ll the ll to set
         */
        public void setLl(Long ll) {
            this.ll = ll;
        }

        /**
         * @return the f
         */
        public float getF() {
            return f;
        }

        /**
         * @param f the f to set
         */
        public void setF(float f) {
            this.f = f;
        }

        /**
         * @return the ff
         */
        public Float getFf() {
            return ff;
        }

        /**
         * @param ff the ff to set
         */
        public void setFf(Float ff) {
            this.ff = ff;
        }

        /**
         * @return the d
         */
        public double getD() {
            return d;
        }

        /**
         * @param d the d to set
         */
        public void setD(double d) {
            this.d = d;
        }

        /**
         * @return the dd
         */
        public Double getDd() {
            return dd;
        }

        /**
         * @param dd the dd to set
         */
        public void setDd(Double dd) {
            this.dd = dd;
        }

        /**
         * @return the bi
         */
        public BigInteger getBi() {
            return bi;
        }

        /**
         * @param bi the bi to set
         */
        public void setBi(BigInteger bi) {
            this.bi = bi;
        }

        /**
         * @return the bd
         */
        public BigDecimal getBd() {
            return bd;
        }

        /**
         * @param bd the bd to set
         */
        public void setBd(BigDecimal bd) {
            this.bd = bd;
        }

        /**
         * @return the bo
         */
        public boolean isBo() {
            return bo;
        }

        /**
         * @param bo the bo to set
         */
        public void setBo(boolean bo) {
            this.bo = bo;
        }

        /**
         * @return the bobo
         */
        public Boolean getBobo() {
            return bobo;
        }

        /**
         * @param bobo the bobo to set
         */
        public void setBobo(Boolean bobo) {
            this.bobo = bobo;
        }

        /**
         * @return the ch
         */
        public char getCh() {
            return ch;
        }

        /**
         * @param ch the ch to set
         */
        public void setCh(char ch) {
            this.ch = ch;
        }

        /**
         * @return the chch
         */
        public Character getChch() {
            return chch;
        }

        /**
         * @param chch the chch to set
         */
        public void setChch(Character chch) {
            this.chch = chch;
        }

        /**
         * @return the s
         */
        public String getS() {
            return s;
        }

        /**
         * @param s the s to set
         */
        public void setS(String s) {
            this.s = s;
        }

        /**
         * @return the ab
         */
        public byte[] getAb() {
            return ab;
        }

        /**
         * @param ab the ab to set
         */
        public void setAb(byte[] ab) {
            this.ab = ab;
        }

        /**
         * @return the abb
         */
        public Byte[] getAbb() {
            return abb;
        }

        /**
         * @param abb the abb to set
         */
        public void setAbb(Byte[] abb) {
            this.abb = abb;
        }

        /**
         * @return the ash
         */
        public short[] getAsh() {
            return ash;
        }

        /**
         * @param ash the ash to set
         */
        public void setAsh(short[] ash) {
            this.ash = ash;
        }

        /**
         * @return the ashsh
         */
        public Short[] getAshsh() {
            return ashsh;
        }

        /**
         * @param ashsh the ashsh to set
         */
        public void setAshsh(Short[] ashsh) {
            this.ashsh = ashsh;
        }

        /**
         * @return the ai
         */
        public int[] getAi() {
            return ai;
        }

        /**
         * @param ai the ai to set
         */
        public void setAi(int[] ai) {
            this.ai = ai;
        }

        /**
         * @return the aii
         */
        public Integer[] getAii() {
            return aii;
        }

        /**
         * @param aii the aii to set
         */
        public void setAii(Integer[] aii) {
            this.aii = aii;
        }

        /**
         * @return the al
         */
        public long[] getAl() {
            return al;
        }

        /**
         * @param al the al to set
         */
        public void setAl(long[] al) {
            this.al = al;
        }

        /**
         * @return the all
         */
        public Long[] getAll() {
            return all;
        }

        /**
         * @param all the all to set
         */
        public void setAll(Long[] all) {
            this.all = all;
        }

        /**
         * @return the af
         */
        public float[] getAf() {
            return af;
        }

        /**
         * @param af the af to set
         */
        public void setAf(float[] af) {
            this.af = af;
        }

        /**
         * @return the aff
         */
        public Float[] getAff() {
            return aff;
        }

        /**
         * @param aff the aff to set
         */
        public void setAff(Float[] aff) {
            this.aff = aff;
        }

        /**
         * @return the ad
         */
        public double[] getAd() {
            return ad;
        }

        /**
         * @param ad the ad to set
         */
        public void setAd(double[] ad) {
            this.ad = ad;
        }

        /**
         * @return the add
         */
        public Double[] getAdd() {
            return add;
        }

        /**
         * @param add the add to set
         */
        public void setAdd(Double[] add) {
            this.add = add;
        }

        /**
         * @return the abi
         */
        public BigInteger[] getAbi() {
            return abi;
        }

        /**
         * @param abi the abi to set
         */
        public void setAbi(BigInteger[] abi) {
            this.abi = abi;
        }

        /**
         * @return the abd
         */
        public BigDecimal[] getAbd() {
            return abd;
        }

        /**
         * @param abd the abd to set
         */
        public void setAbd(BigDecimal[] abd) {
            this.abd = abd;
        }

        /**
         * @return the abo
         */
        public boolean[] getAbo() {
            return abo;
        }

        /**
         * @param abo the abo to set
         */
        public void setAbo(boolean[] abo) {
            this.abo = abo;
        }

        /**
         * @return the abobo
         */
        public Boolean[] getAbobo() {
            return abobo;
        }

        /**
         * @param abobo the abobo to set
         */
        public void setAbobo(Boolean[] abobo) {
            this.abobo = abobo;
        }

        /**
         * @return the ach
         */
        public char[] getAch() {
            return ach;
        }

        /**
         * @param ach the ach to set
         */
        public void setAch(char[] ach) {
            this.ach = ach;
        }

        /**
         * @return the achch
         */
        public Character[] getAchch() {
            return achch;
        }

        /**
         * @param achch the achch to set
         */
        public void setAchch(Character[] achch) {
            this.achch = achch;
        }

        /**
         * @return the as
         */
        public String[] getAs() {
            return as;
        }

        /**
         * @param as the as to set
         */
        public void setAs(String[] as) {
            this.as = as;
        }

        /**
         * @return the self
         */
        public TestPOJO3 getSelf() {
            return self;
        }

        /**
         * @param self the self to set
         */
        public void setSelf(TestPOJO3 self) {
            this.self = self;
        }

        /**
         * @return the aself
         */
        public TestPOJO3[] getAself() {
            return aself;
        }

        /**
         * @param aself the aself to set
         */
        public void setAself(TestPOJO3[] aself) {
            this.aself = aself;
        }

        /**
         * @return the amb
         */
        public Map<String, byte[]> getAmb() {
            return amb;
        }

        /**
         * @param amb the amb to set
         */
        public void setAmb(Map<String, byte[]> amb) {
            this.amb = amb;
        }

        /**
         * @return the alch
         */
        public List<char[]> getAlch() {
            return alch;
        }

        /**
         * @param alch the alch to set
         */
        public void setAlch(List<char[]> alch) {
            this.alch = alch;
        }

        /**
         * @return the en
         */
        public POJOEnum1 getEn() {
            return en;
        }

        /**
         * @param en the en to set
         */
        public void setEn(POJOEnum1 en) {
            this.en = en;
        }

        /**
         * @return the aen
         */
        public POJOEnum1[] getAen() {
            return aen;
        }

        /**
         * @param aen the aen to set
         */
        public void setAen(POJOEnum1[] aen) {
            this.aen = aen;
        }
    }

}
