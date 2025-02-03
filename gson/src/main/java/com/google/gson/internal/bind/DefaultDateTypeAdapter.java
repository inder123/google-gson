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

package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.JavaVersion;
import com.google.gson.internal.PreJava9DateFormatProvider;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * This type adapter supports subclasses of date by defining a {@link
 * DefaultDateTypeAdapter.DateType} and then using its {@code createAdapterFactory} methods.
 *
 * <p><b>Important:</b> Instances of this class (or rather the {@link SimpleDateFormat} they use)
 * capture the current default {@link Locale} and {@link TimeZone} when they are created. Therefore
 * avoid storing factories obtained from {@link DateType} in {@code static} fields, since they only
 * create a single adapter instance and its behavior would then depend on when Gson classes are
 * loaded first, and which default {@code Locale} and {@code TimeZone} was used at that point.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class DefaultDateTypeAdapter<T extends Date> extends TypeAdapter<T> {
  private static final String SIMPLE_NAME = "DefaultDateTypeAdapter";

  /** Factory for {@link Date} adapters which use {@link DateFormat#DEFAULT} as style. */
  public static final TypeAdapterFactory DEFAULT_STYLE_FACTORY =
      // Because SimpleDateFormat captures the default TimeZone when it was created, let the factory
      // always create new DefaultDateTypeAdapter instances (which are then cached by the Gson
      // instances) instead of having a single static DefaultDateTypeAdapter instance
      // Otherwise the behavior would depend on when an application first loads Gson classes and
      // which default TimeZone is set at that point, which would be quite brittle
      new TypeAdapterFactory() {
        @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
          return typeToken.getRawType() == Date.class
              ? (TypeAdapter<T>)
                  new DefaultDateTypeAdapter<>(
                      DateType.DATE, DateFormat.DEFAULT, DateFormat.DEFAULT)
              : null;
        }

        @Override
        public String toString() {
          return "DefaultDateTypeAdapter#DEFAULT_STYLE_FACTORY";
        }
      };

  public abstract static class DateType<T extends Date> {
    public static final DateType<Date> DATE =
        new DateType<Date>(Date.class) {
          @Override
          protected Date deserialize(Date date) {
            return date;
          }
        };

    private final Class<T> dateClass;

    protected DateType(Class<T> dateClass) {
      this.dateClass = dateClass;
    }

    protected abstract T deserialize(Date date);

    private TypeAdapterFactory createFactory(DefaultDateTypeAdapter<T> adapter) {
      return TypeAdapters.newFactory(dateClass, adapter);
    }

    public final TypeAdapterFactory createAdapterFactory(String datePattern) {
      return createFactory(new DefaultDateTypeAdapter<>(this, datePattern));
    }

    public final TypeAdapterFactory createAdapterFactory(int dateStyle, int timeStyle) {
      return createFactory(new DefaultDateTypeAdapter<>(this, dateStyle, timeStyle));
    }
  }

  private final DateType<T> dateType;

  /**
   * List of 1 or more different date formats used for de-serialization attempts. The first of them
   * is used for serialization as well.
   */
  private final List<DateFormat> dateFormats = new ArrayList<>();

  private DefaultDateTypeAdapter(DateType<T> dateType, String datePattern) {
    this.dateType = Objects.requireNonNull(dateType);
    dateFormats.add(new SimpleDateFormat(datePattern, Locale.US));
    if (!Locale.getDefault().equals(Locale.US)) {
      dateFormats.add(new SimpleDateFormat(datePattern));
    }
  }

  private DefaultDateTypeAdapter(DateType<T> dateType, int dateStyle, int timeStyle) {
    this.dateType = Objects.requireNonNull(dateType);
    dateFormats.add(DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.US));
    if (!Locale.getDefault().equals(Locale.US)) {
      dateFormats.add(DateFormat.getDateTimeInstance(dateStyle, timeStyle));
    }
    if (JavaVersion.isJava9OrLater()) {
      dateFormats.add(PreJava9DateFormatProvider.getUsDateTimeFormat(dateStyle, timeStyle));
    }
    // Include fall-back for "timeless" Date (could be from a java.sql.Date, for example)
    dateFormats.add(DateFormat.getDateInstance(dateStyle, Locale.US));
  }

  @Override
  public void write(JsonWriter out, Date value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }

    DateFormat dateFormat = dateFormats.get(0);
    String dateFormatAsString;
    // Needs to be synchronized since JDK DateFormat classes are not thread-safe
    synchronized (dateFormats) {
      dateFormatAsString = dateFormat.format(value);
    }
    out.value(dateFormatAsString);
  }

  @Override
  public T read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    Date date = deserializeToDate(in);
    return dateType.deserialize(date);
  }

  private Date deserializeToDate(JsonReader in) throws IOException {
    String s = in.nextString();
    // Needs to be synchronized since JDK DateFormat classes are not thread-safe
    synchronized (dateFormats) {
      for (DateFormat dateFormat : dateFormats) {
        TimeZone originalTimeZone = dateFormat.getTimeZone();
        try {
          return dateFormat.parse(s);
        } catch (ParseException ignored) {
          // OK: try the next format
        } finally {
          dateFormat.setTimeZone(originalTimeZone);
        }
      }
    }

    try {
      return ISO8601Utils.parse(s, new ParsePosition(0));
    } catch (ParseException e) {
      throw new JsonSyntaxException(
          "Failed parsing '" + s + "' as Date; at path " + in.getPreviousPath(), e);
    }
  }

  @Override
  public String toString() {
    DateFormat defaultFormat = dateFormats.get(0);
    if (defaultFormat instanceof SimpleDateFormat) {
      return SIMPLE_NAME + '(' + ((SimpleDateFormat) defaultFormat).toPattern() + ')';
    } else {
      return SIMPLE_NAME + '(' + defaultFormat.getClass().getSimpleName() + ')';
    }
  }
}
