/*
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

package com.google.gson.functional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test to demonstrate bug described in <a href="https://github.com/google/gson/issues/1028">issue 1028</a>.
 */
public class TypeAdapterFactoryAnnotatonsTest {
  private static final TypeToken expectedDelegateType;

  static {
    NonEmptyListAdapterFactory adapterFactory = new NonEmptyListAdapterFactory();
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(adapterFactory)
        .create();
    // This mimics how the delegate adapter would be created when not using annotations so that it can be checked for
    // the correct type later. Test below shows that the NonEmptyListAdapterFactory works correctly when annotations are
    // not in use.
    TypeAdapter adapter = gson.getDelegateAdapter(adapterFactory, TypeToken.getParameterized(List.class, String.class));
    expectedDelegateType = TypeToken.get(adapter.getClass());
  }

  @Test
  public void testTypeAdapterFactoryUsingDelegateWithoutAnnotation() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(new NonEmptyListAdapterFactory())
        .create();
    final String testName = "Test";
    final String testItem = "item";
    final String testJsonEmpty = "{\"name\":\"" + testName + "\"}";
    final String testJson = "{\"name\":\"" + testName + "\",\"things\":[\"" + testItem + "\"]}";

    ListThingNoAnnotation toJsonThing = new ListThingNoAnnotation();
    toJsonThing.name = testName;
    assertEquals(testJsonEmpty, gson.toJson(toJsonThing));
    toJsonThing.things.add(testItem);
    assertEquals(testJson, gson.toJson(toJsonThing));

    ListThingNoAnnotation emptyThing = gson.fromJson(testJsonEmpty, ListThingNoAnnotation.class);
    assertEquals(testName, emptyThing.name);
    assertTrue(emptyThing.things.isEmpty());

    ListThingNoAnnotation thing = gson.fromJson(testJson, ListThingNoAnnotation.class);
    assertEquals(testName, thing.name);
    assertEquals(1, thing.things.size());
    assertEquals(testItem, thing.things.get(0));
  }

  private static final class ListThingNoAnnotation {
    String name;
    final List<String> things;

    ListThingNoAnnotation() {
      things = new ArrayList<String>();
    }
  }

  @Test
  @Ignore("This test will fail due to wrong result for getDelegateAdapter() in NonEmptyListAdapterFactory.")
  public void testTypeAdapterFactoryUsingDelegateWithAnnotation() {
    Gson gson = new Gson();
    final String testName = "Test";
    final String testItem = "item";
    final String testJsonEmpty = "{\"name\":\"" + testName + "\"}";
    final String testJson = "{\"name\":\"" + testName + "\",\"things\":[\"" + testItem + "\"]}";

    ListThing toJsonThing = new ListThing();
    toJsonThing.name = testName;
    assertEquals(testJsonEmpty, gson.toJson(toJsonThing));
    toJsonThing.things.add(testItem);
    assertEquals(testJson, gson.toJson(toJsonThing));

    ListThing emptyThing = gson.fromJson(testJsonEmpty, ListThing.class);
    assertEquals(testName, emptyThing.name);
    assertTrue(emptyThing.things.isEmpty());

    ListThing thing = gson.fromJson(testJson, ListThing.class);
    assertEquals(testName, thing.name);
    assertEquals(1, thing.things.size());
    assertEquals(testItem, thing.things.get(0));
  }

  private static final class ListThing {
    String name;
    @JsonAdapter(value = NonEmptyListAdapterFactory.class, nullSafe = false)
    final List<String> things;

    ListThing() {
      things = new ArrayList<String>();
    }
  }

  private static final class NonEmptyListAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
      if (!List.class.isAssignableFrom(type.getRawType())) {
        return null;
      }
      // When using JsonAdapter annotation, this factory will not be registered yet, so wrong thing will be returned
      // by getDelegateAdapter due to issue #1028.
      final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
      assertEquals("Wrong delegate TypeAdapter!", expectedDelegateType, TypeToken.get(delegate.getClass()));
      return new TypeAdapter<T>() {
        @Override
        public void write(final JsonWriter writer, T value) throws IOException {
          final List list = (List) value;
          if (list == null || list.isEmpty()) {
            writer.nullValue();
          } else {
            delegate.write(writer, value);
          }
        }

        @SuppressWarnings("unchecked")
        @Override
        public T read(final JsonReader reader) throws IOException {
          final T list = delegate.read(reader);
          if (list == null) {
            return (T) new ArrayList();
          } else {
            return list;
          }
        }
      };
    }
  }
}
