/*
 * AS IS
 */
package ssg.serialize.utils;

import java.util.Comparator;
import java.util.List;

/**
 *
 * @author 000ssg
 */
public class Stat {

    protected String description;
    protected long len;
    protected long time;

    public Stat() {
    }

    public Stat(String d) {
        description = d;
    }

    public Stat(String d, long len, long time) {
        description = d;
        this.len = len;
        this.time = time;
    }

    public Stat(String d, boolean dummy, long len, long time) {
        description = d;
        this.len = len;
        this.time = time;
    }

    @Override
    public String toString() {
        return "Stat{" + "" + getLen() + "\t " + (getTime() / 1000000f) + "ms." + "\t \"" + getDescription() + "\"" + '}';
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the len
     */
    public long getLen() {
        return len;
    }

    /**
     * @return the time
     */
    public long getTime() {
        return time;
    }

    /**
     *
     * @author 000ssg
     */
    public static class SStat {

        protected String description;
        protected long len;
        protected long time;
        protected double dlen;
        protected double dtime;
        protected int count;
        protected Float rate;

        public SStat(Stat stat) {
            super();
            this.description = stat.getDescription();
            add(stat);
        }

        public void add(Stat stat) {
            if (stat == null || !description.equals(stat.description)) {
                return;
            }
            len += stat.getLen();
            time += stat.getTime() / 1000000;
            dlen += stat.getLen();
            dtime += stat.getTime() / 1000000.0;
            count++;
        }

        @Override
        public String toString() {
            String rs = null;
            if (getRate() != null) {
                rs = getRate().toString();
                if (rs.length() > 6) {
                    rs = rs.substring(0, 6);
                }
            }
            return "SStat{" + ((getRate() != null) ? rs + "\t" : "") + "cnt=" + getCount() + "\t len=" + (float) getDlen() + "\t (" + (float) getDlen() / getCount() + ")" + "\t time=" + (float) getDtime() + "ms" + "\t (" + (float) getDtime() / getCount() + "ms)" + "\t description=" + getDescription() + '}';
        }

        public void rate(List<SStat> coll) {
            rate(coll, null);
        }

        public void rate(List<SStat> coll, Double ref) {
            if (coll == null || coll.isEmpty()) {
                return;
            }
            Double d = ref;
            if (ref == null) {
                for (SStat st : coll) {
                    if (d == null) {
                        d = st.getDtime();
                    } else if (d > st.getDtime()) {
                        d = st.getDtime();
                    }
                }
            }
            for (SStat st : coll) {
                st.rate = (float) (st.getDtime() / d * 100);
            }
            coll.sort(new Comparator<SStat>() {
                @Override
                public int compare(SStat o1, SStat o2) {
                    Float f = (o1.getRate() != null) ? o1.getRate() : 0.0F;
                    return f.compareTo((o2.getRate() != null) ? o2.getRate() : 0);
                }
            });
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return the len
         */
        public long getLen() {
            return len;
        }

        /**
         * @return the time
         */
        public long getTime() {
            return time;
        }

        /**
         * @return the dlen
         */
        public double getDlen() {
            return dlen;
        }

        /**
         * @return the dtime
         */
        public double getDtime() {
            return dtime;
        }

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * @return the rate
         */
        public Float getRate() {
            return rate;
        }
    }
}
