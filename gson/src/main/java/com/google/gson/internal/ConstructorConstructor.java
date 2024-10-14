/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.gson.internal;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonIOException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.ReflectionAccessFilter.FilterResult;
import com.google.gson.internal.reflect.ReflectionHelper;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Returns a function that can construct an instance of a requested type. */
public final class ConstructorConstructor {
  private final Map<Type, InstanceCreator<?>> instanceCreators;
  private final boolean useJdkUnsafe;
  private final List<ReflectionAccessFilter> reflectionFilters;

  public ConstructorConstructor(
      Map<Type, InstanceCreator<?>> instanceCreators,
      boolean useJdkUnsafe,
      List<ReflectionAccessFilter> reflectionFilters) {
    this.instanceCreators = instanceCreators;
    this.useJdkUnsafe = useJdkUnsafe;
    this.reflectionFilters = reflectionFilters;
  }

  /**
   * Check if the class can be instantiated by Unsafe allocator. If the instance has interface or
   * abstract modifiers return an exception message.
   *
   * @param c instance of the class to be checked
   * @return if instantiable {@code null}, else a non-{@code null} exception message
   */
  static String checkInstantiable(Class<?> c) {
    int modifiers = c.getModifiers();
    if (Modifier.isInterface(modifiers)) {
      return "Interfaces can't be instantiated! Register an InstanceCreator"
          + " or a TypeAdapter for this type. Interface name: "
          + c.getName();
    }
    if (Modifier.isAbstract(modifiers)) {
      // R8 performs aggressive optimizations where it removes the default constructor of a class
      // and makes the class `abstract`; check for that here explicitly
      /*
       * Note: Ideally should only show this R8-specific message when it is clear that R8 was
       * used (e.g. when `c.getDeclaredConstructors().length == 0`), but on Android where this
       * issue with R8 occurs most, R8 seems to keep some constructors for some reason while
       * still making the class abstract
       */
      return "Abstract classes can't be instantiated! Adjust the R8 configuration or register"
          + " an InstanceCreator or a TypeAdapter for this type. Class name: "
          + c.getName()
          + "\nSee "
          + TroubleshootingGuide.createUrl("r8-abstract-class");
    }
    return null;
  }

  public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
    Type type = typeToken.getType();
    Class<? super T> rawType = typeToken.getRawType();

    // first try an instance creator

