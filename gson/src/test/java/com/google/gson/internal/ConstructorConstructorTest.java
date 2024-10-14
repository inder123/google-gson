/*
 * Copyright (C) 2022 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.Test;

public class ConstructorConstructorTest {
  private ConstructorConstructor constructorConstructor =
      new ConstructorConstructor(Collections.emptyMap(), true, Collections.emptyList());

  private abstract static class AbstractClass {
    @SuppressWarnings("unused")
    public AbstractClass() {}
  }

  private interface Interface {}

  /**
   * Verify that ConstructorConstructor does not try to invoke no-args constructor of abstract
   * class.
   */
  @Test
  public void testGet_AbstractClassNoArgConstructor() {
    ObjectConstructor<AbstractClass> constructor =
        constructorConstructor.get(TypeToken.get(AbstractClass.class));
    var e = assertThrows(RuntimeException.class, () -> constructor.construct());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Abstract classes can't be instantiated! Adjust the R8 configuration or register an"
                + " InstanceCreator or a TypeAdapter for this type. Class name:"
                + " com.google.gson.internal.ConstructorConstructorTest$AbstractClass\n"
                + "See https://github.com/google/gson/blob/main/Troubleshooting.md#r8-abstract-class");
  }

  @Test
  public void testGet_Interface() {
    ObjectConstructor<Interface> constructor =
        constructorConstructor.get(TypeToken.get(Interface.class));
    var e = assertThrows(RuntimeException.class, () -> constructor.construct());
    assertThat(e)
        .hasMessageThat()
        .isEqualTo(
            "Interfaces can't be instantiated! Register an InstanceCreator or a TypeAdapter for"
                + " this type. Interface name:"
                + " com.google.gson.internal.ConstructorConstructorTest$Interface");
  }

  @SuppressWarnings("serial")
  private static class CustomSortedSet<E> extends TreeSet<E> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomSortedSet(Void v) {}
  }

  @SuppressWarnings("serial")
  private static class CustomSet<E> extends HashSet<E> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomSet(Void v) {}
  }

  @SuppressWarnings("serial")
  private static class CustomQueue<E> extends LinkedBlockingDeque<E> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomQueue(Void v) {}
  }

  @SuppressWarnings("serial")
  private static class CustomList<E> extends ArrayList<E> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomList(Void v) {}
  }

  /**
   * Tests that creation of custom {@code Collection} subclasses without no-args constructor should
   * not use default JDK types (which would cause {@link ClassCastException}).
   *
   * <p>Currently this test is rather contrived because the instances created using Unsafe are not
   * usable because their fields are not properly initialized, but assume that user has custom
   * classes which would be functional.
   */
  @Test
  public void testCustomCollectionCreation() {
    Class<?>[] collectionTypes = {
      CustomSortedSet.class, CustomSet.class, CustomQueue.class, CustomList.class,
    };

    for (Class<?> collectionType : collectionTypes) {
      Object actual =
          constructorConstructor
              .get(TypeToken.getParameterized(collectionType, Integer.class))
              .construct();
      assertWithMessage(
              "Failed for " + collectionType + "; created instance of " + actual.getClass())
          .that(actual)
          .isInstanceOf(collectionType);
    }
  }

  private static interface CustomCollectionInterface extends Collection<String> {}

  private static interface CustomSetInterface extends Set<String> {}

  private static interface CustomListInterface extends List<String> {}

  @Test
  public void testCustomCollectionInterfaceCreation() {
    Class<?>[] interfaces = {
      CustomCollectionInterface.class, CustomSetInterface.class, CustomListInterface.class,
    };

    for (Class<?> interfaceType : interfaces) {
      var objectConstructor = constructorConstructor.get(TypeToken.get(interfaceType));
      var exception = assertThrows(RuntimeException.class, () -> objectConstructor.construct());
      assertThat(exception)
          .hasMessageThat()
          .isEqualTo(
              "Interfaces can't be instantiated! Register an InstanceCreator or a TypeAdapter"
                  + " for this type. Interface name: "
                  + interfaceType.getName());
    }
  }

  @Test
  public void testStringMapCreation() {
    // When creating raw Map should use Gson's LinkedTreeMap, assuming keys could be String
    Object actual = constructorConstructor.get(TypeToken.get(Map.class)).construct();
    assertThat(actual).isInstanceOf(LinkedTreeMap.class);

    // When creating a `Map<String, ...>` should use Gson's LinkedTreeMap
    actual = constructorConstructor.get(new TypeToken<Map<String, Integer>>() {}).construct();
    assertThat(actual).isInstanceOf(LinkedTreeMap.class);

    // But when explicitly requesting a JDK `LinkedHashMap<String, ...>` should use LinkedHashMap
    actual =
        constructorConstructor.get(new TypeToken<LinkedHashMap<String, Integer>>() {}).construct();
    assertThat(actual).isInstanceOf(LinkedHashMap.class);

    // For all Map types with non-String key, should use JDK LinkedHashMap by default
    // This is also done to avoid ClassCastException later, because Gson's LinkedTreeMap requires
    // that keys are Comparable
    Class<?>[] nonStringTypes = {Integer.class, CharSequence.class, Object.class};
    for (Class<?> keyType : nonStringTypes) {
      actual =
          constructorConstructor
              .get(TypeToken.getParameterized(Map.class, keyType, Integer.class))
              .construct();
      assertWithMessage(
              "Failed for key type " + keyType + "; created instance of " + actual.getClass())
          .that(actual)
          .isInstanceOf(LinkedHashMap.class);
    }
  }

  private enum MyEnum {}

  @SuppressWarnings("serial")
  private static class CustomEnumMap<K, V> extends EnumMap<MyEnum, V> {
    @SuppressWarnings("unused")
    CustomEnumMap(Void v) {
      super(MyEnum.class);
    }
  }

  @SuppressWarnings("serial")
  private static class CustomConcurrentNavigableMap<K, V> extends ConcurrentSkipListMap<K, V> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomConcurrentNavigableMap(Void v) {}
  }

  @SuppressWarnings("serial")
  private static class CustomConcurrentMap<K, V> extends ConcurrentHashMap<K, V> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomConcurrentMap(Void v) {}
  }

  @SuppressWarnings("serial")
  private static class CustomSortedMap<K, V> extends TreeMap<K, V> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomSortedMap(Void v) {}
  }

  @SuppressWarnings("serial")
  private static class CustomLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    // Removes default no-args constructor
    @SuppressWarnings("unused")
    CustomLinkedHashMap(Void v) {}
  }

  /**
   * Tests that creation of custom {@code Map} subclasses without no-args constructor should not use
   * default JDK types (which would cause {@link ClassCastException}).
   *
   * <p>Currently this test is rather contrived because the instances created using Unsafe are not
   * usable because their fields are not properly initialized, but assume that user has custom
   * classes which would be functional.
   */
  @Test
  public void testCustomMapCreation() {
    Class<?>[] mapTypes = {
      CustomEnumMap.class,
      CustomConcurrentNavigableMap.class,
      CustomConcurrentMap.class,
      CustomSortedMap.class,
      CustomLinkedHashMap.class,
    };

    for (Class<?> mapType : mapTypes) {
      Object actual =
          constructorConstructor
              .get(TypeToken.getParameterized(mapType, String.class, Integer.class))
              .construct();
      assertWithMessage("Failed for " + mapType + "; created instance of " + actual.getClass())
          .that(actual)
          .isInstanceOf(mapType);
    }
  }

  private static interface CustomMapInterface extends Map<String, Integer> {}

  @Test
  public void testCustomMapInterfaceCreation() {
    var objectConstructor = constructorConstructor.get(TypeToken.get(CustomMapInterface.class));
    var exception = assertThrows(RuntimeException.class, () -> objectConstructor.construct());
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Interfaces can't be instantiated! Register an InstanceCreator or a TypeAdapter"
                + " for this type. Interface name: "
                + CustomMapInterface.class.getName());
  }
}
