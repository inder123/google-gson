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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;

public class Java7TypeAdapterFactoryTest {
	private final Gson gson = new GsonBuilder()
			.registerTypeAdapterFactory(Java7TypeAdapterFactory.INSTANCE)
			.registerTypeAdapter(TransferQueue.class, new InstanceCreator<TransferQueue<?>>() {
				@Override
				public TransferQueue<?> createInstance(Type type) {
					return new LinkedTransferQueue<>();
				}
			})
			.create();

	@Test
	public void testLinkedTransferQueue() {
		String json = "['a', 'b', 'c']";
		Type type = new TypeToken<LinkedTransferQueue<String>>() {}.getType();
		LinkedTransferQueue<String> queue = gson.fromJson(json, type);
		assertEquals("a", queue.poll());
		assertEquals("b", queue.poll());
		assertEquals("c", queue.poll());
	}

	@Test
	public void testTransferQueue() {
		String json = "['a', 'b', 'c']";
		Type type = new TypeToken<TransferQueue<String>>() {}.getType();
		TransferQueue<String> queue = gson.fromJson(json, type);
		assertEquals("a", queue.poll());
		assertEquals("b", queue.poll());
		assertEquals("c", queue.poll());
	}
}