    @SuppressWarnings("unchecked") // types must agree
    InstanceCreator<T> typeCreator = (InstanceCreator<T>) instanceCreators.get(type);
    if (typeCreator != null) {
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          return typeCreator.createInstance(type);
        }
      };
    }

    // Next try raw type match for instance creators
    @SuppressWarnings("unchecked") // types must agree
    InstanceCreator<T> rawTypeCreator = (InstanceCreator<T>) instanceCreators.get(rawType);
    if (rawTypeCreator != null) {
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          return rawTypeCreator.createInstance(type);
        }
      };
    }

    // First consider special constructors before checking for no-args constructors
    // below to avoid matching internal no-args constructors which might be added in
    // future JDK versions
    ObjectConstructor<T> specialConstructor = newSpecialCollectionConstructor(type, rawType);
    if (specialConstructor != null) {
      return specialConstructor;
    }

    FilterResult filterResult =
        ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, rawType);
    ObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType, filterResult);
    if (defaultConstructor != null) {
      return defaultConstructor;
    }

    ObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(type, rawType);
    if (defaultImplementation != null) {
      return defaultImplementation;
    }

    // Check whether type is instantiable; otherwise ReflectionAccessFilter recommendation
    // of adjusting filter suggested below is irrelevant since it would not solve the problem
    String exceptionMessage = checkInstantiable(rawType);
    if (exceptionMessage != null) {
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          throw new JsonIOException(exceptionMessage);
        }
      };
    }

    // Consider usage of Unsafe as reflection, so don't use if BLOCK_ALL
    // Additionally, since it is not calling any constructor at all, don't use if BLOCK_INACCESSIBLE
    if (filterResult == FilterResult.ALLOW) {
      // finally try unsafe
      return newUnsafeAllocator(rawType);
    } else {
      String message =
          "Unable to create instance of "
              + rawType
              + "; ReflectionAccessFilter does not permit using reflection or Unsafe. Register an"
              + " InstanceCreator or a TypeAdapter for this type or adjust the access filter to"
              + " allow using reflection.";
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          throw new JsonIOException(message);
        }
      };
    }
  }

  /**
   * Creates constructors for special JDK collection types which do not have a public no-args
   * constructor.
   */
  private static <T> ObjectConstructor<T> newSpecialCollectionConstructor(
      Type type, Class<? super T> rawType) {
    if (EnumSet.class.isAssignableFrom(rawType)) {
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          if (type instanceof ParameterizedType) {
            Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (elementType instanceof Class) {
              @SuppressWarnings({"unchecked", "rawtypes"})
              T set = (T) EnumSet.noneOf((Class) elementType);
              return set;
            } else {
              throw new JsonIOException("Invalid EnumSet type: " + type.toString());
            }
          } else {
            throw new JsonIOException("Invalid EnumSet type: " + type.toString());
          }
        }
      };
    }
    // Only support creation of EnumMap, but not of custom subtypes; for them type parameters
    // and constructor parameter might have completely different meaning
    else if (rawType == EnumMap.class) {
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          if (type instanceof ParameterizedType) {
            Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (elementType instanceof Class) {
              @SuppressWarnings({"unchecked", "rawtypes"})
              T map = (T) new EnumMap((Class) elementType);
              return map;
            } else {
              throw new JsonIOException("Invalid EnumMap type: " + type.toString());
            }
          } else {
            throw new JsonIOException("Invalid EnumMap type: " + type.toString());
          }
        }
      };
    }

    return null;
  }

  private static <T> ObjectConstructor<T> newDefaultConstructor(
      Class<? super T> rawType, FilterResult filterResult) {
    // Cannot invoke constructor of abstract class
    if (Modifier.isAbstract(rawType.getModifiers())) {
      return null;
    }

    Constructor<? super T> constructor;
    try {
      constructor = rawType.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      return null;
    }

    boolean canAccess =
        filterResult == FilterResult.ALLOW
            || (ReflectionAccessFilterHelper.canAccess(constructor, null)
                // Be a bit more lenient here for BLOCK_ALL; if constructor is accessible and public
                // then allow calling it
                && (filterResult != FilterResult.BLOCK_ALL
                    || Modifier.isPublic(constructor.getModifiers())));

    if (!canAccess) {
      String message =
          "Unable to invoke no-args constructor of "
              + rawType
              + ";"
              + " constructor is not accessible and ReflectionAccessFilter does not permit making"
              + " it accessible. Register an InstanceCreator or a TypeAdapter for this type, change"
              + " the visibility of the constructor or adjust the access filter.";
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          throw new JsonIOException(message);
        }
      };
    }

    // Only try to make accessible if allowed; in all other cases checks above should
    // have verified that constructor is accessible
    if (filterResult == FilterResult.ALLOW) {
      String exceptionMessage = ReflectionHelper.tryMakeAccessible(constructor);
      if (exceptionMessage != null) {
        /*
         * Create ObjectConstructor which throws exception.
         * This keeps backward compatibility (compared to returning `null` which
         * would then choose another way of creating object).
         * And it supports types which are only serialized but not deserialized
         * (compared to directly throwing exception here), e.g. when runtime type
         * of object is inaccessible, but compile-time type is accessible.
         */
        return new ObjectConstructor<T>() {
          @Override
          public T construct() {
            // New exception is created every time to avoid keeping reference
            // to exception with potentially long stack trace, causing a
            // memory leak
            throw new JsonIOException(exceptionMessage);
          }
        };
      }
    }

    return new ObjectConstructor<T>() {
      @Override
      public T construct() {
        try {
          @SuppressWarnings("unchecked") // T is the same raw type as is requested
          T newInstance = (T) constructor.newInstance();
          return newInstance;
        }
        // Note: InstantiationException should be impossible because check at start of method made
        // sure that class is not abstract
        catch (InstantiationException e) {
          throw new RuntimeException(
              "Failed to invoke constructor '"
                  + ReflectionHelper.constructorToString(constructor)
                  + "' with no args",
              e);
        } catch (InvocationTargetException e) {
          // TODO: don't wrap if cause is unchecked?
          // TODO: JsonParseException ?
          throw new RuntimeException(
              "Failed to invoke constructor '"
                  + ReflectionHelper.constructorToString(constructor)
                  + "' with no args",
              e.getCause());
        } catch (IllegalAccessException e) {
          throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
        }
      }
    };
  }

  /** Constructors for common interface types like Map and List and their subtypes. */
  private static <T> ObjectConstructor<T> newDefaultImplementationConstructor(
      Type type, Class<? super T> rawType) {

    /*
     * IMPORTANT: Must only create instances for classes with public no-args constructor.
     * For classes with special constructors / factory methods (e.g. EnumSet)
     * `newSpecialCollectionConstructor` defined above must be used, to avoid no-args
     * constructor check (which is called before this method) detecting internal no-args
     * constructors which might be added in a future JDK version
     */

    if (Collection.class.isAssignableFrom(rawType)) {
      @SuppressWarnings("unchecked")
      ObjectConstructor<T> constructor = (ObjectConstructor<T>) newCollectionConstructor(rawType);
      return constructor;
    }

    if (Map.class.isAssignableFrom(rawType)) {
      @SuppressWarnings("unchecked")
      ObjectConstructor<T> constructor = (ObjectConstructor<T>) newMapConstructor(type, rawType);
      return constructor;
    }

    // Unsupported type; try other means of creating constructor
    return null;
  }

  // Suppress Error Prone false positive: Pattern is reported for overridden methods, see
  // https://github.com/google/error-prone/issues/4563
  @SuppressWarnings("NonApiType")
  private static ObjectConstructor<? extends Collection<? extends Object>> newCollectionConstructor(
      Class<?> rawType) {

    // First try List implementation
    if (rawType.isAssignableFrom(ArrayList.class)) {
      return new ObjectConstructor<ArrayList<Object>>() {
        @Override
        public ArrayList<Object> construct() {
          return new ArrayList<>();
        }
      };
    }
    // Then try Set implementation
    else if (rawType.isAssignableFrom(LinkedHashSet.class)) {
      return new ObjectConstructor<LinkedHashSet<Object>>() {
        @Override
        public LinkedHashSet<Object> construct() {
          return new LinkedHashSet<>();
        }
      };
    }
    // Then try SortedSet / NavigableSet implementation
    else if (rawType.isAssignableFrom(TreeSet.class)) {
      return new ObjectConstructor<TreeSet<Object>>() {
        @Override
        public TreeSet<Object> construct() {
          return new TreeSet<>();
        }
      };
    }
    // Then try Queue implementation
    else if (rawType.isAssignableFrom(ArrayDeque.class)) {
      return new ObjectConstructor<ArrayDeque<Object>>() {
        @Override
        public ArrayDeque<Object> construct() {
          return new ArrayDeque<>();
        }
      };
    }

    // Was unable to create matching Collection constructor
    return null;
  }

  private static boolean hasStringKeyType(Type mapType) {
    // If mapType is not parameterized, assume it might have String as key type
    if (!(mapType instanceof ParameterizedType)) {
      return true;
    }

    Type[] typeArguments = ((ParameterizedType) mapType).getActualTypeArguments();
    if (typeArguments.length == 0) {
      return false;
    }
    return $Gson$Types.getRawType(typeArguments[0]) == String.class;
  }

  // Suppress Error Prone false positive: Pattern is reported for overridden methods, see
  // https://github.com/google/error-prone/issues/4563
  @SuppressWarnings("NonApiType")
  private static ObjectConstructor<? extends Map<? extends Object, Object>> newMapConstructor(
      final Type type, Class<?> rawType) {
    // First try Map implementation
    /*
     * Legacy special casing for Map<String, ...> to avoid DoS from colliding String hashCode
     * values for older JDKs; use own LinkedTreeMap<String, Object> instead
     */
    if (rawType.isAssignableFrom(LinkedTreeMap.class) && hasStringKeyType(type)) {
      return new ObjectConstructor<LinkedTreeMap<Object, Object>>() {
        @Override
        public LinkedTreeMap<Object, Object> construct() {
          return new LinkedTreeMap<>();
        }
      };
    } else if (rawType.isAssignableFrom(LinkedHashMap.class)) {
      return new ObjectConstructor<LinkedHashMap<Object, Object>>() {
        @Override
        public LinkedHashMap<Object, Object> construct() {
          return new LinkedHashMap<>();
        }
      };
    }
    // Then try SortedMap / NavigableMap implementation
    else if (rawType.isAssignableFrom(TreeMap.class)) {
      return new ObjectConstructor<TreeMap<Object, Object>>() {
        @Override
        public TreeMap<Object, Object> construct() {
          return new TreeMap<>();
        }
      };
    }
    // Then try ConcurrentMap implementation
    else if (rawType.isAssignableFrom(ConcurrentHashMap.class)) {
      return new ObjectConstructor<ConcurrentHashMap<Object, Object>>() {
        @Override
        public ConcurrentHashMap<Object, Object> construct() {
          return new ConcurrentHashMap<>();
        }
      };
    }
    // Then try ConcurrentNavigableMap implementation
    else if (rawType.isAssignableFrom(ConcurrentSkipListMap.class)) {
      return new ObjectConstructor<ConcurrentSkipListMap<Object, Object>>() {
        @Override
        public ConcurrentSkipListMap<Object, Object> construct() {
          return new ConcurrentSkipListMap<>();
        }
      };
    }

    // Was unable to create matching Map constructor
    return null;
  }

  private <T> ObjectConstructor<T> newUnsafeAllocator(Class<? super T> rawType) {
    if (useJdkUnsafe) {
      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          try {
            @SuppressWarnings("unchecked")
            T newInstance = (T) UnsafeAllocator.INSTANCE.newInstance(rawType);
            return newInstance;
          } catch (Exception e) {
            throw new RuntimeException(
                ("Unable to create instance of "
                    + rawType
                    + ". Registering an InstanceCreator or a TypeAdapter for this type, or adding a"
                    + " no-args constructor may fix this problem."),
                e);
          }
        }
      };
    } else {
      String exceptionMessage =
          "Unable to create instance of "
              + rawType
              + "; usage of JDK Unsafe is disabled. Registering an InstanceCreator or a TypeAdapter"
              + " for this type, adding a no-args constructor, or enabling usage of JDK Unsafe may"
              + " fix this problem.";

      // Check if R8 removed all constructors
      if (rawType.getDeclaredConstructors().length == 0) {
        // R8 with Unsafe disabled might not be common enough to warrant a separate Troubleshooting
        // Guide entry
        exceptionMessage +=
            " Or adjust your R8 configuration to keep the no-args constructor of the class.";
      }

      // Separate effectively final variable to allow usage in the anonymous class below
      String exceptionMessageF = exceptionMessage;

      return new ObjectConstructor<T>() {
        @Override
        public T construct() {
          throw new JsonIOException(exceptionMessageF);
        }
      };
    }
  }

  @Override
  public String toString() {
    return instanceCreators.toString();
  }
}
