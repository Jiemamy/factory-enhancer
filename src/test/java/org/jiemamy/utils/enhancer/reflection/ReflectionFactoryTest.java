/*
 * Copyright 2007-2009 Jiemamy Project and the Others.
 * Created on 2009/10/04
 *
 * This file is part of Jiemamy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.jiemamy.utils.enhancer.reflection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.text.DateFormat;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test for {@link ReflectionFactory}.
 * @version $Id$
 * @author Suguru ARAKAWA
 */
public class ReflectionFactoryTest {
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test
	public void testReflectionFactory() {
		ReflectionFactory<String> factory = new ReflectionFactory<String>(String.class);
		assertThat(factory.getTargetClass(), is((Object) String.class));
	}
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReflectionFactory_Interface() {
		factory(Runnable.class);
	}
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReflectionFactory_Enum() {
		factory(TimeUnit.class);
	}
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReflectionFactory_AbstractClass() {
		factory(DateFormat.class);
	}
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReflectionFactory_MemberClass() {
		factory(java.awt.geom.Point2D.Float.class);
	}
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReflectionFactory_LocalClass() {
		class Local {
			// no members
		}
		factory(Local.class);
	}
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReflectionFactory_AnonymousClass() {
		Object o = new Object() {
			// no members
		};
		factory(o.getClass());
	}
	
	/**
	 * Test method for {@link ReflectionFactory#ReflectionFactory(java.lang.Class)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReflectionFactory_PackagePrivate() {
		factory(PackagePrivateClass.class);
	}
	
	/**
	 * Test method for {@link ReflectionFactory#newInstance(java.lang.Object[])}.
	 * @throws Exception if occur
	 */
	@Test
	public void testNewInstance() throws Exception {
		ReflectionFactory<String> factory = factory(String.class);
		assertThat(factory.newInstance(), is(""));
	}
	
	/**
	 * Test method for {@link ReflectionFactory#newInstance(java.lang.Object[])}.
	 * @throws Exception if occur
	 */
	@Test
	public void testNewInstance_Arguments() throws Exception {
		ReflectionFactory<String> factory = factory(String.class);
		assertThat(factory.newInstance("Hello, world!"), is("Hello, world!"));
	}
	
	/**
	 * Test method for {@link ReflectionFactory#newInstance(java.lang.Object[])}.
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNewInstance_NotFound() throws Exception {
		ReflectionFactory<Integer> factory = factory(Integer.class);
		factory.newInstance(100L);
	}
	
	/**
	 * Test method for {@link ReflectionFactory#newInstance(java.lang.Object[])}.
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNewInstance_Ambiguous() throws Exception {
		ReflectionFactory<String> factory = factory(String.class);
		factory.newInstance(new Object[] {
			null
		});
	}
	
	/**
	 * Test method for {@link ReflectionFactory#newInstance(java.lang.Object[])}.
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNewInstance_NotPublic() throws Exception {
		ReflectionFactory<PackagePrivateConstructor> factory =
				factory(PackagePrivateConstructor.class);
		factory.newInstance();
	}
	
	private <T>ReflectionFactory<T> factory(Class<T> aClass) {
		return new ReflectionFactory<T>(aClass);
	}
}
