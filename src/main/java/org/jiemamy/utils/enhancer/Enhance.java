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

import java.text.MessageFormat;

/**
 * エンハンサが拡張する単位を表す。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class Enhance {
	
	private final InvocationPointcut pointcut;
	
	private final InvocationHandler handler;
	

	/**
	 * インスタンスを生成する。
	 * @param pointcut 拡張するメソッドおよびコンストラクタの対象を定義するオブジェクト
	 * @param handler メソッドおよびコンストラクタの拡張方法を定義するオブジェクト
	 */
	public Enhance(InvocationPointcut pointcut, InvocationHandler handler) {
		super();
		if (pointcut == null) {
			throw new NullPointerException("pointcut"); //$NON-NLS-1$
		}
		if (handler == null) {
			throw new NullPointerException("handler"); //$NON-NLS-1$
		}
		this.pointcut = pointcut;
		this.handler = handler;
	}
	
	/**
	 * このオブジェクトに登録された、拡張する方法を定義するオブジェクトを返す。
	 * @return 拡張する方法を定義するオブジェクト
	 */
	public InvocationHandler getHandler() {
		return handler;
	}
	
	/**
	 * このオブジェクトに登録された、拡張する対象を定義するオブジェクトを返す。
	 * @return 拡張する対象を定義するオブジェクト
	 */
	public InvocationPointcut getPointcut() {
		return pointcut;
	}
	
	/**
	 * このオブジェクトの文字列表現を返す。
	 * <p>
	 * 返される文字列はポイントカットに関する情報と、ハンドラに関する情報を含む。
	 * ただし、形式は変更される可能性があるので、デバッグ用途意外に使ってはならない。
	 * </p>
	 * @return このオブジェクトのデバッグ用の文字列表現
	 */
	@Override
	public String toString() {
		return MessageFormat.format("'{'pointcut={0}, handler={1}'}'", //$NON-NLS-1$
				pointcut, handler);
	}
}
