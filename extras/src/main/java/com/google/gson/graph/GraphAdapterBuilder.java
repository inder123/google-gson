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

package com.google.gson.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;

/**
 * A builder for constructing a graph-aware type adapter. This class allows you to register types
 * for which cyclic references are allowed by serializing the graph of objects as a list of named
 * nodes. This approach ensures that objects referencing each other (or themselves) are properly
 * serialized and deserialized.
 *
 * <p>The builder maintains a mapping between types and their corresponding {@link InstanceCreator}
 * instances. When a type is registered, it will be serialized using a graph adapter that assigns a
 * unique identifier to each object instance. During deserialization, the graph adapter first builds
 * a mapping from these identifiers to their JSON representations and then reconstructs the object
 * graph.
 *
 * <p>Example usage:
 *
 * <pre>
 *   GraphAdapterBuilder graphBuilder = new GraphAdapterBuilder();
 *   graphBuilder.addType(MyClass.class);
 *
 *   GsonBuilder gsonBuilder = new GsonBuilder();
 *   graphBuilder.registerOn(gsonBuilder);
 *   Gson gson = gsonBuilder.create();
 *
 *   // Serialization
 *   String json = gson.toJson(myObject);
 *
 *   // Deserialization
 *   MyClass deserialized = gson.fromJson(json, MyClass.class);
 * </pre>
 *
 * @see Gson
 * @see GsonBuilder
 */
public final class GraphAdapterBuilder {
  private final Map<Type, InstanceCreator<?>> instanceCreators;
  private final ConstructorConstructor constructorConstructor;

  public GraphAdapterBuilder() {
    this.instanceCreators = new HashMap<>();
    this.constructorConstructor =
        new ConstructorConstructor(
            instanceCreators, true, Collections.<ReflectionAccessFilter>emptyList());
  }

  /**
   * Registers the specified type with a default instance creator.
   *
   * @param type the type to register
   * @return this builder instance for chaining
   */
  public GraphAdapterBuilder addType(Type type) {
    ObjectConstructor<?> objectConstructor = constructorConstructor.get(TypeToken.get(type));
    InstanceCreator<Object> instanceCreator =
        new InstanceCreator<Object>() {
          @Override
          public Object createInstance(Type type) {
            return objectConstructor.construct();
          }
        };
    return addType(type, instanceCreator);
  }

  /**
   * Registers the specified type with the provided instance creator.
   *
   * @param type the type to register
   * @param instanceCreator the instance creator used to create instances of the type during
   *     deserialization
   * @return this builder instance for chaining
   */
  public GraphAdapterBuilder addType(Type type, InstanceCreator<?> instanceCreator) {
    if (type == null || instanceCreator == null) {
      throw new NullPointerException();
    }
    instanceCreators.put(type, instanceCreator);
    return this;
  }

  /**
   * Registers the graph adapter on the provided {@link GsonBuilder}. This method adds a {@link
   * TypeAdapterFactory} and registers the necessary type adapters for all types previously
   * registered via {@link #addType(Type)}.
   *
   * @param gsonBuilder the {@code GsonBuilder} on which to register the graph adapter
   */
  public void registerOn(GsonBuilder gsonBuilder) {
    Factory factory = new Factory(instanceCreators);
    gsonBuilder.registerTypeAdapterFactory(factory);
    for (Map.Entry<Type, InstanceCreator<?>> entry : instanceCreators.entrySet()) {
      gsonBuilder.registerTypeAdapter(entry.getKey(), factory);
    }
  }

  /**
   * A factory that creates type adapters capable of serializing and deserializing object graphs.
   *
   * <p>This factory implements both {@link TypeAdapterFactory} and {@link InstanceCreator}
   * interfaces. It is responsible for handling cyclic references by assigning unique names to
   * objects and managing a graph during both serialization and deserialization.
   */
  static class Factory implements TypeAdapterFactory, InstanceCreator<Object> {
    private final Map<Type, InstanceCreator<?>> instanceCreators;

    @SuppressWarnings("ThreadLocalUsage")
    private final ThreadLocal<Graph> graphThreadLocal = new ThreadLocal<>();

