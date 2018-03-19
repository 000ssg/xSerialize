/*
 * AS IS
 */
package ssg.serialize.tools;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author 000ssg
 */
public class Indent implements Cloneable {
    
    int level = 0;
    int indentSize = 2;
    byte[] indent = "                    ".getBytes();

    public Indent() {
    }

    public Indent(String indent) {
        if (indent != null) {
            byte[] bb = indent.getBytes();
            indentSize = bb.length;
            this.indent = new byte[indentSize * 10];
            for (int i = 0; i < 10; i++) {
                System.arraycopy(bb, 0, this.indent, indentSize * i, indentSize);
            }
        }
    }

    public int levelDown() {
        return ++level;
    }

    public int levelUp() {
        if (level > 0) {
            return --level;
        } else {
            return level;
        }
    }

    public long write(OutputStream os) throws IOException {
        long c = 1;
        os.write('\n');
        int ci = level * indentSize;
        while (ci > 0) {
            int cc = Math.min(ci, indent.length);
            os.write(indent, 0, cc);
            c += cc;
            ci -= cc;
        }
        return c;
    }

    @Override
    public Indent clone() {
        try {
            return (Indent) super.clone();
        } catch (CloneNotSupportedException cnsex) {
            return null;
        }
    }
    
}
