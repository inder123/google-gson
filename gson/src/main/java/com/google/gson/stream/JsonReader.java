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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.internal.TroubleshootingGuide;
import com.google.gson.internal.bind.JsonTreeReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Objects;

/**
 * Reads a JSON (<a href="https://www.ietf.org/rfc/rfc8259.txt">RFC 8259</a>) encoded value as a
 * stream of tokens. This stream includes both literal values (strings, numbers, booleans, and
 * nulls) as well as the begin and end delimiters of objects and arrays. The tokens are traversed in
 * depth-first order, the same order that they appear in the JSON document. Within JSON objects,
 * name/value pairs are represented by a single token.
 *
 * <h2>Parsing JSON</h2>
 *
 * To create a recursive descent parser for your own JSON streams, first create an entry point
 * method that creates a {@code JsonReader}.
 *
 * <p>Next, create handler methods for each structure in your JSON text. You'll need a method for
 * each object type and for each array type.
 *
 * <ul>
 *   <li>Within <strong>array handling</strong> methods, first call {@link #beginArray} to consume
 *       the array's opening bracket. Then create a while loop that accumulates values, terminating
 *       when {@link #hasNext} is false. Finally, read the array's closing bracket by calling {@link
 *       #endArray}.
 *   <li>Within <strong>object handling</strong> methods, first call {@link #beginObject} to consume
 *       the object's opening brace. Then create a while loop that assigns values to local variables
 *       based on their name. This loop should terminate when {@link #hasNext} is false. Finally,
 *       read the object's closing brace by calling {@link #endObject}.
 * </ul>
 *
 * <p>When a nested object or array is encountered, delegate to the corresponding handler method.
 *
 * <p>When an unknown name is encountered, strict parsers should fail with an exception. Lenient
 * parsers should call {@link #skipValue()} to recursively skip the value's nested tokens, which may
 * otherwise conflict.
 *
 * <p>If a value may be null, you should first check using {@link #peek()}. Null literals can be
 * consumed using either {@link #nextNull()} or {@link #skipValue()}.
 *
 * <h2>Configuration</h2>
 *
 * The behavior of this reader can be customized with the following methods:
 *
 * <ul>
 *   <li>{@link #setStrictness(Strictness)}, the default is {@link Strictness#LEGACY_STRICT}
 *   <li>{@link #setNestingLimit(int)}, the default is {@value #DEFAULT_NESTING_LIMIT}
 * </ul>
 *
 * The default configuration of {@code JsonReader} instances used internally by the {@link Gson}
 * class differs, and can be adjusted with the various {@link GsonBuilder} methods.
 *
 * <h2>Example</h2>
 *
 * Suppose we'd like to parse a stream of messages such as the following:
 *
 * <pre>{@code
 * [
 *   {
 *     "id": 912345678901,
 *     "text": "How do I read a JSON stream in Java?",
 *     "geo": null,
 *     "user": {
 *       "name": "json_newb",
 *       "followers_count": 41
 *      }
 *   },
 *   {
 *     "id": 912345678902,
 *     "text": "@json_newb just use JsonReader!",
 *     "geo": [50.454722, -104.606667],
 *     "user": {
 *       "name": "jesse",
 *       "followers_count": 2
 *     }
 *   }
 * ]
 * }</pre>
 *
 * This code implements the parser for the above structure:
 *
 * <pre>{@code
 * public List<Message> readJsonStream(InputStream in) throws IOException {
 *   JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
 *   try {
 *     return readMessagesArray(reader);
 *   } finally {
 *     reader.close();
 *   }
 * }
 *
 * public List<Message> readMessagesArray(JsonReader reader) throws IOException {
 *   List<Message> messages = new ArrayList<>();
 *
 *   reader.beginArray();
 *   while (reader.hasNext()) {
 *     messages.add(readMessage(reader));
 *   }
 *   reader.endArray();
 *   return messages;
 * }
 *
 * public Message readMessage(JsonReader reader) throws IOException {
 *   long id = -1;
 *   String text = null;
 *   User user = null;
 *   List<Double> geo = null;
 *
 *   reader.beginObject();
 *   while (reader.hasNext()) {
 *     String name = reader.nextName();
 *     if (name.equals("id")) {
 *       id = reader.nextLong();
 *     } else if (name.equals("text")) {
 *       text = reader.nextString();
 *     } else if (name.equals("geo") && reader.peek() != JsonToken.NULL) {
 *       geo = readDoublesArray(reader);
 *     } else if (name.equals("user")) {
 *       user = readUser(reader);
 *     } else {
 *       reader.skipValue();
 *     }
 *   }
 *   reader.endObject();
 *   return new Message(id, text, user, geo);
 * }
 *
 * public List<Double> readDoublesArray(JsonReader reader) throws IOException {
 *   List<Double> doubles = new ArrayList<>();
 *
 *   reader.beginArray();
 *   while (reader.hasNext()) {
 *     doubles.add(reader.nextDouble());
 *   }
 *   reader.endArray();
 *   return doubles;
 * }
 *
 * public User readUser(JsonReader reader) throws IOException {
 *   String username = null;
 *   int followersCount = -1;
 *
 *   reader.beginObject();
 *   while (reader.hasNext()) {
 *     String name = reader.nextName();
 *     if (name.equals("name")) {
 *       username = reader.nextString();
 *     } else if (name.equals("followers_count")) {
 *       followersCount = reader.nextInt();
 *     } else {
 *       reader.skipValue();
 *     }
 *   }
 *   reader.endObject();
 *   return new User(username, followersCount);
 * }
 * }</pre>
 *
 * <h2>Number Handling</h2>
 *
 * This reader permits numeric values to be read as strings and string values to be read as numbers.
 * For example, both elements of the JSON array {@code [1, "1"]} may be read using either {@link
 * #nextInt} or {@link #nextString}. This behavior is intended to prevent lossy numeric conversions:
 * double is JavaScript's only numeric type and very large values like {@code 9007199254740993}
 * cannot be represented exactly on that platform. To minimize precision loss, extremely large
 * values should be written and read as strings in JSON.
 *
 * <h2 id="nonexecuteprefix">Non-Execute Prefix</h2>
 *
 * Web servers that serve private data using JSON may be vulnerable to <a
 * href="http://en.wikipedia.org/wiki/JSON#Cross-site_request_forgery">Cross-site request
 * forgery</a> attacks. In such an attack, a malicious site gains access to a private JSON file by
 * executing it with an HTML {@code <script>} tag.
 *
 * <p>Prefixing JSON files with <code>")]}'\n"</code> makes them non-executable by {@code <script>}
 * tags, disarming the attack. Since the prefix is malformed JSON, strict parsing fails when it is
 * encountered. This class permits the non-execute prefix when {@linkplain
 * #setStrictness(Strictness) lenient parsing} is enabled.
 *
 * <p>Each {@code JsonReader} may be used to read a single JSON stream. Instances of this class are
 * not thread safe.
 *
 * @author Jesse Wilson
 * @since 1.6
 */
