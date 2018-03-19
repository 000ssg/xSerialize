/*
 * AS IS
 */
package ssg.serialize.impl;

import ssg.serialize.utils.Stat;
import ssg.serialize.utils.RW;
import ssg.serialize.utils.TestPOJO;
import ssg.serialize.utils.TestPOJO.TestPOJO1;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author 000ssg
 */
public class FunctionalJSON {
    
    public static Object generate(int level, int maxAttrs, int maxItems) {
        if (level < 0) {
            return null;
        }
        Map m = new LinkedHashMap();
        
        for (int i = 0; i < (int) (maxAttrs * Math.random()); i++) {
            int type = (int) (Math.random() * 6);
            String key = "_" + level + "_" + i + "_" + type;
            switch (type) {
                case 0: // int
                    m.put(key, (int) (Math.random() * Integer.MAX_VALUE));
                    break;
                case 1: // real
                    m.put(key, (Math.random() * Integer.MAX_VALUE));
                    break;
                case 2: // map
                    m.put(key, generate(level - 1, maxAttrs, maxItems));
                    break;
                case 3: // list
                    List lst = new ArrayList();
                    for (int j = 0; j < (int) (Math.random() * maxItems); j++) {
                        if (Math.random() > 0.8) {
                            lst.add(generate(level - 1, maxAttrs, maxItems));
                        } else {
                            lst.add("eftjtu,dtu,.d");
                        }
                    }
                    m.put(key, lst);
                    break;
                case 4: // boolean
                    m.put(key, Math.random() >= 0.5);
                    break;
                case 5: // null
                    m.put(key, null);
                    break;
                default:
                    m.put(key, "rthkdghldgyuödcghl.dtlö");
            }
        }
        
        return m;
    }
    
    public static String deepCompare(String path, Object o1, Object o2) {
        StringBuilder sb = new StringBuilder();
        if (o1 instanceof Map && o2 instanceof Map) {
            Map m1 = (Map) o1;
            Map m2 = (Map) o2;
            if (m1.size() != m2.size()) {
                sb.append(path + ": map size mismatch: " + m1.size() + " <> " + m2.size());
            } else {
                for (Object key : m1.keySet()) {
                    Object i1 = m1.get(key);
                    Object i2 = m2.get(key);
                    String c = deepCompare(path + "{'" + key + "'}/", i1, i2);
                    if (!c.isEmpty()) {
                        return c;
                    }
                }
            }
        } else if (o1 instanceof Collection && o2 instanceof Collection) {
            Collection c1 = (Collection) o1;
            Collection c2 = (Collection) o2;
            if (c1.size() != c2.size()) {
                sb.append(path + ": collection size mismatch: " + c1.size() + " <> " + c2.size());
            } else {
                Iterator it1 = c1.iterator();
                Iterator it2 = c2.iterator();
                int idx = 0;
                while (it1.hasNext()) {
                    Object i1 = it1.next();
                    Object i2 = it2.next();
                    String c = deepCompare(path + "[" + idx + "]/", i1, i2);
                    if (!c.isEmpty()) {
                        return c;
                    }
                    idx++;
                }
            }
        } else {
            
        }
        return sb.toString();
    }
    
    public static void introduction(Object root, Object... items) {
        System.out.println("JSON conversion implementations performance comparison.");
        
        System.out.println("  Root object type: " + ((root != null) ? root.getClass() : "null"));
        if (items != null) {
            System.out.println("    JSON/BJSON converters (" + items.length + "):");
            for (Object o : items) {
                System.out.println("      " + ((o != null) ? o.getClass().getName() : ""));
            }
        }
    }
    
    public static int size(Object o) {
        if (o instanceof String) {
            return ((String) o).length();
        }
        if (o instanceof Collection) {
            return ((Collection) o).size();
        }
        if (o instanceof Map) {
            return ((Map) o).size();
        } else if (o != null && o.getClass().isArray()) {
            return Array.getLength(o);
        } else {
            return 0;
        }
    }
    
    public static void prepare() {
        Runtime.getRuntime().gc();
        try {
            Thread.sleep(10);
        } catch (Throwable th) {
        }
    }
    
