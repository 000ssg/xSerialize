/*
 * AS IS
 */
package ssg.serialize.tools.casters;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author 000ssg
 */
public class String2DateCC extends RCC<String, Date> {

    List<DateFormat> formats = new ArrayList<DateFormat>();

    public String2DateCC() {
    }

    public String2DateCC(String... formats) {
        add(formats);
    }

    public String2DateCC(int priority) {
        super(priority);
    }

    public String2DateCC(int priority, String... formats) {
        super(priority);
        add(formats);
    }

    public void add(String... formats) {
        if (formats != null) {
            for (String s : formats) {
                try {
                    DateFormat df = new SimpleDateFormat(s);
                    this.formats.add(df);
                } catch (Throwable th) {
                }
            }
        }
    }

    @Override
    public Date cast(String obj) throws IOException {
        Throwable error = null;
        for (DateFormat sdf : formats) {
            try {
                return sdf.parse(obj);
            } catch (Throwable th) {
                error = th;
            }
        }
        if (error instanceof IOException) {
            throw (IOException) error;
        } else {
            throw new IOException("No suitable date parsing format found for '" + obj + "'");
        }
    }

}
