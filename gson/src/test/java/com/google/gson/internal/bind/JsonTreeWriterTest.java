/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.gson.internal.bind;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.Strictness;
import com.google.gson.common.MoreAsserts;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

@SuppressWarnings("resource")
public final class JsonTreeWriterTest {
  @Test
  public void testArray() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.beginArray();
    writer.value(1);
    writer.value(2);
    writer.value(3);
    writer.endArray();
    assertThat(writer.get().toString()).isEqualTo("[1,2,3]");
  }

  @Test
  public void testNestedArray() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.beginArray();
    writer.beginArray();
    writer.endArray();
    writer.beginArray();
    writer.beginArray();
    writer.endArray();
    writer.endArray();
    writer.endArray();
    assertThat(writer.get().toString()).isEqualTo("[[],[[]]]");
  }

  @Test
  public void testObject() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.beginObject();
    writer.name("A").value(1);
    writer.name("B").value(2);
    writer.endObject();
    assertThat(writer.get().toString()).isEqualTo("{\"A\":1,\"B\":2}");
  }

  @Test
  public void testNestedObject() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.beginObject();
    writer.name("A");
    writer.beginObject();
    writer.name("B");
    writer.beginObject();
    writer.endObject();
    writer.endObject();
    writer.name("C");
    writer.beginObject();
    writer.endObject();
    writer.endObject();
    assertThat(writer.get().toString()).isEqualTo("{\"A\":{\"B\":{}},\"C\":{}}");
  }

  @Test
  public void testWriteAfterClose() throws Exception {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.setStrictness(Strictness.LENIENT);
    writer.beginArray();
    writer.value("A");
    writer.endArray();
    writer.close();
    assertThrows(IllegalStateException.class, () -> writer.beginArray());
  }

  @Test
  public void testPrematureClose() throws Exception {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.setStrictness(Strictness.LENIENT);
    writer.beginArray();
    var e = assertThrows(IOException.class, () -> writer.close());
    assertThat(e).hasMessageThat().isEqualTo("Incomplete document");
  }

  @Test
  public void testNameAsTopLevelValue() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> writer.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("Did not expect a name");

    writer.value(12);
    writer.close();

    e = assertThrows(IllegalStateException.class, () -> writer.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("Please begin an object before writing a name.");
  }

  @Test
  public void testNameInArray() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();

    writer.beginArray();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> writer.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("Please begin an object before writing a name.");

    writer.value(12);
    e = assertThrows(IllegalStateException.class, () -> writer.name("hello"));
    assertThat(e).hasMessageThat().isEqualTo("Please begin an object before writing a name.");

    writer.endArray();

    assertThat(writer.get().toString()).isEqualTo("[12]");
  }

  @Test
  public void testTwoNames() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.beginObject();
    writer.name("a");
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> writer.name("a"));
    assertThat(e).hasMessageThat().isEqualTo("Did not expect a name");
  }

  @Test
  public void testSerializeNullsFalse() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.setSerializeNulls(false);
    writer.beginObject();
    writer.name("A");
    writer.nullValue();
    writer.endObject();
    assertThat(writer.get().toString()).isEqualTo("{}");
  }

  @Test
  public void testSerializeNullsTrue() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.setSerializeNulls(true);
    writer.beginObject();
    writer.name("A");
    writer.nullValue();
    writer.endObject();
    assertThat(writer.get().toString()).isEqualTo("{\"A\":null}");
  }

  @Test
  public void testEmptyWriter() {
    JsonTreeWriter writer = new JsonTreeWriter();
    assertThat(writer.get()).isEqualTo(JsonNull.INSTANCE);
  }

  @Test
  public void testBeginArray() throws Exception {
    JsonTreeWriter writer = new JsonTreeWriter();
    assertThat(writer.beginArray()).isEqualTo(writer);
  }

  @Test
  public void testBeginObject() throws Exception {
    JsonTreeWriter writer = new JsonTreeWriter();
    assertThat(writer.beginObject()).isEqualTo(writer);
  }

  @Test
  public void testValueString() throws Exception {
    JsonTreeWriter writer = new JsonTreeWriter();
    String n = "as";
    assertThat(writer.value(n)).isEqualTo(writer);
  }

  @Test
  public void testBoolValue() throws Exception {
    JsonTreeWriter writer = new JsonTreeWriter();
    boolean bool = true;
    assertThat(writer.value(bool)).isEqualTo(writer);
  }

  @Test
  public void testBoolMaisValue() throws Exception {
    JsonTreeWriter writer = new JsonTreeWriter();
    Boolean bool = true;
    assertThat(writer.value(bool)).isEqualTo(writer);
  }

  @Test
  public void testLenientNansAndInfinities() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.setStrictness(Strictness.LENIENT);
    writer.beginArray();
    writer.value(Float.NaN);
    writer.value(Float.NEGATIVE_INFINITY);
    writer.value(Float.POSITIVE_INFINITY);
    writer.value(Double.NaN);
    writer.value(Double.NEGATIVE_INFINITY);
    writer.value(Double.POSITIVE_INFINITY);
    writer.endArray();
    assertThat(writer.get().toString())
        .isEqualTo("[NaN,-Infinity,Infinity,NaN,-Infinity,Infinity]");
  }

  @Test
  public void testStrictNansAndInfinities() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.setStrictness(Strictness.LEGACY_STRICT);
    writer.beginArray();
    assertThrows(IllegalArgumentException.class, () -> writer.value(Float.NaN));
    assertThrows(IllegalArgumentException.class, () -> writer.value(Float.NEGATIVE_INFINITY));
    assertThrows(IllegalArgumentException.class, () -> writer.value(Float.POSITIVE_INFINITY));
    assertThrows(IllegalArgumentException.class, () -> writer.value(Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> writer.value(Double.NEGATIVE_INFINITY));
    assertThrows(IllegalArgumentException.class, () -> writer.value(Double.POSITIVE_INFINITY));
  }

  @Test
  public void testStrictBoxedNansAndInfinities() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.setStrictness(Strictness.LEGACY_STRICT);
    writer.beginArray();
    assertThrows(IllegalArgumentException.class, () -> writer.value(Float.valueOf(Float.NaN)));
    assertThrows(
        IllegalArgumentException.class, () -> writer.value(Float.valueOf(Float.NEGATIVE_INFINITY)));
    assertThrows(
        IllegalArgumentException.class, () -> writer.value(Float.valueOf(Float.POSITIVE_INFINITY)));
    assertThrows(IllegalArgumentException.class, () -> writer.value(Double.valueOf(Double.NaN)));
    assertThrows(
        IllegalArgumentException.class,
        () -> writer.value(Double.valueOf(Double.NEGATIVE_INFINITY)));
    assertThrows(
        IllegalArgumentException.class,
        () -> writer.value(Double.valueOf(Double.POSITIVE_INFINITY)));
  }

  @Test
  public void testJsonValue() throws IOException {
    JsonTreeWriter writer = new JsonTreeWriter();
    writer.beginArray();
    assertThrows(UnsupportedOperationException.class, () -> writer.jsonValue("test"));
  }

  /**
   * {@link JsonTreeWriter} effectively replaces the complete writing logic of {@link JsonWriter} to
   * create a {@link JsonElement} tree instead of writing to a {@link Writer}. Therefore all
   * relevant methods of {@code JsonWriter} must be overridden.
   */
  @Test
  public void testOverrides() {
    List<String> ignoredMethods =
        Arrays.asList(
            "setLenient(boolean)",
            "isLenient()",
            "setStrictness(com.google.gson.Strictness)",
            "getStrictness()",
            "setTopLevelSeparator(java.lang.String)",
            "getTopLevelSeparator()",
            "setIndent(java.lang.String)",
            "setHtmlSafe(boolean)",
            "isHtmlSafe()",
            "setFormattingStyle(com.google.gson.FormattingStyle)",
            "getFormattingStyle()",
            "setSerializeNulls(boolean)",
            "getSerializeNulls()");
    MoreAsserts.assertOverridesMethods(JsonWriter.class, JsonTreeWriter.class, ignoredMethods);
  }
}
