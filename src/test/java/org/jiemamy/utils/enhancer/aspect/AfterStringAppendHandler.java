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
 * 戻り値に文字列を追記する。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class AfterStringAppendHandler implements InvocationHandler {
	
	private String tail;
	

	/**
	 * インスタンスを生成する。
	 * @param tail 末尾に追加する文字列
	 */
	public AfterStringAppendHandler(String tail) {
		super();
		this.tail = tail;
	}
	
	/**
	 * @return proceed() + {@code <tail>}
	 */
	public Object handle(Invocation invocation) throws Throwable {
		String result = (String) invocation.proceed();
		return result + tail;
	}
	
	/**
	 * @return {@code return+<tail>}
	 */
	@Override
	public String toString() {
		return "return+\"" + tail + "\"";
	}
}
