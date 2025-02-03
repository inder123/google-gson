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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.DefaultDateTypeAdapter.DateType;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;

/**
 * A simple unit test for the {@link DefaultDateTypeAdapter} class.
 *
 * @author Joel Leitch
 */
@SuppressWarnings("JavaUtilDate")
public class DefaultDateTypeAdapterTest {

  @Test
  public void testFormattingInEnUs() {
    assertFormattingAlwaysEmitsUsLocale(Locale.US);
  }

  @Test
  public void testFormattingInFr() {
    assertFormattingAlwaysEmitsUsLocale(Locale.FRANCE);
  }

  private static void assertFormattingAlwaysEmitsUsLocale(Locale locale) {
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault(locale);
    try {
      // The patterns here attempt to accommodate minor date-time formatting differences between JDK
      // versions. Ideally Gson would serialize in a way that is independent of the JDK version.
      // Note: \h means "horizontal space", because some JDK versions use Narrow No Break Space
      // (U+202F) before the AM or PM indication.
      String utcFull = "(Coordinated Universal Time|UTC)";
      assertFormatted("Jan 1, 1970,? 12:00:00\\hAM", DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
      assertFormatted(
          "1/1/70,? 12:00\\hAM",
          DateType.DATE.createAdapterFactory(DateFormat.SHORT, DateFormat.SHORT));
      assertFormatted(
          "Jan 1, 1970,? 12:00:00\\hAM",
          DateType.DATE.createAdapterFactory(DateFormat.MEDIUM, DateFormat.MEDIUM));
      assertFormatted(
          "January 1, 1970(,| at)? 12:00:00\\hAM UTC",
          DateType.DATE.createAdapterFactory(DateFormat.LONG, DateFormat.LONG));
      assertFormatted(
          "Thursday, January 1, 1970(,| at)? 12:00:00\\hAM " + utcFull,
          DateType.DATE.createAdapterFactory(DateFormat.FULL, DateFormat.FULL));
    } finally {
      TimeZone.setDefault(defaultTimeZone);
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void testParsingDatesFormattedWithSystemLocale() throws Exception {
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.FRANCE);
    try {
      Date date = new Date(0);
      assertParsed(
          DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(date),
          DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
      assertParsed(
          DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date),
          DateType.DATE.createAdapterFactory(DateFormat.SHORT, DateFormat.SHORT));
      assertParsed(
          DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(date),
          DateType.DATE.createAdapterFactory(DateFormat.MEDIUM, DateFormat.MEDIUM));
      assertParsed(
          DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(date),
          DateType.DATE.createAdapterFactory(DateFormat.LONG, DateFormat.LONG));
      assertParsed(
          DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date),
          DateType.DATE.createAdapterFactory(DateFormat.FULL, DateFormat.FULL));
    } finally {
      TimeZone.setDefault(defaultTimeZone);
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void testParsingDatesFormattedWithUsLocale() throws Exception {
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
    try {
      assertParsed("Jan 1, 1970 0:00:00 AM", DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
      assertParsed("Jan 1, 1970", DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
      assertParsed(
          "1/1/70 0:00 AM", DateType.DATE.createAdapterFactory(DateFormat.SHORT, DateFormat.SHORT));
      assertParsed(
          "Jan 1, 1970 0:00:00 AM",
          DateType.DATE.createAdapterFactory(DateFormat.MEDIUM, DateFormat.MEDIUM));
      assertParsed(
          "January 1, 1970 0:00:00 AM UTC",
          DateType.DATE.createAdapterFactory(DateFormat.LONG, DateFormat.LONG));
      assertParsed(
          "Thursday, January 1, 1970 0:00:00 AM UTC",
          DateType.DATE.createAdapterFactory(DateFormat.FULL, DateFormat.FULL));
    } finally {
      TimeZone.setDefault(defaultTimeZone);
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void testFormatUsesDefaultTimezone() throws Exception {
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
    try {
      assertFormatted("Dec 31, 1969,? 4:00:00\\hPM", DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
      assertParsed("Dec 31, 1969 4:00:00 PM", DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
    } finally {
      TimeZone.setDefault(defaultTimeZone);
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void testDateDeserializationISO8601() throws Exception {
    TypeAdapterFactory adapterFactory = DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY;
    assertParsed("1970-01-01T00:00:00.000Z", adapterFactory);
    assertParsed("1970-01-01T00:00Z", adapterFactory);
    assertParsed("1970-01-01T00:00:00+00:00", adapterFactory);
    assertParsed("1970-01-01T01:00:00+01:00", adapterFactory);
    assertParsed("1970-01-01T01:00:00+01", adapterFactory);
  }

  @Test
  public void testDatePattern() {
    String pattern = "yyyy-MM-dd";
    TypeAdapter<Date> dateTypeAdapter = dateAdapter(DateType.DATE.createAdapterFactory(pattern));
    DateFormat formatter = new SimpleDateFormat(pattern);
    Date currentDate = new Date();

    String dateString = dateTypeAdapter.toJson(currentDate);
    assertThat(dateString).isEqualTo(toLiteral(formatter.format(currentDate)));
  }

  @Test
  public void testInvalidDatePattern() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DateType.DATE.createAdapterFactory("I am a bad Date pattern...."));
  }

  @Test
  public void testNullValue() throws Exception {
    TypeAdapter<Date> adapter = dateAdapter(DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
    assertThat(adapter.fromJson("null")).isNull();
    assertThat(adapter.toJson(null)).isEqualTo("null");
  }

  @Test
  public void testUnexpectedToken() throws Exception {
    TypeAdapter<Date> adapter = dateAdapter(DefaultDateTypeAdapter.DEFAULT_STYLE_FACTORY);
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> adapter.fromJson("{}"));
    assertThat(e).hasMessageThat().startsWith("Expected a string but was BEGIN_OBJECT");
  }

  @Test
  public void testGsonDateFormat() {
    TimeZone originalTimeZone = TimeZone.getDefault();
    // Set the default timezone to UTC
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    try {
      Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm z").create();
      Date originalDate = new Date(0);

      // Serialize the date object
      String json = gson.toJson(originalDate);
      assertThat(json).isEqualTo("\"1970-01-01 00:00 UTC\"");

      // Deserialize a date string with the PST timezone
      Date deserializedDate = gson.fromJson("\"1970-01-01 00:00 PST\"", Date.class);
      // Assert that the deserialized date's time is correct
      assertThat(deserializedDate.getTime()).isEqualTo(new Date(28800000).getTime());

      // Serialize the deserialized date object again
      String jsonAfterDeserialization = gson.toJson(deserializedDate);
      // The expectation is that the date, after deserialization, when serialized again should still
      // be in the UTC timezone
      assertThat(jsonAfterDeserialization).isEqualTo("\"1970-01-01 08:00 UTC\"");
    } finally {
      TimeZone.setDefault(originalTimeZone);
    }
  }

  private static TypeAdapter<Date> dateAdapter(TypeAdapterFactory adapterFactory) {
    TypeAdapter<Date> adapter = adapterFactory.create(new Gson(), TypeToken.get(Date.class));
    assertThat(adapter).isNotNull();
    return adapter;
  }

  private static void assertFormatted(String formattedPattern, TypeAdapterFactory adapterFactory) {
    TypeAdapter<Date> adapter = dateAdapter(adapterFactory);
    String json = adapter.toJson(new Date(0));
    assertThat(json).matches(toLiteral(formattedPattern));
  }

  @SuppressWarnings("UndefinedEquals")
  private static void assertParsed(String date, TypeAdapterFactory adapterFactory)
      throws IOException {
    TypeAdapter<Date> adapter = dateAdapter(adapterFactory);
    assertWithMessage(date).that(adapter.fromJson(toLiteral(date))).isEqualTo(new Date(0));
    assertWithMessage("ISO 8601")
        .that(adapter.fromJson(toLiteral("1970-01-01T00:00:00Z")))
        .isEqualTo(new Date(0));
  }

  private static String toLiteral(String s) {
    return '"' + s + '"';
  }
}
