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

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.ReflectionAccessFilter.FilterResult;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.ReflectionAccessFilterHelper;
import com.google.gson.internal.reflect.ReflectionHelper;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type adapter that reflects over the fields and methods of a class.
 */
public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
  private final ConstructorConstructor constructorConstructor;
  private final FieldNamingStrategy fieldNamingPolicy;
  private final Excluder excluder;
  private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
  private final List<ReflectionAccessFilter> reflectionFilters;

  public ReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor,
      FieldNamingStrategy fieldNamingPolicy, Excluder excluder,
      JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory,
      List<ReflectionAccessFilter> reflectionFilters) {
    this.constructorConstructor = constructorConstructor;
    this.fieldNamingPolicy = fieldNamingPolicy;
    this.excluder = excluder;
    this.jsonAdapterFactory = jsonAdapterFactory;
    this.reflectionFilters = reflectionFilters;
  }

  private boolean includeField(Field f, boolean serialize) {
    return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
  }

  /** first element holds the default name */
  private List<String> getFieldNames(Field f) {
    SerializedName annotation = f.getAnnotation(SerializedName.class);
    if (annotation == null) {
      String name = fieldNamingPolicy.translateName(f);
      return Collections.singletonList(name);
    }

    String serializedName = annotation.value();
    String[] alternates = annotation.alternate();
    if (alternates.length == 0) {
      return Collections.singletonList(serializedName);
    }

    List<String> fieldNames = new ArrayList<>(alternates.length + 1);
    fieldNames.add(serializedName);
    for (String alternate : alternates) {
      fieldNames.add(alternate);
    }
    return fieldNames;
  }

  @Override
  public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
    Class<? super T> raw = type.getRawType();

    if (!Object.class.isAssignableFrom(raw)) {
      return null; // it's a primitive!
    }

    FilterResult filterResult =
        ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
    if (filterResult == FilterResult.BLOCK_ALL) {
      throw new JsonIOException(
          "ReflectionAccessFilter does not permit using reflection for "
              + raw
              + ". Register a TypeAdapter for this type or adjust the access filter.");
    }
    boolean blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;

    // If the type is actually a Java Record, we need to use the RecordAdapter instead. This will always be false
    // on JVMs that do not support records.
    if (ReflectionHelper.isRecord(raw)) {
      return new RecordAdapter<>(raw, getBoundFields(gson, type, raw, true, true));
    }

    ObjectConstructor<T> constructor = constructorConstructor.get(type);
    return new FieldReflectionAdapter<>(constructor, getBoundFields(gson, type, raw, blockInaccessible, false));
  }

  private static void checkAccessible(Object object, Field field) {
    if (!ReflectionAccessFilterHelper.canAccess(field, Modifier.isStatic(field.getModifiers()) ? null : object)) {
      throw new JsonIOException("Field '" + field.getDeclaringClass().getName() + "#"
          + field.getName() + "' is not accessible and ReflectionAccessFilter does not "
          + "permit making it accessible. Register a TypeAdapter for the declaring type "
          + "or adjust the access filter.");
    }
  }

  private ReflectiveTypeAdapterFactory.BoundField createBoundField(
      final Gson context, final Field field, final Method accessor, final String name,
      final TypeToken<?> fieldType, boolean serialize, boolean deserialize,
      final boolean blockInaccessible) {
    final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
    JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
    TypeAdapter<?> mapped = null;
    if (annotation != null) {
      // This is not safe; requires that user has specified correct adapter class for @JsonAdapter
      mapped = jsonAdapterFactory.getTypeAdapter(
          constructorConstructor, context, fieldType, annotation);
    }
    final boolean jsonAdapterPresent = mapped != null;
    if (mapped == null) mapped = context.getAdapter(fieldType);

    @SuppressWarnings("unchecked")
    final TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) mapped;
    return new ReflectiveTypeAdapterFactory.BoundField(name, field.getName(), serialize, deserialize) {
      @Override void write(JsonWriter writer, Object source)
          throws IOException, ReflectiveOperationException {
        if (!serialized) return;
        if (blockInaccessible && accessor == null) {
          checkAccessible(source, field);
        }

        Object fieldValue = (accessor != null)
          ? accessor.invoke(source)
          : field.get(source);
        if (fieldValue == source) {
          // avoid direct recursion
          return;
        }
        writer.name(name);
        TypeAdapter<Object> t = jsonAdapterPresent ? typeAdapter
            : new TypeAdapterRuntimeTypeWrapper<>(context, typeAdapter, fieldType.getType());
        t.write(writer, fieldValue);
      }

      @Override
      void readIntoArray(JsonReader reader, int index, Object[] target) throws IOException {
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue != null || !isPrimitive) {
          target[index] = fieldValue;
        }
      }

      @Override
      void readIntoField(JsonReader reader, Object target)
          throws IOException, IllegalAccessException {
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue != null || !isPrimitive) {
          if (blockInaccessible) {
            checkAccessible(target, field);
          }
          field.set(target, fieldValue);
        }
      }
    };
  }

  private Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw,
                                                 boolean blockInaccessible, boolean isRecord) {
    Map<String, BoundField> result = new LinkedHashMap<>();
    if (raw.isInterface()) {
      return result;
    }

    Type declaredType = type.getType();
    Class<?> originalRaw = raw;
    while (raw != Object.class) {
      Field[] fields = raw.getDeclaredFields();

      // For inherited fields, check if access to their declaring class is allowed
      if (raw != originalRaw && fields.length > 0) {
        FilterResult filterResult = ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
        if (filterResult == FilterResult.BLOCK_ALL) {
          throw new JsonIOException("ReflectionAccessFilter does not permit using reflection for "
              + raw + " (supertype of " + originalRaw + "). Register a TypeAdapter for this type "
              + "or adjust the access filter.");
        }
        blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
      }

      for (Field field : fields) {
        boolean serialize = includeField(field, true);
        boolean deserialize = includeField(field, false);
        if (!serialize && !deserialize) {
          continue;
        }
        // The accessor method is only used for records. If the type is a record, we will read out values
        // via its accessor method instead of via reflection. This way we will bypass the accessible restrictions
        Method accessor = null;
        if (isRecord) {
          accessor = ReflectionHelper.getAccessor(raw, field);
        }

        // If blockInaccessible, skip and perform access check later. When constructing a BoundedField for a Record
        // field, blockInaccessible is always true, thus makeAccessible will never get called. This is not an issue
        // though, as we will use the accessor method instead for reading record fields, and the constructor for
        // writing fields.
        if (!blockInaccessible) {
          ReflectionHelper.makeAccessible(field);
        }
        Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
        List<String> fieldNames = getFieldNames(field);
        BoundField previous = null;
        for (int i = 0, size = fieldNames.size(); i < size; ++i) {
          String name = fieldNames.get(i);
          if (i != 0) serialize = false; // only serialize the default name
          BoundField boundField = createBoundField(context, field, accessor, name,
              TypeToken.get(fieldType), serialize, deserialize, blockInaccessible);
          BoundField replaced = result.put(name, boundField);
          if (previous == null) previous = replaced;
        }
        if (previous != null) {
          throw new IllegalArgumentException(declaredType
              + " declares multiple JSON fields named " + previous.name);
        }
      }
      type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
      raw = type.getRawType();
    }
    return result;
  }

  static abstract class BoundField {
    final String name;
    /** Name of the underlying field */
    final String componentName;
    final boolean serialized;
    final boolean deserialized;

    protected BoundField(String name, String componentName, boolean serialized, boolean deserialized) {
      this.name = name;
      this.componentName = componentName;
      this.serialized = serialized;
      this.deserialized = deserialized;
    }

    /** Read this field value from the source, and append its json value to the writer */
    abstract void write(JsonWriter writer, Object source) throws IOException, ReflectiveOperationException;

    /** Read the value into the target array, used to provide constructor arguments for records */
    abstract void readIntoArray(JsonReader reader, int index, Object[] target) throws IOException;

    /** Read the value from the reader, and set it on the corresponding field on target via reflection */
    abstract void readIntoField(JsonReader reader, Object target) throws IOException, IllegalAccessException;
  }

  /**
   * Base class for Adapters produced by this factory.
   *
   * <p>The {@link RecordAdapter} is a special case to handle records for JVMs that support it, for
   * all other types we use the {@link FieldReflectionAdapter}. This class encapsulates the common
   * logic for serialization and deserialization. During deserialization, we construct an
   * accumulator A, which we use to accumulate values from the source Json. After the object has been read in
   * full, the {@link #finalize(Object)} method is used to convert the accumulator to an instance
   * of T.
   *
   * @param <T> type of objects that this Adapter creates.
   * @param <A> type of accumulator used to build the deserialization result.
   */
  abstract static class Adapter<T, A> extends TypeAdapter<T> {
    protected final Map<String, BoundField> boundFields;

    protected Adapter(Map<String, BoundField> boundFields) {
      this.boundFields = boundFields;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }

      out.beginObject();
      try {
        for (BoundField boundField : boundFields.values()) {
          boundField.write(out, value);
        }
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      } catch (ReflectiveOperationException e) {
        throw ReflectionHelper.createExceptionForRecordReflectionException(e);
      }
      out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      A accumulator = createAccumulator();

      try {
        in.beginObject();
        while (in.hasNext()) {
          String name = in.nextName();
          BoundField field = boundFields.get(name);
          if (field == null || !field.deserialized) {
            in.skipValue();
          } else {
            readField(accumulator, in, field);
          }
        }
      } catch (IllegalStateException e) {
        throw new JsonSyntaxException(e);
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      in.endObject();
      return finalize(accumulator);
    }

    /** Create the Object that will be used to collect each field value */
    abstract A createAccumulator();
    /**
     * Read a single Bounded field into the accumulator. The JsonReader will be pointed at the
     * start of the value for the BoundField to read from.
     */
    abstract void readField(A accumulator, JsonReader in, BoundField field)
        throws IllegalAccessException, IOException;
    /** Convert the accumulator to a final instance of T. */
    abstract T finalize(A accumulator);
  }

  private static final class FieldReflectionAdapter<T> extends Adapter<T, T> {
    private final ObjectConstructor<T> constructor;

    FieldReflectionAdapter(ObjectConstructor<T> constructor, Map<String, BoundField> boundFields) {
      super(boundFields);
      this.constructor = constructor;
    }

    @Override
    T createAccumulator() {
      return constructor.construct();
    }

    @Override
    void readField(T accumulator, JsonReader in, BoundField field)
        throws IllegalAccessException, IOException {
      field.readIntoField(in, accumulator);
    }

    @Override
    T finalize(T accumulator) {
      return accumulator;
    }
  }

  private static final class RecordAdapter<T> extends Adapter<T, Object[]> {
    // The actual record constructor.
    private final Constructor<? super T> constructor;
    // Array of arguments to the constructor, initialized with default values for primitives
    private final Object[] constructorArgsDefaults;
    // Map from field names to index into the constructors arguments.
    private final Map<String, Integer> componentIndices = new HashMap<>();

    RecordAdapter(Class<? super T> raw, Map<String, BoundField> boundFields) {
      super(boundFields);
      this.constructor = ReflectionHelper.getCanonicalRecordConstructor(raw);
      String[] recordFields = ReflectionHelper.getRecordComponentNames(raw);
      for (int i = 0; i < recordFields.length; i++) {
        componentIndices.put(recordFields[i], i);
      }
      Class<?>[] parameterTypes = constructor.getParameterTypes();

      // We need to ensure that we are passing non-null values to primitive fields in the constructor. To do this,
      // we create an Object[] where all primitives are initialized to non-null values.
      constructorArgsDefaults = new Object[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        if (parameterTypes[i].isPrimitive()) {
          // Voodoo magic, we create a new instance of this primitive type using reflection via an
          // array. The array has 1 element, that of course will be initialized to the primitives
          // default value. We then retrieve this value back from the array to get the properly
          // initialized default value for the primitve type.
          constructorArgsDefaults[i] = Array.get(Array.newInstance(parameterTypes[i], 1), 0);
        }
      }
    }

    @Override
    Object[] createAccumulator() {
      return constructorArgsDefaults.clone();
    }

    @Override
    void readField(Object[] accumulator, JsonReader in, BoundField field) throws IOException {
      Integer fieldIndex = componentIndices.get(field.componentName);
      if (fieldIndex == null) {
        throw new IllegalStateException(
            "Could not find the index in the constructor "
                + constructor
                + " for field with name "
                + field.name
                + ", unable to determine which argument in the constructor the field corresponds"
                + " to. This is unexpected behaviour, as we expect the RecordComponents to have the"
                + " same names as the fields in the Java class, and that the order of the"
                + " RecordComponents is the same as the order of the canonical arguments.");
      }
      field.readIntoArray(in, fieldIndex, accumulator);
    }

    @Override
    @SuppressWarnings("unchecked")
    T finalize(Object[] accumulator) {
      try {
        return (T) constructor.newInstance(accumulator);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(
            "Failed to invoke " + constructor + " with args " + Arrays.toString(accumulator), e);
      }
    }
  }
}
