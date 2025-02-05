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
import static com.google.gson.stream.JsonToken.BEGIN_ARRAY;
import static com.google.gson.stream.JsonToken.BEGIN_OBJECT;
import static com.google.gson.stream.JsonToken.BOOLEAN;
import static com.google.gson.stream.JsonToken.END_ARRAY;
import static com.google.gson.stream.JsonToken.END_OBJECT;
import static com.google.gson.stream.JsonToken.NAME;
import static com.google.gson.stream.JsonToken.NULL;
import static com.google.gson.stream.JsonToken.NUMBER;
import static com.google.gson.stream.JsonToken.STRING;
import static org.junit.Assert.assertThrows;

import com.google.gson.Strictness;
import com.google.gson.internal.bind.TypeAdapters;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("resource")
public final class JsonReaderTest {

  @Test
  public void testDefaultStrictness() {
    JsonReader reader = new JsonReader(reader("{}"));
    assertThat(reader.getStrictness()).isEqualTo(Strictness.LEGACY_STRICT);
  }

  @SuppressWarnings("deprecation") // for JsonReader.setLenient
  @Test
  public void testSetLenientTrue() {
    JsonReader reader = new JsonReader(reader("{}"));
    reader.setLenient(true);
    assertThat(reader.getStrictness()).isEqualTo(Strictness.LENIENT);
  }

  @SuppressWarnings("deprecation") // for JsonReader.setLenient
  @Test
  public void testSetLenientFalse() {
    JsonReader reader = new JsonReader(reader("{}"));
    reader.setLenient(false);
    assertThat(reader.getStrictness()).isEqualTo(Strictness.LEGACY_STRICT);
  }

  @Test
  public void testSetStrictness() {
    JsonReader reader = new JsonReader(reader("{}"));
    reader.setStrictness(Strictness.STRICT);
    assertThat(reader.getStrictness()).isEqualTo(Strictness.STRICT);
  }

  @Test
  public void testSetStrictnessNull() {
    JsonReader reader = new JsonReader(reader("{}"));
    assertThrows(NullPointerException.class, () -> reader.setStrictness(null));
  }

  @Test
  public void testEscapedNewlineNotAllowedInStrictMode() {
    String json = "\"\\\n\"";
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.STRICT);

