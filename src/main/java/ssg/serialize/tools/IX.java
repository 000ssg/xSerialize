/*
 * AS IS
 */
package ssg.serialize.tools;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Size/get accessor to collection or array
 */
public class IX {

    List c;
    Object arr;

    public IX(Object o) {
        if (o instanceof List) {
            c = (List) o;
        } else if (o instanceof Collection) {
            c = new ArrayList();
            c.addAll((Collection) o);
        } else if (o != null && o.getClass().isArray()) {
            arr = o;
        }
    }

    public int size() {
        return (c != null) ? c.size() : (arr != null) ? Array.getLength(arr) : -1;
    }

    public Object item(int idx) {
        return (c != null) ? c.get(idx) : Array.get(arr, idx);
    }
    
}
