/*
 * Copyright 2007-2009 Jiemamy Project and the Others.
 * Created on 2009/03/26
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
package org.jiemamy.utils.enhancer.helper;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.Callable;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import org.junit.Test;

/**
 * Test for {@link JavassistConverter}.
 * @author Suguru ARAKAWA
 */
public class JavassistConverterTest {
	
	/**
	 * Test method for {@link JavassistConverter#JavassistConverter(java.lang.Class)}.
	 * @throws Exception if occur
	 */
	@Test
	public void testJavassistConverter() throws Exception {
		JavassistConverter converter = new JavassistConverter(getClass());
		ClassPool pool = converter.getClassPool();
		CtClass example1 = pool.makeClass("Example1");
		CtClass example2 = pool.makeClass("Example2");
		
		Class<?> created1 = converter.toClass(example1);
		Class<?> created2 = converter.toClass(example2);
		
		assertThat(created1.getName(), is("Example1"));
		assertThat(created2.getName(), is("Example2"));
		
		Class<?> self = Class.forName(getClass().getName(), false, created1.getClassLoader());
		assertThat("uses parent class loader", self, sameInstance((Object) getClass()));
		
		assertThat(created1.getClassLoader(), not(sameInstance(getClass().getClassLoader())));
		assertThat(created1.getClassLoader(), sameInstance(created2.getClassLoader()));
		
		JavassistConverter another = new JavassistConverter(getClass());
		CtClass example3 = another.getClassPool().makeClass("Example1");
		Class<?> created3 = another.toClass(example3);
		assertThat(created3.getName(), is("Example1"));
		assertThat(created3, is(not((Object) created1)));
	}
	
	/**
	 * Test method for {@link JavassistConverter#loadCtClass(Class)}.
	 * @throws Exception if occur
	 */
	@Test
	public void testLoadCtClass() throws Exception {
		JavassistConverter converter = new JavassistConverter(getClass());
		CtClass example = converter.getClassPool().makeClass("Example");
		Class<?> exampleClass = converter.toClass(example);
		
		CtClass object = converter.loadCtClass(Object.class);
		CtClass self = converter.loadCtClass(getClass());
		CtClass example2 = converter.loadCtClass(exampleClass);
		
		assertThat(object.getName(), is("java.lang.Object"));
		assertThat(self.getName(), is(getClass().getName()));
		assertThat(example2.getName(), is("Example"));
		
		try {
			converter.loadCtClass(int.class);
			fail("primitive type");
		} catch (IllegalArgumentException e) {
			// ok.
		}
		try {
			converter.loadCtClass(Object[].class);
			fail("array type");
		} catch (IllegalArgumentException e) {
			// ok.
		}
		try {
			converter.loadCtClass(Map.Entry.class);
			fail("member type");
		} catch (IllegalArgumentException e) {
			// ok.
		}
		try {
			class Local {
				// no members
			}
			converter.loadCtClass(Local.class);
			fail("local class");
		} catch (IllegalArgumentException e) {
			// ok.
		}
		try {
			converter.loadCtClass(new Object[] {}.getClass());
			fail("anonymous class");
		} catch (IllegalArgumentException e) {
			// ok.
		}
	}
	
	/**
	 * Test method for {@link JavassistConverter#toClass(javassist.CtClass)}.
	 * @throws Exception if occur
	 */
	@Test
	public void testToClass_Array() throws Exception {
		JavassistConverter converter = new JavassistConverter(getClass());
		ClassPool pool = converter.getClassPool();
		
		pool.makeClass("Example");
		CtClass a = pool.get("Example[]");
		CtClass aa = pool.get("Example[][]");
		
		Class<?> array = converter.toClass(a);
		Class<?> arrayarray = converter.toClass(aa);
		
		assertThat(array.isArray(), is(true));
		Class<?> component = array.getComponentType();
		assertThat(component.isArray(), is(false));
		assertThat(component.getName(), is("Example"));
		
		assertThat(arrayarray.isArray(), is(true));
		assertThat(arrayarray.getComponentType(), sameInstance((Object) array));
	}
	
	/**
	 * Test method for {@link JavassistConverter#toClass(javassist.CtClass)}.
	 * @throws Exception if occur
	 */
	@Test
	public void testToClass_Class() throws Exception {
		JavassistConverter converter = new JavassistConverter(getClass());
		ClassPool pool = converter.getClassPool();
		
		CtClass example = pool.makeClass("Example");
		example.addInterface(pool.get("java.util.concurrent.Callable"));
		
		CtConstructor ctor = new CtConstructor(new CtClass[0], example);
		ctor.setBody("super();");
		example.addConstructor(ctor);
		
		CtMethod call = new CtMethod(pool.get("java.lang.Object"), "call", new CtClass[0], example);
		call.setBody("return \"TEST\";");
		example.addMethod(call);
		
		Class<?> klass = converter.toClass(example);
		assertThat(example.isFrozen(), is(true));
		Object instance = klass.newInstance();
		assertThat(instance, instanceOf(Callable.class));
		Object result = ((Callable<?>) instance).call();
		assertThat(result, is((Object) "TEST"));
		
		Class<?> replay = converter.toClass(example);
		assertThat(replay, sameInstance((Object) klass));
	}
	
	/**
	 * Test method for {@link JavassistConverter#toClass(javassist.CtClass)}.
	 * @throws Exception if occur
	 */
	@Test
	public void testToClass_Primitive() throws Exception {
		JavassistConverter converter = new JavassistConverter(getClass());
		assertThat(converter.toClass(CtClass.voidType), is((Object) void.class));
		assertThat(converter.toClass(CtClass.intType), is((Object) int.class));
		assertThat(converter.toClass(CtClass.longType), is((Object) long.class));
		assertThat(converter.toClass(CtClass.floatType), is((Object) float.class));
		assertThat(converter.toClass(CtClass.doubleType), is((Object) double.class));
		assertThat(converter.toClass(CtClass.byteType), is((Object) byte.class));
		assertThat(converter.toClass(CtClass.shortType), is((Object) short.class));
		assertThat(converter.toClass(CtClass.charType), is((Object) char.class));
		assertThat(converter.toClass(CtClass.booleanType), is((Object) boolean.class));
	}
	
	/**
	 * Test method for {@link JavassistConverter#toClass(javassist.CtClass)}.
	 * @throws Exception if occur
	 */
	@Test
	public void testToClass_PrimitiveArray() throws Exception {
		JavassistConverter converter = new JavassistConverter(getClass());
		CtClass aInt = converter.getClassPool().get("[I");
		CtClass aaBoolean = converter.getClassPool().get("[[Z");
		
		assertThat(converter.toClass(aInt), is((Object) int[].class));
		assertThat(converter.toClass(aaBoolean), is((Object) boolean[][].class));
	}
	
	/**
	 * Test method for {@link JavassistConverter#toClasses(javassist.CtClass[])}.
	 * @throws Exception if occur
	 */
	@Test
	public void testToClasses() throws Exception {
		JavassistConverter converter = new JavassistConverter(getClass());
		ClassPool pool = converter.getClassPool();
		CtClass example = pool.makeClass("Example");
		CtClass[] classes = {
			CtClass.voidType,
			pool.get("java.lang.Object"),
			example,
			pool.get("[[I"),
		};
		Class<?>[] results = converter.toClasses(classes);
		assertThat(results.length, is(4));
		assertThat(results[0], is((Object) void.class));
		assertThat(results[1], is((Object) Object.class));
		assertThat(results[2].getName(), is("Example"));
		assertThat(results[3], is((Object) int[][].class));
		assertThat(example.isFrozen(), is(true));
	}
}
