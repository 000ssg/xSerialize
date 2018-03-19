/*
 * AS IS
 */
package ssg.serialize.utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON implementation wrapper
 *
 * @author 000ssg
 */
public class RW {

    public String title;
    String className;
    Class cl;
    Object instance;
    Object instanceW;
    Object instanceR;
    Throwable error;
    Method writer;
    Method reader;
    Object[] wrp;
    Object[] rdp;

    public RW(RWD rwd) {
        try {
            rwd.init(this);
        } catch (Throwable th) {
        }
    }

    public static Collection<String> getRegisteredNames() {
        return RWD.rwds.keySet();
    }

    public static Collection<String> getRegisteredClassNames() {
        return RWD.rwds2.keySet();
    }

    public static RW getRW(String name) {
        if (name == null) {
            return null;
        }
        RWD rwd = RWD.rwds.get(name);
        if (rwd == null) {
            rwd = RWD.rwds2.get(name);
        }
        return new RW(rwd);
    }

    public static Class classOf(Object obj) {
        try {
            if (obj instanceof Class) {
                return (Class) obj;
            } else if (obj instanceof String) {
                return RW.class.getClassLoader().loadClass((String) obj);
            } else if (obj != null) {
                return obj.getClass();
            } else {
                return null;
            }
        } catch (Throwable th) {
            return null;
        }
    }

    public static Object instanceOf(Object obj) {
        try {
            Class cl = (obj instanceof Class)
                    ? (Class) obj
                    : classOf(obj);
            return cl.newInstance();
        } catch (Throwable th) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "RW{" + "title=" + title + ", className=" + className + ", cl=" + cl + ", instance=" + instance + ", instanceW=" + instanceW + ", instanceR=" + instanceR + ", error=" + error + ", writer=" + writer + ", reader=" + reader + ", wrp=" + wrp + ", rdp=" + rdp + '}';
    }

    public Class getType() {
        return cl;
    }

    public <T> T getInstance() {
        return (T) instance;
    }

    public <T> T getReaderInstance() {
        return (T) instanceR;
    }

    public <T> T getWriterInstance() {
        return (T) instanceW;
    }

    public boolean canRead() {
        return instanceR != null && reader != null;
    }

    public boolean canWrite() {
        return instanceW != null && writer != null;
    }

    public <T> T write(Object obj) throws IOException {
        if (canWrite()) {
            if (wrp != null) {
                wrp[0] = obj;
                try {
                    return (T) writer.invoke(instanceW, wrp);
                } catch (Throwable th) {
                    if (th instanceof IOException) {
                        throw (IOException) th;
                    } else {
                        throw new IOException(th);
                    }
                }
            } else {
                try {
                    return (T) writer.invoke(instanceW, obj);
                } catch (Throwable th) {
                    if (th instanceof IOException) {
                        throw (IOException) th;
                    } else {
                        throw new IOException(th);
                    }
                }
            }
        }
        return null;
    }

    public <T> T read(Object s) throws IOException {
        if (canRead()) {
            if (rdp != null) {
                rdp[0] = s;
                try {
                    return (T) reader.invoke(instanceR, rdp);
                } catch (Throwable th) {
                    if (th instanceof IOException) {
                        throw (IOException) th;
                    } else {
                        throw new IOException(th);
                    }
                }
            } else {
                try {
                    return (T) reader.invoke(instanceR, s);
                } catch (Throwable th) {
                    if (th instanceof IOException) {
                        throw (IOException) th;
                    } else {
                        throw new IOException(th);
                    }
                }
            }
        }
        return null;
    }

    public static abstract class RWD {

        public static Map<String, RWD> rwds = new LinkedHashMap<String, RWD>();
        public static Map<String, RWD> rwds2 = new LinkedHashMap<String, RWD>();

        String title;
        String className;

        public RWD(String title, String className) {
            this.title = title;
            this.className = className;
        }

        public void init(RW rw) throws Exception {
            rw.title = title;
            rw.className = className;
            rw.cl = classOf(className);
            rw.instance = instanceOf(rw.cl);
            initRW(rw);
        }

        public abstract void initRW(RW rw) throws Exception;

