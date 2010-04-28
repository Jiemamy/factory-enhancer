/*
 * Copyright 2009 Jiemamy Project and the Others.
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
package org.jiemamy.utils.enhancer;

import static org.hamcrest.Matchers.is;
import static org.jiemamy.utils.enhancer.Pointcuts.FALSE;
import static org.jiemamy.utils.enhancer.Pointcuts.TRUE;
import static org.jiemamy.utils.enhancer.Pointcuts.and;
import static org.jiemamy.utils.enhancer.Pointcuts.not;
import static org.jiemamy.utils.enhancer.Pointcuts.or;
import static org.junit.Assert.assertThat;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link Pointcuts}.
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class PointcutsTest {
	
	/**
	 * Test method for {@link Pointcuts#TRUE}.
	 */
	@Test
	public void testTrue() {
		assertThat(Pointcuts.TRUE.isTarget(object(), tostring()), is(true));
		assertThat(Pointcuts.TRUE.isTarget(object(), constructor()), is(true));
	}
	
	/**
	 * Test method for {@link Pointcuts#FALSE}.
	 */
	@Test
	public void testFalse() {
		assertThat(Pointcuts.FALSE.isTarget(object(), tostring()), is(false));
		assertThat(Pointcuts.FALSE.isTarget(object(), constructor()), is(false));
	}
	
	/**
	 * Test method for {@link Pointcuts#METHODS}.
	 */
	@Test
	public void testMethods() {
		assertThat(Pointcuts.METHODS.isTarget(object(), tostring()), is(true));
		assertThat(Pointcuts.METHODS.isTarget(object(), constructor()), is(false));
	}
	
	/**
	 * Test method for {@link Pointcuts#CONSTRUCTORS}.
	 */
	@Test
	public void testConstructors() {
		assertThat(Pointcuts.CONSTRUCTORS.isTarget(object(), tostring()), is(false));
		assertThat(Pointcuts.CONSTRUCTORS.isTarget(object(), constructor()), is(true));
	}
	
	/**
	 * Test method for {@link Pointcuts#and(InvocationPointcut, InvocationPointcut)}.
	 */
	@Test
	public void testAnd() {
		assertThat(and(TRUE, TRUE).isTarget(object(), constructor()), is(true));
		assertThat(and(FALSE, FALSE).isTarget(object(), constructor()), is(false));
		assertThat(and(TRUE, FALSE).isTarget(object(), constructor()), is(false));
		assertThat(and(FALSE, TRUE).isTarget(object(), constructor()), is(false));
	}
	
	/**
	 * Test method for {@link Pointcuts#and(InvocationPointcut, InvocationPointcut, InvocationPointcut[])}.
	 */
	@Test
	public void testAndMany() {
		assertThat(and(TRUE, TRUE, TRUE, TRUE, TRUE).isTarget(object(), constructor()), is(true));
		assertThat(and(FALSE, TRUE, TRUE, TRUE, TRUE).isTarget(object(), constructor()), is(false));
		assertThat(and(TRUE, TRUE, FALSE, TRUE, TRUE).isTarget(object(), constructor()), is(false));
		assertThat(and(TRUE, TRUE, TRUE, TRUE, FALSE).isTarget(object(), constructor()), is(false));
		assertThat(and(FALSE, FALSE, FALSE, FALSE, FALSE).isTarget(object(), constructor()), is(false));
	}
	
	/**
	 * Test method for {@link Pointcuts#or(InvocationPointcut, InvocationPointcut)}.
	 */
	@Test
	public void testOr() {
		assertThat(or(TRUE, TRUE).isTarget(object(), constructor()), is(true));
		assertThat(or(FALSE, FALSE).isTarget(object(), constructor()), is(false));
		assertThat(or(TRUE, FALSE).isTarget(object(), constructor()), is(true));
		assertThat(or(FALSE, TRUE).isTarget(object(), constructor()), is(true));
	}
	
	/**
	 * Test method for {@link Pointcuts#or(InvocationPointcut, InvocationPointcut, InvocationPointcut[])}.
	 */
	@Test
	public void testOrMany() {
		assertThat(or(FALSE, FALSE, FALSE, FALSE, FALSE).isTarget(object(), constructor()), is(false));
		assertThat(or(TRUE, FALSE, FALSE, FALSE, FALSE).isTarget(object(), constructor()), is(true));
		assertThat(or(FALSE, FALSE, TRUE, FALSE, FALSE).isTarget(object(), constructor()), is(true));
		assertThat(or(FALSE, FALSE, FALSE, FALSE, TRUE).isTarget(object(), constructor()), is(true));
		assertThat(or(TRUE, TRUE, TRUE, TRUE, TRUE).isTarget(object(), constructor()), is(true));
	}
	
	/**
	 * Test method for {@link Pointcuts#not(InvocationPointcut)}.
	 */
	@Test
	public void testNot() {
		assertThat(not(TRUE).isTarget(object(), constructor()), is(false));
		assertThat(not(FALSE).isTarget(object(), constructor()), is(true));
	}
	

	private ClassPool pool;
	

	/**
	 * Initializes the test.
	 * @throws Exception if some errors were occurred
	 */
	@Before
	public void setUp() throws Exception {
		pool = new ClassPool();
		pool.appendClassPath(new ClassClassPath(getClass()));
	}
	
	/**
	 * Cleans up the test.
	 * @throws Exception if some errors were occurred
	 */
	@After
	public void tearDown() throws Exception {
		// do nothing
	}
	
	/**
	 * 指定のクラスをJavassistでロードする。
	 * @param klass 対象のクラス
	 * @return ロードしたクラス
	 */
	public CtClass load(Class<?> klass) {
		try {
			return pool.get(klass.getName());
		} catch (NotFoundException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * {@link Object}クラスを返す。
	 * @return {@link Object}クラス
	 */
	public CtClass object() {
		return load(Object.class);
	}
	
	/**
	 * {@link Object#Object()}を返す。
	 * @return {@link Object#Object()}
	 */
	public CtBehavior constructor() {
		CtClass object = object();
		try {
			return object.getDeclaredConstructor(new CtClass[0]);
		} catch (NotFoundException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * {@link Object#toString()}を返す。
	 * @return {@link Object#toString()}
	 */
	public CtBehavior tostring() {
		CtClass object = object();
		try {
			return object.getDeclaredMethod("toString");
		} catch (NotFoundException e) {
			throw new AssertionError(e);
		}
	}
}
