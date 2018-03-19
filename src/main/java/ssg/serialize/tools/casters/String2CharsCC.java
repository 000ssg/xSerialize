/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;

/**
 *
 * @author 000ssg
 */
public class String2CharsCC extends RCC<String,char[]> {

    @Override
    public char[] cast(String obj) throws IOException {
        return obj.toCharArray();
    }
    
}
