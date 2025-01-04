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

import static com.google.gson.stream.JsonScope.DANGLING_NAME;
import static com.google.gson.stream.JsonScope.EMPTY_ARRAY;
import static com.google.gson.stream.JsonScope.EMPTY_DOCUMENT;
import static com.google.gson.stream.JsonScope.EMPTY_OBJECT;
import static com.google.gson.stream.JsonScope.NONEMPTY_ARRAY;
import static com.google.gson.stream.JsonScope.NONEMPTY_DOCUMENT;
import static com.google.gson.stream.JsonScope.NONEMPTY_OBJECT;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Writes a JSON (<a href="https://www.ietf.org/rfc/rfc8259.txt">RFC 8259</a>) encoded value to a
 * stream, one token at a time. The stream includes both literal values (strings, numbers, booleans
 * and nulls) as well as the begin and end delimiters of objects and arrays.
 *
 * <h2>Encoding JSON</h2>
 *
 * To encode your data as JSON, create a new {@code JsonWriter}. Call methods on the writer as you
 * walk the structure's contents, nesting arrays and objects as necessary:
 *
 * <ul>
 *   <li>To write <strong>arrays</strong>, first call {@link #beginArray()}. Write each of the
 *       array's elements with the appropriate {@link #value} methods or by nesting other arrays and
 *       objects. Finally close the array using {@link #endArray()}.
 *   <li>To write <strong>objects</strong>, first call {@link #beginObject()}. Write each of the
 *       object's properties by alternating calls to {@link #name} with the property's value. Write
 *       property values with the appropriate {@link #value} method or by nesting other objects or
 *       arrays. Finally close the object using {@link #endObject()}.
 * </ul>
 *
 * <h2>Configuration</h2>
 *
 * The behavior of this writer can be customized with the following methods:
 *
 * <ul>
 *   <li>{@link #setFormattingStyle(FormattingStyle)}, the default is {@link
 *       FormattingStyle#COMPACT}
 *   <li>{@link #setHtmlSafe(boolean)}, by default HTML characters are not escaped in the JSON
 *       output
 *   <li>{@link #setStrictness(Strictness)}, the default is {@link Strictness#LEGACY_STRICT}
 *   <li>{@link #setSerializeNulls(boolean)}, by default {@code null} is serialized
 *   <li>{@link #setTopLevelSeparator(String)}, by default {@code null} (= disabled)
 * </ul>
 *
 * The default configuration of {@code JsonWriter} instances used internally by the {@link Gson}
 * class differs, and can be adjusted with the various {@link GsonBuilder} methods.
 *
 * <h2>Example</h2>
 *
 * Suppose we'd like to encode a stream of messages such as the following:
 *
 * <pre>{@code
 * [
 *   {
 *     "id": 912345678901,
 *     "text": "How do I stream JSON in Java?",
 *     "geo": null,
 *     "user": {
 *       "name": "json_newb",
 *       "followers_count": 41
 *      }
 *   },
 *   {
 *     "id": 912345678902,
 *     "text": "@json_newb just use JsonWriter!",
 *     "geo": [50.454722, -104.606667],
 *     "user": {
 *       "name": "jesse",
 *       "followers_count": 2
 *     }
 *   }
 * ]
 * }</pre>
 *
 * This code encodes the above structure:
 *
 * <pre>{@code
 * public void writeJsonStream(OutputStream out, List<Message> messages) throws IOException {
 *   JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
 *   writer.setIndent("    ");
 *   writeMessagesArray(writer, messages);
 *   writer.close();
 * }
 *
 * public void writeMessagesArray(JsonWriter writer, List<Message> messages) throws IOException {
 *   writer.beginArray();
 *   for (Message message : messages) {
 *     writeMessage(writer, message);
 *   }
 *   writer.endArray();
 * }
 *
 * public void writeMessage(JsonWriter writer, Message message) throws IOException {
 *   writer.beginObject();
 *   writer.name("id").value(message.getId());
 *   writer.name("text").value(message.getText());
 *   if (message.getGeo() != null) {
 *     writer.name("geo");
 *     writeDoublesArray(writer, message.getGeo());
 *   } else {
 *     writer.name("geo").nullValue();
 *   }
 *   writer.name("user");
 *   writeUser(writer, message.getUser());
 *   writer.endObject();
 * }
 *
 * public void writeUser(JsonWriter writer, User user) throws IOException {
 *   writer.beginObject();
 *   writer.name("name").value(user.getName());
 *   writer.name("followers_count").value(user.getFollowersCount());
 *   writer.endObject();
 * }
 *
 * public void writeDoublesArray(JsonWriter writer, List<Double> doubles) throws IOException {
 *   writer.beginArray();
 *   for (Double value : doubles) {
 *     writer.value(value);
 *   }
 *   writer.endArray();
 * }
 * }</pre>
 *
 * <p>Each {@code JsonWriter} may be used to write a single JSON stream. Instances of this class are
 * not thread safe. Calls that would result in a malformed JSON string will fail with an {@link
 * IllegalStateException}.
 *
 * @author Jesse Wilson
 * @since 1.6
 */
public class JsonWriter implements Closeable, Flushable {

  // Syntax as defined by https://datatracker.ietf.org/doc/html/rfc8259#section-6
  private static final Pattern VALID_JSON_NUMBER_PATTERN =
      Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?");

  /*
   * From RFC 8259, "All Unicode characters may be placed within the
   * quotation marks except for the characters that must be escaped:
   * quotation mark, reverse solidus, and the control characters
   * (U+0000 through U+001F)."
   *
   * We also escape '\u2028' and '\u2029', which JavaScript interprets as
   * newline characters. This prevents eval() from failing with a syntax
   * error. http://code.google.com/p/google-gson/issues/detail?id=341
   */
  private static final String[] REPLACEMENT_CHARS;
  private static final String[] HTML_SAFE_REPLACEMENT_CHARS;

  static {
    REPLACEMENT_CHARS = new String[128];
    for (int i = 0; i <= 0x1f; i++) {
      REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
    }
    REPLACEMENT_CHARS['"'] = "\\\"";
    REPLACEMENT_CHARS['\\'] = "\\\\";
    REPLACEMENT_CHARS['\t'] = "\\t";
    REPLACEMENT_CHARS['\b'] = "\\b";
    REPLACEMENT_CHARS['\n'] = "\\n";
    REPLACEMENT_CHARS['\r'] = "\\r";
    REPLACEMENT_CHARS['\f'] = "\\f";
    HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
    HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
    HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
    HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
    HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
    HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
  }

  /** The JSON output destination */
  private final Writer out;

  private int[] stack = new int[32];
  private int stackSize = 0;

  {
    push(EMPTY_DOCUMENT);
  }

  private FormattingStyle formattingStyle;
  // These fields cache data derived from the formatting style, to avoid having to
  // re-evaluate it every time something is written
  private String formattedColon;
  private String formattedComma;
  private boolean usesEmptyNewlineAndIndent;

  private Strictness strictness = Strictness.LEGACY_STRICT;

  /**
   * Separator between top-level values; {@code null} if multiple top-level values are not allowed
   */
  private String topLevelSeparator = null;

  private boolean htmlSafe;

  private String deferredName;

  private boolean serializeNulls = true;

  /**
   * Creates a new instance that writes a JSON-encoded stream to {@code out}. For best performance,
   * ensure {@link Writer} is buffered; wrapping in {@link java.io.BufferedWriter BufferedWriter} if
   * necessary.
   */
  public JsonWriter(Writer out) {
    this.out = Objects.requireNonNull(out, "out == null");
    setFormattingStyle(FormattingStyle.COMPACT);
  }

  /**
   * Sets the indentation string to be repeated for each level of indentation in the encoded
   * document. If {@code indent.isEmpty()} the encoded document will be compact. Otherwise the
   * encoded document will be more human-readable.
   *
   * <p>This is a convenience method which overwrites any previously {@linkplain
   * #setFormattingStyle(FormattingStyle) set formatting style} with either {@link
   * FormattingStyle#COMPACT} if the given indent string is empty, or {@link FormattingStyle#PRETTY}
   * with the given indent if not empty.
   *
   * @param indent a string containing only whitespace.
   */
  public final void setIndent(String indent) {
    if (indent.isEmpty()) {
      setFormattingStyle(FormattingStyle.COMPACT);
    } else {
      setFormattingStyle(FormattingStyle.PRETTY.withIndent(indent));
    }
  }

  /**
   * Sets the formatting style to be used in the encoded document.
   *
   * <p>The formatting style specifies for example the indentation string to be repeated for each
   * level of indentation, or the newline style, to accommodate various OS styles.
   *
   * @param formattingStyle the formatting style to use, must not be {@code null}.
   * @see #getFormattingStyle()
   * @since 2.11.0
   */
  public final void setFormattingStyle(FormattingStyle formattingStyle) {
    this.formattingStyle = Objects.requireNonNull(formattingStyle);

    this.formattedComma = ",";
    if (this.formattingStyle.usesSpaceAfterSeparators()) {
      this.formattedColon = ": ";

      // Only add space if no newline is written
      if (this.formattingStyle.getNewline().isEmpty()) {
        this.formattedComma = ", ";
      }
    } else {
      this.formattedColon = ":";
    }

    this.usesEmptyNewlineAndIndent =
        this.formattingStyle.getNewline().isEmpty() && this.formattingStyle.getIndent().isEmpty();
  }

  /**
   * Returns the pretty printing style used by this writer.
   *
   * @return the {@code FormattingStyle} that will be used.
   * @see #setFormattingStyle(FormattingStyle)
   * @since 2.11.0
   */
  public final FormattingStyle getFormattingStyle() {
    return formattingStyle;
  }

  /**
   * Sets the strictness of this writer.
   *
   * @deprecated Please use {@link #setStrictness(Strictness)} instead. {@code
   *     JsonWriter.setLenient(true)} should be replaced by {@code
   *     JsonWriter.setStrictness(Strictness.LENIENT)} and {@code JsonWriter.setLenient(false)}
   *     should be replaced by {@code JsonWriter.setStrictness(Strictness.LEGACY_STRICT)}.<br>
   *     However, if you used {@code setLenient(false)} before, you might prefer {@link
   *     Strictness#STRICT} now instead.
   * @param lenient whether this writer should be lenient. If true, the strictness is set to {@link
   *     Strictness#LENIENT}. If false, the strictness is set to {@link Strictness#LEGACY_STRICT}.
   * @see #setStrictness(Strictness)
   */
  @Deprecated
  // Don't specify @InlineMe, so caller with `setLenient(false)` becomes aware of new
  // Strictness.STRICT
  @SuppressWarnings("InlineMeSuggester")
  public final void setLenient(boolean lenient) {
    setStrictness(lenient ? Strictness.LENIENT : Strictness.LEGACY_STRICT);
  }

  /**
   * Returns true if the {@link Strictness} of this writer is equal to {@link Strictness#LENIENT}.
   *
   * @see #getStrictness()
   */
  public boolean isLenient() {
    return strictness == Strictness.LENIENT;
  }

  /**
   * Configures how strict this writer is with regard to the syntax rules specified in <a
   * href="https://www.ietf.org/rfc/rfc8259.txt">RFC 8259</a>. By default, {@link
   * Strictness#LEGACY_STRICT} is used.
   *
   * <dl>
   *   <dt>{@link Strictness#STRICT} &amp; {@link Strictness#LEGACY_STRICT}
   *   <dd>The behavior of these is currently identical. In these strictness modes, the writer only
   *       writes JSON in accordance with RFC 8259.
   *   <dt>{@link Strictness#LENIENT}
   *   <dd>In lenient mode, the following departures from RFC 8259 are permitted:
   *       <ul>
   *         <li>Writing of {@link Double#isNaN() NaNs} and {@link Double#isInfinite() infinities}.
   *         <li>Writing multiple top level values. The values are concatenated without any
   *             separating whitespace. Can be enabled independently and can be further customized
   *             using {@link #setTopLevelSeparator(String)}.
   *       </ul>
   * </dl>
   *
   * @param strictness the new strictness of this writer. May not be {@code null}.
   * @see #getStrictness()
   * @since 2.11.0
   */
  public final void setStrictness(Strictness strictness) {
    this.strictness = Objects.requireNonNull(strictness);
    setTopLevelSeparator(strictness == Strictness.LENIENT ? "" : null);
  }

  /**
   * Returns the {@linkplain Strictness strictness} of this writer.
   *
   * @see #setStrictness(Strictness)
   * @since 2.11.0
   */
  public final Strictness getStrictness() {
    return strictness;
  }

  /**
   * Sets the separator to use between multiple top-level values. When writing multiple top-level
   * values the separator is written between the values, this can for example be useful for formats
   * such as <a href="https://jsonlines.org/">JSON Lines</a>.<br>
   * Using {@code null} disables support for multiple top-level values (the default).
   *
   * <p>This setting overwrites and is overwritten by whether {@link #setStrictness(Strictness)}
   * enabled support for multiple top-level values.
   *
   * @param separator separator between top-level values, or {@code null} to disable.
   * @see #getTopLevelSeparator()
   * @since $next-version$
   */
  public final void setTopLevelSeparator(String separator) {
    this.topLevelSeparator = separator;
  }

  /**
   * Returns the top-level separator, or {@code null} if disabled.
   *
   * @see #setTopLevelSeparator(String)
   * @since $next-version$
   */
  public final String getTopLevelSeparator() {
    return topLevelSeparator;
  }

  /**
   * Configures this writer to emit JSON that's safe for direct inclusion in HTML and XML documents.
   * This escapes the HTML characters {@code <}, {@code >}, {@code &}, {@code =} and {@code '}
   * before writing them to the stream. Without this setting, your XML/HTML encoder should replace
   * these characters with the corresponding escape sequences.
   *
   * @see #isHtmlSafe()
   */
  public final void setHtmlSafe(boolean htmlSafe) {
    this.htmlSafe = htmlSafe;
  }

  /**
   * Returns true if this writer writes JSON that's safe for inclusion in HTML and XML documents.
   *
   * @see #setHtmlSafe(boolean)
   */
  public final boolean isHtmlSafe() {
    return htmlSafe;
  }

  /**
   * Sets whether object members are serialized when their value is null. This has no impact on
   * array elements. The default is true.
   *
   * @see #getSerializeNulls()
   */
  public final void setSerializeNulls(boolean serializeNulls) {
    this.serializeNulls = serializeNulls;
  }

  /**
   * Returns true if object members are serialized when their value is null. This has no impact on
   * array elements. The default is true.
   *
   * @see #setSerializeNulls(boolean)
   */
  public final boolean getSerializeNulls() {
    return serializeNulls;
  }

  /**
   * Begins encoding a new array. Each call to this method must be paired with a call to {@link
   * #endArray}.
   *
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter beginArray() throws IOException {
    writeDeferredName();
    return openScope(EMPTY_ARRAY, '[');
  }

  /**
   * Ends encoding the current array.
   *
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter endArray() throws IOException {
    return closeScope(EMPTY_ARRAY, NONEMPTY_ARRAY, ']');
  }

  /**
   * Begins encoding a new object. Each call to this method must be paired with a call to {@link
   * #endObject}.
   *
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter beginObject() throws IOException {
    writeDeferredName();
    return openScope(EMPTY_OBJECT, '{');
  }

  /**
   * Ends encoding the current object.
   *
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter endObject() throws IOException {
    return closeScope(EMPTY_OBJECT, NONEMPTY_OBJECT, '}');
  }

  /** Enters a new scope by appending any necessary whitespace and the given bracket. */
  @CanIgnoreReturnValue
  private JsonWriter openScope(int empty, char openBracket) throws IOException {
    beforeValue();
    push(empty);
    out.write(openBracket);
    return this;
  }

  /** Closes the current scope by appending any necessary whitespace and the given bracket. */
  @CanIgnoreReturnValue
  private JsonWriter closeScope(int empty, int nonempty, char closeBracket) throws IOException {
    int context = peek();
    if (context != nonempty && context != empty) {
      throw new IllegalStateException("Nesting problem.");
    }
    if (deferredName != null) {
      throw new IllegalStateException("Dangling name: " + deferredName);
    }

    stackSize--;
    if (context == nonempty) {
      newline();
    }
    out.write(closeBracket);
    return this;
  }

  private void push(int newTop) {
    if (stackSize == stack.length) {
      stack = Arrays.copyOf(stack, stackSize * 2);
    }
    stack[stackSize++] = newTop;
  }

  /** Returns the value on the top of the stack. */
  private int peek() {
    if (stackSize == 0) {
      throw new IllegalStateException("JsonWriter is closed.");
    }
    return stack[stackSize - 1];
  }

  /** Replace the value on the top of the stack with the given value. */
  private void replaceTop(int topOfStack) {
    stack[stackSize - 1] = topOfStack;
  }

  /**
   * Encodes the property name.
   *
   * @param name the name of the forthcoming value. May not be {@code null}.
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter name(String name) throws IOException {
    Objects.requireNonNull(name, "name == null");
    if (deferredName != null) {
      throw new IllegalStateException("Already wrote a name, expecting a value.");
    }
    int context = peek();
    if (context != EMPTY_OBJECT && context != NONEMPTY_OBJECT) {
      throw new IllegalStateException("Please begin an object before writing a name.");
    }
    deferredName = name;
    return this;
  }

  private void writeDeferredName() throws IOException {
    if (deferredName != null) {
      beforeName();
      string(deferredName);
      deferredName = null;
    }
  }

  /**
   * Encodes {@code value}.
   *
   * @param value the literal string value, or null to encode a null literal.
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter value(String value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    writeDeferredName();
    beforeValue();
    string(value);
    return this;
  }

  /**
   * Encodes {@code value}.
   *
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter value(boolean value) throws IOException {
    writeDeferredName();
    beforeValue();
    out.write(value ? "true" : "false");
    return this;
  }

  /**
   * Encodes {@code value}.
   *
   * @return this writer.
   * @since 2.7
   */
  @CanIgnoreReturnValue
  public JsonWriter value(Boolean value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    writeDeferredName();
    beforeValue();
    out.write(value ? "true" : "false");
    return this;
  }

  /**
   * Encodes {@code value}.
   *
   * @param value a finite value, or if {@link #setStrictness(Strictness) lenient}, also {@link
   *     Float#isNaN() NaN} or {@link Float#isInfinite() infinity}.
   * @return this writer.
   * @throws IllegalArgumentException if the value is NaN or Infinity and this writer is not {@link
   *     #setStrictness(Strictness) lenient}.
   * @since 2.9.1
   */
  @CanIgnoreReturnValue
  public JsonWriter value(float value) throws IOException {
    writeDeferredName();
    if (strictness != Strictness.LENIENT && (Float.isNaN(value) || Float.isInfinite(value))) {
      throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
    }
    beforeValue();
    out.append(Float.toString(value));
    return this;
  }

  /**
   * Encodes {@code value}.
   *
   * @param value a finite value, or if {@link #setStrictness(Strictness) lenient}, also {@link
   *     Double#isNaN() NaN} or {@link Double#isInfinite() infinity}.
   * @return this writer.
   * @throws IllegalArgumentException if the value is NaN or Infinity and this writer is not {@link
   *     #setStrictness(Strictness) lenient}.
   */
  @CanIgnoreReturnValue
  public JsonWriter value(double value) throws IOException {
    writeDeferredName();
    if (strictness != Strictness.LENIENT && (Double.isNaN(value) || Double.isInfinite(value))) {
      throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
    }
    beforeValue();
    out.append(Double.toString(value));
    return this;
  }

  /**
   * Encodes {@code value}.
   *
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter value(long value) throws IOException {
    writeDeferredName();
    beforeValue();
    out.write(Long.toString(value));
    return this;
  }

  /**
   * Encodes {@code value}. The value is written by directly writing the {@link Number#toString()}
   * result to JSON. Implementations must make sure that the result represents a valid JSON number.
   *
   * @param value a finite value, or if {@link #setStrictness(Strictness) lenient}, also {@link
   *     Double#isNaN() NaN} or {@link Double#isInfinite() infinity}.
   * @return this writer.
   * @throws IllegalArgumentException if the value is NaN or Infinity and this writer is not {@link
   *     #setStrictness(Strictness) lenient}; or if the {@code toString()} result is not a valid
   *     JSON number.
   */
  @CanIgnoreReturnValue
  public JsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    }

    writeDeferredName();
    String string = value.toString();
    Class<? extends Number> numberClass = value.getClass();

    if (!alwaysCreatesValidJsonNumber(numberClass)) {
      // Validate that string is valid before writing it directly to JSON output
      if (string.equals("-Infinity") || string.equals("Infinity") || string.equals("NaN")) {
        if (strictness != Strictness.LENIENT) {
          throw new IllegalArgumentException("Numeric values must be finite, but was " + string);
        }
      } else if (numberClass != Float.class
          && numberClass != Double.class
          && !VALID_JSON_NUMBER_PATTERN.matcher(string).matches()) {
        throw new IllegalArgumentException(
            "String created by " + numberClass + " is not a valid JSON number: " + string);
      }
    }

    beforeValue();
    out.append(string);
    return this;
  }

  /**
   * Encodes {@code null}.
   *
   * @return this writer.
   */
  @CanIgnoreReturnValue
  public JsonWriter nullValue() throws IOException {
    if (deferredName != null) {
      if (serializeNulls) {
        writeDeferredName();
      } else {
        deferredName = null;
        return this; // skip the name and the value
      }
    }
    beforeValue();
    out.write("null");
    return this;
  }

  /**
   * Writes {@code value} directly to the writer without quoting or escaping. This might not be
   * supported by all implementations, if not supported an {@code UnsupportedOperationException} is
   * thrown.
   *
   * @param value the literal string value, or null to encode a null literal.
   * @return this writer.
   * @throws UnsupportedOperationException if this writer does not support writing raw JSON values.
   * @since 2.4
   */
  @CanIgnoreReturnValue
  public JsonWriter jsonValue(String value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    writeDeferredName();
    beforeValue();
    out.append(value);
    return this;
  }

  /**
   * Ensures all buffered data is written to the underlying {@link Writer} and flushes that writer.
   */
  @Override
  public void flush() throws IOException {
    if (stackSize == 0) {
      throw new IllegalStateException("JsonWriter is closed.");
    }
    out.flush();
  }

  /**
   * Flushes and closes this writer and the underlying {@link Writer}.
   *
   * @throws IOException if the JSON document is incomplete.
   */
  @Override
  public void close() throws IOException {
    out.close();

    int size = stackSize;
    if (size > 1 || (size == 1 && stack[size - 1] != NONEMPTY_DOCUMENT)) {
      throw new IOException("Incomplete document");
    }
    stackSize = 0;
  }

  /** Returns whether the {@code toString()} of {@code c} will always return a valid JSON number. */
  private static boolean alwaysCreatesValidJsonNumber(Class<? extends Number> c) {
    // Does not include Float or Double because their value can be NaN or Infinity
    // Does not include LazilyParsedNumber because it could contain a malformed string
    return c == Integer.class
        || c == Long.class
        || c == Byte.class
        || c == Short.class
        || c == BigDecimal.class
        || c == BigInteger.class
        || c == AtomicInteger.class
        || c == AtomicLong.class;
  }

  private void string(String value) throws IOException {
    String[] replacements = htmlSafe ? HTML_SAFE_REPLACEMENT_CHARS : REPLACEMENT_CHARS;
    out.write('\"');
    int last = 0;
    int length = value.length();
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      String replacement;
      if (c < 128) {
        replacement = replacements[c];
        if (replacement == null) {
          continue;
        }
      } else if (c == '\u2028') {
        replacement = "\\u2028";
      } else if (c == '\u2029') {
        replacement = "\\u2029";
      } else {
        continue;
      }
      if (last < i) {
        out.write(value, last, i - last);
      }
      out.write(replacement);
      last = i + 1;
    }
    if (last < length) {
      out.write(value, last, length - last);
    }
    out.write('\"');
  }

  private void newline() throws IOException {
    if (usesEmptyNewlineAndIndent) {
      return;
    }

    out.write(formattingStyle.getNewline());
    for (int i = 1, size = stackSize; i < size; i++) {
      out.write(formattingStyle.getIndent());
    }
  }

  /**
   * Inserts any necessary separators and whitespace before a name. Also adjusts the stack to expect
   * the name's value.
   */
  private void beforeName() throws IOException {
    int context = peek();
    if (context == NONEMPTY_OBJECT) { // first in object
      out.write(formattedComma);
    } else if (context != EMPTY_OBJECT) { // not in an object!
      throw new IllegalStateException("Nesting problem.");
    }
    newline();
    replaceTop(DANGLING_NAME);
  }

  /**
   * Inserts any necessary separators and whitespace before a literal value, inline array, or inline
   * object. Also adjusts the stack to expect either a closing bracket or another element.
   */
  @SuppressWarnings("fallthrough")
  private void beforeValue() throws IOException {
    switch (peek()) {
      case NONEMPTY_DOCUMENT:
        if (topLevelSeparator == null) {
          throw new IllegalStateException(
              "Multiple top-level values support has not been enabled, use"
                  + " `JsonWriter.setTopLevelSeparator(String)`");
        }
        out.append(topLevelSeparator);
        break;

      case EMPTY_DOCUMENT: // first in document
        replaceTop(NONEMPTY_DOCUMENT);
        break;

      case EMPTY_ARRAY: // first in array
        replaceTop(NONEMPTY_ARRAY);
        newline();
        break;

      case NONEMPTY_ARRAY: // another in array
        out.append(formattedComma);
        newline();
        break;

      case DANGLING_NAME: // value for name
        out.append(formattedColon);
        replaceTop(NONEMPTY_OBJECT);
        break;

      default:
        throw new IllegalStateException("Nesting problem.");
    }
  }
}
