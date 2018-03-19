/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;

/**
 *
 * @author 000ssg
 */
public class Chars2StringCC extends RCC<char[],String> {

    @Override
    public String cast(char[] obj) throws IOException {
        return new String(obj);
    }
    
}
