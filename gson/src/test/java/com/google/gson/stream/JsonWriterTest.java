/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.stream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.FormattingStyle;
import com.google.gson.Strictness;
import com.google.gson.internal.LazilyParsedNumber;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;

@SuppressWarnings("resource")
public final class JsonWriterTest {

  @Test
  public void testDefaultStrictness() throws IOException {
    JsonWriter jsonWriter = new JsonWriter(new StringWriter());
    assertThat(jsonWriter.getStrictness()).isEqualTo(Strictness.LEGACY_STRICT);
    jsonWriter.value(false);
    jsonWriter.close();
  }

  @SuppressWarnings("deprecation") // for JsonWriter.setLenient
  @Test
  public void testSetLenientTrue() throws IOException {
    JsonWriter jsonWriter = new JsonWriter(new StringWriter());
    jsonWriter.setLenient(true);
    assertThat(jsonWriter.getStrictness()).isEqualTo(Strictness.LENIENT);
    jsonWriter.value(false);
    jsonWriter.close();
  }

  @SuppressWarnings("deprecation") // for JsonWriter.setLenient
  @Test
  public void testSetLenientFalse() throws IOException {
    JsonWriter jsonWriter = new JsonWriter(new StringWriter());
    jsonWriter.setLenient(false);
    assertThat(jsonWriter.getStrictness()).isEqualTo(Strictness.LEGACY_STRICT);
    jsonWriter.value(false);
    jsonWriter.close();
  }

  @Test
  public void testSetStrictness() throws IOException {
    JsonWriter jsonWriter = new JsonWriter(new StringWriter());
    jsonWriter.setStrictness(Strictness.STRICT);
    assertThat(jsonWriter.getStrictness()).isEqualTo(Strictness.STRICT);
    jsonWriter.value(false);
    jsonWriter.close();
  }

  @Test
  public void testSetStrictnessNull() throws IOException {
    JsonWriter jsonWriter = new JsonWriter(new StringWriter());
    assertThrows(NullPointerException.class, () -> jsonWriter.setStrictness(null));
    jsonWriter.value(false);
    jsonWriter.close();
  }

  @Test
  public void testTopLevelValueTypes() throws IOException {
    StringWriter string1 = new StringWriter();
    JsonWriter writer1 = new JsonWriter(string1);
    writer1.value(true);
    writer1.close();
    assertThat(string1.toString()).isEqualTo("true");

    StringWriter string2 = new StringWriter();
    JsonWriter writer2 = new JsonWriter(string2);
    writer2.nullValue();
    writer2.close();
    assertThat(string2.toString()).isEqualTo("null");

    StringWriter string3 = new StringWriter();
    JsonWriter writer3 = new JsonWriter(string3);
    writer3.value(123);
    writer3.close();
    assertThat(string3.toString()).isEqualTo("123");

    StringWriter string4 = new StringWriter();
    JsonWriter writer4 = new JsonWriter(string4);
    writer4.value(123.4);
    writer4.close();
    assertThat(string4.toString()).isEqualTo("123.4");

    StringWriter string5 = new StringWriter();
    JsonWriter writert = new JsonWriter(string5);
    writert.value("a");
    writert.close();
    assertThat(string5.toString()).isEqualTo("\"a\"");
  }

  @Test
  public void testNameAsTopLevelValue() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> jsonWriter.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("Please begin an object before writing a name.");

    jsonWriter.value(12);
    jsonWriter.close();

    e = assertThrows(IllegalStateException.class, () -> jsonWriter.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("JsonWriter is closed.");
  }

