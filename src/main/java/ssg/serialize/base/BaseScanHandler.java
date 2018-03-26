/*
 * AS IS
 */
package ssg.serialize.base;

import ssg.serialize.ObjectSerializer.OSScanHandler;
import ssg.serialize.tools.Decycle.DecycledList;
import ssg.serialize.tools.Decycle.DecycledMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 *
 * @author 000ssg
 */
public abstract class BaseScanHandler<T, R> implements OSScanHandler<T, R> {

    public static final Object SCALAR = new Object();
    Stack stack = new Stack();
    Stack<R> refStack = new Stack<R>();
    Map<R, Object> referrables = new HashMap<R, Object>();
    // chars
    StringBuilder sb;
    char[] cbuf = new char[1024];
    int cbufPos = 0;
    // bytes
    List<byte[]> bytes;
    byte[] buf = new byte[1024];
    int bufPos = 0;

    @Override
    public <Z> Z root() {
        return (Z) ((!stack.isEmpty()) ? stack.get(0) : null);
    }

    @Override
    public void onStart() {
        stack.clear();
        refStack.clear();
        referrables.clear();
        sb = null;
        cbufPos = 0;
        //
        bytes = null;
        bufPos = 0;
    }

    @Override
    public void onEnd(Object value) {
        Object root = (!stack.isEmpty()) ? stack.get(0) : null;
        if (value != null) {
            root = value;
        }
        stack.clear();
        refStack.clear();
        referrables.clear();
        stack.push(root);
        sb = null;
        cbufPos = 0;
        bytes = null;
        bufPos = 0;
    }

    @Override
    public void onOpen(T type, boolean referrable) {
        Object o = null;
        if (isObject(type)) {
            o = new DecycledMap();
        } else if (isCollection(type)) {
            o = new DecycledList();
        } else {
            o = SCALAR;
        }
        stack.push(o);
        // no values are relevant between types, only within atomic open/close area
        onDropped(sb, cbuf, cbufPos);
        sb = null;
        cbufPos = 0;
        bytes = null;
        bufPos = 0;
        if (referrable) {
            R r = createReference(type, o);
            //System.out.println("  REFERENCE("+r+") for "+o.getClass().getSimpleName());
            refStack.push(r);
            if (r != null) {
                referrables.put(r, o);
            }
        } else {
            refStack.push(null);
        }
    }

    @Override
    public void onClose(T type) {
        onClose(type, null, null);
    }

    @Override
    public void onClose(T type, R ref) {
        onClose(type, ref, null);
    }

    public void onClose(T type, R ref, Object value) {
        //System.out.println("  CLOSE("+type+", "+((ref!=null) ? " (R="+ref+")":"")+") ? "+value);
        Object v = stack.pop();
        R r = refStack.pop();
        if (v instanceof Map || v instanceof Collection) {
            // object/collection -> no pushed value
        } else {// if (v == SCALAR) {
            if (ref == null) {
                if (value == null) {
                    v = adjust(pushedValue(), type);
                } else {
                    v = value;
                }
            } else {
                v = referrables.get(ref);
            }
            if (r != null) {
                referrables.put(r, v);
            }
        }
        if (stack.isEmpty()) {
            stack.push(v);
        } else {
            Object p = stack.peek();
            if (p instanceof Map) {
                // create new Entry...
                MapKey key = new MapKey();
                key.a = v;
                stack.push(key);
                refStack.push(null);
                //System.out.println("    MKEY("+type+", "+((ref!=null) ? " (R="+ref+")":"")+") ? "+v);
            } else if (p instanceof MapKey) {
                MapKey key = (MapKey) stack.pop();
                refStack.pop();
                Map m = (Map) stack.peek();
                m.put(key.a, v);
                //System.out.println("    MVALUE("+type+", "+((ref!=null) ? " (R="+ref+")":"")+") ? "+v);
            } else if (p instanceof Collection) {
                ((Collection) p).add(v);
                //System.out.println("    CVALUE("+type+", "+((ref!=null) ? " (R="+ref+")":"")+") ? "+v);
            }
        }
        sb = null;
        cbufPos = 0;
        bytes = null;
        bufPos = 0;
    }

    @Override
    public void onScalar(T type, Object value, boolean referrable) {
        onOpen(type, referrable);
        onClose(type, null, value);
        sb = null;
        cbufPos = 0;
    }

    @Override
    public void pushChar(char... chs
    ) {
        for (char ch : chs) {
            cbuf[cbufPos++] = ch;
            if (cbufPos == cbuf.length) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(cbuf, 0, cbufPos);
                cbufPos = 0;
            }
        }
    }

    @Override
    public void pushByte(byte... bs
    ) {
        for (byte b : bs) {
            buf[bufPos++] = b;
            if (bufPos == buf.length) {
                if (bytes == null) {
                    bytes = new ArrayList<byte[]>();
                }
                bytes.add(Arrays.copyOf(buf, bufPos));
                bufPos = 0;
            }
        }
    }

    public Object pushedValue() {
        if (sb != null) {
            if (cbufPos > 0) {
                sb.append(cbuf, 0, cbufPos);
                cbufPos = 0;
            }
            return sb.toString();
        } else if (cbufPos > 0) {
            return new String(cbuf, 0, cbufPos);
        } else if (bytes != null) {
            int l = 0;
            for (byte[] bb : bytes) {
                l += bb.length;
            }
            byte[] v = new byte[l];
            int off = 0;
            for (byte[] bb : bytes) {
                System.arraycopy(bb, 0, v, off, bb.length);
                off += bb.length;
            }
            return v;
        } else if (bufPos > 0) {
            return Arrays.copyOf(buf, bufPos);
        } else {
            return null;
        }
    }

    /**
     * call.back when dropping any data outside scalars
     *
     * @param sb
     * @param buf
     * @param len
     */
    public void onDropped(StringBuilder sb, char[] buf, int len) {
    }

    /**
     * Return true if type represents object -> create map
     *
     * @param type
     * @return
     */
    public abstract boolean isObject(T type);

    /**
     * Return true if type represents collection -> create List
     *
     * @param type
     * @return
     */
    public abstract boolean isCollection(T type);

    /**
     * Convert value to desired type (i.e. scalar value casting)
     *
     * @param value
     * @param type
     * @return
     */
    public abstract Object adjust(Object value, T type);

    /**
     * Returns reference for type/object.
     *
     * @param type
     * @param value
     * @return
     */
    public abstract R createReference(T type, Object value);

    static class MapKey {

        Object a;
    }
}
