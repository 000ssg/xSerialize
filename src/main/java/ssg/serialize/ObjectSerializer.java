/*
 * AS IS
 */
package ssg.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Serializer extension for objects support.
 *
 * @author 000ssg
 */
public interface ObjectSerializer extends Serializer {

    /**
     * Returns deserialized data as object of expected type.
     *
     * @param <T>
     * @param text
     * @param type
     * @return
     * @throws IOException
     */
    <T> T fromText(String text, Class<T> type) throws IOException;

    /**
     * Returns deserialized data as object of expected type.
     *
     * @param <T>
     * @param data
     * @param type
     * @return
     * @throws IOException
     */
    <T> T fromBytes(byte[] data, Class<T> type) throws IOException;

    /**
     * Returns deserialized data as object of expected type.
     *
     * @param <T>
     * @param is
     * @param type
     * @return
     * @throws IOException
     */
    <T> T fromStream(InputStream is, Class<T> type) throws IOException;

    /**
     * Returns deserialized data as object of expected type.
     *
     * @param <T>
     * @param url
     * @param type
     * @return
     * @throws IOException
     */
    <T> T fromURL(URL url, Class<T> type) throws IOException;

    /**
     * Converts arbitrary object to scalar/map/list compatible format for easy
     * serialization.
     *
     * @param <T>
     * @param obj
     * @return
     */
    <T> T simplify(Object obj);

    /**
     * Restores from abstracted scalar/map/list object representation to target
     * type instance.
     *
     * @param <T>
     * @param obj
     * @param type
     * @return
     * @throws IOException
     */
    <T> T enrich(Object obj, Class<T> type) throws IOException;

    /**
     * If true then cyclic references/duplicates should be handled.
     *
     * @return
     */
    boolean isResolveCyclicReferences();

    /**
     * Switch on/off cyclic references/duplicates handling support.
     *
     * @param resolve
     */
    void setResolveCyclicReferences(boolean resolve);

    /**
     * Scan structure to evaluate default statistics.
     *
     * @param is
     * @return
     * @throws IOException
     */
    OSStat scan(InputStream is) throws IOException;

    /**
     * Scan structure and evaluate statistics.
     *
     * @param handler
     * @param stat
     * @param is
     * @return
     * @throws IOException
     */
    OSStat scan(OSScanHandler handler, OSStat stat, InputStream is) throws IOException;

    /**
     * ObjectSerializer statistics. Statistics is gathered while applying parse
     * logic to input data stream. If statistics has associated scan handler,
     * then data are evaluated and passed as well.
     *
     * @param <T>
     * @param <R>
     */
    public static interface OSStat<T, R extends Number> {

        /**
         * Scan position tracking: retrieve
         *
         * @return
         */
        long pos();

        /**
         * Scan position tracking: set
         *
         * @param pos
         */
        void pos(long pos);

        void addType(T type);

        void addRef(R ref);
    }

    /**
     * Scan handler works as registrator of type open/close events and
     * characters/bytes sequence bewteen via pushChar/pushByte.
     *
     * Macro extensions for handling atomic scalar events are available as
     * onScalar and onRef. These open, set value and close corresponding types.
     *
     * Scan result is available via root() method that returns top-most object
     * or a wrapper to enclosed content.
     *
     * @param <T>
     */
    public static interface OSScanHandler<T, R> {

        /**
         * Return top-most item.
         *
         * @param <Z>
         * @return
         */
        <Z> Z root();

        /**
         * Start scanning (e.g. initialize structures)
         */
        void onStart();

        /**
         * Stop scanning (e.g. finalize structures and adjust root)
         */
        void onEnd(Object value);

        /**
         * Open nested item of type.
         *
         * @param type
         */
        void onOpen(T type, boolean referrable);

        /**
         * Close nested item of type.
         *
         * @param type
         */
        void onClose(T type);

        /**
         * Close nested item with optional reference. If valid reference then
         * the original item is replaced with its reference.
         *
         * @param type
         * @param newType
         * @param newValue
         */
        void onClose(T type, R reference);

        /**
         * Use or evaluate item's value for given type.
         *
         * @param type
         * @param value
         */
        void onScalar(T type, Object value, boolean referrable);

        /**
         * Add char to current item value.
         *
         * @param ch
         */
        void pushChar(char... ch);

        /**
         * Add byte to current item value
         *
         * @param b
         */
        void pushByte(byte... b);
    }

    public static class DummyScanStatistics<T, R> implements OSScanHandler<T, R> {

        long started;
        long completed;
        int elements;
        int maxDepth;
        int depth;
        int referrables;
        long chars;
        long bytes;

        @Override
        public <Z> Z root() {
            return null;
        }

        @Override
        public void onStart() {
            started = System.nanoTime();
            completed = 0;
            elements = 0;
            maxDepth = 0;
            depth = 0;
            referrables = 0;
            chars = 0;
            bytes = 0;
        }

        @Override
        public void onEnd(Object value) {
            completed = System.nanoTime();
        }

        @Override
        public void onOpen(T type, boolean referrable) {
            elements++;
            depth++;
            if (depth > maxDepth) {
                maxDepth++;
            }
            if (referrable) {
                referrables++;
            }
        }

        @Override
        public void onClose(T type) {
            depth--;
        }

        @Override
        public void onClose(T type, R reference) {
            depth--;
        }

        @Override
        public void onScalar(T type, Object value, boolean referrable) {
            elements++;
            if ((depth + 1) > maxDepth) {
                maxDepth++;
            }
            if (referrable) {
                referrables++;
            }
        }

        @Override
        public void pushChar(char... ch) {
            if (ch != null) {
                chars += ch.length;
            }
        }

        @Override
        public void pushByte(byte... b) {
            if (b != null) {
                bytes += b.length;
            }
        }

        @Override
        public String toString() {
            return "DummyScanStatistics{"
                    + "\n  duration=" + (completed - started) / 1000000f + "ms"
                    + "\n  elements=" + elements
                    + "\n  maxDepth=" + maxDepth
                    + "\n  referrables=" + referrables
                    + "\n  chars=" + chars
                    + "\n  bytes=" + bytes
                    + "\n}";
        }

    }

    public static class CompositeOSScanHandler<T, R> implements OSScanHandler<T, R> {

        OSScanHandler<T, R>[] handlers;

        @Override
        public <Z> Z root() {
            Object[] roots = new Object[handlers.length];
            for (int i = 0; i < handlers.length; i++) {
                roots[i] = handlers[i].root();
            }
            return (Z) roots;
        }

        @Override
        public void onStart() {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].onStart();
            }
        }

        @Override
        public void onEnd(Object value) {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].onEnd(value);
            }
        }

        @Override
        public void onOpen(T type, boolean referrable) {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].onOpen(type, referrable);
            }
        }

        @Override
        public void onClose(T type) {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].onClose(type);
            }
        }

        @Override
        public void onClose(T type, R ref) {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].onClose(type, ref);
            }
        }

        @Override
        public void onScalar(T type, Object value, boolean referrable) {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].onScalar(type, value, referrable);
            }
        }

        @Override
        public void pushChar(char... ch) {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].pushChar(ch);
            }
        }

        @Override
        public void pushByte(byte... b) {
            for (int i = 0; i < handlers.length; i++) {
                handlers[i].pushByte(b);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CompositeOSScanHandler{" + "handlers=" + handlers.length);
            for (int i = 0; i < handlers.length; i++) {
                sb.append("\n  [" + i + "] " + handlers[i].toString().replace("\n", "\n    "));
            }
            sb.append("\n");
            sb.append('}');
            return sb.toString();
        }

    }
}