public class JsonReader implements Closeable {
  private static final long MIN_INCOMPLETE_INTEGER = Long.MIN_VALUE / 10;

  private static final int PEEKED_NONE = 0;
  private static final int PEEKED_BEGIN_OBJECT = 1;
  private static final int PEEKED_END_OBJECT = 2;
  private static final int PEEKED_BEGIN_ARRAY = 3;
  private static final int PEEKED_END_ARRAY = 4;
  private static final int PEEKED_TRUE = 5;
  private static final int PEEKED_FALSE = 6;
  private static final int PEEKED_NULL = 7;
  private static final int PEEKED_SINGLE_QUOTED = 8;
  private static final int PEEKED_DOUBLE_QUOTED = 9;
  private static final int PEEKED_UNQUOTED = 10;

  /** When this is returned, the string value is stored in peekedString. */
  private static final int PEEKED_BUFFERED = 11;

  private static final int PEEKED_SINGLE_QUOTED_NAME = 12;
  private static final int PEEKED_DOUBLE_QUOTED_NAME = 13;
  private static final int PEEKED_UNQUOTED_NAME = 14;

  /** When this is returned, the integer value is stored in peekedLong. */
  private static final int PEEKED_LONG = 15;

  private static final int PEEKED_NUMBER = 16;
  private static final int PEEKED_EOF = 17;

  /* State machine when parsing numbers */
  private static final int NUMBER_CHAR_NONE = 0;
  private static final int NUMBER_CHAR_SIGN = 1;
  private static final int NUMBER_CHAR_DIGIT = 2;
  private static final int NUMBER_CHAR_DECIMAL = 3;
  private static final int NUMBER_CHAR_FRACTION_DIGIT = 4;
  private static final int NUMBER_CHAR_EXP_E = 5;
  private static final int NUMBER_CHAR_EXP_SIGN = 6;
  private static final int NUMBER_CHAR_EXP_DIGIT = 7;

  /** The input JSON. */
  private final Reader in;

  private Strictness strictness = Strictness.LEGACY_STRICT;
  // Default nesting limit is based on
  // https://github.com/square/moshi/blob/parent-1.15.0/moshi/src/main/java/com/squareup/moshi/JsonReader.java#L228-L230
  static final int DEFAULT_NESTING_LIMIT = 255;
  private int nestingLimit = DEFAULT_NESTING_LIMIT;

  static final int BUFFER_SIZE = 1024;

  /**
   * Use a manual buffer to easily read and unread upcoming characters, and also so we can create
   * strings without an intermediate StringBuilder. We decode literals directly out of this buffer,
   * so it must be at least as long as the longest token that can be reported as a number.
   */
  private final char[] buffer = new char[BUFFER_SIZE];

  private int pos = 0;
  private int limit = 0;

  private int lineNumber = 0;
  private int lineStart = 0;

  int peeked = PEEKED_NONE;

  /**
   * A peeked value that was composed entirely of digits with an optional leading dash. Positive
   * values may not have a leading 0.
   */
  private long peekedLong;

  /**
   * The number of characters in a peeked number literal. Increment 'pos' by this after reading a
   * number.
   */
  private int peekedNumberLength;

  /**
   * A peeked string that should be parsed on the next double, long or string. This is populated
   * before a numeric value is parsed and used if that parsing fails.
   */
  private String peekedString;

  /** The nesting stack. Using a manual array rather than an ArrayList saves 20%. */
  private int[] stack = new int[32];

  private int stackSize = 0;

  {
    stack[stackSize++] = JsonScope.EMPTY_DOCUMENT;
  }

  /*
   * The path members. It corresponds directly to stack: At indices where the
   * stack contains an object (EMPTY_OBJECT, DANGLING_NAME or NONEMPTY_OBJECT),
   * pathNames contains the name at this scope. Where it contains an array
   * (EMPTY_ARRAY, NONEMPTY_ARRAY) pathIndices contains the current index in
   * that array. Otherwise the value is undefined, and we take advantage of that
   * by incrementing pathIndices when doing so isn't useful.
   */
  private String[] pathNames = new String[32];
  private int[] pathIndices = new int[32];

  /** Creates a new instance that reads a JSON-encoded stream from {@code in}. */
  public JsonReader(Reader in) {
    this.in = Objects.requireNonNull(in, "in == null");
  }

  /**
   * Sets the strictness of this reader.
   *
   * @deprecated Please use {@link #setStrictness(Strictness)} instead. {@code
   *     JsonReader.setLenient(true)} should be replaced by {@code
   *     JsonReader.setStrictness(Strictness.LENIENT)} and {@code JsonReader.setLenient(false)}
   *     should be replaced by {@code JsonReader.setStrictness(Strictness.LEGACY_STRICT)}.<br>
   *     However, if you used {@code setLenient(false)} before, you might prefer {@link
   *     Strictness#STRICT} now instead.
   * @param lenient whether this reader should be lenient. If true, the strictness is set to {@link
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
   * Returns true if the {@link Strictness} of this reader is equal to {@link Strictness#LENIENT}.
   *
   * @see #getStrictness()
   */
  public final boolean isLenient() {
    return strictness == Strictness.LENIENT;
  }

  /**
   * Configures how liberal this parser is in what it accepts.
   *
   * <p>In {@linkplain Strictness#STRICT strict} mode, the parser only accepts JSON in accordance
   * with <a href="https://www.ietf.org/rfc/rfc8259.txt">RFC 8259</a>. In {@linkplain
   * Strictness#LEGACY_STRICT legacy strict} mode (the default), only JSON in accordance with the
   * RFC 8259 is accepted, with a few exceptions denoted below for backwards compatibility reasons.
   * In {@linkplain Strictness#LENIENT lenient} mode, all sort of non-spec compliant JSON is
   * accepted (see below).
   *
   * <dl>
   *   <dt>{@link Strictness#STRICT}
   *   <dd>In strict mode, only input compliant with RFC 8259 is accepted.
   *   <dt>{@link Strictness#LEGACY_STRICT}
   *   <dd>In legacy strict mode, the following departures from RFC 8259 are accepted:
   *       <ul>
   *         <li>JsonReader allows the literals {@code true}, {@code false} and {@code null} to have
   *             any capitalization, for example {@code fAlSe} or {@code NULL}
   *         <li>JsonReader supports the escape sequence {@code \'}, representing a {@code '}
   *             (single-quote)
   *         <li>JsonReader supports the escape sequence <code>\<i>LF</i></code> (with {@code LF}
   *             being the Unicode character {@code U+000A}), resulting in a {@code LF} within the
   *             read JSON string
   *         <li>JsonReader allows unescaped control characters ({@code U+0000} through {@code
   *             U+001F})
   *       </ul>
   *   <dt>{@link Strictness#LENIENT}
   *   <dd>In lenient mode, all input that is accepted in legacy strict mode is accepted in addition
   *       to the following departures from RFC 8259:
   *       <ul>
   *         <li>Streams that start with the <a href="#nonexecuteprefix">non-execute prefix</a>,
   *             {@code ")]}'\n"}
   *         <li>Streams that include multiple top-level values. With legacy strict or strict
   *             parsing, each stream must contain exactly one top-level value.
   *         <li>Numbers may be {@link Double#isNaN() NaNs} or {@link Double#isInfinite()
   *             infinities} represented by {@code NaN} and {@code (-)Infinity} respectively.
   *         <li>End of line comments starting with {@code //} or {@code #} and ending with a
   *             newline character.
   *         <li>C-style comments starting with {@code /*} and ending with {@code *}{@code /}. Such
   *             comments may not be nested.
   *         <li>Names that are unquoted or {@code 'single quoted'}.
   *         <li>Strings that are unquoted or {@code 'single quoted'}.
   *         <li>Array elements separated by {@code ;} instead of {@code ,}.
   *         <li>Unnecessary array separators. These are interpreted as if null was the omitted
   *             value.
   *         <li>Names and values separated by {@code =} or {@code =>} instead of {@code :}.
   *         <li>Name/value pairs separated by {@code ;} instead of {@code ,}.
   *       </ul>
   * </dl>
   *
   * @param strictness the new strictness value of this reader. May not be {@code null}.
   * @see #getStrictness()
   * @since 2.11.0
   */
  public final void setStrictness(Strictness strictness) {
    Objects.requireNonNull(strictness);
    this.strictness = strictness;
  }