    IOException expected = assertThrows(IOException.class, reader::nextString);
    assertThat(expected)
        .hasMessageThat()
        .startsWith("Cannot escape a newline character in strict mode");
  }

  @Test
  public void testEscapedNewlineAllowedInDefaultMode() throws IOException {
    String json = "\"\\\n\"";
    JsonReader reader = new JsonReader(reader(json));
    assertThat(reader.nextString()).isEqualTo("\n");
  }

  @Test
  public void testStrictModeFailsToParseUnescapedControlCharacter() {
    String json = "\"\0\"";
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.STRICT);

    IOException expected = assertThrows(IOException.class, reader::nextString);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Unescaped control characters (\\u0000-\\u001F) are not allowed in strict mode");

    json = "\"\t\"";
    reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.STRICT);

    expected = assertThrows(IOException.class, reader::nextString);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Unescaped control characters (\\u0000-\\u001F) are not allowed in strict mode");

    json = "\"\u001F\"";
    reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.STRICT);

    expected = assertThrows(IOException.class, reader::nextString);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Unescaped control characters (\\u0000-\\u001F) are not allowed in strict mode");
  }

  @Test
  public void testStrictModeAllowsOtherControlCharacters() throws IOException {
    // JSON specification only forbids control characters U+0000 - U+001F, other control characters
    // should be allowed
    String json = "\"\u007F\u009F\"";
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.STRICT);
    assertThat(reader.nextString()).isEqualTo("\u007F\u009F");
  }

  @Test
  public void testNonStrictModeParsesUnescapedControlCharacter() throws IOException {
    String json = "\"\t\"";
    JsonReader reader = new JsonReader(reader(json));
    assertThat(reader.nextString()).isEqualTo("\t");
  }

  @Test
  public void testCapitalizedTrueFailWhenStrict() {
    JsonReader reader = new JsonReader(reader("TRUE"));
    reader.setStrictness(Strictness.STRICT);

    IOException expected = assertThrows(IOException.class, reader::nextBoolean);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON"
                + " at line 1 column 1 path $\n");

    reader = new JsonReader(reader("True"));
    reader.setStrictness(Strictness.STRICT);

    expected = assertThrows(IOException.class, reader::nextBoolean);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON"
                + " at line 1 column 1 path $\n");
  }

  @Test
  public void testCapitalizedFalseFailWhenStrict() {
    JsonReader reader = new JsonReader(reader("FALSE"));
    reader.setStrictness(Strictness.STRICT);

    IOException expected = assertThrows(IOException.class, reader::nextBoolean);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON"
                + " at line 1 column 1 path $\n");

    reader = new JsonReader(reader("FaLse"));
    reader.setStrictness(Strictness.STRICT);

    expected = assertThrows(IOException.class, reader::nextBoolean);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON"
                + " at line 1 column 1 path $\n");
  }

  @Test
  public void testCapitalizedNullFailWhenStrict() {
    JsonReader reader = new JsonReader(reader("NULL"));
    reader.setStrictness(Strictness.STRICT);

    IOException expected = assertThrows(IOException.class, reader::nextNull);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON"
                + " at line 1 column 1 path $\n");

    reader = new JsonReader(reader("nulL"));
    reader.setStrictness(Strictness.STRICT);

    expected = assertThrows(IOException.class, reader::nextNull);
    assertThat(expected)
        .hasMessageThat()
        .startsWith(
            "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON"
                + " at line 1 column 1 path $\n");
  }

  @Test
  public void testReadArray() throws IOException {
    JsonReader reader = new JsonReader(reader("[true, true]"));
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.nextBoolean()).isTrue();
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testReadEmptyArray() throws IOException {
    JsonReader reader = new JsonReader(reader("[]"));
    reader.beginArray();
    assertThat(reader.hasNext()).isFalse();
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testReadObject() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\": \"android\", \"b\": \"banana\"}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextString()).isEqualTo("android");
    assertThat(reader.nextName()).isEqualTo("b");
    assertThat(reader.nextString()).isEqualTo("banana");
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testReadEmptyObject() throws IOException {
    JsonReader reader = new JsonReader(reader("{}"));
    reader.beginObject();
    assertThat(reader.hasNext()).isFalse();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testHasNextEndOfDocument() throws IOException {
    JsonReader reader = new JsonReader(reader("{}"));
    reader.beginObject();
    reader.endObject();
    assertThat(reader.hasNext()).isFalse();
  }

  @Test
  public void testSkipArray() throws IOException {
    JsonReader reader =
        new JsonReader(reader("{\"a\": [\"one\", \"two\", \"three\"], \"b\": 123}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    reader.skipValue();
    assertThat(reader.nextName()).isEqualTo("b");
    assertThat(reader.nextInt()).isEqualTo(123);
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipArrayAfterPeek() throws Exception {
    JsonReader reader =
        new JsonReader(reader("{\"a\": [\"one\", \"two\", \"three\"], \"b\": 123}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.peek()).isEqualTo(BEGIN_ARRAY);
    reader.skipValue();
    assertThat(reader.nextName()).isEqualTo("b");
    assertThat(reader.nextInt()).isEqualTo(123);
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipTopLevelObject() throws Exception {
    JsonReader reader =
        new JsonReader(reader("{\"a\": [\"one\", \"two\", \"three\"], \"b\": 123}"));
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipObject() throws IOException {
    JsonReader reader =
        new JsonReader(
            reader("{\"a\": { \"c\": [], \"d\": [true, true, {}] }, \"b\": \"banana\"}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    reader.skipValue();
    assertThat(reader.nextName()).isEqualTo("b");
    reader.skipValue();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipObjectAfterPeek() throws Exception {
    String json =
        "{"
            + "  \"one\": { \"num\": 1 }"
            + ", \"two\": { \"num\": 2 }"
            + ", \"three\": { \"num\": 3 }"
            + "}";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("one");
    assertThat(reader.peek()).isEqualTo(BEGIN_OBJECT);
    reader.skipValue();
    assertThat(reader.nextName()).isEqualTo("two");
    assertThat(reader.peek()).isEqualTo(BEGIN_OBJECT);
    reader.skipValue();
    assertThat(reader.nextName()).isEqualTo("three");
    reader.skipValue();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipObjectName() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\": 1}"));
    reader.beginObject();
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.NUMBER);
    assertThat(reader.getPath()).isEqualTo("$.<skipped>");
    assertThat(reader.nextInt()).isEqualTo(1);
  }

  @Test
  public void testSkipObjectNameSingleQuoted() throws IOException {
    JsonReader reader = new JsonReader(reader("{'a': 1}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.NUMBER);
    assertThat(reader.getPath()).isEqualTo("$.<skipped>");
    assertThat(reader.nextInt()).isEqualTo(1);
  }

  @Test
  public void testSkipObjectNameUnquoted() throws IOException {
    JsonReader reader = new JsonReader(reader("{a: 1}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.NUMBER);
    assertThat(reader.getPath()).isEqualTo("$.<skipped>");
    assertThat(reader.nextInt()).isEqualTo(1);
  }

  @Test
  public void testSkipInteger() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":123456789,\"b\":-123456789}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    reader.skipValue();
    assertThat(reader.nextName()).isEqualTo("b");
    reader.skipValue();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipDouble() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":-123.456e-789,\"b\":123456789.0}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    reader.skipValue();
    assertThat(reader.nextName()).isEqualTo("b");
    reader.skipValue();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipValueAfterEndOfDocument() throws IOException {
    JsonReader reader = new JsonReader(reader("{}"));
    reader.beginObject();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);

    assertThat(reader.getPath()).isEqualTo("$");
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
    assertThat(reader.getPath()).isEqualTo("$");
  }

  @Test
  public void testSkipValueAtArrayEnd() throws IOException {
    JsonReader reader = new JsonReader(reader("[]"));
    reader.beginArray();
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
    assertThat(reader.getPath()).isEqualTo("$");
  }

  @Test
  public void testSkipValueAtObjectEnd() throws IOException {
    JsonReader reader = new JsonReader(reader("{}"));
    reader.beginObject();
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
    assertThat(reader.getPath()).isEqualTo("$");
  }

  @Test
  public void testHelloWorld() throws IOException {
    String json =
        "{\n" //
            + "   \"hello\": true,\n" //
            + "   \"foo\": [\"world\"]\n" //
            + "}";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("hello");
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.nextName()).isEqualTo("foo");
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo("world");
    reader.endArray();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testInvalidJsonInput() throws IOException {
    String json =
        "{\n" //
            + "   \"h\\ello\": true,\n" //
            + "   \"foo\": [\"world\"]\n" //
            + "}";

    JsonReader reader = new JsonReader(reader(json));
    reader.beginObject();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextName());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Invalid escape sequence at line 2 column 8 path $.\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @SuppressWarnings("unused")
  @Test
  public void testNulls() {
    assertThrows(NullPointerException.class, () -> new JsonReader(null));
  }

  @Test
  public void testEmptyString() {
    assertThrows(EOFException.class, () -> new JsonReader(reader("")).beginArray());
    assertThrows(EOFException.class, () -> new JsonReader(reader("")).beginObject());
  }

  @Test
  public void testCharacterUnescaping() throws IOException {
    String json =
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
            + "\"\\u0019\","
            + "\"\\u20AC\""
            + "]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo("a");
    assertThat(reader.nextString()).isEqualTo("a\"");
    assertThat(reader.nextString()).isEqualTo("\"");
    assertThat(reader.nextString()).isEqualTo(":");
    assertThat(reader.nextString()).isEqualTo(",");
    assertThat(reader.nextString()).isEqualTo("\b");
    assertThat(reader.nextString()).isEqualTo("\f");
    assertThat(reader.nextString()).isEqualTo("\n");
    assertThat(reader.nextString()).isEqualTo("\r");
    assertThat(reader.nextString()).isEqualTo("\t");
    assertThat(reader.nextString()).isEqualTo(" ");
    assertThat(reader.nextString()).isEqualTo("\\");
    assertThat(reader.nextString()).isEqualTo("{");
    assertThat(reader.nextString()).isEqualTo("}");
    assertThat(reader.nextString()).isEqualTo("[");
    assertThat(reader.nextString()).isEqualTo("]");
    assertThat(reader.nextString()).isEqualTo("\0");
    assertThat(reader.nextString()).isEqualTo("\u0019");
    assertThat(reader.nextString()).isEqualTo("\u20AC");
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testReaderDoesNotTreatU2028U2029AsNewline() throws IOException {
    // This test shows that the JSON string [\n"whatever"] is seen as valid
    // And the JSON string [\u2028"whatever"] is not.
    String jsonInvalid2028 = "[\u2028\"whatever\"]";
    JsonReader readerInvalid2028 = new JsonReader(reader(jsonInvalid2028));
    readerInvalid2028.beginArray();
    assertThrows(IOException.class, readerInvalid2028::nextString);

    String jsonInvalid2029 = "[\u2029\"whatever\"]";
    JsonReader readerInvalid2029 = new JsonReader(reader(jsonInvalid2029));
    readerInvalid2029.beginArray();
    assertThrows(IOException.class, readerInvalid2029::nextString);

    String jsonValid = "[\n\"whatever\"]";
    JsonReader readerValid = new JsonReader(reader(jsonValid));
    readerValid.beginArray();
    assertThat(readerValid.nextString()).isEqualTo("whatever");

    // And even in STRICT mode U+2028 and U+2029 are not considered control characters
    // and can appear unescaped in JSON string
    String jsonValid2028And2029 = "\"whatever\u2028\u2029\"";
    JsonReader readerValid2028And2029 = new JsonReader(reader(jsonValid2028And2029));
    readerValid2028And2029.setStrictness(Strictness.STRICT);
    assertThat(readerValid2028And2029.nextString()).isEqualTo("whatever\u2028\u2029");
  }

  @Test
  public void testEscapeCharacterQuoteInStrictMode() {
    String json = "\"\\'\"";
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.STRICT);

    IOException expected = assertThrows(IOException.class, reader::nextString);
    assertThat(expected)
        .hasMessageThat()
        .startsWith("Invalid escaped character \"'\" in strict mode");
  }

  @Test
  public void testEscapeCharacterQuoteWithoutStrictMode() throws IOException {
    String json = "\"\\'\"";
    JsonReader reader = new JsonReader(reader(json));
    assertThat(reader.nextString()).isEqualTo("'");
  }

  @Test
  public void testUnescapingInvalidCharacters() throws IOException {
    String json = "[\"\\u000g\"]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Malformed Unicode escape \\u000g at line 1 column 5 path $[0]\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testUnescapingTruncatedCharacters() throws IOException {
    String json = "[\"\\u000";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Unterminated escape sequence at line 1 column 5 path $[0]\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testUnescapingTruncatedSequence() throws IOException {
    String json = "[\"\\";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Unterminated escape sequence at line 1 column 4 path $[0]\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testIntegersWithFractionalPartSpecified() throws IOException {
    JsonReader reader = new JsonReader(reader("[1.0,1.0,1.0]"));
    reader.beginArray();
    assertThat(reader.nextDouble()).isEqualTo(1.0);
    assertThat(reader.nextInt()).isEqualTo(1);
    assertThat(reader.nextLong()).isEqualTo(1L);
  }

  @Test
  public void testDoubles() throws IOException {
    String json =
        "[-0.0,"
            + "1.0,"
            + "1.7976931348623157E308,"
            + "4.9E-324,"
            + "0.0,"
            + "0.00,"
            + "-0.5,"
            + "2.2250738585072014E-308,"
            + "3.141592653589793,"
            + "2.718281828459045,"
            + "0,"
            + "0.01,"
            + "0e0,"
            + "1e+0,"
            + "1e-0,"
            + "1e0000," // leading 0 is allowed for exponent
            + "1e00001,"
            + "1e+1]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    assertThat(reader.nextDouble()).isEqualTo(-0.0);
    assertThat(reader.nextDouble()).isEqualTo(1.0);
    assertThat(reader.nextDouble()).isEqualTo(1.7976931348623157E308);
    assertThat(reader.nextDouble()).isEqualTo(4.9E-324);
    assertThat(reader.nextDouble()).isEqualTo(0.0);
    assertThat(reader.nextDouble()).isEqualTo(0.0);
    assertThat(reader.nextDouble()).isEqualTo(-0.5);
    assertThat(reader.nextDouble()).isEqualTo(2.2250738585072014E-308);
    assertThat(reader.nextDouble()).isEqualTo(3.141592653589793);
    assertThat(reader.nextDouble()).isEqualTo(2.718281828459045);
    assertThat(reader.nextDouble()).isEqualTo(0.0);
    assertThat(reader.nextDouble()).isEqualTo(0.01);
    assertThat(reader.nextDouble()).isEqualTo(0.0);
    assertThat(reader.nextDouble()).isEqualTo(1.0);
    assertThat(reader.nextDouble()).isEqualTo(1.0);
    assertThat(reader.nextDouble()).isEqualTo(1.0);
    assertThat(reader.nextDouble()).isEqualTo(10.0);
    assertThat(reader.nextDouble()).isEqualTo(10.0);
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testStrictNonFiniteDoubles() throws IOException {
    String json = "[NaN]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextDouble());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @Test
  public void testStrictQuotedNonFiniteDoubles() throws IOException {
    String json = "[\"NaN\"]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextDouble());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "JSON forbids NaN and infinities: NaN at line 1 column 7 path $[0]\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testLenientNonFiniteDoubles() throws IOException {
    String json = "[NaN, -Infinity, Infinity]";
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextDouble()).isNaN();
    assertThat(reader.nextDouble()).isEqualTo(Double.NEGATIVE_INFINITY);
    assertThat(reader.nextDouble()).isEqualTo(Double.POSITIVE_INFINITY);
    reader.endArray();
  }

  @Test
  public void testLenientQuotedNonFiniteDoubles() throws IOException {
    String json = "[\"NaN\", \"-Infinity\", \"Infinity\"]";
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextDouble()).isNaN();
    assertThat(reader.nextDouble()).isEqualTo(Double.NEGATIVE_INFINITY);
    assertThat(reader.nextDouble()).isEqualTo(Double.POSITIVE_INFINITY);
    reader.endArray();
  }

  @Test
  public void testStrictNonFiniteDoublesWithSkipValue() throws IOException {
    String json = "[NaN]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @Test
  public void testLongs() throws IOException {
    String json =
        "[0,0,0," + "1,1,1," + "-1,-1,-1," + "-9223372036854775808," + "9223372036854775807]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    assertThat(reader.nextLong()).isEqualTo(0L);
    assertThat(reader.nextInt()).isEqualTo(0);
    assertThat(reader.nextDouble()).isEqualTo(0.0);
    assertThat(reader.nextLong()).isEqualTo(1L);
    assertThat(reader.nextInt()).isEqualTo(1);
    assertThat(reader.nextDouble()).isEqualTo(1.0);
    assertThat(reader.nextLong()).isEqualTo(-1L);
    assertThat(reader.nextInt()).isEqualTo(-1);
    assertThat(reader.nextDouble()).isEqualTo(-1.0);

    assertThrows(NumberFormatException.class, () -> reader.nextInt());
    assertThat(reader.nextLong()).isEqualTo(Long.MIN_VALUE);

    assertThrows(NumberFormatException.class, () -> reader.nextInt());
    assertThat(reader.nextLong()).isEqualTo(Long.MAX_VALUE);

    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testNumberWithOctalPrefix() throws IOException {
    String number = "01";
    String expectedLocation = "line 1 column 1 path $";

    var e = assertThrows(MalformedJsonException.class, () -> new JsonReader(reader(number)).peek());
    assertStrictError(e, expectedLocation);

    e = assertThrows(MalformedJsonException.class, () -> new JsonReader(reader(number)).nextInt());
    assertStrictError(e, expectedLocation);

    e = assertThrows(MalformedJsonException.class, () -> new JsonReader(reader(number)).nextLong());
    assertStrictError(e, expectedLocation);

    e =
        assertThrows(
            MalformedJsonException.class, () -> new JsonReader(reader(number)).nextDouble());
    assertStrictError(e, expectedLocation);

    e =
        assertThrows(
            MalformedJsonException.class, () -> new JsonReader(reader(number)).nextString());
    assertStrictError(e, expectedLocation);
  }

  @Test
  public void testBooleans() throws IOException {
    JsonReader reader = new JsonReader(reader("[true,false]"));
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.nextBoolean()).isFalse();
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testPeekingUnquotedStringsPrefixedWithBooleans() throws IOException {
    JsonReader reader = new JsonReader(reader("[truey]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(STRING);

    var e = assertThrows(IllegalStateException.class, () -> reader.nextBoolean());
    assertUnexpectedStructureError(e, "a boolean", "STRING", "line 1 column 2 path $[0]");

    assertThat(reader.nextString()).isEqualTo("truey");
    reader.endArray();
  }

  @Test
  public void testMalformedNumbers() throws IOException {
    assertNotANumber("-");
    assertNotANumber(".");

    // plus sign is not allowed for integer part
    assertNotANumber("+1");

    // leading 0 is not allowed for integer part
    assertNotANumber("00");
    assertNotANumber("01");

    // exponent lacks digit
    assertNotANumber("e");
    assertNotANumber("0e");
    assertNotANumber(".e");
    assertNotANumber("0.e");
    assertNotANumber("-.0e");

    // no integer
    assertNotANumber("e1");
    assertNotANumber(".e1");
    assertNotANumber("-e1");

    // trailing characters
    assertNotANumber("1x");
    assertNotANumber("1.1x");
    assertNotANumber("1e1x");
    assertNotANumber("1ex");
    assertNotANumber("1.1ex");
    assertNotANumber("1.1e1x");

    // fraction has no digit
    assertNotANumber("0.");
    assertNotANumber("-0.");
    assertNotANumber("0.e1");
    assertNotANumber("-0.e1");

    // no leading digit
    assertNotANumber(".0");
    assertNotANumber("-.0");
    assertNotANumber(".0e1");
    assertNotANumber("-.0e1");
  }

  private static void assertNotANumber(String s) throws IOException {
    JsonReader reader = new JsonReader(reader(s));
    reader.setStrictness(Strictness.LENIENT);
    assertThat(reader.peek()).isEqualTo(JsonToken.STRING);
    assertThat(reader.nextString()).isEqualTo(s);

    JsonReader strictReader = new JsonReader(reader(s));
    var e =
        assertThrows(
            "Should have failed reading " + s + " as double",
            MalformedJsonException.class,
            () -> strictReader.nextDouble());
    assertThat(e)
        .hasMessageThat()
        .startsWith("Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON");
  }

  @Test
  public void testPeekingUnquotedStringsPrefixedWithIntegers() throws IOException {
    JsonReader reader = new JsonReader(reader("[12.34e5x]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(STRING);

    assertThrows(NumberFormatException.class, () -> reader.nextInt());
    assertThat(reader.nextString()).isEqualTo("12.34e5x");
  }

  @Test
  public void testPeekLongMinValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[-9223372036854775808]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThat(reader.nextLong()).isEqualTo(-9223372036854775808L);
  }

  @Test
  public void testPeekLongMaxValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[9223372036854775807]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThat(reader.nextLong()).isEqualTo(9223372036854775807L);
  }

  @Test
  public void testLongLargerThanMaxLongThatWrapsAround() throws IOException {
    JsonReader reader = new JsonReader(reader("[22233720368547758070]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThrows(NumberFormatException.class, () -> reader.nextLong());
  }

  @Test
  public void testLongLargerThanMinLongThatWrapsAround() throws IOException {
    JsonReader reader = new JsonReader(reader("[-22233720368547758070]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThrows(NumberFormatException.class, () -> reader.nextLong());
  }

  /** Issue 1053, negative zero. */
  @Test
  public void testNegativeZero() throws Exception {
    JsonReader reader = new JsonReader(reader("[-0]"));
    reader.setStrictness(Strictness.LEGACY_STRICT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThat(reader.nextString()).isEqualTo("-0");
  }

  /**
   * This test fails because there's no double for 9223372036854775808, and our long parsing uses
   * Double.parseDouble() for fractional values.
   */
  @Test
  @Ignore
  public void testPeekLargerThanLongMaxValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[9223372036854775808]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThrows(NumberFormatException.class, () -> reader.nextLong());
  }

  /**
   * This test fails because there's no double for -9223372036854775809, and our long parsing uses
   * Double.parseDouble() for fractional values.
   */
  @Test
  @Ignore
  public void testPeekLargerThanLongMinValue() throws IOException {
    @SuppressWarnings("FloatingPointLiteralPrecision")
    double d = -9223372036854775809d;
    JsonReader reader = new JsonReader(reader("[-9223372036854775809]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThrows(NumberFormatException.class, () -> reader.nextLong());
    assertThat(reader.nextDouble()).isEqualTo(d);
  }

  /**
   * This test fails because there's no double for 9223372036854775806, and our long parsing uses
   * Double.parseDouble() for fractional values.
   */
  @Test
  @Ignore
  public void testHighPrecisionLong() throws IOException {
    String json = "[9223372036854775806.000]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    assertThat(reader.nextLong()).isEqualTo(9223372036854775806L);
    reader.endArray();
  }

  @Test
  public void testPeekMuchLargerThanLongMinValue() throws IOException {
    @SuppressWarnings("FloatingPointLiteralPrecision")
    double d = -92233720368547758080d;
    JsonReader reader = new JsonReader(reader("[-92233720368547758080]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(NUMBER);
    assertThrows(NumberFormatException.class, () -> reader.nextLong());
    assertThat(reader.nextDouble()).isEqualTo(d);
  }

  @Test
  public void testQuotedNumberWithEscape() throws IOException {
    JsonReader reader = new JsonReader(reader("[\"12\\u00334\"]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(STRING);
    assertThat(reader.nextInt()).isEqualTo(1234);
  }

  @Test
  public void testMixedCaseLiterals() throws IOException {
    JsonReader reader = new JsonReader(reader("[True,TruE,False,FALSE,NULL,nulL]"));
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.nextBoolean()).isFalse();
    assertThat(reader.nextBoolean()).isFalse();
    reader.nextNull();
    reader.nextNull();
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testMissingValue() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Expected value at line 1 column 6 path $.a\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testPrematureEndOfInput() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":true,"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextBoolean()).isTrue();
    assertThrows(EOFException.class, () -> reader.nextName());
  }

  @Test
  public void testPrematurelyClosed() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":[]}"));
    reader.beginObject();
    reader.close();
    var e = assertThrows(IllegalStateException.class, () -> reader.nextName());
    assertThat(e).hasMessageThat().isEqualTo("JsonReader is closed");

    JsonReader reader2 = new JsonReader(reader("{\"a\":[]}"));
    reader2.close();
    e = assertThrows(IllegalStateException.class, () -> reader2.beginObject());
    assertThat(e).hasMessageThat().isEqualTo("JsonReader is closed");

    JsonReader reader3 = new JsonReader(reader("{\"a\":true}"));
    reader3.beginObject();
    String unused1 = reader3.nextName();
    JsonToken unused2 = reader3.peek();
    reader3.close();
    e = assertThrows(IllegalStateException.class, () -> reader3.nextBoolean());
    assertThat(e).hasMessageThat().isEqualTo("JsonReader is closed");
  }

  @Test
  public void testNextFailuresDoNotAdvance() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":true}"));
    reader.beginObject();

    var e = assertThrows(IllegalStateException.class, () -> reader.nextString());
    assertUnexpectedStructureError(e, "a string", "NAME", "line 1 column 3 path $.");

    assertThat(reader.nextName()).isEqualTo("a");

    e = assertThrows(IllegalStateException.class, () -> reader.nextName());
    assertUnexpectedStructureError(e, "a name", "BOOLEAN", "line 1 column 10 path $.a");

    e = assertThrows(IllegalStateException.class, () -> reader.beginArray());
    assertUnexpectedStructureError(e, "BEGIN_ARRAY", "BOOLEAN", "line 1 column 10 path $.a");

    e = assertThrows(IllegalStateException.class, () -> reader.endArray());
    assertUnexpectedStructureError(e, "END_ARRAY", "BOOLEAN", "line 1 column 10 path $.a");

    e = assertThrows(IllegalStateException.class, () -> reader.beginObject());
    assertUnexpectedStructureError(e, "BEGIN_OBJECT", "BOOLEAN", "line 1 column 10 path $.a");

    e = assertThrows(IllegalStateException.class, () -> reader.endObject());
    assertUnexpectedStructureError(e, "END_OBJECT", "BOOLEAN", "line 1 column 10 path $.a");

    assertThat(reader.nextBoolean()).isTrue();

    e = assertThrows(IllegalStateException.class, () -> reader.nextString());
    assertUnexpectedStructureError(e, "a string", "END_OBJECT", "line 1 column 11 path $.a");

    e = assertThrows(IllegalStateException.class, () -> reader.nextName());
    assertUnexpectedStructureError(e, "a name", "END_OBJECT", "line 1 column 11 path $.a");

    e = assertThrows(IllegalStateException.class, () -> reader.beginArray());
    assertUnexpectedStructureError(e, "BEGIN_ARRAY", "END_OBJECT", "line 1 column 11 path $.a");

    e = assertThrows(IllegalStateException.class, () -> reader.endArray());
    assertUnexpectedStructureError(e, "END_ARRAY", "END_OBJECT", "line 1 column 11 path $.a");

    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
    reader.close();
  }

  @Test
  public void testIntegerMismatchFailuresDoNotAdvance() throws IOException {
    JsonReader reader = new JsonReader(reader("[1.5]"));
    reader.beginArray();
    assertThrows(NumberFormatException.class, () -> reader.nextInt());
    assertThat(reader.nextDouble()).isEqualTo(1.5d);
    reader.endArray();
  }

  @Test
  public void testStringNullIsNotNull() throws IOException {
    JsonReader reader = new JsonReader(reader("[\"null\"]"));
    reader.beginArray();
    var e = assertThrows(IllegalStateException.class, () -> reader.nextNull());
    assertUnexpectedStructureError(e, "null", "STRING", "line 1 column 3 path $[0]");
  }

  @Test
  public void testNullLiteralIsNotAString() throws IOException {
    JsonReader reader = new JsonReader(reader("[null]"));
    reader.beginArray();
    var e = assertThrows(IllegalStateException.class, () -> reader.nextString());
    assertUnexpectedStructureError(e, "a string", "NULL", "line 1 column 6 path $[0]");
  }

  @Test
  public void testStrictNameValueSeparator() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\"=true}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextBoolean());
    assertStrictError(e, "line 1 column 6 path $.a");

    JsonReader reader2 = new JsonReader(reader("{\"a\"=>true}"));
    reader2.beginObject();

    assertThat(reader2.nextName()).isEqualTo("a");

    e = assertThrows(MalformedJsonException.class, () -> reader2.nextBoolean());
    assertStrictError(e, "line 1 column 6 path $.a");
  }

  @Test
  public void testLenientNameValueSeparator() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\"=true}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextBoolean()).isTrue();

    reader = new JsonReader(reader("{\"a\"=>true}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextBoolean()).isTrue();
  }

  @Test
  public void testStrictNameValueSeparatorWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\"=true}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 6 path $.a");

    JsonReader reader2 = new JsonReader(reader("{\"a\"=>true}"));
    reader2.beginObject();
    assertThat(reader2.nextName()).isEqualTo("a");

    e = assertThrows(MalformedJsonException.class, () -> reader2.skipValue());
    assertStrictError(e, "line 1 column 6 path $.a");
  }

  @Test
  public void testCommentsInStringValue() throws Exception {
    JsonReader reader = new JsonReader(reader("[\"// comment\"]"));
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo("// comment");
    reader.endArray();

    reader = new JsonReader(reader("{\"a\":\"#someComment\"}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextString()).isEqualTo("#someComment");
    reader.endObject();

    reader = new JsonReader(reader("{\"#//a\":\"#some //Comment\"}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("#//a");
    assertThat(reader.nextString()).isEqualTo("#some //Comment");
    reader.endObject();
  }

  @Test
  public void testStrictComments() throws IOException {
    JsonReader reader = new JsonReader(reader("[// comment \n true]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextBoolean());
    assertStrictError(e, "line 1 column 3 path $[0]");

    JsonReader reader2 = new JsonReader(reader("[# comment \n true]"));
    reader2.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader2.nextBoolean());
    assertStrictError(e, "line 1 column 3 path $[0]");

    JsonReader reader3 = new JsonReader(reader("[/* comment */ true]"));
    reader3.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader3.nextBoolean());
    assertStrictError(e, "line 1 column 3 path $[0]");
  }

  @Test
  public void testLenientComments() throws IOException {
    JsonReader reader = new JsonReader(reader("[// comment \n true]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();

    reader = new JsonReader(reader("[# comment \n true]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();

    reader = new JsonReader(reader("[/* comment */ true]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
  }

  @Test
  public void testStrictCommentsWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[// comment \n true]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 3 path $[0]");

    JsonReader reader2 = new JsonReader(reader("[# comment \n true]"));
    reader2.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader2.skipValue());
    assertStrictError(e, "line 1 column 3 path $[0]");

    JsonReader reader3 = new JsonReader(reader("[/* comment */ true]"));
    reader3.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader3.skipValue());
    assertStrictError(e, "line 1 column 3 path $[0]");
  }

  @Test
  public void testStrictUnquotedNames() throws IOException {
    JsonReader reader = new JsonReader(reader("{a:true}"));
    reader.beginObject();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextName());
    assertStrictError(e, "line 1 column 3 path $.");
  }

  @Test
  public void testLenientUnquotedNames() throws IOException {
    JsonReader reader = new JsonReader(reader("{a:true}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
  }

  @Test
  public void testStrictUnquotedNamesWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("{a:true}"));
    reader.beginObject();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 3 path $.");
  }

  @Test
  public void testStrictSingleQuotedNames() throws IOException {
    JsonReader reader = new JsonReader(reader("{'a':true}"));
    reader.beginObject();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextName());
    assertStrictError(e, "line 1 column 3 path $.");
  }

  @Test
  public void testLenientSingleQuotedNames() throws IOException {
    JsonReader reader = new JsonReader(reader("{'a':true}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
  }

  @Test
  public void testStrictSingleQuotedNamesWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("{'a':true}"));
    reader.beginObject();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 3 path $.");
  }

  @Test
  public void testStrictUnquotedStrings() throws IOException {
    JsonReader reader = new JsonReader(reader("[a]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @Test
  public void testStrictUnquotedStringsWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[a]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @Test
  public void testLenientUnquotedStrings() throws IOException {
    JsonReader reader = new JsonReader(reader("[a]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo("a");
  }

  @Test
  public void testStrictSingleQuotedStrings() throws IOException {
    JsonReader reader = new JsonReader(reader("['a']"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertStrictError(e, "line 1 column 3 path $[0]");
  }

  @Test
  public void testLenientSingleQuotedStrings() throws IOException {
    JsonReader reader = new JsonReader(reader("['a']"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo("a");
  }

  @Test
  public void testStrictSingleQuotedStringsWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("['a']"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 3 path $[0]");
  }

  @Test
  public void testStrictSemicolonDelimitedArray() throws IOException {
    JsonReader reader = new JsonReader(reader("[true;true]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextBoolean());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @Test
  public void testLenientSemicolonDelimitedArray() throws IOException {
    JsonReader reader = new JsonReader(reader("[true;true]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.nextBoolean()).isTrue();
  }

  @Test
  public void testStrictSemicolonDelimitedArrayWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[true;true]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @Test
  public void testStrictSemicolonDelimitedNameValuePair() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":true;\"b\":true}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextBoolean());
    assertStrictError(e, "line 1 column 6 path $.a");
  }

  @Test
  public void testLenientSemicolonDelimitedNameValuePair() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":true;\"b\":true}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.nextName()).isEqualTo("b");
  }

  @Test
  public void testStrictSemicolonDelimitedNameValuePairWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":true;\"b\":true}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 6 path $.a");
  }

  @Test
  public void testStrictUnnecessaryArraySeparators() throws IOException {
    // The following calls `nextNull()` because a lenient JsonReader would treat redundant array
    // separators as implicit JSON null

    JsonReader reader = new JsonReader(reader("[true,,true]"));
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextNull());
    assertStrictError(e, "line 1 column 8 path $[1]");

    JsonReader reader2 = new JsonReader(reader("[,true]"));
    reader2.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader2.nextNull());
    assertStrictError(e, "line 1 column 3 path $[0]");

    JsonReader reader3 = new JsonReader(reader("[true,]"));
    reader3.beginArray();
    assertThat(reader3.nextBoolean()).isTrue();
    e = assertThrows(MalformedJsonException.class, () -> reader3.nextNull());
    assertStrictError(e, "line 1 column 8 path $[1]");

    JsonReader reader4 = new JsonReader(reader("[,]"));
    reader4.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader4.nextNull());
    assertStrictError(e, "line 1 column 3 path $[0]");
  }

  @Test
  public void testLenientUnnecessaryArraySeparators() throws IOException {
    JsonReader reader = new JsonReader(reader("[true,,true]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    // Redundant array separators are treated as implicit JSON null
    reader.nextNull();
    assertThat(reader.nextBoolean()).isTrue();
    reader.endArray();

    reader = new JsonReader(reader("[,true]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    reader.nextNull();
    assertThat(reader.nextBoolean()).isTrue();
    reader.endArray();

    reader = new JsonReader(reader("[true,]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    reader.nextNull();
    reader.endArray();

    reader = new JsonReader(reader("[,]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    reader.nextNull();
    reader.nextNull();
    reader.endArray();
  }

  @Test
  public void testStrictUnnecessaryArraySeparatorsWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[true,,true]"));
    reader.beginArray();
    assertThat(reader.nextBoolean()).isTrue();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 8 path $[1]");

    JsonReader reader2 = new JsonReader(reader("[,true]"));
    reader2.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader2.skipValue());
    assertStrictError(e, "line 1 column 3 path $[0]");

    JsonReader reader3 = new JsonReader(reader("[true,]"));
    reader3.beginArray();
    assertThat(reader3.nextBoolean()).isTrue();
    e = assertThrows(MalformedJsonException.class, () -> reader3.skipValue());
    assertStrictError(e, "line 1 column 8 path $[1]");

    JsonReader reader4 = new JsonReader(reader("[,]"));
    reader4.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader4.skipValue());
    assertStrictError(e, "line 1 column 3 path $[0]");
  }

  @Test
  public void testStrictMultipleTopLevelValues() throws IOException {
    JsonReader reader = new JsonReader(reader("[] []"));
    reader.beginArray();
    reader.endArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertStrictError(e, "line 1 column 5 path $");
  }

  @Test
  public void testLenientMultipleTopLevelValues() throws IOException {
    JsonReader reader = new JsonReader(reader("[] true {}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    reader.endArray();
    assertThat(reader.nextBoolean()).isTrue();
    reader.beginObject();
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testStrictMultipleTopLevelValuesWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("[] []"));
    reader.beginArray();
    reader.endArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 5 path $");
  }

  @Test
  public void testTopLevelValueTypes() throws IOException {
    JsonReader reader1 = new JsonReader(reader("true"));
    assertThat(reader1.nextBoolean()).isTrue();
    assertThat(reader1.peek()).isEqualTo(JsonToken.END_DOCUMENT);

    JsonReader reader2 = new JsonReader(reader("false"));
    assertThat(reader2.nextBoolean()).isFalse();
    assertThat(reader2.peek()).isEqualTo(JsonToken.END_DOCUMENT);

    JsonReader reader3 = new JsonReader(reader("null"));
    assertThat(reader3.peek()).isEqualTo(JsonToken.NULL);
    reader3.nextNull();
    assertThat(reader3.peek()).isEqualTo(JsonToken.END_DOCUMENT);

    JsonReader reader4 = new JsonReader(reader("123"));
    assertThat(reader4.nextInt()).isEqualTo(123);
    assertThat(reader4.peek()).isEqualTo(JsonToken.END_DOCUMENT);

    JsonReader reader5 = new JsonReader(reader("123.4"));
    assertThat(reader5.nextDouble()).isEqualTo(123.4);
    assertThat(reader5.peek()).isEqualTo(JsonToken.END_DOCUMENT);

    JsonReader reader6 = new JsonReader(reader("\"a\""));
    assertThat(reader6.nextString()).isEqualTo("a");
    assertThat(reader6.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testTopLevelValueTypeWithSkipValue() throws IOException {
    JsonReader reader = new JsonReader(reader("true"));
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testStrictNonExecutePrefix() {
    JsonReader reader = new JsonReader(reader(")]}'\n []"));
    var e = assertThrows(MalformedJsonException.class, () -> reader.beginArray());
    assertStrictError(e, "line 1 column 1 path $");
  }

  @Test
  public void testStrictNonExecutePrefixWithSkipValue() {
    JsonReader reader = new JsonReader(reader(")]}'\n []"));
    var e = assertThrows(MalformedJsonException.class, () -> reader.skipValue());
    assertStrictError(e, "line 1 column 1 path $");
  }

  @Test
  public void testLenientNonExecutePrefix() throws IOException {
    JsonReader reader = new JsonReader(reader(")]}'\n []"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testLenientNonExecutePrefixWithLeadingWhitespace() throws IOException {
    JsonReader reader = new JsonReader(reader("\r\n \t)]}'\n []"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testLenientPartialNonExecutePrefix() throws IOException {
    JsonReader reader = new JsonReader(reader(")]}' []"));
    reader.setStrictness(Strictness.LENIENT);
    assertThat(reader.nextString()).isEqualTo(")");
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Unexpected value at line 1 column 3 path $\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testBomIgnoredAsFirstCharacterOfDocument() throws IOException {
    JsonReader reader = new JsonReader(reader("\ufeff[]"));
    reader.beginArray();
    reader.endArray();
  }

  @Test
  public void testBomForbiddenAsOtherCharacterInDocument() throws IOException {
    JsonReader reader = new JsonReader(reader("[\ufeff]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.endArray());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @SuppressWarnings("UngroupedOverloads")
  @Test
  public void testFailWithPosition() throws IOException {
    testFailWithPosition("Expected value at line 6 column 5 path $[1]", "[\n\n\n\n\n\"a\",}]");
  }

  @Test
  public void testFailWithPositionGreaterThanBufferSize() throws IOException {
    String spaces = repeat(' ', 8192);
    testFailWithPosition(
        "Expected value at line 6 column 5 path $[1]", "[\n\n" + spaces + "\n\n\n\"a\",}]");
  }

  @Test
  public void testFailWithPositionOverSlashSlashEndOfLineComment() throws IOException {
    testFailWithPosition(
        "Expected value at line 5 column 6 path $[1]", "\n// foo\n\n//bar\r\n[\"a\",}");
  }

  @Test
  public void testFailWithPositionOverHashEndOfLineComment() throws IOException {
    testFailWithPosition(
        "Expected value at line 5 column 6 path $[1]", "\n# foo\n\n#bar\r\n[\"a\",}");
  }

  @Test
  public void testFailWithPositionOverCStyleComment() throws IOException {
    testFailWithPosition(
        "Expected value at line 6 column 12 path $[1]", "\n\n/* foo\n*\n*\r\nbar */[\"a\",}");
  }

  @Test
  public void testFailWithPositionOverQuotedString() throws IOException {
    testFailWithPosition(
        "Expected value at line 5 column 3 path $[1]", "[\"foo\nbar\r\nbaz\n\",\n  }");
  }

  @Test
  public void testFailWithPositionOverUnquotedString() throws IOException {
    testFailWithPosition("Expected value at line 5 column 2 path $[1]", "[\n\nabcd\n\n,}");
  }

  @Test
  public void testFailWithEscapedNewlineCharacter() throws IOException {
    testFailWithPosition("Expected value at line 5 column 3 path $[1]", "[\n\n\"\\\n\n\",}");
  }

  @Test
  public void testFailWithPositionIsOffsetByBom() throws IOException {
    testFailWithPosition("Expected value at line 1 column 6 path $[1]", "\ufeff[\"a\",}]");
  }

  private static void testFailWithPosition(String message, String json) throws IOException {
    // Validate that it works reading the string normally.
    JsonReader reader1 = new JsonReader(reader(json));
    reader1.setStrictness(Strictness.LENIENT);
    reader1.beginArray();
    String unused1 = reader1.nextString();
    var e = assertThrows(MalformedJsonException.class, () -> reader1.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            message
                + "\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");

    // Also validate that it works when skipping.
    JsonReader reader2 = new JsonReader(reader(json));
    reader2.setStrictness(Strictness.LENIENT);
    reader2.beginArray();
    reader2.skipValue();
    e = assertThrows(MalformedJsonException.class, () -> reader2.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            message
                + "\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testFailWithPositionDeepPath() throws IOException {
    JsonReader reader = new JsonReader(reader("[1,{\"a\":[2,3,}"));
    reader.beginArray();
    int unused1 = reader.nextInt();
    reader.beginObject();
    String unused2 = reader.nextName();
    reader.beginArray();
    int unused3 = reader.nextInt();
    int unused4 = reader.nextInt();
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Expected value at line 1 column 14 path $[1].a[2]\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testStrictVeryLongNumber() throws IOException {
    JsonReader reader = new JsonReader(reader("[0." + repeat('9', 8192) + "]"));
    reader.beginArray();
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextDouble());
    assertStrictError(e, "line 1 column 2 path $[0]");
  }

  @Test
  public void testLenientVeryLongNumber() throws IOException {
    JsonReader reader = new JsonReader(reader("[0." + repeat('9', 8192) + "]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.STRING);
    assertThat(reader.nextDouble()).isEqualTo(1d);
    reader.endArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testVeryLongUnquotedLiteral() throws IOException {
    String literal = "a" + repeat('b', 8192) + "c";
    JsonReader reader = new JsonReader(reader("[" + literal + "]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo(literal);
    reader.endArray();
  }

  @Test
  public void testDeeplyNestedArrays() throws IOException {
    // this is nested 40 levels deep; Gson is tuned for nesting is 30 levels deep or fewer
    JsonReader reader =
        new JsonReader(
            reader(
                "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]"));
    for (int i = 0; i < 40; i++) {
      reader.beginArray();
    }
    assertThat(reader.getPath())
        .isEqualTo(
            "$[0][0][0][0][0][0][0][0][0][0][0][0][0][0][0]"
                + "[0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0]");
    for (int i = 0; i < 40; i++) {
      reader.endArray();
    }
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testDeeplyNestedObjects() throws IOException {
    // Build a JSON document structured like {"a":{"a":{"a":{"a":true}}}}, but 40 levels deep
    String array = "{\"a\":%s}";
    String json = "true";
    for (int i = 0; i < 40; i++) {
      json = String.format(array, json);
    }

    JsonReader reader = new JsonReader(reader(json));
    for (int i = 0; i < 40; i++) {
      reader.beginObject();
      assertThat(reader.nextName()).isEqualTo("a");
    }
    assertThat(reader.getPath())
        .isEqualTo(
            "$.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a"
                + ".a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a.a");
    assertThat(reader.nextBoolean()).isTrue();
    for (int i = 0; i < 40; i++) {
      reader.endObject();
    }
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testNestingLimitDefault() throws IOException {
    int defaultLimit = JsonReader.DEFAULT_NESTING_LIMIT;
    String json = repeat('[', defaultLimit + 1);
    JsonReader reader = new JsonReader(reader(json));
    assertThat(reader.getNestingLimit()).isEqualTo(defaultLimit);

    for (int i = 0; i < defaultLimit; i++) {
      reader.beginArray();
    }
    MalformedJsonException e =
        assertThrows(MalformedJsonException.class, () -> reader.beginArray());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Nesting limit "
                + defaultLimit
                + " reached at line 1 column "
                + (defaultLimit + 2)
                + " path $"
                + "[0]".repeat(defaultLimit));
  }

  // Note: The column number reported in the expected exception messages is slightly off and points
  // behind instead of directly at the '[' or '{'
  @Test
  public void testNestingLimit() throws IOException {
    JsonReader reader = new JsonReader(reader("[{\"a\":1}]"));
    reader.setNestingLimit(2);
    assertThat(reader.getNestingLimit()).isEqualTo(2);
    reader.beginArray();
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextInt()).isEqualTo(1);
    reader.endObject();
    reader.endArray();

    JsonReader reader2 = new JsonReader(reader("[{\"a\":[]}]"));
    reader2.setNestingLimit(2);
    reader2.beginArray();
    reader2.beginObject();
    assertThat(reader2.nextName()).isEqualTo("a");
    MalformedJsonException e =
        assertThrows(MalformedJsonException.class, () -> reader2.beginArray());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo("Nesting limit 2 reached at line 1 column 8 path $[0].a");

    JsonReader reader3 = new JsonReader(reader("[]"));
    reader3.setNestingLimit(0);
    e = assertThrows(MalformedJsonException.class, () -> reader3.beginArray());
    assertThat(e).hasMessageThat().isEqualTo("Nesting limit 0 reached at line 1 column 2 path $");

    JsonReader reader4 = new JsonReader(reader("[]"));
    reader4.setNestingLimit(0);
    // Currently also checked when skipping values
    e = assertThrows(MalformedJsonException.class, () -> reader4.skipValue());
    assertThat(e).hasMessageThat().isEqualTo("Nesting limit 0 reached at line 1 column 2 path $");

    JsonReader reader5 = new JsonReader(reader("1"));
    reader5.setNestingLimit(0);
    // Reading value other than array or object should be allowed
    assertThat(reader5.nextInt()).isEqualTo(1);

    // Test multiple top-level arrays
    JsonReader reader6 = new JsonReader(reader("[] [[]]"));
    reader6.setStrictness(Strictness.LENIENT);
    reader6.setNestingLimit(1);
    reader6.beginArray();
    reader6.endArray();
    reader6.beginArray();
    e = assertThrows(MalformedJsonException.class, () -> reader6.beginArray());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo("Nesting limit 1 reached at line 1 column 6 path $[0]");

    JsonReader reader7 = new JsonReader(reader("[]"));
    IllegalArgumentException argException =
        assertThrows(IllegalArgumentException.class, () -> reader7.setNestingLimit(-1));
    assertThat(argException).hasMessageThat().isEqualTo("Invalid nesting limit: -1");
  }

  // http://code.google.com/p/google-gson/issues/detail?id=409
  @Test
  public void testStringEndingInSlash() {
    JsonReader reader = new JsonReader(reader("/"));
    reader.setStrictness(Strictness.LENIENT);
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Expected value at line 1 column 1 path $\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testDocumentWithCommentEndingInSlash() {
    JsonReader reader = new JsonReader(reader("/* foo *//"));
    reader.setStrictness(Strictness.LENIENT);
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Expected value at line 1 column 10 path $\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testStringWithLeadingSlash() {
    JsonReader reader = new JsonReader(reader("/x"));
    reader.setStrictness(Strictness.LENIENT);
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Expected value at line 1 column 1 path $\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testUnterminatedObject() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":\"android\"x"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextString()).isEqualTo("android");
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Unterminated object at line 1 column 16 path $.a\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testVeryLongQuotedString() throws IOException {
    char[] stringChars = new char[1024 * 16];
    Arrays.fill(stringChars, 'x');
    String string = new String(stringChars);
    String json = "[\"" + string + "\"]";
    JsonReader reader = new JsonReader(reader(json));
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo(string);
    reader.endArray();
  }

  @Test
  public void testVeryLongUnquotedString() throws IOException {
    char[] stringChars = new char[1024 * 16];
    Arrays.fill(stringChars, 'x');
    String string = new String(stringChars);
    String json = "[" + string + "]";
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo(string);
    reader.endArray();
  }

  @Test
  public void testVeryLongUnterminatedString() throws IOException {
    char[] stringChars = new char[1024 * 16];
    Arrays.fill(stringChars, 'x');
    String string = new String(stringChars);
    String json = "[" + string;
    JsonReader reader = new JsonReader(reader(json));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.nextString()).isEqualTo(string);
    assertThrows(EOFException.class, () -> reader.peek());
  }

  @Test
  public void testSkipVeryLongUnquotedString() throws IOException {
    JsonReader reader = new JsonReader(reader("[" + repeat('x', 8192) + "]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    reader.skipValue();
    reader.endArray();
  }

  @Test
  public void testSkipTopLevelUnquotedString() throws IOException {
    JsonReader reader = new JsonReader(reader(repeat('x', 8192)));
    reader.setStrictness(Strictness.LENIENT);
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testSkipVeryLongQuotedString() throws IOException {
    JsonReader reader = new JsonReader(reader("[\"" + repeat('x', 8192) + "\"]"));
    reader.beginArray();
    reader.skipValue();
    reader.endArray();
  }

  @Test
  public void testSkipTopLevelQuotedString() throws IOException {
    JsonReader reader = new JsonReader(reader("\"" + repeat('x', 8192) + "\""));
    reader.setStrictness(Strictness.LENIENT);
    reader.skipValue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testStringAsNumberWithTruncatedExponent() throws IOException {
    JsonReader reader = new JsonReader(reader("[123e]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(STRING);
  }

  @Test
  public void testStringAsNumberWithDigitAndNonDigitExponent() throws IOException {
    JsonReader reader = new JsonReader(reader("[123e4b]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(STRING);
  }

  @Test
  public void testStringAsNumberWithNonDigitExponent() throws IOException {
    JsonReader reader = new JsonReader(reader("[123eb]"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(STRING);
  }

  @Test
  public void testEmptyStringName() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"\":true}"));
    reader.setStrictness(Strictness.LENIENT);
    assertThat(reader.peek()).isEqualTo(BEGIN_OBJECT);
    reader.beginObject();
    assertThat(reader.peek()).isEqualTo(NAME);
    assertThat(reader.nextName()).isEqualTo("");
    assertThat(reader.peek()).isEqualTo(JsonToken.BOOLEAN);
    assertThat(reader.nextBoolean()).isTrue();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_OBJECT);
    reader.endObject();
    assertThat(reader.peek()).isEqualTo(JsonToken.END_DOCUMENT);
  }

  @Test
  public void testStrictExtraCommasInMaps() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":\"b\",}"));
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextString()).isEqualTo("b");
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Expected name at line 1 column 11 path $.a\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  @Test
  public void testLenientExtraCommasInMaps() throws IOException {
    JsonReader reader = new JsonReader(reader("{\"a\":\"b\",}"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginObject();
    assertThat(reader.nextName()).isEqualTo("a");
    assertThat(reader.nextString()).isEqualTo("b");
    var e = assertThrows(MalformedJsonException.class, () -> reader.peek());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Expected name at line 1 column 11 path $.a\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  private static String repeat(char c, int count) {
    char[] array = new char[count];
    Arrays.fill(array, c);
    return new String(array);
  }

  @Test
  public void testMalformedDocuments() throws IOException {
    assertDocument("{]", BEGIN_OBJECT, MalformedJsonException.class);
    assertDocument("{,", BEGIN_OBJECT, MalformedJsonException.class);
    assertDocument("{{", BEGIN_OBJECT, MalformedJsonException.class);
    assertDocument("{[", BEGIN_OBJECT, MalformedJsonException.class);
    assertDocument("{:", BEGIN_OBJECT, MalformedJsonException.class);
    assertDocument("{\"name\",", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{\"name\",", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{\"name\":}", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{\"name\"::", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{\"name\":,", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{\"name\"=}", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{\"name\"=>}", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument(
        "{\"name\"=>\"string\":", BEGIN_OBJECT, NAME, STRING, MalformedJsonException.class);
    assertDocument(
        "{\"name\"=>\"string\"=", BEGIN_OBJECT, NAME, STRING, MalformedJsonException.class);
    assertDocument(
        "{\"name\"=>\"string\"=>", BEGIN_OBJECT, NAME, STRING, MalformedJsonException.class);
    assertDocument("{\"name\"=>\"string\",", BEGIN_OBJECT, NAME, STRING, EOFException.class);
    assertDocument("{\"name\"=>\"string\",\"name\"", BEGIN_OBJECT, NAME, STRING, NAME);
    assertDocument("[}", BEGIN_ARRAY, MalformedJsonException.class);
    assertDocument("[,]", BEGIN_ARRAY, NULL, NULL, END_ARRAY);
    assertDocument("{", BEGIN_OBJECT, EOFException.class);
    assertDocument("{\"name\"", BEGIN_OBJECT, NAME, EOFException.class);
    assertDocument("{\"name\",", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{'name'", BEGIN_OBJECT, NAME, EOFException.class);
    assertDocument("{'name',", BEGIN_OBJECT, NAME, MalformedJsonException.class);
    assertDocument("{name", BEGIN_OBJECT, NAME, EOFException.class);
    assertDocument("[", BEGIN_ARRAY, EOFException.class);
    assertDocument("[string", BEGIN_ARRAY, STRING, EOFException.class);
    assertDocument("[\"string\"", BEGIN_ARRAY, STRING, EOFException.class);
    assertDocument("['string'", BEGIN_ARRAY, STRING, EOFException.class);
    assertDocument("[123", BEGIN_ARRAY, NUMBER, EOFException.class);
    assertDocument("[123,", BEGIN_ARRAY, NUMBER, EOFException.class);
    assertDocument("{\"name\":123", BEGIN_OBJECT, NAME, NUMBER, EOFException.class);
    assertDocument("{\"name\":123,", BEGIN_OBJECT, NAME, NUMBER, EOFException.class);
    assertDocument("{\"name\":\"string\"", BEGIN_OBJECT, NAME, STRING, EOFException.class);
    assertDocument("{\"name\":\"string\",", BEGIN_OBJECT, NAME, STRING, EOFException.class);
    assertDocument("{\"name\":'string'", BEGIN_OBJECT, NAME, STRING, EOFException.class);
    assertDocument("{\"name\":'string',", BEGIN_OBJECT, NAME, STRING, EOFException.class);
    assertDocument("{\"name\":false", BEGIN_OBJECT, NAME, BOOLEAN, EOFException.class);
    assertDocument("{\"name\":false,,", BEGIN_OBJECT, NAME, BOOLEAN, MalformedJsonException.class);
  }

  /**
   * This test behaves slightly differently in Gson 2.2 and earlier. It fails during peek rather
   * than during nextString().
   */
  @Test
  public void testUnterminatedStringFailure() throws IOException {
    JsonReader reader = new JsonReader(reader("[\"string"));
    reader.setStrictness(Strictness.LENIENT);
    reader.beginArray();
    assertThat(reader.peek()).isEqualTo(JsonToken.STRING);
    var e = assertThrows(MalformedJsonException.class, () -> reader.nextString());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Unterminated string at line 1 column 9 path $[0]\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  /** Regression test for an issue with buffer filling and consumeNonExecutePrefix. */
  @Test
  public void testReadAcrossBuffers() throws IOException {
    StringBuilder sb = new StringBuilder("#");
    for (int i = 0; i < JsonReader.BUFFER_SIZE - 3; i++) {
      sb.append(' ');
    }
    sb.append("\n)]}'\n3");
    JsonReader reader = new JsonReader(reader(sb.toString()));
    reader.setStrictness(Strictness.LENIENT);
    JsonToken token = reader.peek();
    assertThat(token).isEqualTo(JsonToken.NUMBER);
  }

  private static void assertStrictError(MalformedJsonException exception, String expectedLocation) {
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON at "
                + expectedLocation
                + "\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#malformed-json");
  }

  private static void assertUnexpectedStructureError(
      IllegalStateException exception,
      String expectedToken,
      String actualToken,
      String expectedLocation) {
    String troubleshootingId =
        actualToken.equals("NULL") ? "adapter-not-null-safe" : "unexpected-json-structure";
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Expected "
                + expectedToken
                + " but was "
                + actualToken
                + " at "
                + expectedLocation
                + "\nSee https://github.com/google/gson/blob/main/Troubleshooting.md#"
                + troubleshootingId);
  }

  private static void assertDocument(String document, Object... expectations) throws IOException {
    JsonReader reader = new JsonReader(reader(document));
    reader.setStrictness(Strictness.LENIENT);
    for (Object expectation : expectations) {
      if (expectation == BEGIN_OBJECT) {
        reader.beginObject();
      } else if (expectation == BEGIN_ARRAY) {
        reader.beginArray();
      } else if (expectation == END_OBJECT) {
        reader.endObject();
      } else if (expectation == END_ARRAY) {
        reader.endArray();
      } else if (expectation == NAME) {
        assertThat(reader.nextName()).isEqualTo("name");
      } else if (expectation == BOOLEAN) {
        assertThat(reader.nextBoolean()).isFalse();
      } else if (expectation == STRING) {
        assertThat(reader.nextString()).isEqualTo("string");
      } else if (expectation == NUMBER) {
        assertThat(reader.nextInt()).isEqualTo(123);
      } else if (expectation == NULL) {
        reader.nextNull();
      } else if (expectation instanceof Class
          && Exception.class.isAssignableFrom((Class<?>) expectation)) {
        var expected = assertThrows(Exception.class, () -> reader.peek());
        assertThat(expected.getClass()).isEqualTo((Class<?>) expectation);
      } else {
        throw new AssertionError("Unsupported expectation value: " + expectation);
      }
    }
  }

  @Test
  public void testJsonReaderWithStrictnessSetToLenientAndNullValue() throws IOException {
    Iterator<String> iterator = Stream.of(null, "value1", "value2").iterator();
    StringWriter str = new StringWriter();

    try (JsonWriter writer = new JsonWriter(str)) {
      writer.setStrictness(Strictness.LENIENT);
      while (iterator.hasNext()) {
        TypeAdapters.STRING.write(writer, iterator.next());
      }
      writer.flush();
    }

    JsonReader reader = new JsonReader(new StringReader(str.toString()));
    reader.setStrictness(Strictness.LENIENT);

    assertThat(TypeAdapters.STRING.read(reader)).isEqualTo("null");
    assertThat(TypeAdapters.STRING.read(reader)).isEqualTo("value1");
    assertThat(TypeAdapters.STRING.read(reader)).isEqualTo("value2");
  }

  /** Returns a reader that returns one character at a time. */
  private static Reader reader(String s) {
    /* if (true) */ return new StringReader(s);
    /* return new Reader() {
      int position = 0;
      @Override public int read(char[] buffer, int offset, int count) throws IOException {
        if (position == s.length()) {
          return -1;
        } else if (count > 0) {
          buffer[offset] = s.charAt(position++);
          return 1;
        } else {
          throw new IllegalArgumentException();
        }
      }
      @Override public void close() throws IOException {
      }
    }; */
  }
}
