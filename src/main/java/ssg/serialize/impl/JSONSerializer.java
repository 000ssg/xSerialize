/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.tools.Indent;
import ssg.serialize.tools.Decycle;
import ssg.serialize.tools.Reflector;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ssg.serialize.tools.ClassCaster;
import ssg.serialize.tools.casters.Chars2StringCC;
import ssg.serialize.tools.casters.Long2DateCC;
import ssg.serialize.tools.casters.String2BigDecimalCC;
import ssg.serialize.tools.casters.String2BytesCC;
import ssg.serialize.tools.casters.String2CharCC;
import ssg.serialize.tools.casters.String2CharsCC;
import ssg.serialize.tools.casters.String2DateCC;

/**
 *
 * @author 000ssg
 */
public class JSONSerializer extends BaseObjectSerializer {

    public static final byte[] NULL = "null".getBytes();
    public static final byte[] TRUE = "true".getBytes();
    public static final byte[] FALSE = "false".getBytes();
    public static final int EOF = -1;
    // if set the map key is not handled as resolvable string (i.e. neve participates in references)
    public static final int TF_DISABLE_MAP_KEY = 0x0020;
    public static final int TF_BIGINT_AS_BYTES = 0x1000;
    // close map (}
    public static final Object CLM = new Object();//"}";//new Object();
    // key-value separator :
    public static final Object KVS = new Object();//":";//new Object();
    // value separator ,
    public static final Object VS = new Object();//",";//new Object();
    // close list ]
    public static final Object CLL = new Object();//"]";//new Object();
    // object reference prefix/suffix
    public static final byte[] REF_PREFIX = "$REF{".getBytes();
    public static final byte[] REF_SUFFIX = "}".getBytes();
    // Type names
    public static final String TN_OBJECT = "object";
    public static final String TN_COLLECTION = "list";
    public static final String TN_STRING = "string";
    public static final String TN_COMMENT = "comment";
    public static final String TN_INTEGER = "integer";
    public static final String TN_FLOAT = "float";
    public static final String TN_BOOLEAN = "boolean";
    public static final String TN_NULL = "null";
    public static final String TN_REF = "ref";

    // configurables
    public static int TEXT_READ_BUFFER_SIZE = 1024 * 10;

    Indent defaultIndent = null;
    ThreadLocal<Indent> indentVar = new ThreadLocal<Indent>();
    ThreadLocal<char[]> charsVar = new ThreadLocal<char[]>();

    public boolean SA = true;
    // used for toString to prevent cyclic references/duoplicates...
    Decycle daDecycle = new Decycle();

    public JSONSerializer() {
        decycleFlags |= TF_DISABLE_MAP_KEY | DF_STRING;
        init();
    }

    public JSONSerializer(boolean indented) {
        decycleFlags |= TF_DISABLE_MAP_KEY | DF_STRING;
        defaultIndent = new Indent();
        init();
    }

