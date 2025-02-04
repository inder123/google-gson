/*
 * Copyright (C) 2018 Gson Authors
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
package com.google.gson.ext;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class Java7TypeAdapterFactory implements TypeAdapterFactory {
	public static GsonBuilder install(GsonBuilder builder) {
		return builder;
	}

	public static final Java7TypeAdapterFactory INSTANCE = new Java7TypeAdapterFactory();

	private final Map<Class<?>, TypeAdapter<?>> adapters = buildAdapterMap();
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		Class<? super T> rawType = type.getRawType();
		return (TypeAdapter<T>) adapters.get(rawType);
	}


	private Map<Class<?>, TypeAdapter<?>> buildAdapterMap() {
		Map<Class<?>, TypeAdapter<?>> map = new HashMap<>();
		return map;
	}

	private Java7TypeAdapterFactory() {}
}
