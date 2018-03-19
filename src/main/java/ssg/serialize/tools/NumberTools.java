/*
 * AS IS
 */
package ssg.serialize.tools;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author 000ssg
 */
public class NumberTools {

    /**
     * Returns true if obj is a number or is numeric class (including
     * primitives)
     *
     * @param obj
     * @return
     */
    public static boolean isNumeric(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Number) {
            return true;
        }
        if (obj instanceof Class) {
            Class cl = (Class) obj;
            if (Number.class.isAssignableFrom(cl)) {
                return true;
            }
            if (cl.isPrimitive() && (byte.class == cl
                    || byte.class == cl
                    || short.class == cl
                    || int.class == cl
                    || long.class == cl
                    || float.class == cl
                    || double.class == cl)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs numeric type casting (if needed) to requested one.
     *
     * If requested type is primitive and null number is passed then 0 value is
     * ensured.
     *
     * @param num
     * @param numberType
     * @return
     */
    public static Number cast(Number num, Class numberType) {
        if (numberType == null) {
            return num;
        }

        if (numberType.isPrimitive() && num == null) {
            num = 0;
        }

        if (num == null) {
            return null;
        }

        if (num.getClass() == numberType) {
            return num;
        }

        {//if (isNumeric(numberType)) {
            if (byte.class == numberType || Byte.class == numberType) {
                return num.byteValue();
            } else if (short.class == numberType || Short.class == numberType) {
                return num.shortValue();
            } else if (int.class == numberType || Integer.class == numberType) {
                return num.intValue();
            } else if (long.class == numberType || Long.class == numberType) {
                return num.longValue();
            } else if (float.class == numberType || Float.class == numberType) {
                return num.floatValue();
            } else if (double.class == numberType || Double.class == numberType) {
                return num.doubleValue();
            } else if (BigDecimal.class == numberType) {
                if (num.getClass() == float.class || num instanceof Float) {
                    return BigDecimal.valueOf(num.floatValue());
                } else if (num.getClass() == double.class || num instanceof Double) {
                    return BigDecimal.valueOf(num.doubleValue());
                } else {
                    return BigDecimal.valueOf(num.longValue());
                }
            } else if (BigInteger.class == numberType) {
                return BigInteger.valueOf(num.longValue());
            }
        }
        return null;
    }

    /**
     * Convert string to number converting result to desired type if numberType
     * is specified.
     *
     * @param s
     * @param numberType
     * @return
     */
    public static Number parse(String s, Integer radix, Class numberType) {
        Number num = null;
        if (s == null) {
            return cast(null, numberType);
        }
        s = s.toUpperCase();
        try {
            if (numberType == BigDecimal.class) {
                return new BigDecimal(s);
            } else if (numberType == BigInteger.class) {
                return new BigInteger(s);
            }
            if (radix != null) {
                // try as long
                if (s.startsWith("0X")) {
                    s = s.substring(2);
                }
                return cast(Long.parseLong(s, radix), numberType);
            } else if (s.contains(".")) {
                return cast(Double.parseDouble(s), numberType);
            } else {
                try {
                    return cast(Double.parseDouble(s), numberType);
                } catch (Throwable th) {
                    try {
                        return cast(Long.parseLong(s, 10), numberType);
                    } catch (Throwable th1) {
                        return cast(Long.parseLong(s, 16), numberType);
                    }
                }
            }
        } catch (Throwable th) {
            return cast(null, numberType);
        }
    }
}
