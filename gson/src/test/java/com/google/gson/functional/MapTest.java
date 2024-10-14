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
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.common.TestTypes;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.junit.Before;
import org.junit.Test;

/**
 * Functional test for Json serialization and deserialization for Maps
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class MapTest {
  private Gson gson;

  @Before
  public void setUp() throws Exception {
    gson = new Gson();
  }

  @Test
  public void testMapSerialization() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);
    assertThat(json).contains("\"a\":1");
    assertThat(json).contains("\"b\":2");
  }

  @Test
  public void testMapDeserialization() {
    String json = "{\"a\":1,\"b\":2}";
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    Map<String, Integer> target = gson.fromJson(json, typeOfMap);
    assertThat(target.get("a")).isEqualTo(1);
    assertThat(target.get("b")).isEqualTo(2);
  }

  @Test
  public void testObjectMapSerialization() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", "string");
    String json = gson.toJson(map);
    assertThat(json).contains("\"a\":1");
    assertThat(json).contains("\"b\":\"string\"");
  }

  @Test
  public void testMapSerializationEmpty() {
    Map<String, Integer> map = new LinkedHashMap<>();
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);
    assertThat(json).isEqualTo("{}");
  }

  @Test
  public void testMapDeserializationEmpty() {
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    Map<String, Integer> map = gson.fromJson("{}", typeOfMap);
    assertThat(map).isEmpty();
  }

  @Test
  public void testMapSerializationWithNullValue() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("abc", null);
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);

    // Maps are represented as JSON objects, so ignoring null field
    assertThat(json).isEqualTo("{}");
  }

  @Test
  public void testMapDeserializationWithNullValue() {
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    Map<String, Integer> map = gson.fromJson("{\"abc\":null}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map.get("abc")).isNull();
  }

  @Test
  public void testMapSerializationWithNullValueButSerializeNulls() {
    gson = new GsonBuilder().serializeNulls().create();
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("abc", null);
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);

    assertThat(json).isEqualTo("{\"abc\":null}");
  }

  @Test
  public void testMapSerializationWithNullKey() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put(null, 123);
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);

    assertThat(json).isEqualTo("{\"null\":123}");
  }

  @Test
  public void testMapDeserializationWithNullKey() {
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    Map<String, Integer> map = gson.fromJson("{\"null\":123}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map.get("null")).isEqualTo(123);
    assertThat(map.get(null)).isNull();

    map = gson.fromJson("{null:123}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map.get("null")).isEqualTo(123);
    assertThat(map.get(null)).isNull();
  }

  @Test
  public void testMapSerializationWithIntegerKeys() {
    Map<Integer, String> map = new LinkedHashMap<>();
    map.put(123, "456");
    Type typeOfMap = new TypeToken<Map<Integer, String>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);

    assertThat(json).isEqualTo("{\"123\":\"456\"}");
  }

  @Test
  public void testMapDeserializationWithIntegerKeys() {
    Type typeOfMap = new TypeToken<Map<Integer, String>>() {}.getType();
    Map<Integer, String> map = gson.fromJson("{\"123\":\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
  }

  @Test
  public void testMapDeserializationWithUnquotedIntegerKeys() {
    Type typeOfMap = new TypeToken<Map<Integer, String>>() {}.getType();
    Map<Integer, String> map = gson.fromJson("{123:\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
  }

  @Test
  public void testMapDeserializationWithLongKeys() {
    long longValue = 9876543210L;
    String json = String.format("{\"%d\":\"456\"}", longValue);
    Type typeOfMap = new TypeToken<Map<Long, String>>() {}.getType();
    Map<Long, String> map = gson.fromJson(json, typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(longValue);
    assertThat(map.get(longValue)).isEqualTo("456");
  }

  @Test
  public void testMapDeserializationWithUnquotedLongKeys() {
    long longKey = 9876543210L;
    String json = String.format("{%d:\"456\"}", longKey);
    Type typeOfMap = new TypeToken<Map<Long, String>>() {}.getType();
    Map<Long, String> map = gson.fromJson(json, typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(longKey);
    assertThat(map.get(longKey)).isEqualTo("456");
  }

  @Test
  public void testMapStringKeyDeserialization() {
    Type typeOfMap = new TypeToken<Map<String, Integer>>() {}.getType();
    Map<?, ?> map = gson.fromJson("{\"a\":1}", typeOfMap);

    assertWithMessage(
            "Map<String, ...> should use LinkedTreeMap to protect against DoS in older JDK"
                + " versions")
        .that(map)
        .isInstanceOf(LinkedTreeMap.class);

    Map<?, ?> expectedMap = Collections.singletonMap("a", 1);
    assertThat(map).isEqualTo(expectedMap);
  }

  @Test
  public void testMapStringSupertypeKeyDeserialization() {
    // Should only use Gson's LinkedTreeMap for String as key, but not for supertypes (e.g. Object)
    Type typeOfMap = new TypeToken<Map<Object, Integer>>() {}.getType();
    Map<?, ?> map = gson.fromJson("{\"a\":1}", typeOfMap);

    assertWithMessage("Map<Object, ...> should not use Gson Map implementation")
        .that(map)
        .isNotInstanceOf(LinkedTreeMap.class);

    Map<?, ?> expectedMap = Collections.singletonMap("a", 1);
    assertThat(map).isEqualTo(expectedMap);
  }

  @Test
  public void testMapNonStringKeyDeserialization() {
    Type typeOfMap = new TypeToken<Map<Integer, Integer>>() {}.getType();
    Map<?, ?> map = gson.fromJson("{\"1\":1}", typeOfMap);

    assertWithMessage("Map<Integer, ...> should not use Gson Map implementation")
        .that(map)
        .isNotInstanceOf(LinkedTreeMap.class);

    Map<?, ?> expectedMap = Collections.singletonMap(1, 1);
    assertThat(map).isEqualTo(expectedMap);
  }

  @Test
  public void testHashMapDeserialization() {
    Type typeOfMap = new TypeToken<HashMap<Integer, String>>() {}.getType();
    HashMap<Integer, String> map = gson.fromJson("{\"123\":\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
  }

  @Test
  public void testSortedMap() {
    Type typeOfMap = new TypeToken<SortedMap<Integer, String>>() {}.getType();
    SortedMap<Integer, String> map = gson.fromJson("{\"123\":\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
  }

  @Test
  public void testConcurrentMap() {
    Type typeOfMap = new TypeToken<ConcurrentMap<Integer, String>>() {}.getType();
    ConcurrentMap<Integer, String> map = gson.fromJson("{\"123\":\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
    String json = gson.toJson(map);
    assertThat(json).isEqualTo("{\"123\":\"456\"}");
  }

  @Test
  public void testConcurrentHashMap() {
    Type typeOfMap = new TypeToken<ConcurrentHashMap<Integer, String>>() {}.getType();
    ConcurrentHashMap<Integer, String> map = gson.fromJson("{\"123\":\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
    String json = gson.toJson(map);
    assertThat(json).isEqualTo("{\"123\":\"456\"}");
  }

  @Test
  public void testConcurrentNavigableMap() {
    Type typeOfMap = new TypeToken<ConcurrentNavigableMap<Integer, String>>() {}.getType();
    ConcurrentNavigableMap<Integer, String> map = gson.fromJson("{\"123\":\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
    String json = gson.toJson(map);
    assertThat(json).isEqualTo("{\"123\":\"456\"}");
  }

  @Test
  public void testConcurrentSkipListMap() {
    Type typeOfMap = new TypeToken<ConcurrentSkipListMap<Integer, String>>() {}.getType();
    ConcurrentSkipListMap<Integer, String> map = gson.fromJson("{\"123\":\"456\"}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map).containsKey(123);
    assertThat(map.get(123)).isEqualTo("456");
    String json = gson.toJson(map);
    assertThat(json).isEqualTo("{\"123\":\"456\"}");
  }

  @Test
  public void testParameterizedMapSubclassSerialization() {
    MyParameterizedMap<String, String> map = new MyParameterizedMap<>(10);
    map.put("a", "b");
    Type type = new TypeToken<MyParameterizedMap<String, String>>() {}.getType();
    String json = gson.toJson(map, type);
    assertThat(json).contains("\"a\":\"b\"");
  }

  @SuppressWarnings({"unused", "serial"})
  private static class MyParameterizedMap<K, V> extends LinkedHashMap<K, V> {
    final int foo;

    MyParameterizedMap(int foo) {
      this.foo = foo;
    }
  }

  @Test
  public void testMapSubclassSerialization() {
    MyMap map = new MyMap();
    map.put("a", "b");
    String json = gson.toJson(map, MyMap.class);
    assertThat(json).contains("\"a\":\"b\"");
  }

  @Test
  public void testMapStandardSubclassDeserialization() {
    String json = "{a:'1',b:'2'}";
    Type type = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
    LinkedHashMap<String, String> map = gson.fromJson(json, type);
    assertThat(map).containsEntry("a", "1");
    assertThat(map).containsEntry("b", "2");
  }

  @Test
  public void testMapSubclassDeserialization() {
    Gson gson =
        new GsonBuilder()
            .registerTypeAdapter(
                MyMap.class,
                new InstanceCreator<MyMap>() {
                  @Override
                  public MyMap createInstance(Type type) {
                    return new MyMap();
                  }
                })
            .create();
    String json = "{\"a\":1,\"b\":2}";
    MyMap map = gson.fromJson(json, MyMap.class);
    assertThat(map.get("a")).isEqualTo("1");
    assertThat(map.get("b")).isEqualTo("2");
  }

  @Test
  public void testCustomSerializerForSpecificMapType() {
    Type type =
        $Gson$Types.newParameterizedTypeWithOwner(null, Map.class, String.class, Long.class);
    Gson gson =
        new GsonBuilder()
            .registerTypeAdapter(
                type,
                new JsonSerializer<Map<String, Long>>() {
                  @Override
                  public JsonElement serialize(
                      Map<String, Long> src, Type typeOfSrc, JsonSerializationContext context) {
                    JsonArray array = new JsonArray();
                    for (long value : src.values()) {
                      array.add(new JsonPrimitive(value));
                    }
                    return array;
                  }
                })
            .create();

    Map<String, Long> src = new LinkedHashMap<>();
    src.put("one", 1L);
    src.put("two", 2L);
    src.put("three", 3L);

    assertThat(gson.toJson(src, type)).isEqualTo("[1,2,3]");
  }

  /** Created in response to http://code.google.com/p/google-gson/issues/detail?id=99 */
  private static class ClassWithAMap {
    Map<String, String> map = new TreeMap<>();
  }

  /** Created in response to http://code.google.com/p/google-gson/issues/detail?id=99 */
  @Test
  public void testMapSerializationWithNullValues() {
    ClassWithAMap target = new ClassWithAMap();
    target.map.put("name1", null);
    target.map.put("name2", "value2");
    String json = gson.toJson(target);
    assertThat(json).doesNotContain("name1");
    assertThat(json).contains("name2");
  }

  /** Created in response to http://code.google.com/p/google-gson/issues/detail?id=99 */
  @Test
  public void testMapSerializationWithNullValuesSerialized() {
    Gson gson = new GsonBuilder().serializeNulls().create();
    ClassWithAMap target = new ClassWithAMap();
    target.map.put("name1", null);
    target.map.put("name2", "value2");
    String json = gson.toJson(target);
    assertThat(json).contains("name1");
    assertThat(json).contains("name2");
  }

  @Test
  public void testMapSerializationWithWildcardValues() {
    Map<String, ? extends Collection<? extends Integer>> map = new LinkedHashMap<>();
    map.put("test", null);
    Type typeOfMap =
        new TypeToken<Map<String, ? extends Collection<? extends Integer>>>() {}.getType();
    String json = gson.toJson(map, typeOfMap);

    assertThat(json).isEqualTo("{}");
  }

  @Test
  public void testMapDeserializationWithWildcardValues() {
    Type typeOfMap = new TypeToken<Map<String, ? extends Long>>() {}.getType();
    Map<String, ? extends Long> map = gson.fromJson("{\"test\":123}", typeOfMap);
    assertThat(map).hasSize(1);
    assertThat(map.get("test")).isEqualTo(123L);
  }

  private static class MyMap extends LinkedHashMap<String, String> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    int foo = 10;
  }

  /** From bug report http://code.google.com/p/google-gson/issues/detail?id=95 */
  @Test
  public void testMapOfMapSerialization() {
    Map<String, Map<String, String>> map = new HashMap<>();
    Map<String, String> nestedMap = new HashMap<>();
    nestedMap.put("1", "1");
    nestedMap.put("2", "2");
    map.put("nestedMap", nestedMap);
    String json = gson.toJson(map);
    assertThat(json).contains("nestedMap");
    assertThat(json).contains("\"1\":\"1\"");
    assertThat(json).contains("\"2\":\"2\"");
  }

  /** From bug report http://code.google.com/p/google-gson/issues/detail?id=95 */
  @Test
  public void testMapOfMapDeserialization() {
    String json = "{nestedMap:{'2':'2','1':'1'}}";
    Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
    Map<String, Map<String, String>> map = gson.fromJson(json, type);
    Map<String, String> nested = map.get("nestedMap");
    assertThat(nested.get("1")).isEqualTo("1");
    assertThat(nested.get("2")).isEqualTo("2");
  }

  /** From bug report http://code.google.com/p/google-gson/issues/detail?id=178 */
  @Test
  public void testMapWithQuotes() {
    Map<String, String> map = new HashMap<>();
    map.put("a\"b", "c\"d");
    String json = gson.toJson(map);
    assertThat(json).isEqualTo("{\"a\\\"b\":\"c\\\"d\"}");
  }

  /** From issue 227. */
  @Test
  public void testWriteMapsWithEmptyStringKey() {
    Map<String, Boolean> map = new HashMap<>();
    map.put("", true);
    assertThat(gson.toJson(map)).isEqualTo("{\"\":true}");
  }

  @Test
  public void testReadMapsWithEmptyStringKey() {
    Map<String, Boolean> map =
        gson.fromJson("{\"\":true}", new TypeToken<Map<String, Boolean>>() {}.getType());
    assertThat(map.get("")).isEqualTo(Boolean.TRUE);
  }

  /** From bug report http://code.google.com/p/google-gson/issues/detail?id=204 */
  @Test
  public void testSerializeMaps() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("a", 12);
    map.put("b", null);

    LinkedHashMap<String, Object> innerMap = new LinkedHashMap<>();
    innerMap.put("test", 1);
    innerMap.put("TestStringArray", new String[] {"one", "two"});
    map.put("c", innerMap);

    assertThat(new GsonBuilder().serializeNulls().create().toJson(map))
        .isEqualTo(
            "{\"a\":12,\"b\":null,\"c\":{\"test\":1,\"TestStringArray\":[\"one\",\"two\"]}}");
    assertThat(new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(map))
        .isEqualTo(
            "{\n  \"a\": 12,\n  \"b\": null,\n  \"c\": "
                + "{\n    \"test\": 1,\n    \"TestStringArray\": "
                + "[\n      \"one\",\n      \"two\"\n    ]\n  }\n}");
    assertThat(new GsonBuilder().create().toJson(map))
        .isEqualTo("{\"a\":12,\"c\":{\"test\":1,\"TestStringArray\":[\"one\",\"two\"]}}");
    assertThat(new GsonBuilder().setPrettyPrinting().create().toJson(map))
        .isEqualTo(
            "{\n  \"a\": 12,\n  \"c\": "
                + "{\n    \"test\": 1,\n    \"TestStringArray\": "
                + "[\n      \"one\",\n      \"two\"\n    ]\n  }\n}");
    innerMap.put("d", "e");
    assertThat(new Gson().toJson(map))
        .isEqualTo(
            "{\"a\":12,\"c\":{\"test\":1,\"TestStringArray\":[\"one\",\"two\"],\"d\":\"e\"}}");
  }

  @Test
  public final void testInterfaceTypeMap() {
    MapClass element = new MapClass();
    TestTypes.Sub subType = new TestTypes.Sub();
    element.addBase("Test", subType);
    element.addSub("Test", subType);

    String subTypeJson = new Gson().toJson(subType);
    String expected =
        "{\"bases\":{\"Test\":" + subTypeJson + "},\"subs\":{\"Test\":" + subTypeJson + "}}";

    Gson gsonWithComplexKeys = new GsonBuilder().enableComplexMapKeySerialization().create();
    String json = gsonWithComplexKeys.toJson(element);
    assertThat(json).isEqualTo(expected);

    Gson gson = new Gson();
    json = gson.toJson(element);
    assertThat(json).isEqualTo(expected);
  }

  @Test
  public final void testInterfaceTypeMapWithSerializer() {
    MapClass element = new MapClass();
    TestTypes.Sub subType = new TestTypes.Sub();
    element.addBase("Test", subType);
    element.addSub("Test", subType);

    Gson tempGson = new Gson();
    String subTypeJson = tempGson.toJson(subType);
    JsonElement baseTypeJsonElement = tempGson.toJsonTree(subType, TestTypes.Base.class);
    String baseTypeJson = tempGson.toJson(baseTypeJsonElement);
    String expected =
        "{\"bases\":{\"Test\":" + baseTypeJson + "},\"subs\":{\"Test\":" + subTypeJson + "}}";

    JsonSerializer<TestTypes.Base> baseTypeAdapter =
        new JsonSerializer<>() {
          @Override
          public JsonElement serialize(
              TestTypes.Base src, Type typeOfSrc, JsonSerializationContext context) {
            return baseTypeJsonElement;
          }
        };

    Gson gson =
        new GsonBuilder()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(TestTypes.Base.class, baseTypeAdapter)
            .create();
    String json = gson.toJson(element);
    assertThat(json).isEqualTo(expected);

    gson = new GsonBuilder().registerTypeAdapter(TestTypes.Base.class, baseTypeAdapter).create();
    json = gson.toJson(element);
    assertThat(json).isEqualTo(expected);
  }

  @Test
  public void testGeneralMapField() {
    MapWithGeneralMapParameters map = new MapWithGeneralMapParameters();
    map.map.put("string", "testString");
    map.map.put("stringArray", new String[] {"one", "two"});
    map.map.put("objectArray", new Object[] {1, 2L, "three"});

    String expected =
        "{\"map\":{\"string\":\"testString\",\"stringArray\":"
            + "[\"one\",\"two\"],\"objectArray\":[1,2,\"three\"]}}";
    assertThat(gson.toJson(map)).isEqualTo(expected);

    gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    assertThat(gson.toJson(map)).isEqualTo(expected);
  }

  @Test
  public void testComplexKeysSerialization() {
    Map<Point, String> map = new LinkedHashMap<>();
    map.put(new Point(2, 3), "a");
    map.put(new Point(5, 7), "b");
    String json = "{\"2,3\":\"a\",\"5,7\":\"b\"}";
    assertThat(gson.toJson(map, new TypeToken<Map<Point, String>>() {}.getType())).isEqualTo(json);
    assertThat(gson.toJson(map, Map.class)).isEqualTo(json);
  }

  @Test
  public void testComplexKeysDeserialization() {
    String json = "{'2,3':'a','5,7':'b'}";
    Type type = new TypeToken<Map<Point, String>>() {}.getType();
    var e = assertThrows(JsonParseException.class, () -> gson.fromJson(json, type));
    assertThat(e)
        .hasCauseThat()
        .hasMessageThat()
        .startsWith("Expected BEGIN_OBJECT but was STRING at line 1 column 3 path $.\n");
  }

  @Test
  public void testStringKeyDeserialization() {
    String json = "{'2,3':'a','5,7':'b'}";
    Map<String, String> map = new LinkedHashMap<>();
    map.put("2,3", "a");
    map.put("5,7", "b");
    assertThat(gson.fromJson(json, new TypeToken<Map<String, String>>() {})).isEqualTo(map);
  }

  @Test
  public void testNumberKeyDeserialization() {
    String json = "{'2.3':'a','5.7':'b'}";
    Map<Double, String> map = new LinkedHashMap<>();
    map.put(2.3, "a");
    map.put(5.7, "b");
    assertThat(gson.fromJson(json, new TypeToken<Map<Double, String>>() {})).isEqualTo(map);
  }

  @Test
  public void testBooleanKeyDeserialization() {
    String json = "{'true':'a','false':'b'}";
    Map<Boolean, String> map = new LinkedHashMap<>();
    map.put(true, "a");
    map.put(false, "b");
    assertThat(gson.fromJson(json, new TypeToken<Map<Boolean, String>>() {})).isEqualTo(map);
  }

  @Test
  public void testMapDeserializationWithDuplicateKeys() {
    Type type = new TypeToken<Map<String, Integer>>() {}.getType();
    var e = assertThrows(JsonSyntaxException.class, () -> gson.fromJson("{'a':1,'a':2}", type));
    assertThat(e).hasMessageThat().isEqualTo("duplicate key: a");
  }

  @Test
  public void testSerializeMapOfMaps() {
    Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
    Map<String, Map<String, String>> map =
        newMap(
            "a", newMap("ka1", "va1", "ka2", "va2"),
            "b", newMap("kb1", "vb1", "kb2", "vb2"));
    assertThat(gson.toJson(map, type).replace('"', '\''))
        .isEqualTo("{'a':{'ka1':'va1','ka2':'va2'},'b':{'kb1':'vb1','kb2':'vb2'}}");
  }

  @Test
  public void testDeserializeMapOfMaps() {
    TypeToken<Map<String, Map<String, String>>> type = new TypeToken<>() {};
    Map<String, Map<String, String>> map =
        newMap(
            "a", newMap("ka1", "va1", "ka2", "va2"),
            "b", newMap("kb1", "vb1", "kb2", "vb2"));
    String json = "{'a':{'ka1':'va1','ka2':'va2'},'b':{'kb1':'vb1','kb2':'vb2'}}";
    assertThat(gson.fromJson(json, type)).isEqualTo(map);
  }

  private static <K, V> Map<K, V> newMap(K key1, V value1, K key2, V value2) {
    Map<K, V> result = new LinkedHashMap<>();
    result.put(key1, value1);
    result.put(key2, value2);
    return result;
  }

  @Test
  public void testMapNamePromotionWithJsonElementReader() {
    String json = "{'2.3':'a'}";
    Map<Double, String> map = new LinkedHashMap<>();
    map.put(2.3, "a");
    JsonElement tree = JsonParser.parseString(json);
    assertThat(gson.fromJson(tree, new TypeToken<Map<Double, String>>() {})).isEqualTo(map);
  }

  static class Point {
    private final int x;
    private final int y;

    Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Point && x == ((Point) o).x && y == ((Point) o).y;
    }

    @Override
    public int hashCode() {
      return x * 37 + y;
    }

    @Override
    public String toString() {
      return x + "," + y;
    }
  }

  static final class MapClass {
    private final Map<String, TestTypes.Base> bases = new HashMap<>();
    private final Map<String, TestTypes.Sub> subs = new HashMap<>();

    public final void addBase(String name, TestTypes.Base value) {
      bases.put(name, value);
    }

    public final void addSub(String name, TestTypes.Sub value) {
      subs.put(name, value);
    }
  }

  static final class MapWithGeneralMapParameters {
    final Map<String, Object> map = new LinkedHashMap<>();
  }
}
