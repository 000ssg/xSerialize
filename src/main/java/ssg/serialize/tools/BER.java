/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.serialize.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TimeZone;
import ssg.serialize.utils.Dump;

/**
 * Use scan to get parsed BER-encoded data.
 *
 * Use BEREntry with BERConstructorContext to create proper ASN.1 output.
 *
 * @author 000ssg
 */
public class BER {

    public static final int TAG_UNIVERSAL = 0;
    public static final int TAG_APPLICATION = 1;
    public static final int TAG_CONTEXT_SPECIFIC = 2;
    public static final int TAG_PRIVATE = 3;
    //
    public static final long INDEFINITE_LENGTH = -1;
    //
    public static final int CLASS_EOC = 0;
    public static final int CLASS_BOOLEAN = 1;
    public static final int CLASS_INTEGER = 2;
    public static final int CLASS_BIT_STRING = 3;
    public static final int CLASS_OCTET_STRING = 4;
    public static final int CLASS_NULL = 5;
    public static final int CLASS_OBJECT_IDENTIFIER = 6;
    public static final int CLASS_OBJECT_DESCRIPTOR = 7;
    public static final int CLASS_EXTERNAL_OR_INSTANCE_OF = 8;
    public static final int CLASS_REAL = 9;
    public static final int CLASS_ENUMERATED = 10;
    public static final int CLASS_EMBEDDED_PDV = 11;
    public static final int CLASS_UTF8_STRING = 12;
    public static final int CLASS_RELATIVE_OBJECT_IDENTIFIER = 13;
    public static final int CLASS_RESERVED_14 = 14;
    public static final int CLASS_RESERVED_15 = 15;
    public static final int CLASS_SEQUENCE = 16;
    public static final int CLASS_SET = 17;
    public static final int CLASS_NUMERIC_STRING = 18;
    public static final int CLASS_PRINTABLE_STRING = 19;
    public static final int CLASS_TELETEX_STRING = 20;
    public static final int CLASS_VIDEOTEX_STRING = 21;
    public static final int CLASS_IA5_STRING = 22;
    public static final int CLASS_TIME = 23;
    public static final int CLASS_TIME2 = 24;
    public static final int CLASS_GRAPHIC_STRING = 25;
    public static final int CLASS_VISIBLE_STRING = 26;
    public static final int CLASS_GRAPHIC_STRING2 = 27;
    public static final int CLASS_STRING = 28;

