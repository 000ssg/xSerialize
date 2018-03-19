/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;
import ssg.serialize.impl.BASE64Serializer;
import ssg.serialize.tools.ClassCaster;

/**
 *
 * @author 000ssg
 */
public class String2BytesCC extends RCC<String, byte[]> {
    
    @Override
    public byte[] cast(String obj) throws IOException {
        return BASE64Serializer.decode(obj);
    }
    
}
