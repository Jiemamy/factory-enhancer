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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.jiemamy.utils.enhancer.reflection.ReflectionUtil.findConstructor;
import static org.jiemamy.utils.enhancer.reflection.ReflectionUtil.findMethod;
import static org.jiemamy.utils.enhancer.reflection.ReflectionUtil.isNormalClass;
import static org.jiemamy.utils.enhancer.reflection.ReflectionUtil.isPackageMember;
import static org.jiemamy.utils.enhancer.reflection.ReflectionUtil.toParameterTypes;
import static org.junit.Assert.assertThat;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test for {@link ReflectionUtil}.
 * @version $Id: ReflectionUtilTest.java 3734 2009-10-08 13:05:37Z ashigeru $
 * @author Suguru ARAKAWA
 */
public class ReflectionUtilTest {
	
	/**
	 * Test method for {@link ReflectionUtil#isNormalClass(java.lang.Class)}.
	 */
	@Test
	public void testIsNormalClass() {
		assertThat(isNormalClass(Object.class), is(true));
		assertThat(isNormalClass(List.class), is(false));
		assertThat(isNormalClass(TimeUnit.class), is(false));
		assertThat(isNormalClass(Override.class), is(false));
		assertThat(isNormalClass(Object[].class), is(false));
		assertThat(isNormalClass(int.class), is(false));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#isPackageMember(java.lang.Class)}.
	 */
	@Test
	public void testIsPackageMember() {
		class Local {
			// no members
		}
		Object anonymousObject = new Object() {
			// no members
		};
		
		Class<?> local = Local.class;
		Class<?> anonymous = anonymousObject.getClass();
		
		assertThat(isPackageMember(Object.class), is(true));
		assertThat(isPackageMember(List.class), is(true));
		assertThat(isPackageMember(TimeUnit.class), is(true));
		assertThat(isPackageMember(Override.class), is(true));
		
		assertThat(isPackageMember(Map.Entry.class), is(false));
		assertThat(isPackageMember(local), is(false));
		assertThat(isPackageMember(anonymous), is(false));
		assertThat(isPackageMember(int.class), is(false));
		assertThat(isPackageMember(String[].class), is(false));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#toParameterTypes(java.lang.Object[])}.
	 */
	@Test
	public void testToParameterTypes_Empty() {
		assertThat(toParameterTypes(),
				is(classes()));
		assertThat(toParameterTypes(1),
				is(classes(Integer.class)));
		assertThat(toParameterTypes("Hello"),
				is(classes(String.class)));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#toParameterTypes(java.lang.Object[])}.
	 */
	@Test
	public void testToParameterTypes_Type() {
		assertThat(toParameterTypes(1),
				is(classes(Integer.class)));
		assertThat(toParameterTypes("Hello"),
				is(classes(String.class)));
		assertThat(toParameterTypes(new Object[] {
			new String[0]
		}), is(classes(String[].class)));
		assertThat(toParameterTypes(new Object[] {
			null
		}), is(classes(new Class<?>[] {
			null
		})));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#toParameterTypes(java.lang.Object[])}.
	 */
	@Test
	public void testToParameterTypes_Many() {
		assertThat(toParameterTypes("Hello", 1, new ArrayList<Object>(), null),
				is(classes(String.class, Integer.class, ArrayList.class, null)));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findMethod(java.util.Collection, java.lang.String, java.util.List)}.
	 */
	@Test
	public void testFindMethod_nameIdentified() {
		Collection<Method> methods = findMethod(
				methods(Object.class), "hashCode", classes());
		assertJust(methods, method(Object.class, "hashCode"));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findMethod(java.util.Collection, java.lang.String, java.util.List)}.
	 */
	@Test
	public void testFindMethod_overloadWithSinglePotentially() {
		Collection<Method> methods = findMethod(
				methods(Object.class), "wait", classes());
		assertJust(methods, method(Object.class, "wait"));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findMethod(java.util.Collection, java.lang.String, java.util.List)}.
	 */
	@Test
	public void testFindMethod_overloadWithMultiPotentially() {
		Collection<Method> methods = findMethod(
				methods(String.class), "valueOf", classes(char[].class));
		assertJust(methods, method(String.class, "valueOf", char[].class));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findMethod(java.util.Collection, java.lang.String, java.util.List)}.
	 */
	@Test
	public void testFindMethod_overloadWithPrimitive() {
		Collection<Method> methods = findMethod(
				methods(String.class), "valueOf", classes(int.class));
		assertJust(methods, method(String.class, "valueOf", int.class));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findMethod(java.util.Collection, java.lang.String, java.util.List)}.
	 */
	@Test
	public void testFindMethod_overloadWithWrapper() {
		Collection<Method> methods = findMethod(
				methods(String.class), "valueOf", classes(Integer.class));
		
		// Java言語仕様との差異
		// 通常はサブタイピング変換が優先されるが、現在の仕様ではプリミティブが常に勝つ
		assertJust(methods, method(String.class, "valueOf", int.class));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findMethod(java.util.Collection, java.lang.String, java.util.List)}.
	 */
	@Test
	public void testFindMethod_overloadWithAmbiguous() {
		Collection<Method> methods = findMethod(
				methods(PrintWriter.class), "print",
				classes(new Class<?>[] {
			null
		}));
		
		// PrintWriter.valueOf(null) -> char[] / String / ...
		// 現在の仕様ではほぼすべてのメソッドが合致するはず
		assertThat(methods.size(), greaterThan(1));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findConstructor(java.util.Collection, java.util.List)}.
	 */
	@Test
	public void testFindConstructor_WithSinglePotentially() {
		Collection<Constructor<String>> ctors = findConstructor(
				ctors(String.class), classes());
		
		assertJust(ctors, ctor(String.class));
	}
	
	/**
	 * Test method for {@link ReflectionUtil#findConstructor(java.util.Collection, java.util.List)}.
	 */
	@Test
	public void testFindConstructor_MultiPotentially() {
		Collection<Constructor<Integer>> ctors = findConstructor(
				ctors(Integer.class), classes(String.class));
		
		assertJust(ctors, ctor(Integer.class, String.class));
	}
	
	private static List<Method> methods(Class<?> aClass) {
		return Arrays.asList(aClass.getMethods());
	}
	
	private static <T>List<Constructor<T>> ctors(Class<T> aClass) {
		List<Constructor<T>> results = new ArrayList<Constructor<T>>();
		for (Constructor<?> c : aClass.getConstructors()) {
			@SuppressWarnings("unchecked")
			Constructor<T> assume = (Constructor<T>) c;
			results.add(assume);
		}
		return results;
	}
	
	private List<Class<?>> classes(Class<?>... classes) {
		return Arrays.asList(classes);
	}
	
	private void assertJust(Collection<?> candidates, Object just) {
		assertThat(candidates.toString(), candidates.size(), is(1));
		assertThat(new ArrayList<Object>(candidates),
				is((Object) Arrays.asList(just)));
	}
	
	private <T>Constructor<T> ctor(Class<T> declaring, Class<?>... parameters) {
		try {
			return declaring.getDeclaredConstructor(parameters);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	private Method method(Class<?> declaring, String name, Class<?>... parameters) {
		try {
			return declaring.getDeclaredMethod(name, parameters);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
