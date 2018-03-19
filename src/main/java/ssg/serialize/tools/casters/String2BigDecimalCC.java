/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;
import java.math.BigDecimal;

/**
 *
 * @author 000ssg
 */
public class String2BigDecimalCC extends RCC<String, BigDecimal> {

    @Override
    public BigDecimal cast(String obj) throws IOException {
        return new BigDecimal(obj);
    }

}