        public static void register(RWD rwd) {
            if (rwd == null || rwd.title == null || rwd.className == null) {
                return;
            }
            rwds.put(rwd.title, rwd);
            rwds2.put(rwd.className, rwd);
        }

        static {
            RWD.register(new RWD("MyJSON", "ssg.serialize.impl.JSONSerializer") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = rw.instance;
                    rw.instanceR = rw.instance;
                    rw.writer = rw.instanceW.getClass().getMethod("toText", Object.class);
                    rw.reader = rw.instanceR.getClass().getMethod("fromText", String.class);
                }
            });
            RWD.register(new RWD("MyBJSON", "ssg.serialize.impl.BJSONSerializer") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = rw.instance;
                    rw.instanceR = rw.instance;
                    rw.writer = rw.instanceW.getClass().getMethod("toBytes", Object.class);
                    rw.reader = rw.instanceR.getClass().getMethod("fromBytes", byte[].class);
                }
            });
//            RWD.register(new RWD("MyJSONLite", "com.ssg.xjson.JSONParserLite") {
//                @Override
//                public void initRW(RW rw) throws Exception {
//                    rw.instanceR = rw.instance;
//                    rw.instanceW = instanceOf("com.ssg.xjson.JSONFormatterLite");
//                    rw.writer = rw.instanceW.getClass().getMethod("doFormat", Object.class, boolean.class);
//                    rw.wrp = new Object[]{null, false};
//                    rw.reader = rw.instanceR.getClass().getMethod("parse", Reader.class);
//                }
//            });
            RWD.register(new RWD("Jackson", "org.codehaus.jackson.map.ObjectMapper") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceR = rw.instance;
                    rw.instanceW = rw.instance;
                    rw.writer = rw.instanceW.getClass().getMethod("writeValueAsString", Object.class);
                    rw.reader = rw.instanceR.getClass().getMethod("readValue", String.class, Class.class);
                    rw.rdp = new Object[]{null, Object.class};
                }
            });
            RWD.register(new RWD("JSON.Simple", "org.json.simple.parser.JSONParser") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = null;
                    rw.instanceR = rw.instance;
                    rw.reader = rw.instanceR.getClass().getMethod("parse", String.class);
                }
            });
            RWD.register(new RWD("Gson", "com.google.gson.Gson") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = rw.instance;
                    rw.instanceR = rw.instance;
                    rw.writer = rw.instanceW.getClass().getMethod("toJson", Object.class);
                    rw.reader = rw.instanceR.getClass().getMethod("fromJson", String.class, Class.class);
                    rw.rdp = new Object[]{null, Object.class};
                }
            });
            RWD.register(new RWD("MyBASE64", "ssg.serialize.impl.BASE64Serializer") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = rw.instance;
                    rw.instanceR = rw.instance;
                    rw.writer = rw.instanceW.getClass().getMethod("toText", Object.class);
                    rw.reader = rw.instanceR.getClass().getMethod("fromText", String.class);
                }
            });
            RWD.register(new RWD("Base64", "java.util.Base64") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = rw.cl.getMethod("getEncoder", null).invoke(null);
                    rw.instanceR = rw.cl.getMethod("getDecoder", null).invoke(null);
                    rw.writer = rw.instanceW.getClass().getMethod("encodeToString", byte[].class);
                    rw.reader = rw.instanceR.getClass().getMethod("decode", String.class);
                }
            });
            RWD.register(new RWD("BASE64Encoder", "sun.misc.BASE64Encoder") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = rw.instance;
                    rw.instanceR = instanceOf("sun.misc.BASE64Decoder");
                    rw.writer = rw.instanceW.getClass().getMethod("encode", byte[].class);
                    rw.reader = rw.instanceR.getClass().getMethod("decodeBuffer", String.class);
                }
            });
            RWD.register(new RWD("Java serialization (for JSON compare)", "ssg.serialize.utils.TestPOJO") {
                @Override
                public void initRW(RW rw) throws Exception {
                    rw.instanceW = rw.instance;
                    rw.instanceR = rw.instance;
                    rw.writer = rw.instanceW.getClass().getMethod("javaSerialize", Object.class);
                    rw.reader = rw.instanceR.getClass().getMethod("javaDeserialize", byte[].class);
                }
            });
        }

    }
}
