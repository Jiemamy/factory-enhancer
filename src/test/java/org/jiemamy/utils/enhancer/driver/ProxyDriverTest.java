/*
 * Copyright 2007-2009 Jiemamy Project and the Others.
 * Created on 2009/10/08
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
package org.jiemamy.utils.enhancer.driver;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.junit.Test;

import org.jiemamy.utils.enhancer.Enhance;

/**
 * Test for {@link ProxyDriver}.
 * @version $Id: ProxyDriverTest.java 3734 2009-10-08 13:05:37Z ashigeru $
 * @author Suguru ARAKAWA
 */
public class ProxyDriverTest extends DriverTestRoot {
	
	private static InvocationHandler DUMMY = new InvocationHandler() {
		
		public Object invoke(Object proxy, Method method, Object[] args) {
			throw new AssertionError();
		}
	};
	

	/**
	 * Test method for {@link ProxyDriver#newEnhance(java.lang.Class, java.lang.reflect.InvocationHandler)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNewEnhance_NotInterface() {
		ProxyDriver.newEnhance(Object.class, DUMMY);
	}
	
	/**
	 * Test method for {@link ProxyDriver#newEnhance(java.lang.Class, java.lang.reflect.InvocationHandler)}.
	 */
	@Test
	public void testNewEnhance_TargetInterface() {
		Enhance enhance = ProxyDriver.newEnhance(Runnable.class, DUMMY);
		assertTargetConstructor(true, enhance.getPointcut(),
				Runnable.class,
				Thread.class,
				"()V");
		assertTargetConstructor(true, enhance.getPointcut(),
				Runnable.class,
				Thread.class,
				"(Ljava/lang/String;)V");
	}
	
	/**
	 * Test method for {@link ProxyDriver#newEnhance(java.lang.Class, java.lang.reflect.InvocationHandler)}.
	 */
	@Test
	public void testNewEnhance_TargetSubInterface() {
		Enhance enhance = ProxyDriver.newEnhance(Runnable.class, DUMMY);
		assertTargetConstructor(false, enhance.getPointcut(),
				RunnableEx.class,
				RunnableExImpl.class,
				"()V");
	}
	
	/**
	 * Test method for {@link ProxyDriver#newEnhance(java.lang.Class, java.lang.reflect.InvocationHandler)}.
	 */
	@Test
	public void testNewEnhance_TargetMethod() {
		Enhance enhance = ProxyDriver.newEnhance(Runnable.class, DUMMY);
		assertTargetMethod(false, enhance.getPointcut(),
				Runnable.class,
				Runnable.class,
				"run",
				"()V");
	}
	
	/**
	 * Test method for {@link ProxyDriver#newEnhance(java.lang.Class, java.lang.reflect.InvocationHandler)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testNewEnhance_Handle() throws Throwable {
		Enhance enhance = ProxyDriver.newEnhance(CharSequence.class, new InvocationHandler() {
			
			public Object invoke(Object proxy, Method method, Object[] args) {
				assertThat(method.getName(), is("toString"));
				return "OK";
			}
		});
		
		Object result = construct(enhance.getHandler(),
				String.class.getConstructor());
		
		assertThat(result, instanceOf(CharSequence.class));
		assertThat(result, not(instanceOf(String.class)));
		CharSequence proxy = (CharSequence) result;
		assertThat(proxy.toString(), is("OK"));
	}
}
