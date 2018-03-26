/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.serialize.impl;

import ssg.serialize.base.BaseScanHandler;
import ssg.serialize.base.BaseObjectSerializer;
import ssg.serialize.base.ObjectSerializerContext;
import ssg.serialize.tools.BER;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;
import ssg.serialize.tools.Reflector;
import ssg.serialize.utils.Dump;

/**
 *
 * ASN1 serializer does not support resolution of cyclic references/duplicates.
 *
 * @author 000ssg
 */
public class ASN1Serializer extends BaseObjectSerializer {

    BER ber = new BER();

    @Override
    public void setResolveCyclicReferences(boolean resolve) {
    }

    @Override
    public boolean isResolveCyclicReferences() {
        return false;
    }

    @Override
    public long writeNull(OutputStream os, ObjectSerializerContext ctx) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long writeScalar(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long writeCollection(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long writeMap(Object obj, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long writeObject(Object obj, Reflector rf, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long writeRef(Object obj, Object ref, OutputStream os, ObjectSerializerContext ctx) throws IOException {
        throw new IOException("No references support in " + getClass().getSimpleName());
    }

    @Override
    public Object read(InputStream is) throws IOException {
        ASN1Serializer aser = new ASN1Serializer();
        OSScanHandler sh = new ASN1ScanHandler();
        OSStat stat = aser.scan(sh, null, is);
        return sh.root();
    }

    @Override
    public OSStat scan(final OSScanHandler handler, OSStat stat, InputStream is) throws IOException {
        if (stat == null) {
            stat = new ASN1Stat();
        }
        final OSStat astat = stat;

        if (handler != null) {
            handler.onStart();
        }

        Object obj = ber.scan(is, new BER.BEREventHandler() {
            boolean first = true;

            @Override
            public boolean onOpenItem(Stack<Object[]> stack, byte[] header, int off) {
                astat.addType(ber.getType(header, off));

                if (handler != null) {
                    if (first) {//stack.isEmpty()) {
                        first = false;
                        Integer type = ber.getType(header, off);

                        boolean scalar = false;
                        if (astat instanceof BOSStat) {
                            if (((BOSStat) astat).isScalarType(type, null)) {
                                scalar = true;
                            }
                        }

                        if (scalar) {
                        } else {
                            handler.onOpen(type, false);
                        }
                    }
                }

                return true;
            }

            @Override
            public void onCloseItem(Stack<Object[]> stack, byte[] header, int off, Object value) {
                if (handler != null) {
                    Integer type = ber.getType(header, off);

                    boolean scalar = false;
                    if (astat instanceof BOSStat) {
                        if (((BOSStat) astat).isScalarType(type, null)) {
                            scalar = true;
                        }
                    }

                    if (scalar) {
                        //handler.onScalar(type, value, false);
                    } else if (stack.size() == 1) {
                        handler.onClose(type, value);
                    } //handler.onClose(type, null);
                }
            }

            @Override
            public void onEOC(Stack<Object[]> stack, byte[] header, int off) {
                astat.addType(BER.CLASS_EOC);
            }
        });

        if (handler != null) {
            handler.onEnd(obj);
        }

        return stat;
    }

    public class ASN1Stat extends BOSStat<Integer, Integer> {

        @Override
        public boolean isRefType(Integer type, ObjectSerializerContext ctx) {
            return false;
        }

        @Override
        public boolean isScalarType(Integer type, ObjectSerializerContext ctx) {
            switch (type) {
                case BER.CLASS_SEQUENCE:
                    return false;
                case BER.CLASS_SET:
                    return false;
            }
            return true;
        }

        @Override
        public Integer refType(Integer ref) {
            return null;
        }

        @Override
        public String type2name(Integer type) {
            return ber.type2name(type);
        }
    }

    public static class ASN1ScanHandler extends BaseScanHandler<Integer, Integer> {

        @Override
        public boolean isObject(Integer type) {
            return false;
        }

        @Override
        public boolean isCollection(Integer type) {
            switch (type) {
                case BER.CLASS_SEQUENCE:
                    return true;
                case BER.CLASS_SET:
                    return true;
            }
            return false;
        }

        @Override
        public Object adjust(Object value, Integer type) {
            return value;
        }

        @Override
        public Integer createReference(Integer type, Object value) {
            return null;
        }

    }
}
