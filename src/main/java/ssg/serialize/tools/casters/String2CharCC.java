/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;

/**
 *
 * @author 000ssg
 */
public class String2CharCC extends RCC<String,Character> {

    @Override
    public Character cast(String obj) throws IOException {
        return obj.toCharArray()[0];
    }
    
}
