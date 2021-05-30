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
import com.google.gson.reflect.TypeToken;

import junit.framework.TestCase;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Unit tests for the {@code GenericArrayType}s created by the {@link GsonTypes} class.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class GenericArrayTypeTest extends TestCase {
  private GenericArrayType ourType;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ourType = GsonTypes.arrayOf(GsonTypes.newParameterizedTypeWithOwner(null, List.class, String.class));
  }

  public void testOurTypeFunctionality() throws Exception {
    Type parameterizedType = new TypeToken<List<String>>() {}.getType();
    Type genericArrayType = new TypeToken<List<String>[]>() {}.getType();

    assertEquals(parameterizedType, ourType.getGenericComponentType());
    assertEquals(genericArrayType, ourType);
    assertEquals(genericArrayType.hashCode(), ourType.hashCode());
  }

  public void testNotEquals() throws Exception {
    Type differentGenericArrayType = new TypeToken<List<String>[][]>() {}.getType();
    assertFalse(differentGenericArrayType.equals(ourType));
    assertFalse(ourType.equals(differentGenericArrayType));
  }
}
