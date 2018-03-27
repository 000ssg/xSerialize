/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.serialize.tools;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sesidoro
 */
public class NumberToolsTest {

    public NumberToolsTest() {
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
     * Test of isNumeric method, of class NumberTools.
     */
    @Test
    public void testIsNumeric() throws Exception {
        System.out.println("isNumeric");
        for (Object[] oo : new Object[][]{
            {(byte) 1, true},
            {(short) 1, true},
            {(int) 1, true},
            {(long) 1, true},
            {1f, true},
            {1.0, true},
            {BigInteger.valueOf(1), true},
            {BigDecimal.valueOf(1), true},
            {new File("."), false},
            {new File(".").toURI(), false},
            {new File(".").toURI().toURL(), false},
            {new ArrayList(), false},
            {new HashMap(), false},
            {"x", false}
        }) {
            Object obj = oo[0];
            boolean expResult = (Boolean) oo[1];
            boolean result = NumberTools.isNumeric(obj);
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of cast method, of class NumberTools.
     */
    @Test
    public void testCast() {
        System.out.println("cast");
        for (Object[] oo : new Object[][]{
            {1, byte.class, (byte) 1},
            {1, short.class, (short) 1},
            {1, int.class, 1},
            {1, long.class, (long) 1},
            {1, float.class, 1f},
            {1, double.class, 1.0},
            {10000, BigInteger.class, BigInteger.valueOf(10000)},
            {10000.1, BigDecimal.class, BigDecimal.valueOf(10000.1)},
            // not same!
            {1, byte.class, 1, false},
            {1, short.class, 1, false},
            {1, int.class, 1L, false},
            {1, long.class, 1, false},
            {1, float.class, 1, false},
            {1, double.class, 1f, false},
            {10000, BigInteger.class, 10000, false},
            {10000.1, BigDecimal.class, 10000.1, false}
        }) {
            Number num = (Number) oo[0];
            Class numberType = (Class) oo[1];
            Number expResult = (Number) oo[2];
            boolean ok = (oo.length > 3) ? (Boolean) oo[3] : true;
            Number result = NumberTools.cast(num, numberType);
            if (ok) {
                assertEquals(expResult, result);
            } else {
                assertNotSame(expResult, result);
            }
        }
    }

    /**
     * Test of parse method, of class NumberTools.
     */
    @Test
    public void testParse() {
        System.out.println("parse");
        for (Object[] oo : new Object[][]{
            {"1"},
            {"1", 10},
            {"1", 10, int.class},
            {"1", 10, null, 1},
            {"1", 10, int.class, 1},
            {"1", 16},
            {"1", 8},
            {"80", 16, null, 128},
            {"80", 10, null, 80},
            {"77", 16, null, 119},
            {"77", 10, null, 77},
            {"77", 8, null, 63},
            {"1.0", null, float.class, 1f},
            {"1.0", null, double.class, 1.0}
        }) {
            String s = (String) oo[0];
            Integer radix = (oo.length > 1 && oo[1] != null) ? (Integer) oo[1] : 10;
            Class numberType = (oo.length > 2 && oo[2] != null) ? (Class) oo[2] : int.class;
            Number expResult = (oo.length > 3 && oo[3] != null) ? (Number) oo[3] : 1;
            if (numberType == float.class || numberType == Float.class || numberType == double.class || numberType == Double.class) {
                radix = null;
            }
            Number result = NumberTools.parse(s, radix, numberType);
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of bits method, of class NumberTools.
     */
    @Test
    public void testBits_byte() {
        System.out.println("bits: byte");
        for (Object[] oo : new Object[][]{
            {1, 1},
            {-1, 8},
            {2, 2},
            {4, 3},
            {8, 4},
            {127, 7},
            {128, 8},
            {1000, 8},}) {
            byte n = ((Number) oo[0]).byteValue();
            int expResult = (Integer) oo[1];
            int result = NumberTools.bits(n);
            System.out.println("n=" + n + "/" + expResult + ":" + result + ":" + binary(n & 0xFF, 8).length() + "\t\t" + binary(n & 0xFF, 8));
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of bits method, of class NumberTools.
     */
    @Test
    public void testBits_short() {
        System.out.println("bits: short");
        for (Object[] oo : new Object[][]{
            {1, 1},
            {-1, 16},
            {2, 2},
            {4, 3},
            {8, 4},
            {127, 7},
            {128, 8},
            {1000, 10},
            {32676, 15},
            {32767, 15},
            {32768, 16},
            {100000, 16},}) {
            short n = ((Number) oo[0]).shortValue();
            int expResult = (Integer) oo[1];
            int result = NumberTools.bits(n);
            System.out.println("n=" + n + "/" + expResult + ":" + result + ":" + binary(n & 0xFFFF, 16).length() + "\t\t" + binary(n & 0xFFFF, 16));
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of bits method, of class NumberTools.
     */
    @Test
    public void testBits_int() {
        System.out.println("bits: int");
        for (Object[] oo : new Object[][]{
            {1, 1},
            {-1, 32},
            {2, 2},
            {4, 3},
            {8, 4},
            {127, 7},
            {128, 8},
            {1000, 10},
            {32676, 15},
            {32767, 15},
            {32768, 16},
            {100000, 17},
            {0x7F441100, 31},
            {0x8F441100, 32},
            {0xFF441100, 32}
        }) {
            int n = ((Number) oo[0]).intValue();
            int expResult = (Integer) oo[1];
            int result = NumberTools.bits(n);
            System.out.println("n=" + n + "/" + expResult + ":" + result + ":" + binary(n & 0xFFFFFFFF, 32).length() + "\t\t" + binary(n & 0xFFFFFFFF, 32));
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of bits method, of class NumberTools.
     */
    @Test
    public void testBits_long() {
        System.out.println("bits: long");
        for (Object[] oo : new Object[][]{
            {1, 1},
            {-1L, 64},
            {2, 2},
            {4, 3},
            {8, 4},
            {127, 7},
            {128, 8},
            {1000, 10},
            {32676, 15},
            {32767, 15},
            {32768, 16},
            {100000, 17},
            {0x7F441100L, 31},
            {0x8F441100L, 32},
            {0xFF441100L, 32},
            {0xFF44110000000000L, 64}
        }) {
            long n = ((Number) oo[0]).longValue();
            int expResult = (Integer) oo[1];
            int result = NumberTools.bits(n);
            System.out.println("n=" + n + "/" + expResult + ":" + result + ":" + binary(n & 0xFFFFFFFFFFFFFFFFL, 64).length() + "\t\t" + binary(n & 0xFFFFFFFFFFFFFFFFL, 64));
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of bits method, of class NumberTools.
     */
    @Test
    public void testBits_BigInteger() {
        System.out.println("bits: BigInteger");
        for (Object[] oo : new Object[][]{
            {1, 1},
            {-1L, 0},
            {2, 2},
            {4, 3},
            {8, 4},
            {127, 7},
            {128, 8},
            {1000, 10},
            {32676, 15},
            {32767, 15},
            {32768, 16},
            {100000, 17},
            {0x7F441100L, 31},
            {0x8F441100L, 32},
            {0xFF441100L, 32},
            {0xFF44110000000000L, 56}
        }) {
            BigInteger n = BigInteger.valueOf(((Number) oo[0]).longValue());
            int expResult = (Integer) oo[1];
            int result = NumberTools.bits(n);
            System.out.println("n=" + n + "/" + expResult + ":" + result + ":" + binary(n, 64).length() + "\t\t" + binary(n, 64));
            assertEquals(expResult, result);
        }
    }

    public static String binary(Number n, int len) {
        String s = "I";
        if (n instanceof BigInteger) {
            s = ((BigInteger) n).toString(2);
        } else {
            s = Long.toBinaryString(n.longValue());
        }
        if (s.length() > len) {
            s = s.substring(s.length() - len, s.length());
        }
        return s;
    }

    /**
     * Test of bits method, of class NumberTools.
     */
    @Test
    public void testBits_Number() {
        System.out.println("bits");
        for (Object[] oo : new Object[][]{
            {(byte) 1, 1},
            {(short) 1, 1},
            {1, 1},
            {1L, 1},
            {(byte) -1, 8},
            {(short) -1, 16},
            {-1, 32},
            {-1L, 64},
            {2, 2},
            {4, 3},
            {8, 4},
            {127, 7},
            {128, 8},
            {1000, 10},
            {32676, 15},
            {32767, 15},
            {32768, 16},
            {100000, 17},
            {0x7F441100L, 31},
            {0x8F441100L, 32},
            {0xFF441100L, 32},
            {0xFF44110000000000L, 64},
            {BigInteger.valueOf(0xFF44110000000000L), 56},
            {1f, 0},
            {1.0, 0},
            {null, -1}
        }) {
            Number n = ((Number) oo[0]);
            int expResult = (Integer) oo[1];
            int result = NumberTools.bits(n);
            System.out.println("n=" + n + "/" + expResult + ":" + result);
            assertEquals(expResult, result);
        }
    }
}
