/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import ssg.serialize.tools.ClassCast;

/**
 * Reflective cast caster: uses reflection to initialize caster from/to classes
 * info from its generic declaration to avoid extra coding.
 *
 * @author 000ssg
 */
public abstract class RCC<F, T> implements ClassCast<F, T> {

    int priority = 100;
    Class from;
    Class to;

    public RCC() {
    }

    public RCC(int priority) {
        this.priority = priority;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Class from() {
        if (to == null) {
            init();
        }
        return from;
    }

    @Override
    public Class to() {
        if (to == null) {
            init();
        }
        return to;
    }

    @Override
    public T tryCast(F obj, IOException[] error) {
        try {
            return cast(obj);
        } catch (Throwable th) {
            return null;
        }
    }

    void init() {
        Class cl = getClass();
        Type gt = cl.getGenericSuperclass();
        ParameterizedType pt = (ParameterizedType) gt;
        Type[] ats = pt.getActualTypeArguments();
        if (ats.length > 1) {
            if (ats[0] instanceof Class) {
                from = (Class) ats[0];
            }
            if (ats[1] instanceof Class) {
                to = (Class) ats[1];
            }
        }
    }

}
