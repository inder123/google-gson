package com.google.gson.internal.bind.util;

import com.diffblue.deeptestutils.Reflector;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ISO8601UtilsTestCase {

  @Rule public ExpectedException thrown = ExpectedException.none();

  /* testedClasses: com/google/gson/internal/bind/util/ISO8601Utils.java */
  /*
   * Test generated by Diffblue Deeptest.This test covers `int
   * indexOfNonDigit(String)' block 2 (line 345)
   * This test covers `int indexOfNonDigit(String)' block 3 (line 345)
   * This test covers `int indexOfNonDigit(String)' block 4 (line 345)
   * This test covers `int indexOfNonDigit(String)' block 6 (line 346)
   * This test covers `int indexOfNonDigit(String)' block 7 (line 346)
   * This test covers `int indexOfNonDigit(String)' block 8 (line 347)
   * This test covers `int indexOfNonDigit(String)' block 10 (line 347)
   * This test covers `int indexOfNonDigit(String)' block 11 (line 345)
   * This test covers `int indexOfNonDigit(String)' block 12 (line 347)
   * This test covers `int indexOfNonDigit(String)' block 13 (line 345)
   *
   */

  @Test
  public void indexOfNonDigitInputNotNullZeroOutputPositive()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    // Arrange
    final String string = "2`88888888";
    final int offset = 0;

    // Act
    final Class<?> classUnderTest =
        Reflector.forName("com.google.gson.internal.bind.util.ISO8601Utils");
    final Method methodUnderTest = classUnderTest.getDeclaredMethod(
        "indexOfNonDigit", Reflector.forName("java.lang.String"), Reflector.forName("int"));
    methodUnderTest.setAccessible(true);
    final int retval = (Integer)methodUnderTest.invoke(null, string, offset);

    // Assert result
    Assert.assertEquals(1, retval);
  }

  /*
   * Test generated by Diffblue Deeptest.
   * This test case covers:
   *
   */

  @Test
  public void indexOfNonDigitInputNotNullZeroOutputZero()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    // Arrange
    final String string = "";
    final int offset = 0;

    // Act
    final Class<?> classUnderTest =
        Reflector.forName("com.google.gson.internal.bind.util.ISO8601Utils");
    final Method methodUnderTest = classUnderTest.getDeclaredMethod(
        "indexOfNonDigit", Reflector.forName("java.lang.String"), Reflector.forName("int"));
    methodUnderTest.setAccessible(true);
    final int retval = (Integer)methodUnderTest.invoke(null, string, offset);

    // Assert result
    Assert.assertEquals(0, retval);
  }

  /*
   * Test generated by Diffblue Deeptest.This test covers `int
   * indexOfNonDigit(String)' block 2 (line 345)
   * This test covers `int indexOfNonDigit(String)' block 3 (line 345)
   * This test covers `int indexOfNonDigit(String)' block 4 (line 345)
   * This test covers `int indexOfNonDigit(String)' block 6 (line 346)
   * This test covers `int indexOfNonDigit(String)' block 7 (line 346)
   * This test covers `int indexOfNonDigit(String)' block 8 (line 347)
   * This test covers `int indexOfNonDigit(String)' block 9 (line 347)
   * This test covers `int indexOfNonDigit(String)' block 12 (line 347)
   *
   */

  @Test
  public void indexOfNonDigitInputNotNullZeroOutputZero2()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    // Arrange
    final String string = "\"`((((((((";
    final int offset = 0;

    // Act
    final Class<?> classUnderTest =
        Reflector.forName("com.google.gson.internal.bind.util.ISO8601Utils");
    final Method methodUnderTest = classUnderTest.getDeclaredMethod(
        "indexOfNonDigit", Reflector.forName("java.lang.String"), Reflector.forName("int"));
    methodUnderTest.setAccessible(true);
    final int retval = (Integer)methodUnderTest.invoke(null, string, offset);

    // Assert result
    Assert.assertEquals(0, retval);
  }

  /*
   * Test generated by Diffblue Deeptest.This test covers `int parseInt(String,
   * int)' block 1 (line 301)
   * This test covers `int parseInt(String, int)' block 3 (line 301)
   * This test covers `int parseInt(String, int)' block 4 (line 301)
   * This test covers `int parseInt(String, int)' block 6 (line 301)
   * This test covers `int parseInt(String, int)' block 9 (line 305)
   * This test covers `int parseInt(String, int)' block 10 (line 306)
   * This test covers `int parseInt(String, int)' block 11 (line 308)
   * This test covers `int parseInt(String, int)' block 12 (line 309)
   * This test covers `int parseInt(String, int)' block 13 (line 309)
   * This test covers `int parseInt(String, int)' block 14 (line 309)
   * This test covers `int parseInt(String, int)' block 15 (line 309)
   * This test covers `int parseInt(String, int)' block 16 (line 310)
   * This test covers `int parseInt(String, int)' block 17 (line 313)
   * This test covers `int parseInt(String, int)' block 27 (line 313)
   * This test covers `int parseInt(String, int)' block 28 (line 315)
   * This test covers `int parseInt(String, int)' block 29 (line 316)
   * This test covers `int parseInt(String, int)' block 30 (line 316)
   * This test covers `int parseInt(String, int)' block 31 (line 316)
   * This test covers `int parseInt(String, int)' block 32 (line 316)
   * This test covers `int parseInt(String, int)' block 33 (line 317)
   * This test covers `int parseInt(String, int)' block 34 (line 320)
   * This test covers `int parseInt(String, int)' block 44 (line 320)
   * This test covers `int parseInt(String, int)' block 45 (line 321)
   * This test covers `int parseInt(String, int)' block 46 (line 321)
   * This test covers `int parseInt(String, int)' block 47 (line 323)
   *
   */

  @Test
  public void parseIntInputNotNullPositivePositiveOutputZero()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    // Arrange
    final String value = "0000000000";
    final int beginIndex = 8;
    final int endIndex = 10;

    // Act
    final Class<?> classUnderTest =
        Reflector.forName("com.google.gson.internal.bind.util.ISO8601Utils");
    final Method methodUnderTest =
        classUnderTest.getDeclaredMethod("parseInt", Reflector.forName("java.lang.String"),
                                         Reflector.forName("int"), Reflector.forName("int"));
    methodUnderTest.setAccessible(true);
    final int retval = (Integer)methodUnderTest.invoke(null, value, beginIndex, endIndex);

    // Assert result
    Assert.assertEquals(0, retval);
  }

  /*
   * Test generated by Diffblue Deeptest.This test covers `int parseInt(String,
   * int)' block 1 (line 301)
   * This test covers `int parseInt(String, int)' block 3 (line 301)
   * This test covers `int parseInt(String, int)' block 4 (line 301)
   * This test covers `int parseInt(String, int)' block 6 (line 301)
   * This test covers `int parseInt(String, int)' block 9 (line 305)
   * This test covers `int parseInt(String, int)' block 10 (line 306)
   * This test covers `int parseInt(String, int)' block 11 (line 308)
   * This test covers `int parseInt(String, int)' block 12 (line 309)
   * This test covers `int parseInt(String, int)' block 13 (line 309)
   * This test covers `int parseInt(String, int)' block 14 (line 309)
   * This test covers `int parseInt(String, int)' block 15 (line 309)
   * This test covers `int parseInt(String, int)' block 16 (line 310)
   * This test covers `int parseInt(String, int)' block 17 (line 313)
   * This test covers `int parseInt(String, int)' block 27 (line 313)
   * This test covers `int parseInt(String, int)' block 28 (line 315)
   * This test covers `int parseInt(String, int)' block 47 (line 323)
   *
   */

  @Test
  public void parseIntInputNotNullZeroPositiveOutputPositive()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

    // Arrange
    final String value = "880";
    final int beginIndex = 0;
    final int endIndex = 1;

    // Act
    final Class<?> classUnderTest =
        Reflector.forName("com.google.gson.internal.bind.util.ISO8601Utils");
    final Method methodUnderTest =
        classUnderTest.getDeclaredMethod("parseInt", Reflector.forName("java.lang.String"),
                                         Reflector.forName("int"), Reflector.forName("int"));
    methodUnderTest.setAccessible(true);
    final int retval = (Integer)methodUnderTest.invoke(null, value, beginIndex, endIndex);

    // Assert result
    Assert.assertEquals(8, retval);
  }
}