    // ???
    public static final byte[] EOC = new byte[]{CLASS_EOC, 0};
    public static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "NULL";
        }
    };
    public static final DateFormat time23format = new SimpleDateFormat("yyMMddHHmmssz");
    public static final DateFormat time23formatZ = new SimpleDateFormat("yyMMddHHmmss'Z'") {
        {
            setTimeZone(TimeZone.getTimeZone("UTC"));
        }
    };
    public static final DateFormat time24formatA = new SimpleDateFormat("yyMMddHHmmss.S");
    public static final DateFormat time24formatB = new SimpleDateFormat("yyMMddHHmmss.S'Z'") {
        {
            setTimeZone(TimeZone.getTimeZone("UTC"));
        }
    };
    public static final DateFormat time24formatC = new SimpleDateFormat("yyMMddHHmmss.Sz");

    public static <T> T scan(InputStream is, BEREventHandler eHandler) throws IOException {
        LIS lis = (is instanceof LIS) ? (LIS) is : new LIS(is, -1);

        Stack<Object[]> stack = new Stack<Object[]>();
        if (eHandler == null) {
            eHandler = new DefaultBEREventHandler();
        }
        return scan(stack, lis, eHandler);
    }

    public static <T> T scan(Stack<Object[]> stack, LIS lis, BEREventHandler eHandler) throws IOException {

        byte[] header = null;
        if ((header = readHeader(lis)) != null) {
            Object[] oo = new Object[]{header, null};
            stack.add(oo);
            eHandler.onOpenItem(stack, header, 0);
            Object value = null;
            long len = getLength(header, 0);
            if (!isPrimitive(header, 0)) {
                BERConstructed berv = new BERConstructed(header, 0, null);
                if (len == INDEFINITE_LENGTH) {
                    Object o = scan(stack, lis, eHandler);
                    while (o != EOC) {
                        if (EOC == o) {
                            break;
                        }
                        if (NULL == o) {
                            o = null;
                        }
                        berv.append(o);
                        o = scan(stack, lis, eHandler);
                    }
                    if (EOC == o) {
                        eHandler.onEOC(stack, header, 0);
                        //break;
                    }
                } else {
                    long pos = lis.pos;
                    Object o = scan(stack, lis.pushLimit(len), eHandler);
                    while (o != null) {
                        if (NULL == o) {
                            o = null;
                        }
                        berv.append(o);
                        len -= lis.pos - pos;
                        pos = lis.pos;
                        if (len == 0) {
                            break;
                        }
                        o = scan(stack, lis.pushLimit(len), eHandler);
                        lis.popLimit();
                    }
                    lis.popLimit();
                }
                berv.close();
                value = berv.value;
            } else if (len == INDEFINITE_LENGTH) {
                BERConstructed berv = new BERConstructed(header, 0, null);
                Object o = scan(stack, lis, eHandler);
                while (o != EOC) {
                    if (EOC == o) {
                        eHandler.onEOC(stack, header, 0);
                        break;
                    }
                    if (NULL == o) {
                        o = null;
                    }
                    berv.append(o);
                    o = scan(stack, lis, eHandler);
                }
                berv.close();
                value = berv.value;
            } else {
                value = get(header, 0, lis);
            }
            eHandler.onCloseItem(stack, header, 0, value);
            if (!stack.isEmpty()) {
                stack.peek()[1] = value;
                return (!stack.isEmpty()) ? (T) stack.pop()[1] : null;
            } else {
                return (T) value;
            }
        }
        return null;
    }

    public static class BERConstructed {

        byte[] header;
        int off;
        Object value;

        public BERConstructed(byte[] header, int off, Object value) throws IOException {
            this.header = header;
            this.off = off;
            int type = getType(header, off);
            if (type == CLASS_SEQUENCE || !isPrimitive(header, off) || value instanceof byte[]) {
                this.value = new ArrayList();
                if (value != null) {
                    ((List) this.value).add(value);
                }
            }
        }

        public void append(Object value) throws IOException {
            if (this.value instanceof Collection) {
                ((Collection) this.value).add(value);
            } else if (this.value instanceof String) {
                this.value = this.value.toString() + value;
            }
        }

        public void close() throws IOException {
            if (this.value instanceof Collection && !(getType(header, off) == CLASS_SEQUENCE
                    || getType(header, off) == CLASS_SET)) {
                Collection coll = (Collection) value;
                if (!coll.isEmpty() && coll.iterator().next() instanceof byte[]) {
                    int bufsSize = 0;
                    for (byte[] buf : ((Collection<byte[]>) value)) {
                        bufsSize += buf.length;
                    }
                    byte[] tmp = new byte[bufsSize];
                    int bufOff = 0;
                    for (byte[] buf : ((Collection<byte[]>) value)) {
                        System.arraycopy(buf, 0, tmp, bufOff, buf.length);
                    }
                    value = tmp;
                } else if (coll.size() == 1) {
                    value = coll.iterator().next();
                }
            }
        }
    }

    public static interface BEREventHandler {

        /**
         * Indicates item start (characterized by header) and should respond
         * "true" if value should be read, or "false" if just skipped.
         *
         * @param stack
         * @param header
         * @param off
         * @return
         */
        boolean onOpenItem(Stack<Object[]> stack, byte[] header, int off);

        /**
         *
         * @param stack
         * @param header
         * @param off
         * @param value
         */
        void onCloseItem(Stack<Object[]> stack, byte[] header, int off, Object value);

        void onEOC(Stack<Object[]> stack, byte[] header, int off);
    }

    public static class DefaultBEREventHandler implements BEREventHandler {

        @Override
        public boolean onOpenItem(Stack<Object[]> stack, byte[] header, int off) {
            return true;
        }

        @Override
        public void onCloseItem(Stack<Object[]> stack, byte[] header, int off, Object value) {
        }

        @Override
        public void onEOC(Stack<Object[]> stack, byte[] header, int off) {
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////// headers
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Reads input stream for BER entry header.
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] readHeader(InputStream is) throws IOException {
        byte[] buf = new byte[16];
        int pos = 2;
        if (is.read(buf, 0, 1) == -1) {
            return null;
            //throw new IOException("EOF detected. Expected BER tag byte.");
        }
        if (is.read(buf, 1, 1) == -1) {
            throw new IOException("EOF detected. Expected BER length byte.");
        }

        if (buf[0] == 0 && buf[1] == 0) {
            // EOC detected -> return the EOC
            return EOC;
        }

        ////////////////////////////////// type
        if ((buf[0] & 0x1F) == 0x1F) {
            // 8.1.2.4
            long type = 0;
            while ((is.read(buf, pos++, 1)) != -1) {
                byte b = buf[pos - 1];
                type <<= 7;
                type |= b & 0x7F;
                if ((b & 0x80) == 0) {
                    break;
                }
                if (type == 0) {
                    // 8.1.2.4.2 c
                    throw new IOException("Invalid class tag composite number: cannot be 0");
                }
            }
        }

        ////////////////////////////////// length
        byte b = buf[1];

        if ((b & 0x80) == 0) {
            // 8.1.3.4
        } else if (b == 0x80) {
            // 8.1.3.6 - indefinite length
        } else {
            // 8.1.3.5
            int lBytes = b & 0x7F;
            readFull(buf, 2, lBytes, is, "EOF while reading " + lBytes + " length bytes");
            pos += lBytes;
        }
        return Arrays.copyOf(buf, pos);
    }

    /**
     * Writes tag/primitive/type/length bytes and returns resultant header size.
     *
     * @param buf
     * @param off
     * @param tag
     * @param primitive
     * @param tag
     * @param length
     * @return
     */
    public static int createHeader(byte[] buf, int off, int tag, boolean primitive, int type, long length) {
        setTag(buf, off, tag);
        setPrimitive(buf, off, primitive);
        int tc = setType(buf, off, type);
        int lc = setLength(buf, off, length);
        return 1 + tc + lc;
    }

    /**
     * Writes tag/primitive/type bytes and returns resultant header size
     * (without length!).
     *
     * @param buf
     * @param off
     * @param tag
     * @param primitive
     * @param tag
     * @return
     */
    public static int createHeader(byte[] buf, int off, int tag, boolean primitive, int type) {
        setTag(buf, off, tag);
        setPrimitive(buf, off, primitive);
        int tc = setType(buf, off, type);
        return 1 + tc;
    }

    /**
     * Returns tag value (2 upper bits -> int) from header at offset.
     *
     * @param buf
     * @param off
     * @return
     */
    public static int getTag(byte[] buf, int off) {
        // 8.1.2.2
        return (0xFF & ((buf[0 + off] & 0xC0) >> 6));
    }

    /**
     * Returns tag value (2 upper bits -> int) from header at offset.
     *
     * @param buf
     * @param off
     * @return
     */
    public static void setTag(byte[] buf, int off, int tag) {
        // 8.1.2.2
        // clear tag bits
        buf[off] ^= 0xC0 & buf[off];
        // set actual tag bits
        buf[off] |= ((tag & 0x03) << 6);
    }

    /**
     * Returns true if specified header (at offset) has bit 6=0 (i.e. value is
     * NOT constructed)
     *
     * @param buf
     * @param off
     * @return
     */
    public static boolean isPrimitive(byte[] buf, int off) {
        // 8.1.2.5
        return ((buf[0 + off] & 0x20) == 0);
    }

    /**
     * Returns true if specified header (at offset) has bit 6=0 (i.e. value is
     * NOT constructed)
     *
     * @param buf
     * @param off
     * @return
     */
    public static void setPrimitive(byte[] buf, int off, boolean primitive) {
        // 8.1.2.5
        if (primitive) {
            buf[0 + off] ^= 0x20 & buf[0 + off];
        } else {
            buf[0 + off] |= 0x20;
        }
    }

    /**
     * Returns type value as int. Constructs if of several bytes if needed.
     *
     * @param buf
     * @param off
     * @return
     */
    public static int getType(byte[] buf, int off) { //throws IOException {
        byte b = buf[off];
        long type = 0;
        if ((b & 0x1F) == 0x1F) {
            // 8.1.2.4
            while (true) {
                off++;
                type <<= 7;
                type |= buf[off] & 0x7F;
                if ((b & 0x80) == 0) {
                    break;
                }
                if (type == 0) {
                    // 8.1.2.4.2 c
                    //throw new IOException("Invalid class tag composite number: cannot be 0");
                }
            }
        } else {
            // 8.1.2.3
            type = b & 0x1F;
        }
        return (int) type;
    }

    /**
     * Sets type (possibly multi.byte). Returns number of type bytes.
     *
     * @param buf
     * @param off
     * @param type
     * @return
     */
    public static int setType(byte[] buf, int off, int type) { //throws IOException {
        byte b = buf[off];
        int bits = NumberTools.bits(type);
        int bytes = bits / 7 + 1;
        off++;
        for (int i = bytes - 1; i >= 0; i++) {
            int v = (type & (0x7F << i * 7)) >> i * 7;
            if (i > 0) {
                v = 0x80;
            }
            buf[off++] = (byte) (0xFF & v);
        }
        return bytes;
    }

    /**
     * Returns length of value possibly constructing it from several bytes.
     * Length starting byte may depend on type length (evaluated).
     *
     * @param buf
     * @param off
     * @return
     */
    public static long getLength(byte[] buf, int off) {
        off += getTypeLength(buf, off) + 1;
        if ((buf[off] & 0x80) == 0) {
            // 8.1.3.4
            return buf[off];
        } else if (buf[off] == 0x80) {
            // 8.1.3.6 - indefinite length
            return INDEFINITE_LENGTH;
        } else {
            // 8.1.3.5
            long len = 0;
            int lBytes = (buf[off] & 0x7F) + 1;
            for (int i = 1; i < lBytes; i++) {
                len <<= 8;
                len |= (0xFF & buf[i + off]);
            }
            return len;
        }
    }

    public static int setIndefiniteLength(byte[] buf, int off) {
        // 8.1.3.6 - indefinite length
        off += getTypeLength(buf, off) + 1;
        buf[off] = (byte) 0x80;
        return 1;
    }

    public static int setLength(byte[] buf, int off, long length) {
        off += getTypeLength(buf, off) + 1;
        if (length == INDEFINITE_LENGTH) {
            return setIndefiniteLength(buf, off);
        }
        if (length < 128) {
            buf[off] = (byte) (0xFF & length);
            return 1;
        }

        int bits = NumberTools.bits(length);
        int bytes = bits / 8 + 1;
        buf[off++] = (byte) ((0x7F & bytes) | 0x80);
        for (int i = bytes - 1; i >= 0; i++) {
            int v = (int) ((length & (0xFF << i * 8)) >> i * 8);
            buf[off++] = (byte) (0xFF & v);
        }
        return bytes + 1;
    }

    public static long getEntrySize(byte[] header, int off) {
        long dl = getLength(header, off);
        if (dl == INDEFINITE_LENGTH || dl < 0) {
            return INDEFINITE_LENGTH;
        } else {
            return getHeaderLength(header, off) + dl;
        }
    }

    /**
     * Returns string with header info.
     *
     * @param header
     * @param off
     * @return
     */
    public static String dumpHeader(byte[] header, int off) {
        if (header == null || header.length == 0) {
            return "<no header>";
        }
        int tag = getTag(header, off);
        boolean primitive = isPrimitive(header, off);
        int type = getType(header, off);
        long len = getLength(header, off);
        return "BER{" + tag2name(tag) + ":" + ((primitive) ? 'P' : 'C') + ":" + type2name((int) type) + "/" + type + ":" + len + "}";
    }

    ////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////// values
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Retrieves value based on header information. Input stream is expected to
     * be at position just after header!
     *
     * @param <T>
     * @param header
     * @param off
     * @param is
     * @return
     * @throws IOException
     */
    public static <T> T get(byte[] header, int off, InputStream is) throws IOException {
        LIS lis = (is instanceof LIS) ? (LIS) is : new LIS(is, -1);
        return get(header, off, lis);
    }

    /**
     * Retrieves value based on header information. Input stream is expected to
     * be at position just after header!
     *
     * NOTE: this method should be used for retrieval of primitive values only.
     * If value is constructed, it should be built from primitive values outside
     * of this method.
     *
     * @param <T>
     * @param header
     * @param off
     * @param is
     * @return
     * @throws IOException
     */
    public static <T> T get(byte[] header, int off, LIS is) throws IOException {
        T res = null;
        try {
            int type = getType(header, off);
            long len = getLength(header, off);
            int b = 0;
            switch ((int) type) {
                case CLASS_BOOLEAN: // 1,BOOLEAN
                    b = is.read();
                    if (b == -1) {
                        throw new IOException("EOF while fetching boolean value.");
                    }
                    res = (T) (Boolean) (b != 0);
                    break;
                case CLASS_INTEGER: // 2,INTEGER
                case CLASS_ENUMERATED: // 10,ENUMERATED
                {
                    byte[] buf = readFull((int) len, is, "EOF while reading int or enum.");
                    res = (T) new BigInteger(buf);
                }
                break;
                case CLASS_BIT_STRING: // 3,BIT STRING
                    if (INDEFINITE_LENGTH == len) {
                        throw new IOException("INDEFINITE_LENGTH must be handled prior to get.");
                    } else {
                        // single-object bit string
                        int b0 = is.read();
                        int skipBits = (b0 & 0x07);
                        if (skipBits > 0) {
                            skipBits = 8 - skipBits;
                        }
                        BitString bs = new BitString(((int) len - 1) * 8 - skipBits);

                        if (b0 == -1) {
                            throw new IOException("EOF while reading bits' skip bits.");
                        }
                        for (int i = 1; i < ((int) len); i++) {
                            int bi = is.read();
                            if (bi == -1) {
                                throw new IOException("EOF while reading bits.");
                            }
                            bs.set(i - 1, (byte) (bi & 0xFF));
                        }
                        res = (T) bs;
                    }
                    break;
                case CLASS_OCTET_STRING: // 4,OCTET STRING
                    if (INDEFINITE_LENGTH == len) {
                        throw new IOException("INDEFINITE_LENGTH must be handled prior to get.");
                    } else {
                        byte[] buf = readFull((int) len, is, "EOF while reading octet string of " + len + " bytes.");
                        res = (T) buf;
                    }
                    break;
                case CLASS_NULL: // 5,NULL
                    res = (T) NULL;
                    break;
                case CLASS_OBJECT_IDENTIFIER: // 6,OBJECT IDENTIFIER
                {
                    StringBuilder objId = new StringBuilder();
                    byte[] bytes = readFull((int) len, is, "EOF while reading identifier string of " + len + " bytes.");
                    long value = 0;
                    BigInteger bigValue = null;
                    boolean first = true;

                    for (int i = 0; i != bytes.length; i++) {
                        int bi = bytes[i] & 0xff;

                        if (value < 0x80000000000000L) {
                            //value = value * 128 + (bi & 0x7f);
                            value = (value << 7) | (bi & 0x7f);
                            if ((bi & 0x80) == 0) // end of number reached
                            {
                                if (first) {
                                    switch ((int) value / 40) {
                                        case 0:
                                            objId.append('0');
                                            break;
                                        case 1:
                                            objId.append('1');
                                            value -= 40;
                                            break;
                                        default:
                                            objId.append('2');
                                            value -= 80;
                                    }
                                    first = false;
                                }

                                objId.append('.');
                                objId.append(value);
                                value = 0;
                            }
                        } else {
                            if (bigValue == null) {
                                bigValue = BigInteger.valueOf(value);
                            }
                            bigValue = bigValue.shiftLeft(7);
                            bigValue = bigValue.or(BigInteger.valueOf(bi & 0x7f));
                            if ((bi & 0x80) == 0) {
                                objId.append('.');
                                objId.append(bigValue);
                                bigValue = null;
                                value = 0;
                            }
                        }
                    }
                    res = (T) objId.toString();
                }
                break;
                case CLASS_REAL: // 9,REAL
                {
                    byte[] d = readFull((int) len, is, "EOF while reading real number.");
                    if ((d[0] & 0x80) != 0) {
                        // Binary encoding: M = sign * n * 2^f
                        int sign = ((d[0] & 0x60) != 0) ? -1 : 1;
                        int b56 = (int) ((d[0] & 0xFF) >> 4) & 0x3;
                        int base = (b56 == 0) ? 2 : (b56 == 1) ? 8 : (b56 == 2) ? 16 : 0;
                        int f = (int) (d[0] & 0x3);
                        // TODO: eval exp and n -> m
                        int exp = 0;
                        int mLen = 0;
                        switch (f) {
                            case 0:
                                exp = (0xFF & is.read());
                                mLen = (int) len - 2;
                                break;
                            case 1:
                                exp = (0xFF & is.read()) << 8 | (0xFF & is.read());
                                mLen = (int) len - 3;
                                break;
                            case 2:
                                exp = (0xFF & is.read()) << 16 | (0xFF & is.read()) << 8 | (0xFF & is.read());
                                mLen = (int) len - 4;
                                break;
                            case 3:
                                int expLen = (0xFF & is.read());
                                mLen = (0xFF & is.read());
                                for (int i = 0; i < expLen; i++) {
                                    exp <<= 8;
                                    exp |= (0xFF & is.read());
                                }
                        }
                        long m = 0;
                        for (int i = 0; i < mLen; i++) {
                            m <<= 8;
                            m |= (0xFF & is.read());
                        }
                        Double dbl = Double.parseDouble(((sign < 0) ? "-" : "") + m + "e" + exp);
                        res = (T) dbl;
                    } else if ((d[0] & 0xC0) == 0x00) {
                        // Decimal encoding
                        int b61 = (int) (d[0] & 0x3F);
                        switch (b61) {
                            case 1:
                                // NR1 form
                                break;
                            case 2:
                                // NR2 form
                                break;
                            case 3:
                                // NR3 form
                                break;
                            default:
                                throw new IOException("Unrecognized REAL decimal format: 0x" + Integer.toHexString(b61));
                        }
                        String s = new String(readFull((int) len - 1, is, "EOF while reading string for decimal real of " + len + " bytes."), "UTF-8");
                        Double dbl = Double.parseDouble(s);
                        res = (T) dbl;
                    } else {
                        // SpecialRealValue
                        res = (T) (Double) (((d[0] & 1) == 1) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
                    }
                }
                break;
                case CLASS_TIME: // 23,time
                {
                    String s = new String(readFull((int) len, is, "EOF while reading string of " + len + " bytes."), "ISO-8859-1");
                    try {
                        if (s.endsWith("Z")) {
                            res = (T) time23formatZ.parse(s);
                        } else {
                            res = (T) time23format.parse(s);
                        }
                    } catch (Throwable th) {
                        int a = 0;
                        res = (T) s;
                    }
                }
                break;
                case CLASS_TIME2: // 24,universal time
                {
                    String s = new String(readFull((int) len, is, "EOF while reading string of " + len + " bytes."), "ISO-8859-1");
                    try {
                        if (s.endsWith("Z")) {
                            res = (T) time24formatB.parse(s);
                        } else if (s.length() > 16) {
                            res = (T) time24formatC.parse(s);
                        } else {
                            res = (T) time24formatA.parse(s);
                        }
                    } catch (Throwable th) {
                        int a = 0;
                        res = (T) s;
                    }
                }
                break;
                /////////////////// strings
                case CLASS_UTF8_STRING: // 12,UTF8String
                    res = (T) new String(readFull((int) len, is, "EOF while reading string of " + len + " bytes."), "UTF-8");
                    break;
                case CLASS_NUMERIC_STRING: // 18,NumericString
                    res = (T) toNumericString(readFull((int) len, is, "EOF while reading string of " + len + " bytes."));
                    break;
                case CLASS_PRINTABLE_STRING: // 19,PrintableString
                    res = (T) toPrintableString(readFull((int) len, is, "EOF while reading string of " + len + " bytes."));
                    break;
                case CLASS_TELETEX_STRING: // 20,TeletexString (T61String)
                case CLASS_VIDEOTEX_STRING: // 21,VideotexString
                case CLASS_VISIBLE_STRING: // 26,VisibleString (ISO646String)
                case CLASS_IA5_STRING: // 22,IA5String
                case CLASS_GRAPHIC_STRING: // 25,GraphicString (G sets + space)
                case CLASS_GRAPHIC_STRING2: // 27,GraphicString (C + G sets, space, delete)
                    res = (T) toUniversalString(readFull((int) len, is, "EOF while reading string of " + len + " bytes."));
                    break;
                case CLASS_SEQUENCE: // 16,SEQUENCE
                    throw new IOException("SEQUENCE must be handled prior to get.");
                default:
                    res = (T) readFull((int) len, is, "EOF while reading unrecognized type " + type + " as array of " + len + " bytes.");
            }
            //System.out.println("   get: " + type + "/" + len + "\n    = " + res);
            return res;
        } finally {
            is.popLimit();
        }
    }

    /**
     * Retrieves value based on header information. Input stream is expected to
     * be at position just after header!
     *
     * NOTE: this method should be used for retrieval of primitive values only.
     * If value is constructed, it should be built from primitive values outside
     * of this method.
     *
     * @param <T>
     * @param header
     * @param off
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] createValue(byte[] header, int off, Object value) throws IOException {
        byte[] res = null;
        try {
            int type = getType(header, off);
            long len = getLength(header, off);
            int b = 0;
            switch ((int) type) {
                case CLASS_BOOLEAN: // 1,BOOLEAN
                    boolean bv = (value != null && value instanceof Boolean)
                            ? (Boolean) value
                            : (value != null && value instanceof Number)
                                    ? ((Number) value).longValue() != 0
                                    : false;
                    res = (bv) ? new byte[]{-1} : new byte[]{0};
                    break;
                case CLASS_INTEGER: // 2,INTEGER
                case CLASS_ENUMERATED: // 10,ENUMERATED
                {
                    if (value instanceof BigInteger) {
                        res = ((BigInteger) value).toByteArray();
                    } else if (value instanceof Number) {
                        res = BigInteger.valueOf(((Number) value).longValue()).toByteArray();
                    } else if (value != null && value.getClass().isEnum()) {
                        int eov = Reflector.getEnumOrder(value);
                        res = BigInteger.valueOf(eov).toByteArray();
                    } else {
                        res = new byte[0];
                    }
                }
                break;
                case CLASS_BIT_STRING: // 3,BIT STRING
                {
                    BitString bs = (value instanceof BitString)
                            ? (BitString) value
                            : (value instanceof byte[])
                                    ? new BitString((byte[]) value, ((byte[]) value).length * 8)
                                    : null;
                    if (bs != null) {
                        res = bs.toBERBytes();
                    }
                }
                break;
                case CLASS_OCTET_STRING: // 4,OCTET STRING
                    res = (value instanceof byte[])
                            ? (byte[]) value
                            : (value instanceof String)
                                    ? ((String) value).getBytes("ISO-8859-1")
                                    : null;
                    break;
                case CLASS_NULL: // 5,NULL
                    res = new byte[0];
                    break;
                case CLASS_OBJECT_IDENTIFIER: // 6,OBJECT IDENTIFIER
                {
                    OID oid = (value instanceof OID)
                            ? (OID) value
                            : (value instanceof String)
                                    ? new OID((String) value)
                                    : (value instanceof byte[])
                                            ? new OID((byte[]) value)
                                            : null;
                    if (oid != null) {
                        res = oid.bytes;
                    }
                }
                break;
                case CLASS_REAL: // 9,REAL
                {
                    Double d = (value instanceof Number)
                            ? ((Number) value).doubleValue()
                            : null;
                    if (d != null) {
                        String s = d.toString();
                        if (s.contains("e") || s.contains("E")) {
                            // NR3
                            res = ((char) 3 + s).getBytes("ISO-8859-1");
                        } else if (s.contains(".")) {
                            // NR2
                            res = ((char) 2 + s).getBytes("ISO-8859-1");
                        } else {
                            // NR1
                            res = ((char) 1 + s).getBytes("ISO-8859-1");
                        }
                    }
                }
                break;
                case CLASS_TIME: // 23,time
                {
                    if (value instanceof Date) {
                        res = time23format.format((Date) value).getBytes("ISO-8859-1");
                    } else if (value instanceof Number) {
                        res = time23format.format(new Date(((Number) value).longValue())).getBytes("ISO-8859-1");
                    }
                }
                break;
                case CLASS_TIME2: // 24,universal time
                {
                    if (value instanceof Date) {
                        res = time24formatC.format((Date) value).getBytes("ISO-8859-1");
                    } else if (value instanceof Number) {
                        res = time24formatC.format(new Date(((Number) value).longValue())).getBytes("ISO-8859-1");
                    }
                }
                break;
                /////////////////// strings
                case CLASS_UTF8_STRING: // 12,UTF8String
                    if (value instanceof String) {
                        res = ((String) value).getBytes("UTF-8");
                    } else if (value instanceof byte[]) {
                        res = (byte[]) value;
                    }
                    break;
                case CLASS_NUMERIC_STRING: // 18,NumericString
                    if (value instanceof String) {
                        res = ((String) value).getBytes("ISO-8859-1");
                    } else if (value instanceof byte[]) {
                        res = (byte[]) value;
                    }
                    break;
                case CLASS_PRINTABLE_STRING: // 19,PrintableString
                    if (value instanceof String) {
                        res = ((String) value).getBytes("ISO-8859-1");
                    } else if (value instanceof byte[]) {
                        res = (byte[]) value;
                    }
                    break;
                case CLASS_TELETEX_STRING: // 20,TeletexString (T61String)
                case CLASS_VIDEOTEX_STRING: // 21,VideotexString
                case CLASS_VISIBLE_STRING: // 26,VisibleString (ISO646String)
                case CLASS_IA5_STRING: // 22,IA5String
                case CLASS_GRAPHIC_STRING: // 25,GraphicString (G sets + space)
                case CLASS_GRAPHIC_STRING2: // 27,GraphicString (C + G sets, space, delete)
                    if (value instanceof String) {
                        res = ((String) value).getBytes("ISO-8859-1");
                    } else if (value instanceof byte[]) {
                        res = (byte[]) value;
                    }
                    break;
                case CLASS_SEQUENCE: // 16,SEQUENCE
                    throw new IOException("SEQUENCE must be handled prior to get.");
                default:
                    if (value instanceof String) {
                        res = ((String) value).getBytes("ISO-8859-1");
                    } else if (value instanceof byte[]) {
                        res = (byte[]) value;
                    }
            }
            //System.out.println("   get: " + type + "/" + len + "\n    = " + res);
            return res;
        } finally {
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////// utilities
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Reflection-based int to string conversion. Uses static int field names
     * with removed "CLASS_" prefix.
     *
     * @param type
     * @return
     */
    public static String type2name(int type) {
        if (typeNames == null) {
            typeNames = new HashMap<Integer, Field>();
            for (Field f : BER.class.getFields()) {
                if (Modifier.isFinal(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers())
                        && f.getType() == int.class
                        && f.getName().startsWith("CLASS_")) {
                    try {
                        typeNames.put(f.getInt(null), f);
                    } catch (Throwable th) {
                        int a = 0;
                    }
                }
            }
        }
        Field f = typeNames.get(type);
        if (f != null) {
            return f.getName().substring(6);
        } else {
            return "{" + type + "}";
        }
    }
    private static Map<Integer, Field> typeNames;

    /**
     * Hardcoded converter of int tag to string.
     *
     * @param tag
     * @return
     */
    public static String tag2name(int tag) {
        switch (tag) {
            case TAG_UNIVERSAL:
                return "TAG_UNIVERSAL";
            case TAG_APPLICATION:
                return "TAG_APPLICATION";
            case TAG_CONTEXT_SPECIFIC:
                return "TAG_CONTEXT_SPECIFIC";
            case TAG_PRIVATE:
                return "TAG_PRIVATE";
            default:
                return "UNDEFINED[" + tag + "]";
        }
    }

    /**
     * Method to read specified # of bytes from input stream and throw
     * IOException with given message if EOF or just an error.
     *
     * @param len
     * @param is
     * @param errorMessage
     * @return
     * @throws IOException
     */
    static byte[] readFull(int len, InputStream is, String errorMessage) throws IOException {
        return readFull(new byte[len], 0, len, is, errorMessage);
    }

    /**
     * Reads specified # of bytes into byte buffer at specified offset. Ensures
     * all len bytes are read (e.g. if underlying input stream returns only part
     * of bytes, additional read operation(s) are executed to fetch missing
     * part.)
     *
     * @param buf
     * @param off
     * @param len
     * @param is
     * @param errorMessage
     * @return
     * @throws IOException
     */
    static byte[] readFull(byte[] buf, int off, int len, InputStream is, String errorMessage) throws IOException {
        int b = is.read(buf, off, len);
        if (b == -1) {
            throw new IOException(errorMessage);
        }
        while (b < len) {
            int c = is.read(buf, b + off, (int) len - b);
            if (c == -1) {
                throw new IOException(errorMessage);
            }
            b += c;
        }
        return buf;
    }

    /**
     * Returns length of type in BER entry header.
     *
     * @param buf
     * @param off
     * @return
     */
    public static int getTypeLength(byte[] buf, int off) {
        int r = 0;
        byte b = buf[0 + off];

        if ((b & 0x1F) == 0x1F) {
            // 8.1.2.4
            while (true) {
                r++;
                off++;
                if ((b & 0x80) == 0) {
                    break;
                }
            }
        } else {
            // 8.1.2.3
        }
        return r;
    }

    /**
     * Returns length of BER entry header. Evaluates it based on length byte and
     * taking into account type length.
     *
     * @param buf
     * @param off
     * @return
     */
    public static int getHeaderLength(byte[] buf, int off) {
        int r = getTypeLength(buf, off);
        byte b = buf[1 + r + off];
        r += 2;

        if ((b & 0x80) == 0) {
            // 8.1.3.4
            return r;
        } else if (b == 0x80) {
            // 8.1.3.6 - indefinite length
            return r;
        } else {
            // 8.1.3.5
            int lBytes = b & 0x7F;
            return r + lBytes;
        }
    }

    ////////////////////////////////////////////////////////////////
    public static String toNumericString(byte[] data) throws IOException {
        if (data == null) {
            return null;
        }
        // verify
        for (byte b : data) {
            switch (b) {
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
                case ' ':
                    break;
                default:
                    throw new IOException("Invalid numeric character: 0x" + Integer.toHexString(0xFF & b) + ". Allowed ar3e '0'..'9',' '.");
            }
        }
        return new String(data, "ISO-8859-1");
    }

    public static String toPrintableString(byte[] data) throws IOException {
        if (data == null) {
            return null;
        }
        // verify
        for (byte b : data) {
            switch (b) {
                case ' ':
                case '\'':
                case '(':
                case ')':
                case '+':
                case ',':
                case '-':
                case '.':
                case '/':
                case ':':
                case '=':
                case '?':
                    break;
                default:
                    if ((b >= '0' && b <= '9')
                            || (b >= 'A' && b <= 'Z')
                            || (b >= 'a' && b <= 'z')) {
                        // valid subsets
                    } else {
                        throw new IOException("Invalid numeric character: 0x" + Integer.toHexString(0xFF & b) + ". Allowed ar3e '0'..'9',' '.");
                    }
            }
        }
        return new String(data, "ISO-8859-1");
    }

    public static String toUniversalString(byte[] data) throws IOException {
        if (data == null) {
            return null;
        }
        return new String(data, "ISO-8859-1");
    }

    /**
     * Represents BER bitstring to avoid loss of info.
     */
    public static class BitString {

        int bitLen;
        int skipBits;
        byte[] data;

        public BitString(int bitLen) {
            this.bitLen = bitLen;
            skipBits = bitLen % 8;
            if (skipBits > 0) {
                skipBits = 8 - skipBits;
                int sb = 0xFF;
                for (int i = 0; i < skipBits; i++) {
                    sb ^= (1 << i);
                }
                skipBits = sb;
            }
            data = new byte[bitLen / 8 + ((skipBits > 0) ? 1 : 0)];
        }

        public BitString(byte[] buf, int bitLen) {
            this.bitLen = bitLen;
            skipBits = bitLen % 8;
            if (skipBits > 0) {
                skipBits = 8 - skipBits;
                int sb = 0xFF;
                for (int i = 0; i < skipBits; i++) {
                    sb ^= (1 << i);
                }
                skipBits = sb;
            }
            int dl = bitLen / 8 + ((skipBits > 0) ? 1 : 0);
            data = Arrays.copyOf(data, dl);
        }

        public int bitLen() {
            return bitLen;
        }

        public int skipBits() {
            return skipBits;
        }

        public int bytes() {
            return data.length;
        }

        public void set(int off, byte bits) {
            if (skipBits > 0 && off == data.length - 1) {
                data[off] = (byte) (bits & skipBits);
            } else {
                data[off] = bits;
            }
        }

        public void set(int bit, boolean set) {
            if (bit < 0 || bit >= bitLen) {
                return;
            }
            int off = bit / 8;
            int shift = 8 - bit % 8;
            if (true) {
                data[off] |= (1 << shift);
            } else if (get(bit)) {
                data[off] ^= (1 << shift);
            }
        }

        public Boolean get(int bit) {
            if (bit < 0 || bit >= bitLen) {
                return null;
            }
            int off = bit / 8;
            int shift = 8 - bit % 8;
            return (data[off] & (1 << shift)) == 1;
        }

        public byte[] ddata() {
            return data;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("BitString{" + bitLen + ((skipBits > 0) ? "/0x" + Integer.toHexString(skipBits) : "") + ": ");
            if (data.length > 0) {
                sb.append("0x");
            }
            for (int i = 0; i < data.length - 1; i++) {
                sb.append(Integer.toHexString((0xFF & data[i])));
            }
            if (data.length > 0) {
                if (skipBits > 0) {
                    sb.append(Integer.toHexString((skipBits & data[data.length - 1])));
                } else {
                    sb.append(Integer.toHexString((0xFF & data[data.length - 1])));
                }
            }
            sb.append("}");
            return sb.toString();
        }

        public byte[] toBERBytes() {
            byte[] r = new byte[data.length + 1];
            int skip = bitLen % 8;
            if (skip > 0) {
                skip = 8 - skip;
            }
            r[0] = (byte) (0x07 & skip);
            for (int i = 0; i < data.length; i++) {
                r[i + 1] = data[i];
            }
            return r;
        }
    }

    /**
     * Represents BER OID encoded value with parse/encode functionality.
     */
    public static class OID {

        byte[] bytes;

        public OID() {
        }

        public OID(byte[] buf) {
            bytes = buf;
        }

        public OID(byte[] buf, int off, int len) {
            bytes = Arrays.copyOfRange(buf, off, off + len);
        }

        public OID(String text) {
            // ensure enough space for bytes...
            bytes = new byte[text.length()];
            int pos = 0;
            int l = ("" + Long.MAX_VALUE).length();
            String[] ss = text.split("\\.");
            BigInteger bim = BigInteger.valueOf(0x7f);
            // 2 first items... -> byte[0]
            {
                // X*40+Y 
                int x = Integer.parseInt(ss[0]);
                int y = Integer.parseInt(ss[1]);
                bytes[pos++] = (byte) (x * 40 + y);
            }
            for (int is = 2; is < ss.length; is++) {
                String s = ss[is];
                if (1 == 1 || s.length() > l) {
                    BigInteger bi = new BigInteger(s);
                    int bl = bi.bitLength();
                    bl = bl / 7 + (((bl % 7) > 0) ? 1 : 0);
                    for (int i = bl - 1; i >= 0; i--) {
                        bytes[pos + i] = (byte) (0x80 | bi.and(bim).byteValue());
                        bi = bi.shiftRight(7);
                    }
                    pos += bl;
                    bytes[pos - 1] &= 0x7F;
                } else {
                    long lv = Long.parseLong(s);
                    int bpos = 64;
                    while (bpos > 0 && (lv & (1 << bpos)) == 0) {
                        bpos--;
                    }
                    while (lv > 0) {
                        bytes[pos++] = (byte) (0x80 | (byte) (lv & 0x7F));
                        lv >>= 7;
                    }
                    bytes[pos - 1] &= 0x7F;
                }
            }
            bytes = Arrays.copyOf(bytes, pos);
        }

        public String toString() {
            StringBuilder objId = new StringBuilder();
            long value = 0;
            BigInteger bigValue = null;
            boolean first = true;

            for (int i = 0; i != bytes.length; i++) {
                int bi = bytes[i] & 0xff;

                if (value < 0x80000000000000L) {
                    //value = value * 128 + (bi & 0x7f);
                    value = (value << 7) | (bi & 0x7f);
                    if ((bi & 0x80) == 0) // end of number reached
                    {
                        if (first) {
                            switch ((int) value / 40) {
                                case 0:
                                    objId.append('0');
                                    break;
                                case 1:
                                    objId.append('1');
                                    value -= 40;
                                    break;
                                default:
                                    objId.append('2');
                                    value -= 80;
                            }
                            first = false;
                        }

                        objId.append('.');
                        objId.append(value);
                        value = 0;
                    }
                } else {
                    if (bigValue == null) {
                        bigValue = BigInteger.valueOf(value);
                    }
                    bigValue = bigValue.shiftLeft(7);
                    bigValue = bigValue.or(BigInteger.valueOf(bi & 0x7f));
                    if ((bi & 0x80) == 0) {
                        objId.append('.');
                        objId.append(bigValue);
                        bigValue = null;
                        value = 0;
                    }
                }
            }
            return objId.toString();
        }
    }

    /**
     * Limiting input stream: use read limit to indicate EOF. Use push/pop limit
     * for local limitations.
     */
    public static class LIS extends InputStream {

        InputStream is;
        Stack<Long> stack = new Stack<Long>();
        long pos = 0;

        public LIS(InputStream is, long limit) {
            this.is = is;
            if (limit != -1) {
                stack.push(limit);
            }
        }

        public LIS pushLimit(long count) throws IOException {
            long nextCount = pos + count;
            if (stack.isEmpty()) {
                if (nextCount > 0) {
                    stack.push(nextCount);
                }
            } else if (nextCount <= stack.peek()) {
                stack.push(nextCount);
            } else {
                throw new IOException("Limit exceed upper limit: " + stack.peek() + " < " + nextCount + " (for size=" + count + ")");
            }
            return this;
        }

        public void popLimit() {
            long last = -1;
            if (!stack.isEmpty()) {
                last = stack.pop();
            }
        }

        @Override
        public int read() throws IOException {
            if (stack.isEmpty()) {
                int r = is.read();
                if (r == -1) {
                    return r;
                }
                pos++;
                return r;
            } else if (pos < stack.peek()) {
                int r = is.read();
                if (r == -1) {
                    return r;
                }
                pos++;
                return r;
            } else {
                return -1;
            }
        }

    }

    //////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// object-oriented read/write support
    //////////////////////////////////////////////////////////////////////////
    public static class BEREntry {

        byte[] header;
        byte[] data;
        List<BEREntry> constructed;

        public BEREntry(int tag, int type, byte[] data) {
            header = new byte[1 + 5 + 5];
            int dl = createHeader(header, 0, tag, true, type, data.length);
            if (dl < header.length) {
                header = Arrays.copyOf(header, dl);
            }
            this.data = data;
        }

        public BEREntry(Object obj, BERConstructorContext ctx)
                throws IOException {
            if (ctx == null) {
                ctx = new BERConstructorContext();
            }
            int tag = ctx.tagFor(obj);
            boolean primitive = ctx.primitiveFor(obj);
            int type = ctx.typeFor(obj);
            header = new byte[15];
            int hl = createHeader(header, 0, tag, primitive, type);
            if (primitive) {
                byte[] dd = ctx.toPayload(header, 0, obj);
                setLength(header, 0, dd.length);
            } else {
                long cl = 0;
                List objs = ctx.toConstruct(obj);
                if (objs != null) {
                    constructed = new ArrayList<BEREntry>();
                    for (Object o : objs) {
                        BEREntry child = new BEREntry(o, ctx);
                        if (child != null) {
                            constructed.add(child);
                            if (cl != INDEFINITE_LENGTH) {
                                long es = getEntrySize(header, 0);
                                if (es < 0) {
                                    cl = INDEFINITE_LENGTH;
                                } else {
                                    cl += es;
                                }
                            }
                        }
                    }
                }
                setLength(header, 0, cl);
            }

            hl = getHeaderLength(header, 0);
            if (hl < header.length) {
                header = Arrays.copyOf(header, hl);
            }
        }

        public BEREntry(InputStream is) throws IOException {
            header = readHeader(is);
            //System.out.println("\nDEBUG START: " + this);
            if (header == null) {
                header=new byte[]{0};
                return;
            }
            //System.out.println("\n     header: " + Dump.dump(this.header, false, true));
            long dl = getDataLength();
            if (isPrimitive()) {
                data = new byte[(int) dl];
                readFull(data, 0, data.length, is, "EOF while reading primitive value for " + dumpHeader(header, 0));
            } else {
                constructed = new ArrayList<BEREntry>();
                if (dl == INDEFINITE_LENGTH) {
                    BEREntry be = new BEREntry(is);
                    while (be.getType() != CLASS_EOC) {
                        constructed.add(be);
                        be = new BEREntry(is);
                    }
                } else {
                    LIS lis = new LIS(is, dl);
                    BEREntry be = new BEREntry(lis);
                    while (be.isValidEntry()) {
                        constructed.add(be);
                        be = new BEREntry(lis);
                    }
                }
            }
            //System.out.println("\nDEBUG END  : " + this);
        }

        public int getTag() {
            return BER.getTag(header, 0);
        }

        public boolean isPrimitive() {
            return BER.isPrimitive(header, 0);
        }

        public int getType() {
            return BER.getType(header, 0);
        }

        public long getDataLength() {
            return BER.getLength(header, 0);
        }

        public long getSize() {
            return getEntrySize(header, 0);
        }

        public <T> T getValue() throws IOException {
            return (data == null)
                    ? (T) NULL
                    : (T) BER.get(header, 0, new ByteArrayInputStream(data));
        }

        public <T> T exportValue() throws IOException {
            if (isPrimitive()) {
                Object v = getValue();
                if (v == NULL) {
                    return null;
                } else {
                    return (T) v;
                }
            } else {
                List l = new ArrayList();
                for (BEREntry be : constructed) {
                    l.add(be.exportValue());
                }
                return (T) l;
            }
        }

        public long write(OutputStream os) throws IOException {
            long c = header.length;
            os.write(header);
            if (data != null && data.length > 0) {
                os.write(data);
                c += data.length;
            } else if (constructed != null && !constructed.isEmpty()) {
                for (BEREntry be : constructed) {
                    c += be.write(os);
                }
            }
            if (BER.getLength(header, 0) == INDEFINITE_LENGTH) {
                os.write(EOC);
                c += EOC.length;
            }
            return c;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("BEREntry: ");
            sb.append(dumpHeader(header, 0));
            if (header == null) {
                return sb.toString();
            }
            if (isPrimitive()) {
                try {
                    Object v = getValue();
                    sb.append("\n  " + ("" + ((v == null) ? "null" : Dump.dump(v, true, true).toString()).replace("\n", "\n  ")));
                } catch (Throwable th) {
                    sb.append("\n  ERROR: " + th.toString().replace("\n", "\n  "));
                }
            } else if (constructed != null) {
                sb.append("\n  constructed=" + constructed.size());
                for (BEREntry be : constructed) {
                    sb.append("\n    " + be.toString().replace("\n", "\n    "));
                }
            }
            return sb.toString();
        }
        
        public boolean isValidEntry() {
            return header!=null && header.length>1;
        }
    }

    public static class BERConstructorContext {

        public int tagFor(Object value) {
            return TAG_UNIVERSAL;
        }

        public int typeFor(Object value) {
            if (value == null || value == NULL) {
                return CLASS_NULL;
            } else if (value instanceof Date) {
                return CLASS_TIME2;
            } else if (value instanceof Boolean) {
                return CLASS_BOOLEAN;
            } else if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
                return CLASS_REAL;
            } else if (value instanceof Number || value.getClass().isEnum()) {
                return CLASS_INTEGER;
            } else if (value instanceof Collection || value.getClass().isArray()) {
                return CLASS_SEQUENCE;
            } else if (value instanceof Map) {
                return CLASS_SET;
            } else if (value instanceof BitString) {
                return CLASS_BIT_STRING;
            } else if (value instanceof String) {
                return CLASS_UTF8_STRING;
            } else {
                return CLASS_OCTET_STRING;
            }
        }

        public boolean primitiveFor(Object value) {
            int type = typeFor(value);
            return !(type == CLASS_SEQUENCE || type == CLASS_SET);
        }

        /**
         * Returns list of constructed values for the value.
         *
         * @param value
         * @return
         */
        public List toConstruct(Object value) throws IOException {
            if (primitiveFor(value)) {
                return null;
            }
            List l = new ArrayList();
            if (value instanceof Collection) {
                l.addAll((Collection) value);
            } else if (value != null && value.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(value); i++) {
                    l.add(Array.get(value, i));
                }
            } else if (value instanceof Map) {
                for (Entry e : ((Map<Object, Object>) value).entrySet()) {
                    l.add(new Object[]{e.getKey(), e.getValue()});
                }
            }
            return l;
        }

        public byte[] toPayload(byte[] header, int off, Object value) throws IOException {
            return createValue(header, off, value);
        }
    }
}