    public static void main(String[] args) throws Exception {
        int writeIterations = 30;
        int readIterations = 30;
        Object root = TestPOJO.randomPOJO(TestPOJO1.class, 200);
        if (args == null || args.length == 0) {
            root = generate(15, 30, 15);
        }
        if (root instanceof TestPOJO1) {
            TestPOJO1 pojo = (TestPOJO1) root;
            pojo.aself = new TestPOJO1[100];
            for (int i = 0; i < pojo.aself.length; i++) {
                pojo.aself[i] = TestPOJO.randomPOJO(TestPOJO1.class, 550);
            }
        }
        // pre-init
        List<Stat> stats = new ArrayList<Stat>();
        
        List<RW> rws = new ArrayList<RW>();
        System.out.println("Known RWs: " + RW.getRegisteredNames());
        for (String key : RW.getRegisteredNames()) {
            if (key.toLowerCase().contains("son")) {
                RW rw = RW.getRW(key);
                if (rw.canWrite() || rw.canRead()) {
                    if (rw.getInstance() instanceof BaseObjectSerializer) {
                        rws.add(rw);
                        ((BaseObjectSerializer) rw.getInstance()).setResolveCyclicReferences(false);
                        rw.title += "(q)";
                        System.out.println("added " + rw);
                        rw = RW.getRW(key);
                        if (rw.getInstance() instanceof BJSONSerializer) {
                            rws.add(rw);
                            ((BaseObjectSerializer) rw.getInstance()).decycleFlags
                                    = BaseObjectSerializer.DF_STRING
                                    | BaseObjectSerializer.DF_BIGINT
                                    | BaseObjectSerializer.DF_BIGDEC;
                            rw.title += "(x)";
                            System.out.println("added " + rw);
                            rw = RW.getRW(key);
                        }
                    }
                    if (root instanceof TestPOJO1 && (rw.title.startsWith("Jackson")
                            || rw.title.startsWith("Gson"))) {
                        continue;
                    }
                    rws.add(rw);
                    System.out.println("added " + rw);
                }
            }
        }
        
        RW[] oRW = rws.toArray(new RW[rws.size()]);
        Object[] oOUT = new Object[oRW.length];
        Object[] oIN = new Object[oRW.length];
        
        System.out.println("\nStart WRITE for " + oRW.length + " RW types:");
        try {
            long start = System.nanoTime();
            // init WRITE
            for (int k = 0; k < oRW.length; k++) {
                RW rw = oRW[k];
                if (rw.canWrite()) {
                    System.out.println("Init " + rw);
                    prepare();
                    stats.add(new Stat(
                            rw.title + " init", (start = System.nanoTime()) != 0,
                            size(oOUT[k] = rw.write(root)),
                            (System.nanoTime() - start)));
                }
            }
            stats.add(null);
            
            for (int i = 0; i < writeIterations; i++) {
                int ii = -1;
                for (int k = 0; k < oRW.length; k++) {
                    RW rw = oRW[k];
                    if (rw.canWrite()) {
                        prepare();
                        stats.add(new Stat(
                                rw.title + " " + ii, (start = System.nanoTime()) != 0,
                                size(oOUT[k] = rw.write(root)),
                                (System.nanoTime() - start)));
                    }
                }
                stats.add(null);
            }
            stats.add(null);
            
            System.out.println("\nStart READ for " + oRW.length + " RW types:");
            // init READ
            for (int k = 0; k < oRW.length; k++) {
                RW rw = oRW[k];
                if (rw.canRead()) {
                    System.out.println("Init R " + rw);
                    prepare();
                    stats.add(new Stat(
                            rw.title + " R init", (start = System.nanoTime()) != 0,
                            size(oIN[k] = rw.read((oOUT[k] != null) ? oOUT[k] : oOUT[0])),
                            (System.nanoTime() - start)));
                }
            }
            stats.add(null);
            
            for (int i = 0; i < readIterations; i++) {
                int ii = -2;
                for (int k = 0; k < oRW.length; k++) {
                    RW rw = oRW[k];
                    if (rw.canRead()) {
                        prepare();
                        stats.add(new Stat(
                                rw.title + " R " + ii, (start = System.nanoTime()) != 0,
                                size(oIN[k] = rw.read((oOUT[k] != null) ? oOUT[k] : oOUT[0])),
                                (System.nanoTime() - start)));
                    }
                }
                stats.add(null);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        
        if (writeIterations > 10 || readIterations > 10) {
            // too many results - it is statistics...
        } else {
            System.out.println("\nWRITE/READ statistics for " + oRW.length + " RW types:");
            for (Stat stat : stats) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
        }
        
        Map<String, Stat.SStat> summary = new LinkedHashMap<String, Stat.SStat>();
        for (Stat st : stats) {
            if (st == null || st.getDescription() == null || st.getDescription().contains("init")) {
                continue;
            }
            Stat.SStat sst = summary.get(st.getDescription());
            if (sst == null) {
                summary.put(st.getDescription(), new Stat.SStat(st));
            } else {
                sst.add(st);
            }
        }
        // rate
        {
            List<Stat.SStat> ws = new ArrayList<Stat.SStat>();
            List<Stat.SStat> rs = new ArrayList<Stat.SStat>();
            
            Double refW = null;
            Double refR = null;
            
            for (Stat.SStat stat : summary.values()) {
                if (stat == null) {
                    continue;
                }
                if (stat.getDescription().contains(" R ")) {
                    rs.add(stat);
                    if (stat.getDescription().contains("Jackson")) {
                        refR = stat.getDtime();
                    }
                } else {
                    ws.add(stat);
                    if (stat.getDescription().contains("Jackson")) {
                        refW = stat.getDtime();
                    }
                }
            }
            if (!ws.isEmpty()) {
                ws.iterator().next().rate(ws, refW);
            }
            if (!rs.isEmpty()) {
                rs.iterator().next().rate(rs, refR);
            }
            
            System.out.println("\nWRITE/READ summary:");
            for (Stat.SStat stat : ws) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
            System.out.println();
            for (Stat.SStat stat : rs) {
                if (stat == null) {
                    System.out.println();
                    continue;
                }
                System.out.println(stat);
            }
        }
        int a = 0;
    }
}
