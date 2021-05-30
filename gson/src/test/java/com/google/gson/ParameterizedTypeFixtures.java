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

import com.google.gson.internal.GsonTypes;

import com.google.gson.internal.Primitives;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


/**
 * This class contains some test fixtures for Parameterized types. These classes should ideally
 * belong either in the common or functional package, but they are placed here because they need
 * access to package protected elements of com.google.gson.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class ParameterizedTypeFixtures {

  public static class MyParameterizedType<T> {
    public final T value;
    public MyParameterizedType(T value) {
      this.value = value;
    }
    public T getValue() {
      return value;
    }

    public String getExpectedJson() {
      String valueAsJson = getExpectedJson(value);
      return String.format("{\"value\":%s}", valueAsJson);
    }

    private String getExpectedJson(Object obj) {
      Class<?> clazz = obj.getClass();
      if (Primitives.isWrapperType(Primitives.wrap(clazz))) {
        return obj.toString();
      } else if (obj.getClass().equals(String.class)) {
        return "\"" + obj.toString() + "\"";
      } else {
        // Try invoking a getExpectedJson() method if it exists
        try {
          Method method = clazz.getMethod("getExpectedJson");
          Object results = method.invoke(obj);
          return (String) results;
        } catch (SecurityException e) {
          throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public int hashCode() {
      return value == null ? 0 : value.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MyParameterizedType<T> other = (MyParameterizedType<T>) obj;
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
        return false;
      }
      return true;
    }
  }

  public static class MyParameterizedTypeInstanceCreator<T>
      implements InstanceCreator<MyParameterizedType<T>>{
    private final T instanceOfT;
    /**
     * Caution the specified instance is reused by the instance creator for each call.
     * This means that the fields of the same objects will be overwritten by Gson.
     * This is usually fine in tests since there we deserialize just once, but quite
     * dangerous in practice.
     *
     * @param instanceOfT
     */
    public MyParameterizedTypeInstanceCreator(T instanceOfT) {
      this.instanceOfT = instanceOfT;
    }
    @Override public MyParameterizedType<T> createInstance(Type type) {
      return new MyParameterizedType<T>(instanceOfT);
    }
  }

  public static final class MyParameterizedTypeAdapter<T>
  implements JsonSerializer<MyParameterizedType<T>>, JsonDeserializer<MyParameterizedType<T>> {
    @SuppressWarnings("unchecked")
    public static<T> String getExpectedJson(MyParameterizedType<T> obj) {
      Class<T> clazz = (Class<T>) obj.value.getClass();
      boolean addQuotes = !clazz.isArray() && !Primitives.unwrap(clazz).isPrimitive();
      StringBuilder sb = new StringBuilder("{\"");
      sb.append(obj.value.getClass().getSimpleName()).append("\":");
      if (addQuotes) {
        sb.append("\"");
      }
      sb.append(obj.value.toString());
      if (addQuotes) {
        sb.append("\"");
      }
      sb.append("}");
      return sb.toString();
    }

    @Override public JsonElement serialize(MyParameterizedType<T> src, Type classOfSrc,
        JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      T value = src.getValue();
      json.add(value.getClass().getSimpleName(), context.serialize(value));
      return json;
    }

    @SuppressWarnings("unchecked")
    @Override public MyParameterizedType<T> deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
      Type genericClass = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
      Class<?> rawType = GsonTypes.getRawType(genericClass);
      String className = rawType.getSimpleName();
      JsonElement jsonElement = json.getAsJsonObject().get(className);

      T value;
      if (genericClass == Integer.class) {
        value = (T) Integer.valueOf(jsonElement.getAsInt());
      } else if (genericClass == String.class) {
        value = (T) jsonElement.getAsString();
      } else {
        value = (T) jsonElement;
      }

      if (Primitives.isPrimitive(genericClass)) {
        PrimitiveTypeAdapter typeAdapter = new PrimitiveTypeAdapter();
        value = (T) typeAdapter.adaptType(value, rawType);
      }
      return new MyParameterizedType<T>(value);
    }
  }
}