    Factory(Map<Type, InstanceCreator<?>> instanceCreators) {
      this.instanceCreators = instanceCreators;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (!instanceCreators.containsKey(type.getType())) {
        return null;
      }

      TypeAdapter<T> typeAdapter = gson.getDelegateAdapter(this, type);
      TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
      return new TypeAdapter<T>() {
        @Override
        public void write(JsonWriter out, T value) throws IOException {
          if (value == null) {
            out.nullValue();
            return;
          }

          Graph graph = graphThreadLocal.get();
          boolean writeEntireGraph = false;

          /*
           * We have one of two cases:
           *  1. We've encountered the first known object in this graph. Write
           *     out the graph, starting with that object.
           *  2. We've encountered another graph object in the course of #1.
           *     Just write out this object's name. We'll circle back to writing
           *     out the object's value as a part of #1.
           */

          if (graph == null) {
            writeEntireGraph = true;
            graph = new Graph(new IdentityHashMap<Object, Element<?>>());
          }

          @SuppressWarnings("unchecked") // graph.map guarantees consistency between value and T
          Element<T> element = (Element<T>) graph.map.get(value);
          if (element == null) {
            element = new Element<>(value, graph.nextName(), typeAdapter, null);
            graph.map.put(value, element);
            graph.queue.add(element);
          }

          if (writeEntireGraph) {
            graphThreadLocal.set(graph);
            try {
              out.beginObject();
              Element<?> current;
              while ((current = graph.queue.poll()) != null) {
                out.name(current.id);
                current.write(out);
              }
              out.endObject();
            } finally {
              graphThreadLocal.remove();
            }
          } else {
            out.value(element.id);
          }
        }

        @Override
        public T read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }

          /*
           * Again we have one of two cases:
           *  1. We've encountered the first known object in this graph. Read
           *     the entire graph in as a map from names to their JsonElements.
           *     Then convert the first JsonElement to its Java object.
           *  2. We've encountered another graph object in the course of #1.
           *     Read in its name, then deserialize its value from the
           *     JsonElement in our map. We need to do this lazily because we
           *     don't know which TypeAdapter to use until a value is
           *     encountered in the wild.
           */

          String currentName = null;
          Graph graph = graphThreadLocal.get();
          boolean readEntireGraph = false;

          if (graph == null) {
            graph = new Graph(new HashMap<Object, Element<?>>());
            readEntireGraph = true;

            // read the entire tree into memory
            in.beginObject();
            while (in.hasNext()) {
              String name = in.nextName();
              if (currentName == null) {
                currentName = name;
              }
              JsonElement element = elementAdapter.read(in);
              graph.map.put(name, new Element<>(null, name, typeAdapter, element));
            }
            in.endObject();
          } else {
            currentName = in.nextString();
          }

          if (readEntireGraph) {
            graphThreadLocal.set(graph);
          }
          try {
            @SuppressWarnings("unchecked") // graph.map guarantees consistency between value and T
            Element<T> element = (Element<T>) graph.map.get(currentName);
            // now that we know the typeAdapter for this name, go from JsonElement to 'T'
            if (element.value == null) {
              element.typeAdapter = typeAdapter;
              element.read(graph);
            }
            return element.value;
          } finally {
            if (readEntireGraph) {
              graphThreadLocal.remove();
            }
          }
        }
      };
    }

    /**
     * Hook for the graph adapter to get a reference to a deserialized value before that value is
     * fully populated. This is useful to deserialize values that directly or indirectly reference
     * themselves: we can hand out an instance before read() returns.
     *
     * <p>Gson should only ever call this method when we're expecting it to; that is only when we've
     * called back into Gson to deserialize a tree.
     */
    @Override
    public Object createInstance(Type type) {
      Graph graph = graphThreadLocal.get();
      if (graph == null || graph.nextCreate == null) {
        throw new IllegalStateException("Unexpected call to createInstance() for " + type);
      }
      InstanceCreator<?> creator = instanceCreators.get(type);
      Object result = creator.createInstance(type);
      graph.nextCreate.value = result;
      graph.nextCreate = null;
      return result;
    }
  }

  static class Graph {
    /**
     * The graph elements. On serialization keys are objects (using an identity hash map) and on
     * deserialization keys are the string names (using a standard hash map).
     */
    private final Map<Object, Element<?>> map;

    /** The queue of elements to write during serialization. Unused during deserialization. */
    private final Queue<Element<?>> queue = new ArrayDeque<>();

    /**
     * The instance currently being deserialized. Used as a backdoor between the graph traversal
     * (which needs to know instances) and instance creators which create them.
     */
    private Element<Object> nextCreate;

    private Graph(Map<Object, Element<?>> map) {
      this.map = map;
    }

    /** Returns a unique name for an element to be inserted into the graph. */
    public String nextName() {
      return "0x" + Integer.toHexString(map.size() + 1);
    }
  }

  /** An element of the graph during serialization or deserialization. */
  static class Element<T> {
    /** This element's name in the top level graph object. */
    private final String id;

    /** The value if known. During deserialization this is lazily populated. */
    private T value;

    /** This element's type adapter if known. During deserialization this is lazily populated. */
    private TypeAdapter<T> typeAdapter;

    /** The element to deserialize. Unused in serialization. */
    private final JsonElement element;

    Element(T value, String id, TypeAdapter<T> typeAdapter, JsonElement element) {
      this.value = value;
      this.id = id;
      this.typeAdapter = typeAdapter;
      this.element = element;
    }

    void write(JsonWriter out) throws IOException {
      typeAdapter.write(out, value);
    }

    @SuppressWarnings("unchecked")
    void read(Graph graph) {
      if (graph.nextCreate != null) {
        throw new IllegalStateException("Unexpected recursive call to read() for " + id);
      }
      graph.nextCreate = (Element<Object>) this;
      value = typeAdapter.fromJsonTree(element);
      if (value == null) {
        throw new IllegalStateException("non-null value deserialized to null: " + element);
      }
      if (graph.nextCreate != null) {
        graph.nextCreate.value = value;
        graph.nextCreate = null;
      }
    }
  }
}
