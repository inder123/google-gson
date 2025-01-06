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
package com.google.gson.functional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.common.TestTypes.ClassWithSerializedNameFields;
import com.google.gson.common.TestTypes.StringWrapper;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;

/**
 * Functional tests for naming policies.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class NamingPolicyTest {
  private GsonBuilder builder;

  @Before
  public void setUp() throws Exception {
    builder = new GsonBuilder();
  }

  @Test
  public void testGsonWithNonDefaultFieldNamingPolicySerialization() {
    Gson gson = builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    StringWrapper target = new StringWrapper("blah");
    assertThat(gson.toJson(target))
        .isEqualTo(
            "{\"SomeConstantStringInstanceField\":\""
                + target.someConstantStringInstanceField
                + "\"}");
  }

  @Test
  public void testGsonWithNonDefaultFieldNamingPolicyDeserialiation() {
    Gson gson = builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    String target = "{\"SomeConstantStringInstanceField\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testGsonWithLowerCaseDashPolicySerialization() {
    Gson gson = builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    StringWrapper target = new StringWrapper("blah");
    assertThat(gson.toJson(target))
        .isEqualTo(
            "{\"some-constant-string-instance-field\":\""
                + target.someConstantStringInstanceField
                + "\"}");
  }

  @Test
  public void testGsonWithLowerCaseDotPolicySerialization() {
    Gson gson = builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DOTS).create();
    StringWrapper target = new StringWrapper("blah");
    assertThat(gson.toJson(target))
        .isEqualTo(
            "{\"some.constant.string.instance.field\":\""
                + target.someConstantStringInstanceField
                + "\"}");
  }

  @Test
  public void testGsonWithLowerCaseDotPolicyDeserialiation() {
    Gson gson = builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DOTS).create();
    String target = "{\"some.constant.string.instance.field\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testGsonWithLowerCaseDashPolicyDeserialiation() {
    Gson gson = builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    String target = "{\"some-constant-string-instance-field\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testGsonWithLowerCaseUnderscorePolicySerialization() {
    Gson gson =
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    StringWrapper target = new StringWrapper("blah");
    assertThat(gson.toJson(target))
        .isEqualTo(
            "{\"some_constant_string_instance_field\":\""
                + target.someConstantStringInstanceField
                + "\"}");
  }

  @Test
  public void testGsonWithLowerCaseUnderscorePolicyDeserialiation() {
    Gson gson =
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    String target = "{\"some_constant_string_instance_field\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testGsonWithSerializedNameFieldNamingPolicySerialization() {
    Gson gson = builder.create();
    ClassWithSerializedNameFields expected = new ClassWithSerializedNameFields(5, 6);
    String actual = gson.toJson(expected);
    assertThat(actual).isEqualTo(expected.getExpectedJson());
  }

  @Test
  public void testGsonWithSerializedNameFieldNamingPolicyDeserialization() {
    Gson gson = builder.create();
    ClassWithSerializedNameFields expected = new ClassWithSerializedNameFields(5, 7);
    ClassWithSerializedNameFields actual =
        gson.fromJson(expected.getExpectedJson(), ClassWithSerializedNameFields.class);
    assertThat(actual.f).isEqualTo(expected.f);
  }

  @Test
  public void testGsonDuplicateNameUsingSerializedNameFieldNamingPolicySerialization() {
    Gson gson = builder.create();
    ClassWithDuplicateFields target = new ClassWithDuplicateFields(10);
    var e = assertThrows(IllegalArgumentException.class, () -> gson.toJson(target));
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Class com.google.gson.functional.NamingPolicyTest$ClassWithDuplicateFields declares"
                + " multiple JSON fields named 'a'; conflict is caused by fields"
                + " com.google.gson.functional.NamingPolicyTest$ClassWithDuplicateFields#a and"
                + " com.google.gson.functional.NamingPolicyTest$ClassWithDuplicateFields#b\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#duplicate-fields");
  }

  @Test
  public void testGsonDuplicateNameDueToBadNamingPolicy() {
    Gson gson = builder.setFieldNamingStrategy(f -> "x").create();

    var e =
        assertThrows(IllegalArgumentException.class, () -> gson.toJson(new ClassWithTwoFields()));
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Class com.google.gson.functional.NamingPolicyTest$ClassWithTwoFields declares multiple"
                + " JSON fields named 'x'; conflict is caused by fields"
                + " com.google.gson.functional.NamingPolicyTest$ClassWithTwoFields#a and"
                + " com.google.gson.functional.NamingPolicyTest$ClassWithTwoFields#b\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#duplicate-fields");
  }

  @Test
  public void testGsonWithUpperCamelCaseSpacesPolicySerialiation() {
    Gson gson =
        builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE_WITH_SPACES).create();
    StringWrapper target = new StringWrapper("blah");
    assertThat(gson.toJson(target))
        .isEqualTo(
            "{\"Some Constant String Instance Field\":\""
                + target.someConstantStringInstanceField
                + "\"}");
  }

  @Test
  public void testGsonWithUpperCamelCaseSpacesPolicyDeserialiation() {
    Gson gson =
        builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE_WITH_SPACES).create();
    String target = "{\"Some Constant String Instance Field\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testGsonWithUpperCaseUnderscorePolicySerialization() {
    Gson gson =
        builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CASE_WITH_UNDERSCORES).create();
    StringWrapper target = new StringWrapper("blah");
    assertThat(gson.toJson(target))
        .isEqualTo(
            "{\"SOME_CONSTANT_STRING_INSTANCE_FIELD\":\""
                + target.someConstantStringInstanceField
                + "\"}");
  }

  @Test
  public void testGsonWithUpperCaseUnderscorePolicyDeserialiation() {
    Gson gson =
        builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CASE_WITH_UNDERSCORES).create();
    String target = "{\"SOME_CONSTANT_STRING_INSTANCE_FIELD\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testDeprecatedNamingStrategy() {
    Gson gson = builder.setFieldNamingStrategy(new UpperCaseNamingStrategy()).create();
    ClassWithDuplicateFields target = new ClassWithDuplicateFields(10);
    String actual = gson.toJson(target);
    assertThat(actual).isEqualTo("{\"A\":10}");
  }

  @Test
  public void testComplexFieldNameStrategy() {
    Gson gson = new Gson();
    String json = gson.toJson(new ClassWithComplexFieldName(10));
    String escapedFieldName = "@value\\\"_s$\\\\";
    assertThat(json).isEqualTo("{\"" + escapedFieldName + "\":10}");

    ClassWithComplexFieldName obj = gson.fromJson(json, ClassWithComplexFieldName.class);
    assertThat(obj.value).isEqualTo(10);
  }

  /** http://code.google.com/p/google-gson/issues/detail?id=349 */
  @Test
  public void testAtSignInSerializedName() {
    assertThat(new Gson().toJson(new AtName())).isEqualTo("{\"@foo\":\"bar\"}");
  }

  @Test
  public void testGsonWithNameDeserialiation() {
    Gson gson =
        builder
            .setFieldNamingStrategy(
                new FieldNamingStrategy() {

                  @Override
                  public String translateName(Field f) {
                    return "SomeConstantStringInstanceField";
                  }

                  @Override
                  public List<String> translateToAlternateNames(Field f) {
                    return List.of("alternate-name");
                  }
                })
            .create();
    String target = "{\"SomeConstantStringInstanceField\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testGsonWithAlternateNamesDeserialiation() {
    Gson gson =
        builder
            .setFieldNamingStrategy(
                new FieldNamingStrategy() {

                  @Override
                  public String translateName(Field f) {
                    return "primary-name";
                  }

                  @Override
                  public List<String> translateToAlternateNames(Field f) {
                    return List.of("SomeConstantStringInstanceField");
                  }
                })
            .create();
    String target = "{\"SomeConstantStringInstanceField\":\"someValue\"}";
    StringWrapper deserializedObject = gson.fromJson(target, StringWrapper.class);
    assertThat(deserializedObject.someConstantStringInstanceField).isEqualTo("someValue");
  }

  @Test
  public void testGsonWithAlternateNamesSerialization() {
    Gson gson =
        builder
            .setFieldNamingStrategy(
                new FieldNamingStrategy() {

                  @Override
                  public String translateName(Field f) {
                    return "some-constant-string-instance-field";
                  }

                  @Override
                  public List<String> translateToAlternateNames(Field f) {
                    return List.of("SomeConstantStringInstanceField");
                  }
                })
            .create();
    StringWrapper target = new StringWrapper("blah");
    assertThat(gson.toJson(target))
        .isEqualTo(
            "{\"some-constant-string-instance-field\":\""
                + target.someConstantStringInstanceField
                + "\"}");
  }

  static final class AtName {
    @SerializedName("@foo")
    String f = "bar";
  }

  private static final class UpperCaseNamingStrategy implements FieldNamingStrategy {
    @Override
    public String translateName(Field f) {
      return f.getName().toUpperCase(Locale.ROOT);
    }
  }

  @SuppressWarnings("unused")
  private static class ClassWithDuplicateFields {
    public Integer a;

    @SerializedName("a")
    public Double b;

    public ClassWithDuplicateFields(Integer a) {
      this(a, null);
    }

    public ClassWithDuplicateFields(Double b) {
      this(null, b);
    }

    public ClassWithDuplicateFields(Integer a, Double b) {
      this.a = a;
      this.b = b;
    }
  }

  private static class ClassWithComplexFieldName {
    @SerializedName("@value\"_s$\\")
    public final long value;

    ClassWithComplexFieldName(long value) {
      this.value = value;
    }
  }

  @SuppressWarnings("unused")
  private static class ClassWithTwoFields {
    public int a;
    public int b;

    public ClassWithTwoFields() {}
  }
}
