/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.internal.NumberLimits;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * A class representing a JSON primitive value. A primitive value is either a String, a Java
 * primitive, or a Java primitive wrapper type.
 *
 * <p>See the {@link JsonElement} documentation for details on how to convert {@code JsonPrimitive}
 * and generally any {@code JsonElement} from and to JSON.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class JsonPrimitive extends JsonElement {

  private final Object value;

  /**
   * Create a primitive containing a boolean value.
   *
   * @param bool the value to create the primitive with.
   */
  // "deprecation" suppression for superclass constructor
  // "UnnecessaryBoxedVariable" Error Prone warning is correct since method does not accept
  // null, but cannot be changed anymore since this is public API
  @SuppressWarnings({"deprecation", "UnnecessaryBoxedVariable"})
  public JsonPrimitive(Boolean bool) {
    value = Objects.requireNonNull(bool);
  }

  /**
   * Create a primitive containing a {@link Number}.
   *
   * @param number the value to create the primitive with.
   */
  @SuppressWarnings("deprecation") // superclass constructor
  public JsonPrimitive(Number number) {
    value = Objects.requireNonNull(number);
  }

  /**
   * Create a primitive containing a String value.
   *
   * @param string the value to create the primitive with.
   */
  @SuppressWarnings("deprecation") // superclass constructor
  public JsonPrimitive(String string) {
    value = Objects.requireNonNull(string);
  }

  /**
   * Create a primitive containing a character. The character is turned into a one character String
   * since JSON only supports String.
   *
   * @param c the value to create the primitive with.
   */
  // "deprecation" suppression for superclass constructor
  // "UnnecessaryBoxedVariable" Error Prone warning is correct since method does not accept
  // null, but cannot be changed anymore since this is public API
  @SuppressWarnings({"deprecation", "UnnecessaryBoxedVariable"})
  public JsonPrimitive(Character c) {
    // convert characters to strings since in JSON, characters are represented as a single
    // character string
    value = Objects.requireNonNull(c).toString();
  }

  /**
   * Returns the same value as primitives are immutable.
   *
   * @since 2.8.2
   */
  @Override
  public JsonPrimitive deepCopy() {
    return this;
  }

  /**
   * Check whether this primitive contains a boolean value.
   *
   * @return true if this primitive contains a boolean value, false otherwise.
   */
  public boolean isBoolean() {
    return value instanceof Boolean;
  }

  /**
   * Convenience method to get this element as a boolean value. If this primitive {@linkplain
   * #isBoolean() is not a boolean}, the string value is parsed using {@link
   * Boolean#parseBoolean(String)}. This means {@code "true"} (ignoring case) is considered {@code
   * true} and any other value is considered {@code false}.
   */
  @Override
  public boolean getAsBoolean() {
    if (isBoolean()) {
      return (Boolean) value;
    }
    // Check to see if the value as a String is "true" in any case.
    return Boolean.parseBoolean(getAsString());
  }

  /**
   * Check whether this primitive contains a Number.
   *
   * @return true if this primitive contains a Number, false otherwise.
   */
  public boolean isNumber() {
    return value instanceof Number;
  }

  /**
   * Convenience method to get this element as a {@link Number}. If this primitive {@linkplain
   * #isString() is a string}, a lazily parsed {@code Number} is constructed which parses the string
   * when any of its methods are called (which can lead to a {@link NumberFormatException}).
   *
   * @throws UnsupportedOperationException if this primitive is neither a number nor a string.
   */
  @Override
  public Number getAsNumber() {
    if (value instanceof Number) {
      return (Number) value;
    } else if (value instanceof String) {
      return new LazilyParsedNumber((String) value);
    }
    throw new UnsupportedOperationException("Primitive is neither a number nor a string");
  }

  /**
   * Check whether this primitive contains a String value.
   *
   * @return true if this primitive contains a String value, false otherwise.
   */
  public boolean isString() {
    return value instanceof String;
  }

  // Don't add Javadoc, inherit it from super implementation; no exceptions are thrown here
  @Override
  public String getAsString() {
    if (value instanceof String) {
      return (String) value;
    } else if (isNumber()) {
      return getAsNumber().toString();
    } else if (isBoolean()) {
      return ((Boolean) value).toString();
    }
    throw new AssertionError("Unexpected value type: " + value.getClass());
  }

  /**
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public double getAsDouble() {
    return isNumber() ? getAsNumber().doubleValue() : Double.parseDouble(getAsString());
  }

  /**
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public BigDecimal getAsBigDecimal() {
    return value instanceof BigDecimal
        ? (BigDecimal) value
        : NumberLimits.parseBigDecimal(getAsString());
  }

  /**
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public BigInteger getAsBigInteger() {
    return value instanceof BigInteger
        ? (BigInteger) value
        : isIntegral(this)
            ? BigInteger.valueOf(this.getAsNumber().longValue())
            : NumberLimits.parseBigInteger(this.getAsString());
  }

  /**
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public float getAsFloat() {
    return isNumber() ? getAsNumber().floatValue() : Float.parseFloat(getAsString());
  }

  /**
   * Convenience method to get this element as a primitive long.
   *
   * @return this element as a primitive long.
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public long getAsLong() {
    return isNumber() ? getAsNumber().longValue() : Long.parseLong(getAsString());
  }

  /**
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public short getAsShort() {
    return isNumber() ? getAsNumber().shortValue() : Short.parseShort(getAsString());
  }

  /**
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public int getAsInt() {
    return isNumber() ? getAsNumber().intValue() : Integer.parseInt(getAsString());
  }

  /**
   * @throws NumberFormatException {@inheritDoc}
   */
  @Override
  public byte getAsByte() {
    return isNumber() ? getAsNumber().byteValue() : Byte.parseByte(getAsString());
  }

  /**
   * @throws UnsupportedOperationException if the string value of this primitive is empty.
   * @deprecated This method is misleading, as it does not get this element as a char but rather as
   *     a string's first character.
   */
  @Deprecated
  @Override
  public char getAsCharacter() {
    String s = getAsString();
    if (s.isEmpty()) {
      throw new UnsupportedOperationException("String value is empty");
    } else {
      return s.charAt(0);
    }
  }

  /** Returns the hash code of this object. */
  @Override
  public int hashCode() {
    if (value == null) {
      return 31;
    }
    // Using recommended hashing algorithm from Effective Java for longs and doubles
    if (isIntegral(this)) {
      long value = getAsNumber().longValue();
      return (int) (value ^ (value >>> 32));
    }
    if (value instanceof Number) {
      long value = Double.doubleToLongBits(getAsNumber().doubleValue());
      return (int) (value ^ (value >>> 32));
    }
    return value.hashCode();
  }

  /**
   * Returns whether the other object is equal to this. This method only considers the other object
   * to be equal if it is an instance of {@code JsonPrimitive} and has an equal value.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    JsonPrimitive other = (JsonPrimitive) obj;
    if (value == null) {
      return other.value == null;
    }
    if (isBigNumber(this) || isBigNumber(other)) {
      // As a BigDecimal is essentially a wrapper around a BigInteger that
      // "remembers where the decimal point is", casting BigInteger to BigDecimal
      // makes more sense, avoiding max long/double values comparison.
      return getAsBigDecimal().equals(other.getAsBigDecimal());
    }
    if (isIntegral(this) && isIntegral(other)) {
      return (this.value instanceof BigInteger || other.value instanceof BigInteger)
          ? this.getAsBigInteger().equals(other.getAsBigInteger())
          : this.getAsNumber().longValue() == other.getAsNumber().longValue();
    }
    if (value instanceof Number && other.value instanceof Number) {
      if (value instanceof BigDecimal && other.value instanceof BigDecimal) {
        // Uses compareTo to ignore scale of values, e.g. `0` and `0.00` should be considered equal
        return this.getAsBigDecimal().compareTo(other.getAsBigDecimal()) == 0;
      }

      double thisAsDouble = this.getAsDouble();
      double otherAsDouble = other.getAsDouble();
      // Don't use Double.compare(double, double) because that considers -0.0 and +0.0 not equal
      return (thisAsDouble == otherAsDouble)
          || (Double.isNaN(thisAsDouble) && Double.isNaN(otherAsDouble));
    }
    return value.equals(other.value);
  }

  /**
   * Returns true if the specified number is an integral type (Long, Integer, Short, Byte,
   * BigInteger)
   */
  private static boolean isIntegral(JsonPrimitive primitive) {
    if (primitive.value instanceof Number) {
      Number number = (Number) primitive.value;
      return number instanceof BigInteger
          || number instanceof Long
          || number instanceof Integer
          || number instanceof Short
          || number instanceof Byte;
    }
    return false;
  }

  private static boolean isBigInteger(JsonPrimitive primitive) {
    return primitive.value instanceof BigDecimal;
  }

  private static boolean isBigDecimal(JsonPrimitive primitive) {
    return primitive.value instanceof BigDecimal;
  }

  private static boolean isBigNumber(JsonPrimitive primitive) {
    return isBigDecimal(primitive) || isBigInteger(primitive);
  }
}
