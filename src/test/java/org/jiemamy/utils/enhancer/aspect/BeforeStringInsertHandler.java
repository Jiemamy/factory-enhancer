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
package org.jiemamy.utils.enhancer.aspect;

import org.jiemamy.utils.enhancer.Invocation;
import org.jiemamy.utils.enhancer.InvocationHandler;

/**
 * 戻り値の先頭に任意の文字列を挿入する。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class BeforeStringInsertHandler implements InvocationHandler {
	
	private String head;
	

	/**
	 * インスタンスを生成する。
	 * @param head 先頭に挿入する文字列
	 */
	public BeforeStringInsertHandler(String head) {
		super();
		this.head = head;
	}
	
	/**
	 * @return proceed(arg[0] + arg[0])
	 */
	public Object handle(Invocation invocation) throws Throwable {
		Object[] arguments = invocation.getArguments();
		arguments[0] = head + arguments[0];
		return invocation.proceed();
	}
	
	/**
	 * @return {@code *.*(*)}
	 */
	@Override
	public String toString() {
		return '"' + head + '"' + "+arg[0]";
	}
}
