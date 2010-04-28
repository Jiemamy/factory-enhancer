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
package org.jiemamy.utils.enhancer.helper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

import org.jiemamy.utils.enhancer.Invocation;

/**
 * Mock {@link Invocation}.
 * @version $Date$
 * @author Suguru ARAKAWA
 */
public class MockInvocation implements Invocation {
	
	private Object[] arguments;
	

	/**
	 * インスタンスを生成する。
	 */
	public MockInvocation() {
		this(new Object[0]);
	}
	
	/**
	 * インスタンスを生成する。
	 * @param arguments 引数リスト
	 */
	public MockInvocation(Object[] arguments) {
		super();
		this.arguments = arguments;
	}
	
	/**
	 * @return コンストラクタに指定された値
	 */
	public Object[] getArguments() {
		return arguments;
	}
	
	/**
	 * @return this
	 */
	public Object getInvoker() {
		return this;
	}
	
	/**
	 * @return {@link MockInvocation#proceed()}
	 */
	public Member getTarget() {
		try {
			return getInvoker().getClass().getDeclaredMethod("proceed");
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * @throws InvocationTargetException in subclass
	 */
	public Object proceed() throws InvocationTargetException {
		return null;
	}
}
