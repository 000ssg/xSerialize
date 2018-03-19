/*
 * AS IS
 */
package ssg.serialize.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author 000ssg
 */
public class ClassCaster {

    // Additional (optional) class casting support.
    Map<Class, List<ClassCast>> classCasters = new HashMap<Class, List<ClassCast>>();
    Map<Class, Map<Class, List<ClassCast>>> class2class = new HashMap<Class, Map<Class, List<ClassCast>>>();

    public Class adjust(Class cl) {
        if (cl != null && cl.isPrimitive()) {
            if (cl == byte.class) {
                cl = Byte.class;
            } else if (cl == short.class) {
                cl = Short.class;
            } else if (cl == int.class) {
                cl = Integer.class;
            } else if (cl == long.class) {
                cl = Long.class;
            } else if (cl == float.class) {
                cl = Float.class;
            } else if (cl == double.class) {
                cl = Double.class;
            } else if (cl == char.class) {
                cl = Character.class;
            }
        }
        return cl;
    }

    public boolean supports(Class from, Class to) {
        to=adjust(to);
        if (to == null) {
            return false;
        }
        if (from == null) {
            List<ClassCast> l = classCasters.get(to);
            if (l != null) {
                return true;
            } else {
                return false;
            }
        } else {
            Map<Class, List<ClassCast>> m = class2class.get(to);
            if (m != null) {
                List<ClassCast> l = m.get(from);
                if (l != null) {
                    return true;
                }
            }
            return false;
        }
    }

    public Collection<ClassCast> getClassCasts(Class from, Class to) {
        to=adjust(to);
        if (to == null) {
            return null;
        }
        if (from == null) {
            List<ClassCast> l = classCasters.get(to);
            if (l != null) {
                return Collections.unmodifiableList(l);
            } else {
                return null;
            }
        } else {
            Map<Class, List<ClassCast>> m = class2class.get(to);
            if (m != null) {
                List<ClassCast> l = m.get(from);
                if (l != null) {
                    return Collections.unmodifiableList(l);
                }
            }
            return null;
        }
    }

    public synchronized void addClassCasts(ClassCast... ccs) {
        if (ccs != null) {
            for (ClassCast cc : ccs) {
                Class to=adjust(cc.to());
                if (cc == null || to == null) {
                    continue;
                }
                List<ClassCast> l = classCasters.get(to);
                if (l == null) {
                    l = new ArrayList<ClassCast>();
                    classCasters.put(to, l);
                }
                if (!l.contains(cc)) {
                    l.add(cc);
                    Map<Class, List<ClassCast>> c2c = class2class.get(to);
                    if (c2c == null) {
                        c2c = new HashMap<Class, List<ClassCast>>();
                        class2class.put(to, c2c);
                    }
                    if (cc.from() == null) {

                    } else {
                        List<ClassCast> lcs = c2c.get(cc.from());
                        if (lcs == null) {
                            lcs = new ArrayList<ClassCast>();
                            c2c.put(cc.from(), lcs);
                        }
                        if (!lcs.contains(cc)) {
                            lcs.add(cc);
                            Collections.sort(lcs, new Comparator() {
                                @Override
                                public int compare(Object o1, Object o2) {
                                    Integer i1 = ((ClassCast) o1).priority();
                                    return i1.compareTo(((ClassCast) o2).priority());
                                }
                            });
                        }
                    }

                }
            }
        }
    }

    public synchronized void removeClassCasts(ClassCast... ccs) {
        if (ccs != null) {
            for (ClassCast cc : ccs) {
                Class to=adjust(cc.to());
                if (cc == null || to == null) {
                    continue;
                }
                List<ClassCast> l = classCasters.get(to);
                if (l == null || l.isEmpty()) {
                    continue;
                }
                if (l.contains(cc)) {
                    l.remove(cc);
                }
                //
                Map<Class, List<ClassCast>> c2c = class2class.get(to);
                if (c2c != null) {
                    l = c2c.get(cc.from());
                    if (l != null && l.contains(cc)) {
                        l.remove(cc);
                        if (l.isEmpty()) {
                            c2c.remove(cc.from());
                            if (c2c.isEmpty()) {
                                class2class.remove(to);
                                c2c = null;
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized void removeClassCasts(Class... cs) {
        if (cs != null) {
            for (Class cl : cs) {
                if (cl == null) {
                    continue;
                }
                if (classCasters.containsKey(cl)) {
                    classCasters.remove(cl);
                }
                if (class2class.containsKey(cl)) {
                    class2class.remove(cl);
                }
            }
        }
    }

    public <T> T cast(Object obj, Class to) throws IOException {
        if (obj == null || to == null) {
            return null;
        }
        Collection<ClassCast> ccs = getClassCasts(obj.getClass(), to);
        if (ccs == null || ccs.isEmpty()) {
            return null;
        }
        IOException[] error = new IOException[1];
        for (ClassCast cc : ccs) {
            Object r = cc.tryCast(obj, error);
            if (r != null) {
                return (T) r;
            }
        }
        if (error[0] != null) {
            throw error[0];
        }
        return null;
    }

    public <T> T tryCast(Object obj, Class to) {
        try {
            return cast(obj, to);
        } catch (Throwable th) {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClassCaster{" + "classCasters=" + classCasters.size() + ", class2class=" + class2class.size());
        sb.append("\n  classCasters:");
        for (Class to : classCasters.keySet()) {
            List<ClassCast> ccs = classCasters.get(to);
            sb.append("\n    " + to.getName() + " (" + ccs.size() + ")");
            for (ClassCast cc : ccs) {
                sb.append("\n      " + ((cc.from() != null) ? cc.from().getName() : "<general>") + "  " + cc.getClass().getName());
            }
        }

        sb.append("\n  class2class:");
        for (Class to : class2class.keySet()) {
            Map<Class, List<ClassCast>> mccs = class2class.get(to);
            sb.append("\n    " + to.getName() + " (" + mccs.size() + ")");
            for (Class from : mccs.keySet()) {
                List<ClassCast> ccs = mccs.get(from);
                sb.append("\n      " + from.getName() + " (" + ccs.size() + ")");
                for (ClassCast cc : ccs) {
                    sb.append("\n          " + cc.getClass().getName());
                }
            }
        }
        sb.append("\n");
        sb.append('}');
        return sb.toString();
    }

}