  @Test
  public void testNameInArray() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);

    jsonWriter.beginArray();
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> jsonWriter.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("Please begin an object before writing a name.");

    jsonWriter.value(12);
    e = assertThrows(IllegalStateException.class, () -> jsonWriter.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("Please begin an object before writing a name.");

    jsonWriter.endArray();
    jsonWriter.close();

    assertThat(stringWriter.toString()).isEqualTo("[12]");
  }

  @Test
  public void testTwoNames() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.name("a");
    var e = assertThrows(IllegalStateException.class, () -> jsonWriter.name("a"));
    assertThat(e).hasMessageThat().isEqualTo("Already wrote a name, expecting a value.");
  }

  @Test
  public void testNameWithoutValue() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.name("a");
    var e = assertThrows(IllegalStateException.class, () -> jsonWriter.endObject());
    assertThat(e).hasMessageThat().isEqualTo("Dangling name: a");
  }

  @Test
  public void testValueWithoutName() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    var e = assertThrows(IllegalStateException.class, () -> jsonWriter.value(true));
    assertThat(e).hasMessageThat().isEqualTo("Nesting problem.");
  }

  @Test
  public void testMultipleTopLevelValues() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray().endArray();

    IllegalStateException expected =
        assertThrows(IllegalStateException.class, jsonWriter::beginArray);
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo(
            "Multiple top-level values support has not been enabled, use"
                + " `JsonWriter.setTopLevelSeparator(String)`");
  }

  @Test
  public void testMultipleTopLevelValuesStrict() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setStrictness(Strictness.STRICT);
    jsonWriter.beginArray().endArray();

    IllegalStateException expected =
        assertThrows(IllegalStateException.class, jsonWriter::beginArray);
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo(
            "Multiple top-level values support has not been enabled, use"
                + " `JsonWriter.setTopLevelSeparator(String)`");
  }

  @Test
  public void testMultipleTopLevelValuesLenient() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    writer.setStrictness(Strictness.LENIENT);
    writer.beginArray();
    writer.endArray();
    writer.beginArray();
    writer.endArray();
    writer.close();
    assertThat(stringWriter.toString()).isEqualTo("[][]");
  }

  @Test
  public void testMultipleTopLevelValuesStrictness() {
    JsonWriter writer = new JsonWriter(new StringWriter());
    assertThat(writer.getTopLevelSeparator()).isNull();

    writer.setStrictness(Strictness.STRICT);
    assertThat(writer.getTopLevelSeparator()).isNull();

    writer.setStrictness(Strictness.LEGACY_STRICT);
    assertThat(writer.getTopLevelSeparator()).isNull();

    writer.setStrictness(Strictness.LENIENT);
    assertThat(writer.getTopLevelSeparator()).isEqualTo("");

    writer.setStrictness(Strictness.STRICT);
    assertThat(writer.getTopLevelSeparator()).isNull();
    // Verify that it can be enabled independently of Strictness
    writer.setTopLevelSeparator("\n");
    assertThat(writer.getStrictness()).isEqualTo(Strictness.STRICT);
    assertThat(writer.getTopLevelSeparator()).isEqualTo("\n");
  }

  /**
   * Tests multiple top-level values, enabled with {@link JsonWriter#setTopLevelSeparator(String)}.
   */
  @Test
  public void testMultipleTopLevelValuesEnabled() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    writer.setStrictness(Strictness.STRICT);
    writer.setTopLevelSeparator("");

    writer.beginArray();
    writer.endArray();
    writer.beginObject();
    writer.endObject();
    writer.close();
    assertThat(stringWriter.toString()).isEqualTo("[]{}");

    stringWriter = new StringWriter();
    writer = new JsonWriter(stringWriter);
    writer.setStrictness(Strictness.STRICT);
    writer.setTopLevelSeparator(" \n ");

    writer.value(1);
    writer.value(2);
    writer.close();
    assertThat(stringWriter.toString()).isEqualTo("1 \n 2");
  }

  @Test
  public void testMultipleTopLevelValuesDisabled() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    // Normally lenient mode allows multiple top-level values
    writer.setStrictness(Strictness.LENIENT);
    writer.setTopLevelSeparator(null);

    writer.value(1);

    var e = assertThrows(IllegalStateException.class, () -> writer.value(2));
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Multiple top-level values support has not been enabled, use"
                + " `JsonWriter.setTopLevelSeparator(String)`");
  }

  @Test
  public void testBadNestingObject() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.beginObject();
    var e = assertThrows(IllegalStateException.class, () -> jsonWriter.endArray());
    assertThat(e).hasMessageThat().isEqualTo("Nesting problem.");
  }

  @Test
  public void testBadNestingArray() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.beginArray();
    var e = assertThrows(IllegalStateException.class, () -> jsonWriter.endObject());
    assertThat(e).hasMessageThat().isEqualTo("Nesting problem.");
  }

  @Test
  public void testNullName() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    assertThrows(NullPointerException.class, () -> jsonWriter.name(null));
  }

  @Test
  public void testNullStringValue() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.name("a");
    jsonWriter.value((String) null);
    jsonWriter.endObject();
    assertThat(stringWriter.toString()).isEqualTo("{\"a\":null}");
  }

  @Test
  public void testJsonValue() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.name("a");
    jsonWriter.jsonValue("{\"b\":true}");
    jsonWriter.name("c");
    jsonWriter.value(1);
    jsonWriter.endObject();
    assertThat(stringWriter.toString()).isEqualTo("{\"a\":{\"b\":true},\"c\":1}");
  }

  private static void assertNonFiniteFloatsExceptions(JsonWriter jsonWriter) throws IOException {
    jsonWriter.beginArray();

    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> jsonWriter.value(Float.NaN));
    assertThat(expected).hasMessageThat().isEqualTo("Numeric values must be finite, but was NaN");

    expected =
        assertThrows(
            IllegalArgumentException.class, () -> jsonWriter.value(Float.NEGATIVE_INFINITY));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("Numeric values must be finite, but was -Infinity");

    expected =
        assertThrows(
            IllegalArgumentException.class, () -> jsonWriter.value(Float.POSITIVE_INFINITY));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("Numeric values must be finite, but was Infinity");
  }

  @Test
  public void testNonFiniteFloats() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    assertNonFiniteFloatsExceptions(jsonWriter);
  }

  @Test
  public void testNonFiniteFloatsWhenStrict() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setStrictness(Strictness.STRICT);
    assertNonFiniteFloatsExceptions(jsonWriter);
  }

  private static void assertNonFiniteDoublesExceptions(JsonWriter jsonWriter) throws IOException {
    jsonWriter.beginArray();

    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> jsonWriter.value(Double.NaN));
    assertThat(expected).hasMessageThat().isEqualTo("Numeric values must be finite, but was NaN");

    expected =
        assertThrows(
            IllegalArgumentException.class, () -> jsonWriter.value(Double.NEGATIVE_INFINITY));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("Numeric values must be finite, but was -Infinity");

    expected =
        assertThrows(
            IllegalArgumentException.class, () -> jsonWriter.value(Double.POSITIVE_INFINITY));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("Numeric values must be finite, but was Infinity");
  }

  @Test
  public void testNonFiniteDoubles() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    assertNonFiniteDoublesExceptions(jsonWriter);
  }

  @Test
  public void testNonFiniteDoublesWhenStrict() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setStrictness(Strictness.STRICT);
    assertNonFiniteDoublesExceptions(jsonWriter);
  }

  private static void assertNonFiniteNumbersExceptions(JsonWriter jsonWriter) throws IOException {
    jsonWriter.beginArray();

    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class, () -> jsonWriter.value(Double.valueOf(Double.NaN)));
    assertThat(expected).hasMessageThat().isEqualTo("Numeric values must be finite, but was NaN");

    expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> jsonWriter.value(Double.valueOf(Double.NEGATIVE_INFINITY)));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("Numeric values must be finite, but was -Infinity");

    expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> jsonWriter.value(Double.valueOf(Double.POSITIVE_INFINITY)));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("Numeric values must be finite, but was Infinity");

    expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> jsonWriter.value(new LazilyParsedNumber("Infinity")));
    assertThat(expected)
        .hasMessageThat()
        .isEqualTo("Numeric values must be finite, but was Infinity");
  }

  @Test
  public void testNonFiniteNumbers() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    assertNonFiniteNumbersExceptions(jsonWriter);
  }

  @Test
  public void testNonFiniteNumbersWhenStrict() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setStrictness(Strictness.STRICT);
    assertNonFiniteNumbersExceptions(jsonWriter);
  }

  @Test
  public void testNonFiniteFloatsWhenLenient() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setStrictness(Strictness.LENIENT);
    jsonWriter.beginArray();
    jsonWriter.value(Float.NaN);
    jsonWriter.value(Float.NEGATIVE_INFINITY);
    jsonWriter.value(Float.POSITIVE_INFINITY);
    jsonWriter.endArray();
    assertThat(stringWriter.toString()).isEqualTo("[NaN,-Infinity,Infinity]");
  }

  @Test
  public void testNonFiniteDoublesWhenLenient() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setStrictness(Strictness.LENIENT);
    jsonWriter.beginArray();
    jsonWriter.value(Double.NaN);
    jsonWriter.value(Double.NEGATIVE_INFINITY);
    jsonWriter.value(Double.POSITIVE_INFINITY);
    jsonWriter.endArray();
    assertThat(stringWriter.toString()).isEqualTo("[NaN,-Infinity,Infinity]");
  }

  @Test
  public void testNonFiniteNumbersWhenLenient() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setStrictness(Strictness.LENIENT);
    jsonWriter.beginArray();
    jsonWriter.value(Double.valueOf(Double.NaN));
    jsonWriter.value(Double.valueOf(Double.NEGATIVE_INFINITY));
    jsonWriter.value(Double.valueOf(Double.POSITIVE_INFINITY));
    jsonWriter.value(new LazilyParsedNumber("Infinity"));
    jsonWriter.endArray();
    assertThat(stringWriter.toString()).isEqualTo("[NaN,-Infinity,Infinity,Infinity]");
  }

  @Test
  public void testFloats() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value(-0.0f);
    jsonWriter.value(1.0f);
    jsonWriter.value(Float.MAX_VALUE);
    jsonWriter.value(Float.MIN_VALUE);
    jsonWriter.value(0.0f);
    jsonWriter.value(-0.5f);
    jsonWriter.value(2.2250739E-38f);
    jsonWriter.value(3.723379f);
    jsonWriter.value((float) Math.PI);
    jsonWriter.value((float) Math.E);
    jsonWriter.endArray();
    jsonWriter.close();
    assertThat(stringWriter.toString())
        .isEqualTo(
            "[-0.0,"
                + "1.0,"
                + "3.4028235E38,"
                + "1.4E-45,"
                + "0.0,"
                + "-0.5,"
                + "2.2250739E-38,"
                + "3.723379,"
                + "3.1415927,"
                + "2.7182817]");
  }

  @Test
  public void testDoubles() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value(-0.0);
    jsonWriter.value(1.0);
    jsonWriter.value(Double.MAX_VALUE);
    jsonWriter.value(Double.MIN_VALUE);
    jsonWriter.value(0.0);
    jsonWriter.value(-0.5);
    jsonWriter.value(2.2250738585072014E-308);
    jsonWriter.value(Math.PI);
    jsonWriter.value(Math.E);
    jsonWriter.endArray();
    jsonWriter.close();
    assertThat(stringWriter.toString())
        .isEqualTo(
            "[-0.0,"
                + "1.0,"
                + "1.7976931348623157E308,"
                + "4.9E-324,"
                + "0.0,"
                + "-0.5,"
                + "2.2250738585072014E-308,"
                + "3.141592653589793,"
                + "2.718281828459045]");
  }

  @Test
  public void testLongs() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value(0);
    jsonWriter.value(1);
    jsonWriter.value(-1);
    jsonWriter.value(Long.MIN_VALUE);
    jsonWriter.value(Long.MAX_VALUE);
    jsonWriter.endArray();
    jsonWriter.close();
    assertThat(stringWriter.toString())
        .isEqualTo("[0," + "1," + "-1," + "-9223372036854775808," + "9223372036854775807]");
  }

  @Test
  public void testNumbers() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value(new BigInteger("0"));
    jsonWriter.value(new BigInteger("9223372036854775808"));
    jsonWriter.value(new BigInteger("-9223372036854775809"));
    jsonWriter.value(new BigDecimal("3.141592653589793238462643383"));
    jsonWriter.endArray();
    jsonWriter.close();
    assertThat(stringWriter.toString())
        .isEqualTo(
            "[0,"
                + "9223372036854775808,"
                + "-9223372036854775809,"
                + "3.141592653589793238462643383]");
  }

  /** Tests writing {@code Number} instances which are not one of the standard JDK ones. */
  @Test
  public void testNumbersCustomClass() throws IOException {
    String[] validNumbers = {
      "-0.0",
      "1.0",
      "1.7976931348623157E308",
      "4.9E-324",
      "0.0",
      "0.00",
      "-0.5",
      "2.2250738585072014E-308",
      "3.141592653589793",
      "2.718281828459045",
      "0",
      "0.01",
      "0e0",
      "1e+0",
      "1e-0",
      "1e0000", // leading 0 is allowed for exponent
      "1e00001",
      "1e+1",
    };

    for (String validNumber : validNumbers) {
      StringWriter stringWriter = new StringWriter();
      JsonWriter jsonWriter = new JsonWriter(stringWriter);

      jsonWriter.value(new LazilyParsedNumber(validNumber));
      jsonWriter.close();

      assertThat(stringWriter.toString()).isEqualTo(validNumber);
    }
  }

  @Test
  public void testMalformedNumbers() throws IOException {
    String[] malformedNumbers = {
      "some text",
      "",
      ".",
      "00",
      "01",
      "-00",
      "-",
      "--1",
      "+1", // plus sign is not allowed for integer part
      "+",
      "1,0",
      "1,000",
      "0.", // decimal digit is required
      ".1", // integer part is required
      "e1",
      ".e1",
      ".1e1",
      "1e-",
      "1e+",
      "1e--1",
      "1e+-1",
      "1e1e1",
      "1+e1",
      "1e1.0",
    };

    for (String malformedNumber : malformedNumbers) {
      JsonWriter jsonWriter = new JsonWriter(new StringWriter());
      var e =
          assertThrows(
              IllegalArgumentException.class,
              () -> jsonWriter.value(new LazilyParsedNumber(malformedNumber)));
      assertThat(e)
          .hasMessageThat()
          .isEqualTo(
              "String created by class com.google.gson.internal.LazilyParsedNumber is not a valid"
                  + " JSON number: "
                  + malformedNumber);
    }
  }

  @Test
  public void testBooleans() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value(true);
    jsonWriter.value(false);
    jsonWriter.endArray();
    assertThat(stringWriter.toString()).isEqualTo("[true,false]");
  }

  @Test
  public void testBoxedBooleans() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value((Boolean) true);
    jsonWriter.value((Boolean) false);
    jsonWriter.value((Boolean) null);
    jsonWriter.endArray();
    assertThat(stringWriter.toString()).isEqualTo("[true,false,null]");
  }

  @Test
  public void testNulls() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.nullValue();
    jsonWriter.endArray();
    assertThat(stringWriter.toString()).isEqualTo("[null]");
  }

  @Test
  public void testStrings() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value("a");
    jsonWriter.value("a\"");
    jsonWriter.value("\"");
    jsonWriter.value(":");
    jsonWriter.value(",");
    jsonWriter.value("\b");
    jsonWriter.value("\f");
    jsonWriter.value("\n");
    jsonWriter.value("\r");
    jsonWriter.value("\t");
    jsonWriter.value(" ");
    jsonWriter.value("\\");
    jsonWriter.value("{");
    jsonWriter.value("}");
    jsonWriter.value("[");
    jsonWriter.value("]");
    jsonWriter.value("\0");
    jsonWriter.value("\u0019");
    jsonWriter.endArray();
    assertThat(stringWriter.toString())
        .isEqualTo(
            "[\"a\","
                + "\"a\\\"\","
                + "\"\\\"\","
                + "\":\","
                + "\",\","
                + "\"\\b\","
                + "\"\\f\","
                + "\"\\n\","
                + "\"\\r\","
                + "\"\\t\","
                + "\" \","
                + "\"\\\\\","
                + "\"{\","
                + "\"}\","
                + "\"[\","
                + "\"]\","
                + "\"\\u0000\","
                + "\"\\u0019\"]");
  }

  @Test
  public void testUnicodeLineBreaksEscaped() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.value("\u2028 \u2029");
    jsonWriter.endArray();
    // JSON specification does not require that they are escaped, but Gson escapes them for
    // compatibility with JavaScript where they are considered line breaks
    assertThat(stringWriter.toString()).isEqualTo("[\"\\u2028 \\u2029\"]");
  }

  @Test
  public void testEmptyArray() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.endArray();
    assertThat(stringWriter.toString()).isEqualTo("[]");
  }

  @Test
  public void testEmptyObject() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.endObject();
    assertThat(stringWriter.toString()).isEqualTo("{}");
  }

  @Test
  public void testObjectsInArrays() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginArray();
    jsonWriter.beginObject();
    jsonWriter.name("a").value(5);
    jsonWriter.name("b").value(false);
    jsonWriter.endObject();
    jsonWriter.beginObject();
    jsonWriter.name("c").value(6);
    jsonWriter.name("d").value(true);
    jsonWriter.endObject();
    jsonWriter.endArray();
    assertThat(stringWriter.toString())
        .isEqualTo("[{\"a\":5,\"b\":false}," + "{\"c\":6,\"d\":true}]");
  }

  @Test
  public void testArraysInObjects() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.name("a");
    jsonWriter.beginArray();
    jsonWriter.value(5);
    jsonWriter.value(false);
    jsonWriter.endArray();
    jsonWriter.name("b");
    jsonWriter.beginArray();
    jsonWriter.value(6);
    jsonWriter.value(true);
    jsonWriter.endArray();
    jsonWriter.endObject();
    assertThat(stringWriter.toString()).isEqualTo("{\"a\":[5,false]," + "\"b\":[6,true]}");
  }

  @Test
  public void testDeepNestingArrays() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    for (int i = 0; i < 20; i++) {
      jsonWriter.beginArray();
    }
    for (int i = 0; i < 20; i++) {
      jsonWriter.endArray();
    }
    assertThat(stringWriter.toString()).isEqualTo("[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]");
  }

  @Test
  public void testDeepNestingObjects() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    for (int i = 0; i < 20; i++) {
      jsonWriter.name("a");
      jsonWriter.beginObject();
    }
    for (int i = 0; i < 20; i++) {
      jsonWriter.endObject();
    }
    jsonWriter.endObject();
    assertThat(stringWriter.toString())
        .isEqualTo(
            "{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":"
                + "{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{\"a\":{"
                + "}}}}}}}}}}}}}}}}}}}}}");
  }

  @Test
  public void testRepeatedName() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.beginObject();
    jsonWriter.name("a").value(true);
    jsonWriter.name("a").value(false);
    jsonWriter.endObject();
    // JsonWriter doesn't attempt to detect duplicate names
    assertThat(stringWriter.toString()).isEqualTo("{\"a\":true,\"a\":false}");
  }

  @Test
  public void testPrettyPrintObject() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setIndent("   ");

    jsonWriter.beginObject();
    jsonWriter.name("a").value(true);
    jsonWriter.name("b").value(false);
    jsonWriter.name("c").value(5.0);
    jsonWriter.name("e").nullValue();
    jsonWriter.name("f").beginArray();
    jsonWriter.value(6.0);
    jsonWriter.value(7.0);
    jsonWriter.endArray();
    jsonWriter.name("g").beginObject();
    jsonWriter.name("h").value(8.0);
    jsonWriter.name("i").value(9.0);
    jsonWriter.endObject();
    jsonWriter.endObject();

    String expected =
        "{\n"
            + "   \"a\": true,\n"
            + "   \"b\": false,\n"
            + "   \"c\": 5.0,\n"
            + "   \"e\": null,\n"
            + "   \"f\": [\n"
            + "      6.0,\n"
            + "      7.0\n"
            + "   ],\n"
            + "   \"g\": {\n"
            + "      \"h\": 8.0,\n"
            + "      \"i\": 9.0\n"
            + "   }\n"
            + "}";
    assertThat(stringWriter.toString()).isEqualTo(expected);
  }

  @Test
  public void testPrettyPrintArray() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setIndent("   ");

    jsonWriter.beginArray();
    jsonWriter.value(true);
    jsonWriter.value(false);
    jsonWriter.value(5.0);
    jsonWriter.nullValue();
    jsonWriter.beginObject();
    jsonWriter.name("a").value(6.0);
    jsonWriter.name("b").value(7.0);
    jsonWriter.endObject();
    jsonWriter.beginArray();
    jsonWriter.value(8.0);
    jsonWriter.value(9.0);
    jsonWriter.endArray();
    jsonWriter.endArray();

    String expected =
        "[\n"
            + "   true,\n"
            + "   false,\n"
            + "   5.0,\n"
            + "   null,\n"
            + "   {\n"
            + "      \"a\": 6.0,\n"
            + "      \"b\": 7.0\n"
            + "   },\n"
            + "   [\n"
            + "      8.0,\n"
            + "      9.0\n"
            + "   ]\n"
            + "]";
    assertThat(stringWriter.toString()).isEqualTo(expected);
  }

  @Test
  public void testClosedWriterThrowsOnStructure() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    writer.beginArray();
    writer.endArray();
    writer.close();

    String expectedMessage = "JsonWriter is closed.";
    var e = assertThrows(IllegalStateException.class, () -> writer.beginArray());
    assertThat(e).hasMessageThat().isEqualTo(expectedMessage);

    e = assertThrows(IllegalStateException.class, () -> writer.endArray());
    assertThat(e).hasMessageThat().isEqualTo(expectedMessage);

    e = assertThrows(IllegalStateException.class, () -> writer.beginObject());
    assertThat(e).hasMessageThat().isEqualTo(expectedMessage);

    e = assertThrows(IllegalStateException.class, () -> writer.endObject());
    assertThat(e).hasMessageThat().isEqualTo(expectedMessage);
  }

  @Test
  public void testClosedWriterThrowsOnName() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    writer.beginArray();
    writer.endArray();
    writer.close();
    var e = assertThrows(IllegalStateException.class, () -> writer.name("a"));
    assertThat(e).hasMessageThat().isEqualTo("JsonWriter is closed.");
  }

  @Test
  public void testClosedWriterThrowsOnValue() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    writer.beginArray();
    writer.endArray();
    writer.close();
    var e = assertThrows(IllegalStateException.class, () -> writer.value("a"));
    assertThat(e).hasMessageThat().isEqualTo("JsonWriter is closed.");
  }

  @Test
  public void testClosedWriterThrowsOnFlush() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    writer.beginArray();
    writer.endArray();
    writer.close();
    var e = assertThrows(IllegalStateException.class, () -> writer.flush());
    assertThat(e).hasMessageThat().isEqualTo("JsonWriter is closed.");
  }

  @Test
  public void testWriterCloseIsIdempotent() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter writer = new JsonWriter(stringWriter);
    writer.beginArray();
    writer.endArray();
    writer.close();
    assertThat(stringWriter.toString()).isEqualTo("[]");
    writer.close();
    assertThat(stringWriter.toString()).isEqualTo("[]");
  }

  @Test
  public void testSetGetFormattingStyle() throws IOException {
    String lineSeparator = "\r\n";

    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    // Default should be FormattingStyle.COMPACT
    assertThat(jsonWriter.getFormattingStyle()).isSameInstanceAs(FormattingStyle.COMPACT);
    jsonWriter.setFormattingStyle(
        FormattingStyle.PRETTY.withIndent(" \t ").withNewline(lineSeparator));

    jsonWriter.beginArray();
    jsonWriter.value(true);
    jsonWriter.value("text");
    jsonWriter.value(5.0);
    jsonWriter.nullValue();
    jsonWriter.endArray();

    String expected =
        "[\r\n" //
            + " \t true,\r\n" //
            + " \t \"text\",\r\n" //
            + " \t 5.0,\r\n" //
            + " \t null\r\n" //
            + "]";
    assertThat(stringWriter.toString()).isEqualTo(expected);

    assertThat(jsonWriter.getFormattingStyle().getNewline()).isEqualTo(lineSeparator);
  }

  @Test
  public void testIndentOverwritesFormattingStyle() throws IOException {
    StringWriter stringWriter = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(stringWriter);
    jsonWriter.setFormattingStyle(FormattingStyle.COMPACT);
    // Should overwrite formatting style
    jsonWriter.setIndent("  ");

    jsonWriter.beginObject();
    jsonWriter.name("a");
    jsonWriter.beginArray();
    jsonWriter.value(1);
    jsonWriter.value(2);
    jsonWriter.endArray();
    jsonWriter.endObject();

    String expected =
        "{\n" //
            + "  \"a\": [\n" //
            + "    1,\n" //
            + "    2\n" //
            + "  ]\n" //
            + "}";
    assertThat(stringWriter.toString()).isEqualTo(expected);
  }
}
