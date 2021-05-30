/*
 * Copyright (C) 2017 Gson Authors
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
import com.google.gson.TypeAdapter;
import com.google.gson.internal.GsonTypes;
import junit.framework.TestCase;

import java.io.PrintStream;
import java.lang.ref.WeakReference;

/**
 * Test fixes for infinite recursion on {@link GsonTypes#resolve(java.lang.reflect.Type, Class,
 * java.lang.reflect.Type)}, described at <a href="https://github.com/google/gson/issues/440">Issue #440</a>
 * and similar issues.
 * <p>
 * These tests originally caused {@link StackOverflowError} because of infinite recursion on attempts to
 * resolve generics on types, with an intermediate types like 'Foo2&lt;? extends ? super ? extends ... ? extends A&gt;'
 */
public class RecursiveTypesResolveTest extends TestCase {

  @SuppressWarnings("unused")
  private static class Foo1<A> {
	public Foo2<? extends A> foo2;
  }
  @SuppressWarnings("unused")
  private static class Foo2<B> {
    public Foo1<? super B> foo1;
  }

  /**
   * Test simplest case of recursion.
   */

  public void testRecursiveResolveSimple() {
    TypeAdapter<Foo1> adapter = new Gson().getAdapter(Foo1.class);
    assertNotNull(adapter);
  }

  /**
   * Real-world samples, found in Issues #603 and #440.
   */

  public void testIssue603PrintStream() {
    TypeAdapter<PrintStream> adapter = new Gson().getAdapter(PrintStream.class);
    assertNotNull(adapter);
  }

  public void testIssue440WeakReference() throws Exception {
    TypeAdapter<WeakReference> adapter = new Gson().getAdapter(WeakReference.class);
    assertNotNull(adapter);
  }

  /**
   * Tests belows check the behaviour of the methods changed for the fix.
   */

  public void testDoubleSupertype() {
    assertEquals(GsonTypes.supertypeOf(Number.class),
            GsonTypes.supertypeOf(GsonTypes.supertypeOf(Number.class)));
  }

  public void testDoubleSubtype() {
    assertEquals(GsonTypes.subtypeOf(Number.class),
            GsonTypes.subtypeOf(GsonTypes.subtypeOf(Number.class)));
  }

  public void testSuperSubtype() {
    assertEquals(GsonTypes.subtypeOf(Object.class),
            GsonTypes.supertypeOf(GsonTypes.subtypeOf(Number.class)));
  }

  public void testSubSupertype() {
    assertEquals(GsonTypes.subtypeOf(Object.class),
            GsonTypes.subtypeOf(GsonTypes.supertypeOf(Number.class)));
  }

  /**
   * Tests for recursion while resolving type variables.
   */

  @SuppressWarnings("unused")
  private static class TestType<X> {
    TestType<? super X> superType;
  }

  @SuppressWarnings("unused")
  private static class TestType2<X, Y> {
    TestType2<? super Y, ? super X> superReversedType;
  }

  public void testRecursiveTypeVariablesResolve1() throws Exception {
    TypeAdapter<TestType> adapter = new Gson().getAdapter(TestType.class);
    assertNotNull(adapter);
  }

  public void testRecursiveTypeVariablesResolve12() throws Exception {
    TypeAdapter<TestType2> adapter = new Gson().getAdapter(TestType2.class);
    assertNotNull(adapter);
  }
}