  /**
   * Returns the {@linkplain Strictness strictness} of this reader.
   *
   * @see #setStrictness(Strictness)
   * @since 2.11.0
   */
  public final Strictness getStrictness() {
    return strictness;
  }

  /**
   * Sets the nesting limit of this reader.
   *
   * <p>The nesting limit defines how many JSON arrays or objects may be open at the same time. For
   * example a nesting limit of 0 means no arrays or objects may be opened at all, a nesting limit
   * of 1 means one array or object may be open at the same time, and so on. So a nesting limit of 3
   * allows reading the JSON data <code>[{"a":[true]}]</code>, but for a nesting limit of 2 it would
   * fail at the inner {@code [true]}.
   *
   * <p>The nesting limit can help to protect against a {@link StackOverflowError} when recursive
   * {@link TypeAdapter} implementations process deeply nested JSON data.
   *
   * <p>The default nesting limit is {@value #DEFAULT_NESTING_LIMIT}.
   *
   * @throws IllegalArgumentException if the nesting limit is negative.
   * @since 2.12.0
   * @see #getNestingLimit()
   */
  public final void setNestingLimit(int limit) {
    if (limit < 0) {
      throw new IllegalArgumentException("Invalid nesting limit: " + limit);
    }
    this.nestingLimit = limit;
  }