    public JSONSerializer(Indent indent) {
        decycleFlags |= TF_DISABLE_MAP_KEY | DF_STRING;
        defaultIndent = indent;
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
                //new Bytes2BigIntegerCC(),
                new Long2DateCC(),
                new String2DateCC(),
                new String2BytesCC(),
                new Chars2StringCC(),
                new String2CharsCC(),
                new String2CharCC()
        );
    }

    /**
     * Enable indentation if possible.
     *
     * @param obj
     * @param os
     * @return
     * @throws IOException
     */
    @Override
    public long toStream(Object obj, OutputStream os) throws IOException {
        boolean createdIndent = false;
        if (defaultIndent != null) {
            Indent indent = indentVar.get();
            if (indent == null) {
                indentVar.set(defaultIndent.clone());
                createdIndent = true;
            }
        }
        try {
            return super.toStream(obj, os);
        } finally {
            if (createdIndent) {
                indentVar.set(null);
            }
        }
    }

    @Override
    public long writeNull(OutputStream os, ObjectSerializerContext ctx) throws IOException {
        os.write(NULL);
        return NULL.length;
    }

    @Override
    public long write(ObjectSerializerContext ctx, Object obj, OutputStream os) throws IOException {
        if (obj == null) {
            return writeNull(os, ctx);
        } else if (ctx.isScalar(obj)) {
            Long ref = ((decycleFlags & DF_EXTENSIONS) != 0) ? ctx.checkRef(obj) : null;
            if (ref != null) {
                return writeRef(obj, ref, os, ctx);
            } else if (obj instanceof Number) {
                if (obj instanceof BigDecimal && (decycleFlags & DF_BIGDEC) != 0) {
                    return writeString(obj.toString(), false, os, ctx);
                } else if (obj instanceof BigInteger && (decycleFlags & DF_BIGINT) != 0) {
                    if ((decycleFlags & TF_BIGINT_AS_BYTES) != 0) {
                        return writeString(BASE64Serializer.encode(((BigInteger) obj).toByteArray()), false, os, ctx);
                    } else {
                        return writeString(obj.toString(), false, os, ctx);
                    }
                } else {
                    byte[] bb = obj.toString().getBytes();
                    os.write(bb);
                    return bb.length;
                }
            } else if (obj instanceof String) {
                return writeString((String) obj, false, os, ctx);
            } else if (obj instanceof Character) {
                return writeStringChars(new char[]{(Character) obj}, false, os, ctx);
            }
        } else if (obj instanceof byte[]) {
            return writeString(BASE64Serializer.encode((byte[]) obj), true, os, ctx);
        } else if (obj instanceof char[]) {
            return writeStringChars((char[]) obj, true, os, ctx);
        }
        return super.write(ctx, obj, os);
    }

    ////////////////////////////////////////////////////////////////////////////
    /////////////////// utilities
    ////////////////////////////////////////////////////////////////////////////
    public Indent indent() {
        return indentVar.get();
    }

    public long writeUnrecognized(Object obj, OutputStream os) throws IOException {
        throw new IOException("Unrecognized object to write: " + obj);
    }

    public long writeStringChars(char[] chs, boolean check, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        if (check) {
            Long ref = ctx.checkRef(chs);
            if (ref != null) {
                return this.writeRef(chs, ref, os, ctx);
            }
        }
        long c = 0;
        int pos = 0;
        byte[] buf = new byte[1024];
        buf[pos++] = '"';
        for (char ch : chs) {
            if (pos >= buf.length - 4) {
                os.write(buf, 0, pos);
                c += pos;
                pos = 0;
            }
            switch (ch) {
                case '\\':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('\\');
                    break;
                case '\t':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('t');
                    break;
                case '\b':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('b');
                    break;
                case '\n':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('n');
                    break;
                case '\r':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('r');
                    break;
                case '\f':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('f');
                    break;
                case '\'':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('\'');
                    break;
                case '"':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('"');
                    break;
                case '/':
                    buf[pos++] = ('\\');
                    buf[pos++] = ('/');
                    break;
                default:
                    pos += UTF8Serializer.toUTF8(ch, buf, pos);// utf8(ch, buf, pos);
            }
        }
        if (pos < buf.length - 1) {
            buf[pos++] = '"';
            os.write(buf, 0, pos);
            c += pos;
            pos = 0;
        } else {
            os.write(buf, 0, pos);
            c += pos;
            pos = 0;
            os.write('"');
            c++;
        }
        return c;
    }

    public long writeString(String s, boolean check, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        return writeStringChars(s.toCharArray(), check, os, ctx);
    }

    public long writeString0(String s, boolean check, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        if (check) {
            Long ref = ctx.checkRef(s);
            if (ref != null) {
                return this.writeRef(s, ref, os, ctx);
            }
        }
        long c = 2;
        byte[] bb = s
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("/", "\\/")
                .getBytes(encoding);
        os.write((byte) '"');
        os.write(bb);
        os.write((byte) '"');
        return 2 + bb.length;
    }

    @Override
    public long writeScalar(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        if (obj instanceof Number) {
            if (obj instanceof BigInteger && (decycleFlags & TF_BIGINT_AS_BYTES) != 0) {
                return writeString(BASE64Serializer.encode(((BigInteger) obj).toByteArray()), false, os, ctx);
            } else {
                byte[] bb = obj.toString().getBytes();
                os.write(bb);
                return bb.length;
            }
        } else if (obj instanceof Boolean) {
            if ((Boolean) obj) {
                os.write(TRUE);
                return TRUE.length;
            } else {
                os.write(FALSE);
                return FALSE.length;
            }
        } else if (obj instanceof URL || obj instanceof URI || obj instanceof File) {
            return writeString(obj.toString(), false, os, ctx);
        } else if (obj != null && obj.getClass().isEnum()) {
            return writeString(obj.toString(), false, os, ctx);
        } else {
            return writeUnrecognized(obj, os);
        }
    }

    @Override
    public long writeCollection(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        if (obj instanceof byte[]) {
            return writeString(BASE64Serializer.encode((byte[]) obj), false, os, ctx);
        } else if (obj instanceof char[]) {
            return writeStringChars((char[]) obj, false, os, ctx);
        }
        boolean skipNulls = (decycleFlags & DF_IGNORE_COLLECTION_NULLS) != 0;
        long c = 2;
        os.write((byte) '[');
        Indent indent = indent();
        if (indent != null) {
            indent.levelDown();
        }
        try {
            if (obj instanceof Collection) {
                boolean first = true;
                for (Object o : (Collection) obj) {
                    if (skipNulls && o == null) {
                        continue;
                    }
                    if (first) {
                        first = false;
                    } else {
                        os.write((byte) ',');
                        c++;
                    }
                    if (indent != null) {
                        c += indent.write(os);
                    }
                    c += write(ctx, o, os);
                }
            } else if (obj != null && obj.getClass().isArray()) {
                if (SA) {
                    Class act = obj.getClass().getComponentType();
                    if (act.isPrimitive()) {
                        if (boolean.class == act) {
                            boolean[] arr = (boolean[]) obj;
                            for (int i = 0; i < arr.length; i++) {
                                if (i > 0) {
                                    os.write((byte) ',');
                                    c++;
                                }
                                if (indent != null) {
                                    c += indent.write(os);
                                }
                                if (arr[i]) {
                                    os.write(TRUE);
                                    c += TRUE.length;
                                } else {
                                    os.write(FALSE);
                                    c += FALSE.length;
                                }
                            }
                        } else if (short.class == act) {
                            short[] arr = (short[]) obj;
                            for (int i = 0; i < arr.length; i++) {
                                if (i > 0) {
                                    os.write((byte) ',');
                                    c++;
                                }
                                if (indent != null) {
                                    c += indent.write(os);
                                }
                                c += writeScalar(arr[i], os, ctx);
                            }
                        } else if (int.class == act) {
                            int[] arr = (int[]) obj;
                            for (int i = 0; i < arr.length; i++) {
                                if (i > 0) {
                                    os.write((byte) ',');
                                    c++;
                                }
                                if (indent != null) {
                                    c += indent.write(os);
                                }
                                c += writeScalar(arr[i], os, ctx);
                            }
                        } else if (long.class == act) {
                            long[] arr = (long[]) obj;
                            for (int i = 0; i < arr.length; i++) {
                                if (i > 0) {
                                    os.write((byte) ',');
                                    c++;
                                }
                                if (indent != null) {
                                    c += indent.write(os);
                                }
                                c += writeScalar(arr[i], os, ctx);
                            }
                        } else if (float.class == act) {
                            float[] arr = (float[]) obj;
                            for (int i = 0; i < arr.length; i++) {
                                if (i > 0) {
                                    os.write((byte) ',');
                                    c++;
                                }
                                if (indent != null) {
                                    c += indent.write(os);
                                }
                                c += writeScalar(arr[i], os, ctx);
                            }
                        } else if (double.class == act) {
                            double[] arr = (double[]) obj;
                            for (int i = 0; i < arr.length; i++) {
                                if (i > 0) {
                                    os.write((byte) ',');
                                    c++;
                                }
                                if (indent != null) {
                                    c += indent.write(os);
                                }
                                c += writeScalar(arr[i], os, ctx);
                            }
                        }
                    } else {
                        boolean first = true;
                        for (Object o : (Object[]) obj) {
                            if (skipNulls && o == null) {
                                continue;
                            }
                            if (first) {
                                first = false;
                            } else {
                                os.write((byte) ',');
                                c++;
                            }
                            if (indent != null) {
                                c += indent.write(os);
                            }
                            c += write(ctx, o, os);
                        }
                    }
                } else {
                    int len = Array.getLength(obj);
                    boolean first = true;
                    for (int i = 0; i < len; i++) {
                        Object o = Array.get(obj, i);
                        if (skipNulls && o == null) {
                            continue;
                        }
                        if (first) {
                            first = false;
                        } else {
                            os.write((byte) ',');
                            c++;
                        }
                        if (indent != null) {
                            c += indent.write(os);
                        }
                        c += write(ctx, o, os);
                    }
                }
            }
        } finally {
            if (indent != null) {
                indent.levelUp();
                c += indent.write(os);
            }
        }
        os.write((byte) ']');
        return c;
    }

    @Override
    public long writeMap(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        long c = 2;
        os.write((byte) '{');
        if (obj instanceof Map) {
            Indent indent = indent();
            if (indent != null) {
                indent.levelDown();
            }
            try {
                Map<String, Object> m = (Map) obj;
                boolean first = true;
                boolean keyIsResolvable = (decycleFlags & TF_DISABLE_MAP_KEY) != 0;
                boolean skipNulls = (decycleFlags & DF_IGNORE_MAP_NULLS) != 0;
                for (Object key : m.keySet()) {
                    Object o = m.get(key);
                    if (skipNulls && o == null) {
                        continue;
                    }
                    if (first) {
                        first = false;
                    } else {
                        os.write((byte) ',');
                        c++;
                    }
                    if (indent != null) {
                        c += indent.write(os);
                    }
                    c += writeString("" + key, keyIsResolvable, os, ctx);
                    os.write((byte) ':');
                    c++;
                    if (indent != null) {
                        os.write((byte) ' ');
                        c++;
                    }
                    c += write(ctx, o, os);
                }
            } finally {
                if (indent != null) {
                    indent.levelUp();
                    c += indent.write(os);
                }
            }
        }
        os.write((byte) '}');
        return c;
    }

    @Override
    public long writeObject(Object obj, Reflector rf, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        long c = 2;
        os.write((byte) '{');
        if (!rf.isEmpty()) {
            Indent indent = indent();
            if (indent != null) {
                indent.levelDown();
            }
            try {
                boolean first = true;
                boolean keyIsResolvable = (decycleFlags & TF_DISABLE_MAP_KEY) != 0;
                boolean skipNulls = (decycleFlags & DF_IGNORE_MAP_NULLS) != 0;
                for (Object key : rf.keySet()) {
                    Object o = rf.get(obj, key);
                    if (skipNulls && o == null) {
                        continue;
                    }
                    if (first) {
                        first = false;
                    } else {
                        os.write((byte) ',');
                        c++;
                    }
                    if (indent != null) {
                        c += indent.write(os);
                    }
                    c += writeString("" + key, keyIsResolvable, os, ctx);
                    os.write((byte) ':');
                    c++;
                    if (indent != null) {
                        os.write((byte) ' ');
                        c++;
                    }
                    c += write(ctx, o, os);
                }
            } finally {
                if (indent != null) {
                    indent.levelUp();
                    c += indent.write(os);
                }
            }
        }
        os.write((byte) '}');
        return c;
    }

    @Override
    public long writeRef(Object obj, Object oref, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        Long ref = (Long) oref;//this.checkRef(obj);
        if (ref != null) {
            os.write('"');
            os.write(REF_PREFIX);
            byte[] b = ref.toString().getBytes();
            os.write(b);
            os.write(REF_SUFFIX);
            os.write('"');
            return 2 + REF_PREFIX.length + REF_SUFFIX.length + b.length;
        } else {
            return 0;
        }
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
        Reader rdr = new InputStreamReader(is, encoding) {
            long pos = 0;

            @Override
            public int read() throws IOException {
                int r = super.read();
                //System.out.println("READ [" + pos + "] \t" + r + "\t" + (("" + (char) r)).replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));
                //System.out.print((char)r);
                //if(r=='{' || r=='}' || r=='[' || r==']')System.out.println();
                pos++;
                return r;
            }
        };
        Object o = readBytes(rdr, ctx, false);
        if (o instanceof char[]) {
            o = charArrayToNumber((char[]) o);
        }
        return o;
    }

    /**
     * Recusrive reader...
     *
     * @param is
     * @param terminal
     * @return
     * @throws IOException
     */
    Object readBytes(Reader rdr, ObjectSerializerContext ctx, boolean asKey) throws IOException {
        int ch = 0;
        while ((ch = rdr.read()) != EOF) {
            //System.out.println("CHAR: "+ch+" -> "+((ch=='\n' || ch=='\r') ? "" :(char)ch));
            switch ((char) ch) {
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                case '\f':
                    // white space between elements
                    break;
                case '#':
                    readComment(rdr, null, ctx);
                    break;
                case '/':
                    // comment
                    ch = rdr.read();
                    switch ((char) ch) {
                        case '/':
                            readComment(rdr, null, ctx);
                            break;
                        case '*':
                            readComment(rdr, new char[]{'*', '/'}, ctx);
                            break;
                        default:
                            throw new IOException("Unrecognized comment: /" + ch);
                    }
                    break;
                case '"':
                case '\'':
                    // quoted string
                    return readString(rdr, ch, ctx, asKey);
//                    if (s != null && s.length()>(REF_PREFIX.length+REF_SUFFIX.length)) { // s.startsWith(REF_PREFIX) && s.endsWith(REF_SUFFIX)) {
//                        int len = Integer.parseInt(s.substring(REF_PREFIX.length(), s.length() - REF_SUFFIX.length()));
//                        //System.out.println("  toRef: " + len + " from " + s);
//                        //return resolveRef(null,(long) len);
//                        Object o = resolveRef(null, (long) len);
//                        //System.out.println("REF: " + len + " -> " + o);
//                        return o;
//                    } else {
//                        return resolveRef(s, null);
//                    }
                case '{':
                    // object {name:value,name:value}
                    return readMap(rdr, ctx);
                case '}':
                    return CLM;
                case ':':
                    return KVS;
                case ',':
                    return VS;
                case '[':
                    // list [item,item]
                    return readCollection(rdr, ctx);
                case ']':
                    return CLL;
                case 'n':
                    // null
                    readByExample(rdr, NULL);
                    return null;
                case 't':
                    // true
                    readByExample(rdr, TRUE);
                    return true;
                case 'f':
                    // false
                    readByExample(rdr, FALSE);
                    return false;
                case '-':
                case '+':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '.':
                    // number, optionaly starting with decimal point
                    return readNumber(rdr, ch);
            }
        }
        return null;
    }

    public Map readMap(Reader rdr, ObjectSerializerContext ctx) throws IOException {
        Map m = (!isResolveCyclicReferences())
                ? new LinkedHashMap()
                : new Decycle.DecycledMap();
//                new LinkedHashMap() {
//            @Override
//            public int hashCode() {
//                return System.identityHashCode(this);
//            }
//
//            @Override
//            public boolean equals(Object o) {
//                return hashCode() == System.identityHashCode(o);
//            }
//
//            @Override
//            public String toString() {
//                Decycle.DTYPE _dt = daDecycle.check(this);
//                try {
//                    if (Decycle.DTYPE.DUP.equals(_dt)) {
//                        return "#DUP";
//                    }
//                    return super.toString();
//                } finally {
//                    if (Decycle.DTYPE.NEW.equals(_dt)) {
//                        daDecycle.reset();
//                    }
//                }
//            }
//        };
        ctx.resolveRef(m, null);
        boolean sep = false;
        while (true) {
            Object key = readBytes(rdr, ctx, true);
            //System.out.println("READ: key?=" + key);
            if (CLM.equals(key)) { // key == CLM) {
                break;
            }
            if (sep) {
                if (VS.equals(key)) {// key == VS) {
                    sep = false;
                    continue;
                } else {
                    throw new IOException("Expected values separator, got " + key);
                }
            }
            if (key instanceof String) {
                //resolveRef(key);
                Object val = readBytes(rdr, ctx, false);
                //System.out.println("READ: val?=" + val);
                if (!KVS.equals(val)) { //val != KVS) {
                    throw new IOException("Value separator ':' is expected, got " + val);
                }
                val = readBytes(rdr, ctx, false);
                //System.out.println("READ: key=" + key + ", val=" + val);
                if (val instanceof char[]) {
                    Number n = charArrayToNumber((char[]) val);
                    m.put(key, n);

                    char[] chs = (char[]) val;
                    switch (chs[chs.length - 1]) {
                        case ' ':
                        case '\n':
                        case '\r':
                        case '\t':
                        case '\f':
                            sep = true;
                            break;
                        case ',':
                            sep = false;
                            break;
                        case '}':
                            return m;
                    }
                } else {
                    //Object val2 = resolveRef(val);
                    //m.put(key, (val2 != null) ? val2 : val);
                    m.put(key, val);
                    sep = true;
                }
            } else {
                throw new IOException("Field name expected, got " + key);
            }
        }
//        char ch = 0;
//        while ((ch = (char) rdr.read()) != '}') {
//
//        }
        return m;
    }

    public List readCollection(Reader rdr, ObjectSerializerContext ctx) throws IOException {
        List lst = (!isResolveCyclicReferences())
                ? new ArrayList()
                : new Decycle.DecycledList();
//                new ArrayList() {
//            @Override
//            public int hashCode() {
//                return System.identityHashCode(this);
//            }
//
//            @Override
//            public boolean equals(Object o) {
//                return hashCode() == System.identityHashCode(o);
//            }
//
//            @Override
//            public String toString() {
//                Decycle.DTYPE _dt = daDecycle.check(this);
//                try {
//                    if (Decycle.DTYPE.DUP.equals(_dt)) {
//                        return "#DUP";
//                    }
//                    return super.toString();
//                } finally {
//                    if (Decycle.DTYPE.NEW.equals(_dt)) {
//                        daDecycle.reset();
//                    }
//                }
//            }
//        };
        ctx.resolveRef(lst, null);
        boolean sep = false;
        while (true) {
            Object val = readBytes(rdr, ctx, false);
            if (CLL.equals(val)) {
                break;
            }
            if (sep) {
                if (VS.equals(val)) {
                    sep = false;
                    continue;
                } else {
                    throw new IOException("Expected values separator, got " + val);
                }
            }
            if (val instanceof char[]) {
                Number n = charArrayToNumber((char[]) val);
                lst.add(n);

                char[] chs = (char[]) val;
                switch (chs[chs.length - 1]) {
                    case ' ':
                    case '\n':
                    case '\r':
                    case '\t':
                    case '\f':
                        sep = true;
                        break;
                    case ',':
                        sep = false;
                        break;
                    case ']':
                        return lst;
                }
            } else {
//                Object val2 = resolveRef(val);
//                lst.add((val2 != null) ? val2 : val);
                lst.add(val);
                sep = true;
            }
        }
        return lst;
    }

    public void readByExample(Reader rdr, byte[] sample) throws IOException {
        for (int i = 1; i < sample.length; i++) {
            char ch = (char) rdr.read();
            if (sample[i] != ch) {
                throw new IOException("Unexpected character: " + ch + " in " + new String(sample) + " at " + i);
            }
        }
    }

    public String readComment(Reader rdr, char[] terminator, ObjectSerializerContext ctx) throws IOException {
        char ch = 0;
        StringBuilder sb = null;
        char[] buf = getBuffer();
        int bpos = 0;
        while ((ch = (char) rdr.read()) != EOF) {
            if (ch == EOF) {
                break;
            }

            if (terminator == null) {
                if (ch == '\n' || ch == '\r') {
                    break;
                }
            } else if (terminator[0] == ch) {
                int tpos = 1;
                while (tpos < terminator.length) {
                    ch = (char) rdr.read();
                    if (ch == EOF) {
                        break;
                    }
                    if (terminator[tpos] != ch) {
                        for (int j = 0; j <= tpos; j++) {
                            buf[bpos++] = terminator[j];
                            if (bpos == buf.length) {
                                if (sb == null) {
                                    sb = new StringBuilder();
                                }
                                sb.append(buf, 0, buf.length);
                                bpos = 0;
                            }
                        }
                        tpos = 0;
                    } else {
                        tpos++;
                    }
                    if (tpos == terminator.length) {
                        tpos = -1;
                        break;
                    }
                }
                if (tpos == -1) {
                    break;
                }
            }

            buf[bpos++] = ch;
            if (bpos == buf.length) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(buf, 0, buf.length);
                bpos = 0;
            }
        }

        if (sb != null) {
            if (bpos > 0) {
                sb.append(buf, 0, bpos);
            }
            return sb.toString();
        } else {
            return new String(buf, 0, bpos);
        }
    }

    Number charArrayToNumber(char[] val) {
        Number n = null;
        char[] chs = (char[]) val;
        boolean isFloat = false;
        for (int i = 0; i < chs.length - 1; i++) {
            if (chs[i] == '.' || chs[i] == 'E' || chs[i] == 'e') {
                isFloat = true;
                break;
            }
        }
        if (isFloat) {
            n = Double.parseDouble(new String(chs, 0, chs.length - 1));
        } else {
            n = Long.parseLong(new String(chs, 0, chs.length - 1));
        }
        return n;
    }

    char[] getBuffer() {
        char[] buf = charsVar.get();
        if (buf == null) {
            buf = new char[TEXT_READ_BUFFER_SIZE];//1024 * 65];
            charsVar.set(buf);
        }
        return buf;
    }

    public char[] readNumber(Reader rdr, int ch) throws IOException {
        char[] buf = getBuffer();
        buf[0] = (char) ch;
        int bpos = 1;
        boolean sign = (ch == '-' || ch == '+');
        boolean dot = ch == '.';
        boolean e = false;
        while ((ch = rdr.read()) != EOF) {
            buf[bpos++] = (char) ch;
            switch ((char) ch) {
                case '-':
                case '+':
                    if (sign) {
                        throw new IOException("Duplicated sign in " + new String(buf, 0, bpos));
                    }
                    sign = true;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    break;
                case '.':
                    if (dot) {
                        throw new IOException("Duplicated decimal dot in " + new String(buf, 0, bpos));
                    }
                    dot = true;
                    break;
                case 'E':
                case 'e':
                    if (e) {
                        throw new IOException("Duplicated mantissa in " + new String(buf, 0, bpos));
                    }
                    e = true;
                    dot = false;
                    sign = false;
                    break;
                default:
                    return Arrays.copyOf(buf, bpos);
            }
        }
        if (ch == EOF) {
            buf[bpos++] = (char) ch;
        }
        return Arrays.copyOf(buf, bpos);
    }

    public <T> T readString(Reader rdr, int terminal, ObjectSerializerContext ctx, boolean asKey) throws IOException {
        int ch = 0;
        StringBuilder sb = null;
        char[] buf = getBuffer();
        int bpos = 0;
        boolean isRef = false;
        while ((ch = rdr.read()) != terminal) {
            if (ch == EOF) {
                throw new IOException("Unterminated string: " + ((sb != null) ? sb.toString() : "") + ((bpos > 0) ? new String(buf, 0, bpos) : ""));
            }

            // unescape
            if (ch == '\\') {
                ch = rdr.read();
                if (ch == EOF) {
                    throw new IOException("Unterminated string: " + ((sb != null) ? sb.toString() : "") + ((bpos > 0) ? new String(buf, 0, bpos) : ""));
                }
                switch ((char) ch) {
                    case 't':
                        ch = '\t';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    case '"':
                        ch = '"';
                        break;
                    case '\\':
                        ch = '\\';
                        break;
                    case '/':
                        ch = '/';
                        break;
                }
            }

            buf[bpos++] = (char) ch;
            if (bpos == buf.length) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(buf, 0, buf.length);
                bpos = 0;
            }
        }

        if (sb != null) {
            if (bpos > 0) {
                sb.append(buf, 0, bpos);
            }
            //return (T) ctx.resolveRef(sb.toString(), null);
            String s = sb.toString();
            if (asKey && (decycleFlags & TF_DISABLE_MAP_KEY) == 0) {
                String s1 = ctx.resolveRef(s, null);
                return (T) ((s1 != null) ? s1 : s);
            } else {
                return (T) s;
            }
        } else {
            // check for ref
            if (bpos > REF_PREFIX.length + REF_SUFFIX.length) {
                boolean ok = true;
                for (int i = 0; i < REF_PREFIX.length; i++) {
                    if (buf[i] != REF_PREFIX[i]) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    int off = bpos - REF_SUFFIX.length;
                    for (int i = 0; i < REF_SUFFIX.length; i++) {
                        if (buf[off++] != REF_SUFFIX[i]) {
                            ok = false;
                            break;
                        }
                    }
                }
                if (ok) {
                    long len = Long.parseLong(new String(buf, REF_PREFIX.length, bpos - REF_PREFIX.length - REF_SUFFIX.length));
                    //System.out.println("  toRef: " + len + " from " + s);
                    //return resolveRef(null,(long) len);
                    Object o = ctx.resolveRef(null, (long) len);
                    //System.out.println("REF: " + len + " -> " + o);
                    return (T) o;
                }
            }
            String s = new String(buf, 0, bpos);
//            String s1 = ctx.resolveRef(s, null);
//            return (T) ((s1 != null) ? s1 : s);
            if (!asKey || asKey && (decycleFlags & TF_DISABLE_MAP_KEY) != 0) {
                String s1 = ctx.resolveRef(s, null);
                return (T) ((s1 != null) ? s1 : s);
            } else {
                return (T) s;
            }
        }
    }

    @Override
    public <T> T enrich(ObjectSerializerContext ctx, Object obj, Class<T> clazz) throws IOException {
        if (clazz == byte[].class && obj instanceof String) {
            // base64 decode string
            obj = BASE64Serializer.decode((String) obj);
        } else if ((clazz == char.class || clazz == Character.class) && obj instanceof String) {
            String s = (String) obj;
            obj = (s == null || s.isEmpty()) ? (clazz == char.class) ? (char) 0 : (char) 0 : s.charAt(0);
//        } else if (clazz == BigDecimal.class && obj instanceof String) {
//            char[] chs = ((String) obj).toCharArray();
//            return (T) new BigDecimal(chs, 0, chs.length);
        }
        return super.enrich(ctx, obj, clazz);
    }

    /**
     * Performs stream scan and returns type-based summary
     *
     * @param is
     * @return
     * @throws IOException
     */
    public OSStat scan(OSScanHandler handler, OSStat stat, InputStream is) throws IOException {
        if (stat == null) {
            stat = new JOSStat();
        }
        final OSStat pstat = stat;
        PushbackReader rdr = new PushbackReader(new InputStreamReader(is, encoding) {
            long pos = 0;

            @Override
            public int read() throws IOException {
                int c = super.read();
                pstat.pos(pos);
                //System.out.println("[" + pos++ + "] " + ((c <= ' ') ? "#" + c : "'" + (char) c + "'"));
                return c;
            }
        });
        if (stat instanceof BOSStat) {
            ((BOSStat) stat).ctx = createContext(is);
        }
        return scan(handler, stat, rdr, -1);
    }

    /**
     * Scan until EOC
     *
     * @param stat
     * @param rdr
     * @param EOC
     * @return
     * @throws IOException
     */
    public OSStat scan(OSScanHandler<String, Integer> handler, OSStat<String, Integer> stat, PushbackReader rdr, int... EOC) throws IOException {
        int ch = 0;
        if (EOC[0] == -1) {
            while ((ch = rdr.read()) != EOC[0]) {
                switch (ch) {
                    case '\'':
                    case '"':
                        scan(handler, stat, rdr, ch);
                        return stat;
                    case '#':
                        scan(handler, stat, rdr, '\n');
                        return stat;
                    case '/':
                        ch = rdr.read();
                        switch (ch) {
                            case '/':
                                scan(handler, stat, rdr, '\n');
                                return stat;
                            case '*':
                                scan(handler, stat, rdr, '*', '/');
                                return stat;
                            default:
                                throw new IOException("Unexpected " + ch);
                        }
                    case '{':
                        scan(handler, stat, rdr, '}');
                        return stat;
                    case '[':
                        scan(handler, stat, rdr, ']');
                        return stat;
                    case ']':
                    case '}':
                    case ':':
                    case ',':
                        rdr.unread(ch);
                        return stat;
                    case 'n':
                        // null
                        readByExample(rdr, NULL);
                        stat.addType(TN_NULL);
                        if (handler != null) {
                            handler.onScalar(TN_NULL, null, false);
                        }
                        return stat;
                    case 't':
                        // true
                        readByExample(rdr, TRUE);
                        stat.addType(TN_BOOLEAN);
                        if (handler != null) {
                            handler.onScalar(TN_BOOLEAN, true, false);
                        }
                        return stat;
                    case 'f':
                        // case false
                        readByExample(rdr, FALSE);
                        stat.addType(TN_BOOLEAN);
                        if (handler != null) {
                            handler.onScalar(TN_BOOLEAN, false, false);
                        }
                        return stat;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                    case '-':
                    case '+': {
                        byte[] num = new byte[100];
                        int numi = 0;
                        num[numi++] = (byte) ch;
                        boolean dot = false;
                        boolean exp = false;
                        while ((ch = rdr.read()) != -1) {
                            if (ch == '.') {
                                dot = true;
                            } else if (ch == 'e' || ch == 'E') {
                                exp = true;
                            } else if (ch == ',' || ch == ']' || ch == '}') {
                                rdr.unread(ch);
                                break;
                            }
                            switch (ch) {
                                case ' ':
                                case '\t':
                                case '\n':
                                case '\r':
                                case '\b':
                                case '\f':
                                    break;
                                default:
                                    num[numi++] = (byte) ch;
                            }
                        }
                        try {
                            if (dot || exp) {
                                double d = Double.parseDouble(new String(num, 0, numi));
                                stat.addType(TN_FLOAT);
                                if (handler != null) {
                                    handler.onScalar(TN_FLOAT, d, false);
                                }
                            } else {
                                long l = Long.parseLong(new String(num, 0, numi));
                                stat.addType(TN_INTEGER);
                                if (handler != null) {
                                    handler.onScalar(TN_INTEGER, l, false);
                                }
                            }
                        } catch (Throwable th) {
                            throw new IOException("Invalid numeric value: " + new String(num, 0, numi), th);
                        }
                    }
                    return stat;
//                    case '$':
//                    // reference ?
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                    case '\b':
                    case '\f':
                    default:
                        continue;
                }
            }
        } else if (EOC[0] == '\'' || EOC[0] == '"') {
            if (handler != null) {
                handler.onOpen(TN_STRING, true);
            }
            boolean ref = true;
            Integer refValue = null;
            long startAt = stat.pos();
            //System.out.println("START STR");
            while ((ch = rdr.read()) != EOC[0]) {
                switch (ch) {
                    case '\\':
                        ch = rdr.read();
                        if (handler != null) {
                            switch (ch) {
                                case 'n':
                                    handler.pushChar('\n');
                                    break;
                                case 'r':
                                    handler.pushChar('\r');
                                    break;
                                case 't':
                                    handler.pushChar('\t');
                                    break;
                                case 'f':
                                    handler.pushChar('\f');
                                    break;
                                case 'b':
                                    handler.pushChar('\b');
                                    break;
                                case '\\':
                                    handler.pushChar('\\');
                                    break;
                                default:
                                    handler.pushChar((char) ch);
                                    break;
                            }
                        }
                        continue;
                    case '$':
                        if (!ref) {
                            if (handler != null) {
                                handler.pushChar((char) ch);
                            }
                            continue;
                        }
                        for (int i = 1; i < REF_PREFIX.length; i++) {
                            ch = rdr.read();
                            if (ch == -1) {
                                throw new IOException("Unexpected EOF while checking for reference entry");
                            }
                            if (REF_PREFIX[i] != ch) {
                                ref = false;
                                break;
                            }
                        }
                        if (ch == EOC[0]) {
                            break;
                        }
                        if (ref) {
                            byte[] refs = new byte[100];
                            int refsi = 0;
                            while ((ch = rdr.read()) != REF_SUFFIX[0]) {
                                if (ch == -1) {
                                    throw new IOException("Unexpected EOF while getting reference value");
                                }
                                if (ch == EOC[0]) {
                                    break;
                                }
                                refs[refsi++] = (byte) ch;
                            }
                            if (ch == EOC[0]) {
                                break;
                            }
                            if (REF_SUFFIX.length > 1) {
                                for (int i = 1; i < REF_SUFFIX.length; i++) {
                                    ch = rdr.read();
                                    if (ch == -1) {
                                        throw new IOException("Unexpected EOF while checking for reference entry");
                                    }
                                    if (ch == EOC[0]) {
                                        break;
                                    }
                                    if (REF_SUFFIX[i] != ch) {
                                        ref = false;
                                        refValue = null;
                                        break;
                                    }
                                }
                            }
                            if (ch == EOC[0]) {
                                break;
                            }
                            if (ref) {
                                try {
                                    refValue = Integer.parseInt(new String(refs, 0, refsi));
                                } catch (Throwable th) {
                                    ref = false;
                                }
                            }
                        }
                        break;
                    default:
                        if (handler != null) {
                            handler.pushChar((char) ch);
                        }
                        refValue = null;
                }
                if (ch == EOC[0]) {
                    break;
                }
            }
            //System.out.println("END STR");
            if (refValue != null) {
                stat.addRef(refValue);
                if (handler != null) {
                    handler.onClose(TN_STRING, refValue);
                }
            } else {
                stat.addType(TN_STRING);
                if (handler != null) {
                    handler.onClose(TN_STRING);
                }
            }
        } else if (EOC[0] == '\n' || EOC[0] == '*') {
            stat.addType(TN_COMMENT);
            if (handler != null) {
                handler.onOpen(TN_COMMENT, false);
            }
            while ((ch = rdr.read()) != EOC[0]) {
                if (ch == EOF) {
                    if (EOC[0] != '\n') {
                        throw new IOException("Unexpected EOF within block comment.");
                    }
                    break;
                }
                if (handler != null) {
                    handler.pushChar((char) ch);
                }
            }
            if (handler != null) {
                handler.onClose(TN_COMMENT);
            }
        } else if (EOC[0] == ']') {
            stat.addType(TN_COLLECTION);
            if (handler != null) {
                handler.onOpen(TN_COLLECTION, true);
            }
            // read list
            while ((ch = rdr.read()) != EOC[0]) {
                if (ch == ',') {
                    continue;
                }
                rdr.unread(ch);
                scan(handler, stat, rdr, -1);
            }
            if (handler != null) {
                handler.onClose(TN_COLLECTION);
            }
        } else if (EOC[0] == '}') {
            stat.addType(TN_OBJECT);
            if (handler != null) {
                handler.onOpen(TN_OBJECT, true);
            }
            boolean key = true;
            // read object as key:value pairs with "," separator
            while ((ch = rdr.read()) != EOC[0]) {
                if (ch == ',') {
                    continue;
                }
                rdr.unread(ch);
                scan(handler, stat, rdr, -1);
                ch = rdr.read();
                if (ch == EOC[0]) {
                    break;
                }
                if (ch != ':') {
                    throw new IOException("Missing expected name-value separator (:), got " + (char) ch);
                }
                scan(handler, stat, rdr, -1);
            }
            if (handler != null) {
                handler.onClose(TN_OBJECT);
            }
        }
        return stat;
    }

    public static class JOSStat extends BOSStat<String, Integer> {

        @Override
        public String type2name(String type) {
            return type;
        }

        @Override
        public boolean isRefType(String type, ObjectSerializerContext ctx) {
            return TN_STRING.equals(type) || TN_OBJECT.equals(type) || TN_COLLECTION.equals(type);
        }

        @Override
        public String refType(Integer ref) {
            return (ref < refTypes.size()) ? refTypes.get(ref) : null;
        }

        @Override
        public boolean isScalarType(String type, ObjectSerializerContext ctx) {
            return !(TN_OBJECT.equals(type) || TN_COLLECTION.equals(type));
        }
    }

    public static class JSONScanHandler extends BaseScanHandler<String, Integer> {

        int nextRef = 1;

        @Override
        public void onStart() {
            super.onStart(); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isObject(String type) {
            return TN_OBJECT.equals(type);
        }

        @Override
        public boolean isCollection(String type) {
            return TN_COLLECTION.equals(type);
        }

        @Override
        public Object adjust(Object value, String type) {
            if (TN_REF.equals(type)) {
                return "#REF" + value;
            } else {
                return value;
            }
        }

        @Override
        public Integer createReference(String type, Object value) {
            if (type == null || value == null) {
                return null;
            } else {
                return nextRef++;
            }
        }

    }
}
