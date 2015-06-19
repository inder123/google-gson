/*
 * Copyright (C) 2015 Google Inc.
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

import java.io.IOException;

import com.google.gson.stream.JsonReader;

/**
 * An enumeration that defines a few standard handling strategies for fields
 * present in the incoming Json but not in the Java classes.
 * This enumeration should be used in conjunction with {@link com.google.gson.GsonBuilder}
 * to configure a {@link com.google.gson.Gson} instance
 *
 * @author Matteo Cerina
 * @since 2.3.2
 */
public enum UnknownFieldHandlingPolicy implements UnknownFieldHandlingStrategy {

  /**
   * Using this handling policy with Gson will silently ignore an unknown field.
   */
  IGNORE() {
    public void handleUnknownField(JsonReader in, Object instance, String name) throws IOException {
        in.skipValue();
    }
  },

  /**
   * Using this handling policy with Gson will throw an exception when an unknown field
   * is present.
   */
  THROW_EXCEPTION() {
    public void handleUnknownField(JsonReader in, Object instance, String name) throws IOException {
        String msg = String.format("Unrecognized field \"%s\" (Class %s)", name, instance.getClass().getName());
        throw new JsonParseException(msg);
    }
  }
}