  /**
   * Returns the nesting limit of this reader.
   *
   * @since 2.12.0
   * @see #setNestingLimit(int)
   */
  public final int getNestingLimit() {
    return nestingLimit;
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
   * array.
   *
   * @throws IllegalStateException if the next token is not the beginning of an array.
   */
  public void beginArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_BEGIN_ARRAY) {
      push(JsonScope.EMPTY_ARRAY);
      pathIndices[stackSize - 1] = 0;
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("BEGIN_ARRAY");
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the end of the current
   * array.
   *
   * @throws IllegalStateException if the next token is not the end of an array.
   */
  public void endArray() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_END_ARRAY) {
      stackSize--;
      pathIndices[stackSize - 1]++;
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("END_ARRAY");
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
   * object.
   *
   * @throws IllegalStateException if the next token is not the beginning of an object.
   */
  public void beginObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_BEGIN_OBJECT) {
      push(JsonScope.EMPTY_OBJECT);
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("BEGIN_OBJECT");
    }
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is the end of the current
   * object.
   *
   * @throws IllegalStateException if the next token is not the end of an object.
   */
  public void endObject() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_END_OBJECT) {
      stackSize--;
      pathNames[stackSize] = null; // Free the last path name so that it can be garbage collected!
      pathIndices[stackSize - 1]++;
      peeked = PEEKED_NONE;
    } else {
      throw unexpectedTokenError("END_OBJECT");
    }
  }

  /** Returns true if the current array or object has another element. */
  public boolean hasNext() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY && p != PEEKED_EOF;
  }

  /** Returns the type of the next token without consuming it. */
  public JsonToken peek() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }

    switch (p) {
      case PEEKED_BEGIN_OBJECT:
        return JsonToken.BEGIN_OBJECT;
      case PEEKED_END_OBJECT:
        return JsonToken.END_OBJECT;
      case PEEKED_BEGIN_ARRAY:
        return JsonToken.BEGIN_ARRAY;
      case PEEKED_END_ARRAY:
        return JsonToken.END_ARRAY;
      case PEEKED_SINGLE_QUOTED_NAME:
      case PEEKED_DOUBLE_QUOTED_NAME:
      case PEEKED_UNQUOTED_NAME:
        return JsonToken.NAME;
      case PEEKED_TRUE:
      case PEEKED_FALSE:
        return JsonToken.BOOLEAN;
      case PEEKED_NULL:
        return JsonToken.NULL;
      case PEEKED_SINGLE_QUOTED:
      case PEEKED_DOUBLE_QUOTED:
      case PEEKED_UNQUOTED:
      case PEEKED_BUFFERED:
        return JsonToken.STRING;
      case PEEKED_LONG:
      case PEEKED_NUMBER:
        return JsonToken.NUMBER;
      case PEEKED_EOF:
        return JsonToken.END_DOCUMENT;
      default:
        throw new AssertionError();
    }
  }

  @SuppressWarnings("fallthrough")
  int doPeek() throws IOException {
    int peekStack = stack[stackSize - 1];
    if (peekStack == JsonScope.EMPTY_ARRAY) {
      stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
    } else if (peekStack == JsonScope.NONEMPTY_ARRAY) {
      // Look for a comma before the next element.
      int c = nextNonWhitespace(true);
      switch (c) {
        case ']':
          return peeked = PEEKED_END_ARRAY;
        case ';':
          checkLenient(); // fall-through
        case ',':
          break;
        default:
          throw syntaxError("Unterminated array");
      }
    } else if (peekStack == JsonScope.EMPTY_OBJECT || peekStack == JsonScope.NONEMPTY_OBJECT) {
      stack[stackSize - 1] = JsonScope.DANGLING_NAME;
      // Look for a comma before the next element.
      if (peekStack == JsonScope.NONEMPTY_OBJECT) {
        int c = nextNonWhitespace(true);
        switch (c) {
          case '}':
            return peeked = PEEKED_END_OBJECT;
          case ';':
            checkLenient(); // fall-through
          case ',':
            break;
          default:
            throw syntaxError("Unterminated object");
        }
      }
      int c = nextNonWhitespace(true);
      switch (c) {
        case '"':
          return peeked = PEEKED_DOUBLE_QUOTED_NAME;
        case '\'':
          checkLenient();
          return peeked = PEEKED_SINGLE_QUOTED_NAME;
        case '}':
          if (peekStack != JsonScope.NONEMPTY_OBJECT) {
            return peeked = PEEKED_END_OBJECT;
          } else {
            throw syntaxError("Expected name");
          }
        default:
          checkLenient();
          pos--; // Don't consume the first character in an unquoted string.
          if (isLiteral((char) c)) {
            return peeked = PEEKED_UNQUOTED_NAME;
          } else {
            throw syntaxError("Expected name");
          }
      }
    } else if (peekStack == JsonScope.DANGLING_NAME) {
      stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
      // Look for a colon before the value.
      int c = nextNonWhitespace(true);
      switch (c) {
        case ':':
          break;
        case '=':
          checkLenient();
          if ((pos < limit || fillBuffer(1)) && buffer[pos] == '>') {
            pos++;
          }
          break;
        default:
          throw syntaxError("Expected ':'");
      }
    } else if (peekStack == JsonScope.EMPTY_DOCUMENT) {
      if (strictness == Strictness.LENIENT) {
        consumeNonExecutePrefix();
      }
      stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
    } else if (peekStack == JsonScope.NONEMPTY_DOCUMENT) {
      int c = nextNonWhitespace(false);
      if (c == -1) {
        return peeked = PEEKED_EOF;
      } else {
        checkLenient();
        pos--;
      }
    } else if (peekStack == JsonScope.CLOSED) {
      throw new IllegalStateException("JsonReader is closed");
    }

    int c = nextNonWhitespace(true);
    switch (c) {
      case ']':
        if (peekStack == JsonScope.EMPTY_ARRAY) {
          return peeked = PEEKED_END_ARRAY;
        }
      // fall-through to handle ",]"
      case ';':
      case ',':
        // In lenient mode, a 0-length literal in an array means 'null'.
        if (peekStack == JsonScope.EMPTY_ARRAY || peekStack == JsonScope.NONEMPTY_ARRAY) {
          checkLenient();
          pos--;
          return peeked = PEEKED_NULL;
        } else {
          throw syntaxError("Unexpected value");
        }
      case '\'':
        checkLenient();
        return peeked = PEEKED_SINGLE_QUOTED;
      case '"':
        return peeked = PEEKED_DOUBLE_QUOTED;
      case '[':
        return peeked = PEEKED_BEGIN_ARRAY;
      case '{':
        return peeked = PEEKED_BEGIN_OBJECT;
      default:
        pos--; // Don't consume the first character in a literal value.
    }

    int result = peekKeyword();
    if (result != PEEKED_NONE) {
      return result;
    }

    result = peekNumber();
    if (result != PEEKED_NONE) {
      return result;
    }

    if (!isLiteral(buffer[pos])) {
      throw syntaxError("Expected value");
    }

    checkLenient();
    return peeked = PEEKED_UNQUOTED;
  }

  private int peekKeyword() throws IOException {
    // Figure out which keyword we're matching against by its first character.
    char c = buffer[pos];
    String keyword;
    String keywordUpper;
    int peeking;

    // Look at the first letter to determine what keyword we are trying to match.
    if (c == 't' || c == 'T') {
      keyword = "true";
      keywordUpper = "TRUE";
      peeking = PEEKED_TRUE;
    } else if (c == 'f' || c == 'F') {
      keyword = "false";
      keywordUpper = "FALSE";
      peeking = PEEKED_FALSE;
    } else if (c == 'n' || c == 'N') {
      keyword = "null";
      keywordUpper = "NULL";
      peeking = PEEKED_NULL;
    } else {
      return PEEKED_NONE;
    }

    // Uppercased keywords are not allowed in STRICT mode
    boolean allowsUpperCased = strictness != Strictness.STRICT;

    // Confirm that chars [0..length) match the keyword.
    int length = keyword.length();
    for (int i = 0; i < length; i++) {
      if (pos + i >= limit && !fillBuffer(i + 1)) {
        return PEEKED_NONE;
      }
      c = buffer[pos + i];
      boolean matched = c == keyword.charAt(i) || (allowsUpperCased && c == keywordUpper.charAt(i));
      if (!matched) {
        return PEEKED_NONE;
      }
    }

    if ((pos + length < limit || fillBuffer(length + 1)) && isLiteral(buffer[pos + length])) {
      return PEEKED_NONE; // Don't match trues, falsey or nullsoft!
    }

    // We've found the keyword followed either by EOF or by a non-literal character.
    pos += length;
    return peeked = peeking;
  }

  private int peekNumber() throws IOException {
    // Like nextNonWhitespace, this uses locals 'p' and 'l' to save inner-loop field access.
    char[] buffer = this.buffer;
    int p = pos;
    int l = limit;

    long value = 0; // Negative to accommodate Long.MIN_VALUE more easily.
    boolean negative = false;
    boolean fitsInLong = true;
    int last = NUMBER_CHAR_NONE;

    int i = 0;

    charactersOfNumber:
    for (; true; i++) {
      if (p + i == l) {
        if (i == buffer.length) {
          // Though this looks like a well-formed number, it's too long to continue reading. Give up
          // and let the application handle this as an unquoted literal.
          return PEEKED_NONE;
        }
        if (!fillBuffer(i + 1)) {
          break;
        }
        p = pos;
        l = limit;
      }

      char c = buffer[p + i];
      switch (c) {
        case '-':
          if (last == NUMBER_CHAR_NONE) {
            negative = true;
            last = NUMBER_CHAR_SIGN;
            continue;
          } else if (last == NUMBER_CHAR_EXP_E) {
            last = NUMBER_CHAR_EXP_SIGN;
            continue;
          }
          return PEEKED_NONE;

        case '+':
          if (last == NUMBER_CHAR_EXP_E) {
            last = NUMBER_CHAR_EXP_SIGN;
            continue;
          }
          return PEEKED_NONE;

        case 'e':
        case 'E':
          if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT) {
            last = NUMBER_CHAR_EXP_E;
            continue;
          }
          return PEEKED_NONE;

        case '.':
          if (last == NUMBER_CHAR_DIGIT) {
            last = NUMBER_CHAR_DECIMAL;
            continue;
          }
          return PEEKED_NONE;

        default:
          if (c < '0' || c > '9') {
            if (!isLiteral(c)) {
              break charactersOfNumber;
            }
            return PEEKED_NONE;
          }
          if (last == NUMBER_CHAR_SIGN || last == NUMBER_CHAR_NONE) {
            value = -(c - '0');
            last = NUMBER_CHAR_DIGIT;
          } else if (last == NUMBER_CHAR_DIGIT) {
            if (value == 0) {
              return PEEKED_NONE; // Leading '0' prefix is not allowed (since it could be octal).
            }
            long newValue = value * 10 - (c - '0');
            fitsInLong &=
                value > MIN_INCOMPLETE_INTEGER
                    || (value == MIN_INCOMPLETE_INTEGER && newValue < value);
            value = newValue;
          } else if (last == NUMBER_CHAR_DECIMAL) {
            last = NUMBER_CHAR_FRACTION_DIGIT;
          } else if (last == NUMBER_CHAR_EXP_E || last == NUMBER_CHAR_EXP_SIGN) {
            last = NUMBER_CHAR_EXP_DIGIT;
          }
      }
    }

    // We've read a complete number. Decide if it's a PEEKED_LONG or a PEEKED_NUMBER.
    // Don't store -0 as long; user might want to read it as double -0.0
    // Don't try to convert Long.MIN_VALUE to positive long; it would overflow MAX_VALUE
    if (last == NUMBER_CHAR_DIGIT
        && fitsInLong
        && (value != Long.MIN_VALUE || negative)
        && (value != 0 || !negative)) {
      peekedLong = negative ? value : -value;
      pos += i;
      return peeked = PEEKED_LONG;
    } else if (last == NUMBER_CHAR_DIGIT
        || last == NUMBER_CHAR_FRACTION_DIGIT
        || last == NUMBER_CHAR_EXP_DIGIT) {
      peekedNumberLength = i;
      return peeked = PEEKED_NUMBER;
    } else {
      return PEEKED_NONE;
    }
  }

  @SuppressWarnings("fallthrough")
  private boolean isLiteral(char c) throws IOException {
    switch (c) {
      case '/':
      case '\\':
      case ';':
      case '#':
      case '=':
        checkLenient(); // fall-through
      case '{':
      case '}':
      case '[':
      case ']':
      case ':':
      case ',':
      case ' ':
      case '\t':
      case '\f':
      case '\r':
      case '\n':
        return false;
      default:
        return true;
    }
  }

  /**
   * Returns the next token, a {@link JsonToken#NAME property name}, and consumes it.
   *
   * @throws IllegalStateException if the next token is not a property name.
   */
  public String nextName() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    String result;
    if (p == PEEKED_UNQUOTED_NAME) {
      result = nextUnquotedValue();
    } else if (p == PEEKED_SINGLE_QUOTED_NAME) {
      result = nextQuotedValue('\'');
    } else if (p == PEEKED_DOUBLE_QUOTED_NAME) {
      result = nextQuotedValue('"');
    } else {
      throw unexpectedTokenError("a name");
    }
    peeked = PEEKED_NONE;
    pathNames[stackSize - 1] = result;
    return result;
  }

  /**
   * Returns the {@link JsonToken#STRING string} value of the next token, consuming it. If the next
   * token is a number, this method will return its string form.
   *
   * @throws IllegalStateException if the next token is not a string.
   */
  public String nextString() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    String result;
    if (p == PEEKED_UNQUOTED) {
      result = nextUnquotedValue();
    } else if (p == PEEKED_SINGLE_QUOTED) {
      result = nextQuotedValue('\'');
    } else if (p == PEEKED_DOUBLE_QUOTED) {
      result = nextQuotedValue('"');
    } else if (p == PEEKED_BUFFERED) {
      result = peekedString;
      peekedString = null;
    } else if (p == PEEKED_LONG) {
      result = Long.toString(peekedLong);
    } else if (p == PEEKED_NUMBER) {
      result = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else {
      throw unexpectedTokenError("a string");
    }
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Returns the {@link JsonToken#BOOLEAN boolean} value of the next token, consuming it.
   *
   * @throws IllegalStateException if the next token is not a boolean.
   */
  public boolean nextBoolean() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_TRUE) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return true;
    } else if (p == PEEKED_FALSE) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return false;
    }
    throw unexpectedTokenError("a boolean");
  }

  /**
   * Consumes the next token from the JSON stream and asserts that it is a literal null.
   *
   * @throws IllegalStateException if the next token is not a JSON null.
   */
  public void nextNull() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }
    if (p == PEEKED_NULL) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
    } else {
      throw unexpectedTokenError("null");
    }
  }

  /**
   * Returns the {@link JsonToken#NUMBER double} value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as a double using {@link
   * Double#parseDouble(String)}.
   *
   * @throws IllegalStateException if the next token is neither a number nor a string.
   * @throws NumberFormatException if the next literal value cannot be parsed as a double.
   * @throws MalformedJsonException if the next literal value is NaN or Infinity and this reader is
   *     not {@link #setStrictness(Strictness) lenient}.
   */
  public double nextDouble() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }

    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return (double) peekedLong;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED) {
      peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
    } else if (p == PEEKED_UNQUOTED) {
      peekedString = nextUnquotedValue();
    } else if (p != PEEKED_BUFFERED) {
      throw unexpectedTokenError("a double");
    }

    peeked = PEEKED_BUFFERED;
    double result = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
    if (strictness != Strictness.LENIENT && (Double.isNaN(result) || Double.isInfinite(result))) {
      throw syntaxError("JSON forbids NaN and infinities: " + result);
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Returns the {@link JsonToken#NUMBER long} value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as a long. If the next token's numeric
   * value cannot be exactly represented by a Java {@code long}, this method throws.
   *
   * @throws IllegalStateException if the next token is neither a number nor a string.
   * @throws NumberFormatException if the next literal value cannot be parsed as a number, or
   *     exactly represented as a long.
   */
  public long nextLong() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }

    if (p == PEEKED_LONG) {
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return peekedLong;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
      if (p == PEEKED_UNQUOTED) {
        peekedString = nextUnquotedValue();
      } else {
        peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
      }
      try {
        long result = Long.parseLong(peekedString);
        peeked = PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
      } catch (NumberFormatException ignored) {
        // Fall back to parse as a double below.
      }
    } else {
      throw unexpectedTokenError("a long");
    }

    peeked = PEEKED_BUFFERED;
    double asDouble = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
    long result = (long) asDouble;
    if (result != asDouble) { // Make sure no precision was lost casting to 'long'.
      throw new NumberFormatException("Expected a long but was " + peekedString + locationString());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Returns the string up to but not including {@code quote}, unescaping any character escape
   * sequences encountered along the way. The opening quote should have already been read. This
   * consumes the closing quote, but does not include it in the returned string.
   *
   * @param quote either ' or ".
   */
  private String nextQuotedValue(char quote) throws IOException {
    // Like nextNonWhitespace, this uses locals 'p' and 'l' to save inner-loop field access.
    char[] buffer = this.buffer;
    StringBuilder builder = null;
    while (true) {
      int p = pos;
      int l = limit;
      /* the index of the first character not yet appended to the builder. */
      int start = p;
      while (p < l) {
        int c = buffer[p++];

        // In strict mode, throw an exception when meeting unescaped control characters (U+0000
        // through U+001F)
        if (strictness == Strictness.STRICT && c < 0x20) {
          throw syntaxError(
              "Unescaped control characters (\\u0000-\\u001F) are not allowed in strict mode");
        } else if (c == quote) {
          pos = p;
          int len = p - start - 1;
          if (builder == null) {
            return new String(buffer, start, len);
          } else {
            builder.append(buffer, start, len);
            return builder.toString();
          }
        } else if (c == '\\') {
          pos = p;
          int len = p - start - 1;
          if (builder == null) {
            int estimatedLength = (len + 1) * 2;
            builder = new StringBuilder(Math.max(estimatedLength, 16));
          }
          builder.append(buffer, start, len);
          builder.append(readEscapeCharacter());
          p = pos;
          l = limit;
          start = p;
        } else if (c == '\n') {
          lineNumber++;
          lineStart = p;
        }
      }

      if (builder == null) {
        int estimatedLength = (p - start) * 2;
        builder = new StringBuilder(Math.max(estimatedLength, 16));
      }
      builder.append(buffer, start, p - start);
      pos = p;
      if (!fillBuffer(1)) {
        throw syntaxError("Unterminated string");
      }
    }
  }

  /** Returns an unquoted value as a string. */
  @SuppressWarnings("fallthrough")
  private String nextUnquotedValue() throws IOException {
    StringBuilder builder = null;
    int i = 0;

    findNonLiteralCharacter:
    while (true) {
      for (; pos + i < limit; i++) {
        switch (buffer[pos + i]) {
          case '"':
          case '/':
          case '\\':
          case ';':
          case '#':
          case '=':
            checkLenient(); // fall-through
          case '{':
          case '}':
          case '[':
          case ']':
          case ':':
          case ',':
          case ' ':
          case '\t':
          case '\f':
          case '\r':
          case '\n':
            break findNonLiteralCharacter;
          default:
            // skip character to be included in string value
        }
      }

      // Attempt to load the entire literal into the buffer at once.
      if (i < buffer.length) {
        if (fillBuffer(i + 1)) {
          continue;
        } else {
          break;
        }
      }

      // use a StringBuilder when the value is too long. This is too long to be a number!
      if (builder == null) {
        builder = new StringBuilder(Math.max(i, 16));
      }
      builder.append(buffer, pos, i);
      pos += i;
      i = 0;
      if (!fillBuffer(1)) {
        break;
      }
    }

    String result =
        (builder == null) ? new String(buffer, pos, i) : builder.append(buffer, pos, i).toString();
    pos += i;
    return result;
  }

  private void skipQuotedValue(char quote) throws IOException {
    // Like nextNonWhitespace, this uses locals 'p' and 'l' to save inner-loop field access.
    char[] buffer = this.buffer;
    do {
      int p = pos;
      int l = limit;
      /* the index of the first character not yet appended to the builder. */
      while (p < l) {
        int c = buffer[p++];
        if (c == quote) {
          pos = p;
          return;
        } else if (c == '\\') {
          pos = p;
          char unused = readEscapeCharacter();
          p = pos;
          l = limit;
        } else if (c == '\n') {
          lineNumber++;
          lineStart = p;
        }
      }
      pos = p;
    } while (fillBuffer(1));
    throw syntaxError("Unterminated string");
  }

  @SuppressWarnings("fallthrough")
  private void skipUnquotedValue() throws IOException {
    do {
      int i = 0;
      for (; pos + i < limit; i++) {
        switch (buffer[pos + i]) {
          case '/':
          case '\\':
          case ';':
          case '#':
          case '=':
            checkLenient(); // fall-through
          case '{':
          case '}':
          case '[':
          case ']':
          case ':':
          case ',':
          case ' ':
          case '\t':
          case '\f':
          case '\r':
          case '\n':
            pos += i;
            return;
          default:
            // skip the character
        }
      }
      pos += i;
    } while (fillBuffer(1));
  }

  /**
   * Returns the {@link JsonToken#NUMBER int} value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as an int. If the next token's numeric
   * value cannot be exactly represented by a Java {@code int}, this method throws.
   *
   * @throws IllegalStateException if the next token is neither a number nor a string.
   * @throws NumberFormatException if the next literal value cannot be parsed as a number, or
   *     exactly represented as an int.
   */
  public int nextInt() throws IOException {
    int p = peeked;
    if (p == PEEKED_NONE) {
      p = doPeek();
    }

    int result;
    if (p == PEEKED_LONG) {
      result = (int) peekedLong;
      if (peekedLong != result) { // Make sure no precision was lost casting to 'int'.
        throw new NumberFormatException("Expected an int but was " + peekedLong + locationString());
      }
      peeked = PEEKED_NONE;
      pathIndices[stackSize - 1]++;
      return result;
    }

    if (p == PEEKED_NUMBER) {
      peekedString = new String(buffer, pos, peekedNumberLength);
      pos += peekedNumberLength;
    } else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED || p == PEEKED_UNQUOTED) {
      if (p == PEEKED_UNQUOTED) {
        peekedString = nextUnquotedValue();
      } else {
        peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\'' : '"');
      }
      try {
        result = Integer.parseInt(peekedString);
        peeked = PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
      } catch (NumberFormatException ignored) {
        // Fall back to parse as a double below.
      }
    } else {
      throw unexpectedTokenError("an int");
    }

    peeked = PEEKED_BUFFERED;
    double asDouble = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
    result = (int) asDouble;
    if (result != asDouble) { // Make sure no precision was lost casting to 'int'.
      throw new NumberFormatException("Expected an int but was " + peekedString + locationString());
    }
    peekedString = null;
    peeked = PEEKED_NONE;
    pathIndices[stackSize - 1]++;
    return result;
  }

  /**
   * Closes this JSON reader and the underlying {@link Reader}.
   *
   * <p>Using the JSON reader after it has been closed will throw an {@link IllegalStateException}
   * in most cases.
   */
  @Override
  public void close() throws IOException {
    peeked = PEEKED_NONE;
    stack[0] = JsonScope.CLOSED;
    stackSize = 1;
    in.close();
  }

  /**
   * Skips the next value recursively. This method is intended for use when the JSON token stream
   * contains unrecognized or unhandled values.
   *
   * <p>The behavior depends on the type of the next JSON token:
   *
   * <ul>
   *   <li>Start of a JSON array or object: It and all of its nested values are skipped.
   *   <li>Primitive value (for example a JSON number): The primitive value is skipped.
   *   <li>Property name: Only the name but not the value of the property is skipped. {@code
   *       skipValue()} has to be called again to skip the property value as well.
   *   <li>End of a JSON array or object: Only this end token is skipped.
   *   <li>End of JSON document: Skipping has no effect, the next token continues to be the end of
   *       the document.
   * </ul>
   */
  public void skipValue() throws IOException {
    int count = 0;
    do {
      int p = peeked;
      if (p == PEEKED_NONE) {
        p = doPeek();
      }

      switch (p) {
        case PEEKED_BEGIN_ARRAY:
          push(JsonScope.EMPTY_ARRAY);
          count++;
          break;
        case PEEKED_BEGIN_OBJECT:
          push(JsonScope.EMPTY_OBJECT);
          count++;
          break;
        case PEEKED_END_ARRAY:
          stackSize--;
          count--;
          break;
        case PEEKED_END_OBJECT:
          // Only update when object end is explicitly skipped, otherwise stack is not updated
          // anyways
          if (count == 0) {
            // Free the last path name so that it can be garbage collected
            pathNames[stackSize - 1] = null;
          }
          stackSize--;
          count--;
          break;
        case PEEKED_UNQUOTED:
          skipUnquotedValue();
          break;
        case PEEKED_SINGLE_QUOTED:
          skipQuotedValue('\'');
          break;
        case PEEKED_DOUBLE_QUOTED:
          skipQuotedValue('"');
          break;
        case PEEKED_UNQUOTED_NAME:
          skipUnquotedValue();
          // Only update when name is explicitly skipped, otherwise stack is not updated anyways
          if (count == 0) {
            pathNames[stackSize - 1] = "<skipped>";
          }
          break;
        case PEEKED_SINGLE_QUOTED_NAME:
          skipQuotedValue('\'');
          // Only update when name is explicitly skipped, otherwise stack is not updated anyways
          if (count == 0) {
            pathNames[stackSize - 1] = "<skipped>";
          }
          break;
        case PEEKED_DOUBLE_QUOTED_NAME:
          skipQuotedValue('"');
          // Only update when name is explicitly skipped, otherwise stack is not updated anyways
          if (count == 0) {
            pathNames[stackSize - 1] = "<skipped>";
          }
          break;
        case PEEKED_NUMBER:
          pos += peekedNumberLength;
          break;
        case PEEKED_EOF:
          // Do nothing
          return;
        default:
          // For all other tokens there is nothing to do; token has already been consumed from
          // underlying reader
      }
      peeked = PEEKED_NONE;
    } while (count > 0);

    pathIndices[stackSize - 1]++;
  }

  private void push(int newTop) throws MalformedJsonException {
    // - 1 because stack contains as first element either EMPTY_DOCUMENT or NONEMPTY_DOCUMENT
    if (stackSize - 1 >= nestingLimit) {
      throw new MalformedJsonException(
          "Nesting limit " + nestingLimit + " reached" + locationString());
    }

    if (stackSize == stack.length) {
      int newLength = stackSize * 2;
      stack = Arrays.copyOf(stack, newLength);
      pathIndices = Arrays.copyOf(pathIndices, newLength);
      pathNames = Arrays.copyOf(pathNames, newLength);
    }
    stack[stackSize++] = newTop;
  }

  /**
   * Returns true once {@code limit - pos >= minimum}. If the data is exhausted before that many
   * characters are available, this returns false.
   */
  private boolean fillBuffer(int minimum) throws IOException {
    char[] buffer = this.buffer;
    lineStart -= pos;
    if (limit != pos) {
      limit -= pos;
      System.arraycopy(buffer, pos, buffer, 0, limit);
    } else {
      limit = 0;
    }

    pos = 0;
    int total;
    while ((total = in.read(buffer, limit, buffer.length - limit)) != -1) {
      limit += total;

      // if this is the first read, consume an optional byte order mark (BOM) if it exists
      if (lineNumber == 0 && lineStart == 0 && limit > 0 && buffer[0] == '\ufeff') {
        pos++;
        lineStart++;
        minimum++;
      }

      if (limit >= minimum) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the next character in the stream that is neither whitespace nor a part of a comment.
   * When this returns, the returned character is always at {@code buffer[pos-1]}; this means the
   * caller can always push back the returned character by decrementing {@code pos}.
   */
  private int nextNonWhitespace(boolean throwOnEof) throws IOException {
    /*
     * This code uses ugly local variables 'p' and 'l' representing the 'pos'
     * and 'limit' fields respectively. Using locals rather than fields saves
     * a few field reads for each whitespace character in a pretty-printed
     * document, resulting in a 5% speedup. We need to flush 'p' to its field
     * before any (potentially indirect) call to fillBuffer() and reread both
     * 'p' and 'l' after any (potentially indirect) call to the same method.
     */
    char[] buffer = this.buffer;
    int p = pos;
    int l = limit;
    while (true) {
      if (p == l) {
        pos = p;
        if (!fillBuffer(1)) {
          break;
        }
        p = pos;
        l = limit;
      }

      int c = buffer[p++];
      if (c == '\n') {
        lineNumber++;
        lineStart = p;
        continue;
      } else if (c == ' ' || c == '\r' || c == '\t') {
        continue;
      }

      if (c == '/') {
        pos = p;
        if (p == l) {
          pos--; // push back '/' so it's still in the buffer when this method returns
          boolean charsLoaded = fillBuffer(2);
          pos++; // consume the '/' again
          if (!charsLoaded) {
            return c;
          }
        }

        checkLenient();
        char peek = buffer[pos];
        switch (peek) {
          case '*':
            // skip a /* c-style comment */
            pos++;
            if (!skipTo("*/")) {
              throw syntaxError("Unterminated comment");
            }
            p = pos + 2;
            l = limit;
            continue;

          case '/':
            // skip a // end-of-line comment
            pos++;
            skipToEndOfLine();
            p = pos;
            l = limit;
            continue;

          default:
            return c;
        }
      } else if (c == '#') {
        pos = p;
        /*
         * Skip a # hash end-of-line comment. The JSON RFC doesn't
         * specify this behaviour, but it's required to parse
         * existing documents. See http://b/2571423.
         */
        checkLenient();
        skipToEndOfLine();
        p = pos;
        l = limit;
      } else {
        pos = p;
        return c;
      }
    }
    if (throwOnEof) {
      throw new EOFException("End of input" + locationString());
    } else {
      return -1;
    }
  }

  private void checkLenient() throws MalformedJsonException {
    if (strictness != Strictness.LENIENT) {
      throw syntaxError(
          "Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON");
    }
  }

  /**
   * Advances the position until after the next newline character. If the line is terminated by
   * "\r\n", the '\n' must be consumed as whitespace by the caller.
   */
  private void skipToEndOfLine() throws IOException {
    while (pos < limit || fillBuffer(1)) {
      char c = buffer[pos++];
      if (c == '\n') {
        lineNumber++;
        lineStart = pos;
        break;
      } else if (c == '\r') {
        break;
      }
    }
  }

  /**
   * @param toFind a string to search for. Must not contain a newline.
   */
  private boolean skipTo(String toFind) throws IOException {
    int length = toFind.length();
    outer:
    for (; pos + length <= limit || fillBuffer(length); pos++) {
      if (buffer[pos] == '\n') {
        lineNumber++;
        lineStart = pos + 1;
        continue;
      }
      for (int c = 0; c < length; c++) {
        if (buffer[pos + c] != toFind.charAt(c)) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + locationString();
  }

  String locationString() {
    int line = lineNumber + 1;
    int column = pos - lineStart + 1;
    return " at line " + line + " column " + column + " path " + getPath();
  }

  private String getPath(boolean usePreviousPath) {
    StringBuilder result = new StringBuilder().append('$');
    for (int i = 0; i < stackSize; i++) {
      int scope = stack[i];
      switch (scope) {
        case JsonScope.EMPTY_ARRAY:
        case JsonScope.NONEMPTY_ARRAY:
          int pathIndex = pathIndices[i];
          // If index is last path element it points to next array element; have to decrement
          if (usePreviousPath && pathIndex > 0 && i == stackSize - 1) {
            pathIndex--;
          }
          result.append('[').append(pathIndex).append(']');
          break;
        case JsonScope.EMPTY_OBJECT:
        case JsonScope.DANGLING_NAME:
        case JsonScope.NONEMPTY_OBJECT:
          result.append('.');
          if (pathNames[i] != null) {
            result.append(pathNames[i]);
          }
          break;
        case JsonScope.NONEMPTY_DOCUMENT:
        case JsonScope.EMPTY_DOCUMENT:
        case JsonScope.CLOSED:
          break;
        default:
          throw new AssertionError("Unknown scope value: " + scope);
      }
    }
    return result.toString();
  }

  /**
   * Returns a <a href="https://goessner.net/articles/JsonPath/">JSONPath</a> in <i>dot-notation</i>
   * to the next (or current) location in the JSON document. That means:
   *
   * <ul>
   *   <li>For JSON arrays the path points to the index of the next element (even if there are no
   *       further elements).
   *   <li>For JSON objects the path points to the last property, or to the current property if its
   *       name has already been consumed.
   * </ul>
   *
   * <p>This method can be useful to add additional context to exception messages <i>before</i> a
   * value is consumed, for example when the {@linkplain #peek() peeked} token is unexpected.
   */
  public String getPath() {
    return getPath(false);
  }

  /**
   * Returns a <a href="https://goessner.net/articles/JsonPath/">JSONPath</a> in <i>dot-notation</i>
   * to the previous (or current) location in the JSON document. That means:
   *
   * <ul>
   *   <li>For JSON arrays the path points to the index of the previous element.<br>
   *       If no element has been consumed yet it uses the index 0 (even if there are no elements).
   *   <li>For JSON objects the path points to the last property, or to the current property if its
   *       name has already been consumed.
   * </ul>
   *
   * <p>This method can be useful to add additional context to exception messages <i>after</i> a
   * value has been consumed.
   */
  public String getPreviousPath() {
    return getPath(true);
  }

  /**
   * Unescapes the character identified by the character or characters that immediately follow a
   * backslash. The backslash '\' should have already been read. This supports both Unicode escapes
   * "u000A" and two-character escapes "\n".
   *
   * @throws MalformedJsonException if the escape sequence is malformed
   */
  @SuppressWarnings("fallthrough")
  private char readEscapeCharacter() throws IOException {
    if (pos == limit && !fillBuffer(1)) {
      throw syntaxError("Unterminated escape sequence");
    }

    char escaped = buffer[pos++];
    switch (escaped) {
      case 'u':
        if (pos + 4 > limit && !fillBuffer(4)) {
          throw syntaxError("Unterminated escape sequence");
        }
        // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
        int result = 0;
        for (int i = pos, end = i + 4; i < end; i++) {
          char c = buffer[i];
          result <<= 4;
          if (c >= '0' && c <= '9') {
            result += (c - '0');
          } else if (c >= 'a' && c <= 'f') {
            result += (c - 'a' + 10);
          } else if (c >= 'A' && c <= 'F') {
            result += (c - 'A' + 10);
          } else {
            throw syntaxError("Malformed Unicode escape \\u" + new String(buffer, pos, 4));
          }
        }
        pos += 4;
        return (char) result;

      case 't':
        return '\t';

      case 'b':
        return '\b';

      case 'n':
        return '\n';

      case 'r':
        return '\r';

      case 'f':
        return '\f';

      case '\n':
        if (strictness == Strictness.STRICT) {
          throw syntaxError("Cannot escape a newline character in strict mode");
        }
        lineNumber++;
        lineStart = pos;
      // fall-through

      case '\'':
        if (strictness == Strictness.STRICT) {
          throw syntaxError("Invalid escaped character \"'\" in strict mode");
        }
      case '"':
      case '\\':
      case '/':
        return escaped;
      default:
        // throw error when none of the above cases are matched
        throw syntaxError("Invalid escape sequence");
    }
  }

  /**
   * Throws a new {@link MalformedJsonException} with the given message and information about the
   * current location.
   */
  private MalformedJsonException syntaxError(String message) throws MalformedJsonException {
    throw new MalformedJsonException(
        message + locationString() + "\nSee " + TroubleshootingGuide.createUrl("malformed-json"));
  }

  private IllegalStateException unexpectedTokenError(String expected) throws IOException {
    JsonToken peeked = peek();
    String troubleshootingId =
        peeked == JsonToken.NULL ? "adapter-not-null-safe" : "unexpected-json-structure";
    return new IllegalStateException(
        "Expected "
            + expected
            + " but was "
            + peek()
            + locationString()
            + "\nSee "
            + TroubleshootingGuide.createUrl(troubleshootingId));
  }

  /** Consumes the non-execute prefix if it exists. */
  private void consumeNonExecutePrefix() throws IOException {
    // fast-forward through the leading whitespace
    int unused = nextNonWhitespace(true);
    pos--;

    if (pos + 5 > limit && !fillBuffer(5)) {
      return;
    }

    int p = pos;
    char[] buf = buffer;
    if (buf[p] != ')'
        || buf[p + 1] != ']'
        || buf[p + 2] != '}'
        || buf[p + 3] != '\''
        || buf[p + 4] != '\n') {
      return; // not a security token!
    }

    // we consumed a security token!
    pos += 5;
  }

  static {
    JsonReaderInternalAccess.INSTANCE =
        new JsonReaderInternalAccess() {
          @Override
          public void promoteNameToValue(JsonReader reader) throws IOException {
            if (reader instanceof JsonTreeReader) {
              ((JsonTreeReader) reader).promoteNameToValue();
              return;
            }
            int p = reader.peeked;
            if (p == PEEKED_NONE) {
              p = reader.doPeek();
            }
            if (p == PEEKED_DOUBLE_QUOTED_NAME) {
              reader.peeked = PEEKED_DOUBLE_QUOTED;
            } else if (p == PEEKED_SINGLE_QUOTED_NAME) {
              reader.peeked = PEEKED_SINGLE_QUOTED;
            } else if (p == PEEKED_UNQUOTED_NAME) {
              reader.peeked = PEEKED_UNQUOTED;
            } else {
              throw reader.unexpectedTokenError("a name");
            }
          }
        };
  }
}
