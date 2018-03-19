/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;
import java.util.Date;
import ssg.serialize.tools.ClassCaster;

/**
 *
 * @author 000ssg
 */
public class Long2DateCC extends RCC<Long, Date> {
    
    public Long2DateCC() {
    }

    public Long2DateCC(int priority) {
        super(priority);
    }

    @Override
    public Date cast(Long obj) throws IOException {
        if (obj == null) {
            return null;
        } else {
            return new Date(obj);
        }
    }
    
}
