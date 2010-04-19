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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Constructor;

import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.ConstructorInvocation;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

/**
 * Test for {@link AopAllianceDriver}.
 * @version $Id: AopAllianceDriverTest.java 3734 2009-10-08 13:05:37Z ashigeru $
 * @author Suguru ARAKAWA
 */
public class AopAllianceDriverTest extends DriverTestRoot {
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.MethodInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Method_Through() throws Throwable {
		MethodInterceptor itor = new MethodInterceptor() {
			
			public Object invoke(MethodInvocation invocation) throws Throwable {
				assertThat(invocation.getThis(), is((Object) "Hello"));
				assertThat(invocation.getMethod().getName(), is("charAt"));
				return invocation.proceed();
			}
		};
		Object result = invoke(
				AopAllianceDriver.toHandler(itor),
				String.class.getMethod("charAt", int.class),
				"Hello",
				0);
		assertThat(result, is((Object) 'H'));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.MethodInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Method_Static() throws Throwable {
		MethodInterceptor itor = new MethodInterceptor() {
			
			public Object invoke(MethodInvocation invocation) throws Throwable {
				assertThat(invocation.getThis(), is(nullValue()));
				return invocation.proceed();
			}
		};
		Object result = invoke(
				AopAllianceDriver.toHandler(itor),
				Math.class.getMethod("abs", int.class),
				Math.class,
				-1);
		assertThat(result, is((Object) 1));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.MethodInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Method_RewriteResult() throws Throwable {
		MethodInterceptor itor = new MethodInterceptor() {
			
			public Object invoke(MethodInvocation invocation) {
				return "World";
			}
		};
		Object result = invoke(
				AopAllianceDriver.toHandler(itor),
				String.class.getMethod("toString"),
				"Hello");
		assertThat(result, is((Object) "World"));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.MethodInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Method_RewriteArgument() throws Throwable {
		MethodInterceptor itor = new MethodInterceptor() {
			
			public Object invoke(MethodInvocation invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				assertThat(args[0], is((Object) 1));
				args[0] = 2;
				return invocation.proceed();
			}
		};
		Object result = invoke(
				AopAllianceDriver.toHandler(itor),
				String.class.getMethod("charAt", int.class),
				"Hello",
				1);
		assertThat(result, is((Object) 'l'));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.MethodInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void testToHandler_Method_Exception() throws Throwable {
		MethodInterceptor itor = new MethodInterceptor() {
			
			public Object invoke(MethodInvocation invocation) {
				throw new UnsupportedOperationException();
			}
		};
		invoke(AopAllianceDriver.toHandler(itor),
				String.class.getMethod("toString"),
				"Hello");
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.MethodInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Method_Construct() throws Throwable {
		MethodInterceptor itor = new MethodInterceptor() {
			
			public Object invoke(MethodInvocation invocation) {
				throw new UnsupportedOperationException();
			}
		};
		Object result = construct(AopAllianceDriver.toHandler(itor),
				String.class.getConstructor(String.class),
				"Hello");
		assertThat(result, is((Object) "Hello"));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.ConstructorInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Constructor_Through() throws Throwable {
		ConstructorInterceptor itor = new ConstructorInterceptor() {
			
			public Object construct(ConstructorInvocation invocation) throws Throwable {
				assertThat(invocation.getThis(), is(nullValue()));
				Constructor<?> ctor = invocation.getConstructor();
				assertThat(ctor.getParameterTypes().length, is(1));
				assertThat(ctor.getParameterTypes()[0], is((Object) String.class));
				return invocation.proceed();
			}
		};
		Object result = construct(
				AopAllianceDriver.toHandler(itor),
				String.class.getConstructor(String.class),
				"Hello");
		assertThat(result, is((Object) "Hello"));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.ConstructorInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Constructor_RewriteResult() throws Throwable {
		ConstructorInterceptor itor = new ConstructorInterceptor() {
			
			public Object construct(ConstructorInvocation invocation) {
				return new String("World".toCharArray());
			}
		};
		Object result = construct(
				AopAllianceDriver.toHandler(itor),
				String.class.getConstructor(String.class),
				"Hello");
		assertThat(result, is((Object) "World"));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.ConstructorInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Constructor_RewriteArgument() throws Throwable {
		ConstructorInterceptor itor = new ConstructorInterceptor() {
			
			public Object construct(ConstructorInvocation invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				assertThat(arguments.length, is(1));
				arguments[0] = "World";
				return invocation.proceed();
			}
		};
		Object result = construct(
				AopAllianceDriver.toHandler(itor),
				String.class.getConstructor(String.class),
				"Hello");
		assertThat(result, is((Object) "World"));
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.ConstructorInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test(expected = IllegalStateException.class)
	public void testToHandler_Constructor_Exception() throws Throwable {
		ConstructorInterceptor itor = new ConstructorInterceptor() {
			
			public Object construct(ConstructorInvocation invocation) throws Throwable {
				throw new IllegalStateException();
			}
		};
		construct(
				AopAllianceDriver.toHandler(itor),
				String.class.getConstructor(String.class),
				"Hello");
	}
	
	/**
	 * Test method for {@link AopAllianceDriver#toHandler(org.aopalliance.intercept.ConstructorInterceptor)}.
	 * @throws Throwable if occur
	 */
	@Test
	public void testToHandler_Constructor_Invoke() throws Throwable {
		ConstructorInterceptor itor = new ConstructorInterceptor() {
			
			public Object construct(ConstructorInvocation invocation) throws Throwable {
				throw new AssertionError();
			}
		};
		Object result = invoke(
				AopAllianceDriver.toHandler(itor),
				String.class.getMethod("charAt", int.class),
				"Hello",
				1);
		assertThat(result, is((Object) 'e'));
	}
}
