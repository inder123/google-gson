package com.google.gson.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;

public class ConstructorConstructorTest {
  private static final Map<Type, InstanceCreator<?>> NO_INSTANCE_CREATORS = Collections.emptyMap();

  private abstract static class AbstractClass {
    @SuppressWarnings("unused")
    public AbstractClass() { }
  }
  private interface Interface { }

  /**
   * Verify that ConstructorConstructor does not try to invoke no-arg constructor
   * of abstract class.
   */
  @Test
  public void testGet_AbstractClassNoArgConstructor() {
    ConstructorConstructor constructorFactory = new ConstructorConstructor(NO_INSTANCE_CREATORS);
    ObjectConstructor<AbstractClass> constructor = constructorFactory.get(TypeToken.get(AbstractClass.class));
    try {
      constructor.construct();
      fail("Expected exception");
    } catch (RuntimeException exception) {
      assertEquals(
        "Unable to invoke no-args constructor for " + AbstractClass.class
        + ". Registering an InstanceCreator with Gson for this type may fix this problem.",
        exception.getMessage()
      );
    }
  }

  @Test
  public void testGet_Interface() {
    ConstructorConstructor constructorFactory = new ConstructorConstructor(NO_INSTANCE_CREATORS);
    ObjectConstructor<Interface> constructor = constructorFactory.get(TypeToken.get(Interface.class));
    try {
      constructor.construct();
      fail("Expected exception");
    } catch (RuntimeException exception) {
      assertEquals(
        "Unable to invoke no-args constructor for " + Interface.class
        + ". Registering an InstanceCreator with Gson for this type may fix this problem.",
        exception.getMessage()
      );
    }
  }
}
