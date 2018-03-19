/*
 * AS IS
 */
package ssg.serialize.tools;

import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ssg.serialize.tools.ClassCast;
import ssg.serialize.tools.casters.Long2DateCC;
import ssg.serialize.tools.casters.String2DateCC;

/**
 *
 * @author 000ssg
 */
public class ClassCasterTest {

    public ClassCasterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of tryCast method, of class ClassCaster.
     */
    @Test
    public void testFunctional() {
        System.out.println("functional");

        ClassCaster instance = new ClassCaster();
        ClassCast s2d = new String2DateCC();
        ClassCast s2d2 = new String2DateCC();
        ClassCast l2d = new Long2DateCC();
        instance.addClassCasts(s2d, s2d2, l2d);
        System.out.println(instance);

        Date d = new Date();
        System.out.println(d + "\n" + instance.tryCast(d.getTime(), Date.class));
    }

}
