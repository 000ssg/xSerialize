/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;
import java.math.BigInteger;

/**
 *
 * @author 000ssg
 */
public class Bytes2BigIntegerCC extends RCC<byte[], BigInteger> {

    @Override
    public BigInteger cast(byte[] obj) throws IOException {
        return new BigInteger(obj);
    }

}
