/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.serialize.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Stack;
import ssg.serialize.ObjectSerializer.OSScanHandler;
import ssg.serialize.impl.ASN1Serializer.ASN1ScanHandler;
import ssg.serialize.tools.BER;
import static ssg.serialize.tools.BER.dumpHeader;
import static ssg.serialize.tools.BER.scan;
import ssg.serialize.utils.Dump;

/**
 *
 * @author sesidoro
 */
public class FunctionalASN1 {

    public static void mainBER(String[] args) throws Exception {
        String oids = "1.2.840.113549.1.12.1.3";
        BER.OID oid = new BER.OID(oids);
        System.out.println(oids + "\n" + oid);

        File ff = new File("src/test/resources/der");
        for (File f : ff.listFiles()) {
            if (f.isFile() && (f.getName().toLowerCase().endsWith(".der")
                    || f.getName().toLowerCase().endsWith(".crt")
                    || f.getName().toLowerCase().endsWith(".cer"))) {
                System.out.println("\nTest " + f);
                InputStream is = f.toURI().toURL().openStream();
                if (is.read() == '-') {
                    // assume it is Base64 encoded content within "\r\n" ... "\r\n-" section
                    is = new BASE64InputStream(f.toURI().toURL().openStream(), true);
                } else {
                    // just re-open...
                    is.close();
                    is = f.toURI().toURL().openStream();
                }
                final boolean dump = true;
                Object o = scan(is, new BER.BEREventHandler() {
                    @Override
                    public boolean onOpenItem(Stack<Object[]> stack, byte[] header, int off) {
                        if (dump) {
                            for (int i = 0; i < stack.size() - 1; i++) {
                                System.out.print("  ");
                            }
                            System.out.println("/" + dumpHeader(header, off));
                        }
                        return true;
                    }

                    @Override
                    public void onCloseItem(Stack<Object[]> stack, byte[] header, int off, Object value) {
                        if (dump) {
                            for (int i = 0; i < stack.size() - 1; i++) {
                                System.out.print("  ");
                            }
                            System.out.println("\\" + dumpHeader(header, off) + "  " + value);
                        }
                    }

                    @Override
                    public void onEOC(Stack<Object[]> stack, byte[] header, int off) {
                        if (dump) {
                            for (int i = 0; i < stack.size() - 1; i++) {
                                System.out.print("  ");
                            }
                            System.out.println("<EOC>");
                        }
                    }
                });
                System.out.println("  " + ("" + o).replace("\n", "\n  "));
            }
        }
    }

    public static void mainASN1(String[] args) throws Exception {
        File ff = new File("src/test/resources/der");
        for (File f : ff.listFiles()) {
            if (f.isFile() && (f.getName().toLowerCase().endsWith(".der")
                    || f.getName().toLowerCase().endsWith(".crt")
                    || f.getName().toLowerCase().endsWith(".cer"))) {
                System.out.println("\nTest " + f);
                InputStream is = f.toURI().toURL().openStream();
                boolean asB64 = false;
                if (is.read() == '-') {
                    // assume it is Base64 encoded content within "\r\n" ... "\r\n-" section
                    is = new BASE64InputStream(f.toURI().toURL().openStream(), true);
                    asB64 = true;
                } else {
                    // just re-open...
                    is.close();
                    is = f.toURI().toURL().openStream();
                }
                final boolean dump = true;
                ASN1Serializer aser = new ASN1Serializer();
                Object o = aser.scan(null, null, is);
                System.out.println("  " + ("" + o).replace("\n", "\n  "));
                OSScanHandler sh = new ASN1ScanHandler();
                o = aser.scan(sh, null, (asB64)
                        ? new BASE64InputStream(f.toURI().toURL().openStream(), true)
                        : f.toURI().toURL().openStream());
                System.out.println("  " + Dump.dump(sh.root(), true, false).replace("\n", "\n  "));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("\n---------------------------------------------"
                + "\nTest ASN1 (BER) serializer."
                + "\n---------------------------------------------"
        );
        try {
            System.out.println("\n--------------------------- BER");
            mainBER(args);
        } catch (Throwable th) {
        }
        try {
            System.out.println("\n--------------------------- ASN1Serializer");
            mainASN1(args);
        } catch (Throwable th) {
        }
    }
}
