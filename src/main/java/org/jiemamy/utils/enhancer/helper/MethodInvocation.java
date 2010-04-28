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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jiemamy.utils.enhancer.Invocation;

/**
 * メソッド呼び出しを表現する。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class MethodInvocation implements Invocation {
	
	private static boolean isStatic(Method m) {
		return Modifier.isStatic(m.getModifiers());
	}
	

	private final Method originalTarget;
	
	private final Method actualTarget;
	
	private final Object object;
	
	private final Object[] arguments;
	

	/**
	 * インスタンスを生成する。
	 * @param originalTarget 本来の呼び出し先メソッド
	 * @param actualTarget 実際に呼び出しを行うメソッド
	 * @param object 呼び出しに利用するオブジェクト (対象がクラスメソッドの場合は{@code null})
	 * @param arguments 呼び出しに利用する実引数の一覧、なおこの配列は破壊される場合がある
	 * @throws NullPointerException
	 *     引数{@code object}以外に{@code null}が指定された場合
	 * @throws NullPointerException
	 *     {@code actualTarget}がインスタンスメソッドを表現し、かつ
	 *     {@code object}に{@code null}が指定された場合
	 * @throws IllegalArgumentException
	 *     引数{@code actualTarget}がクラスメソッドを表現し、かつ
	 *     {@code object}に{@code null}以外が指定された場合
	 */
	public MethodInvocation(Method originalTarget, Method actualTarget, Object object, Object[] arguments) {
		super();
		if (originalTarget == null) {
			throw new NullPointerException("originalTarget"); //$NON-NLS-1$
		}
		if (actualTarget == null) {
			throw new NullPointerException("actualTarget"); //$NON-NLS-1$
		}
		if (arguments == null) {
			throw new NullPointerException("arguments"); //$NON-NLS-1$
		}
		if (isStatic(actualTarget)) {
			if (object != null) {
				throw new IllegalArgumentException("object should be null"); //$NON-NLS-1$
			}
		} else if (object == null) {
			throw new NullPointerException("object"); //$NON-NLS-1$
		}
		this.originalTarget = originalTarget;
		this.actualTarget = actualTarget;
		this.object = object;
		this.arguments = arguments;
	}
	
	/**
	 * このオブジェクトが表現するメソッド呼び出しに渡された引数の一覧を返す。
	 * <p>
	 * 実引数にプリミティブ型の値が含まれる場合、その値は対応するラッパー型の値として
	 * 返される配列に含まれる。
	 * </p>
	 * <p>
	 * この配列を直接操作することで、呼び出しの実引数を変更することができる。
	 * ただし、その結果として呼び出し先の引数に適合しなくなる場合、
	 * {@link #proceed()}の呼び出しが失敗する場合がある。
	 * </p>
	 * @return 実行に利用する実引数の一覧
	 */
	public Object[] getArguments() {
		return arguments;
	}
	
	/**
	 * このメソッド呼び出しによって呼び出されるメソッドを公開するオブジェクトを返す。
	 * <p>
	 * 呼び出されるメソッドがクラスメソッドである場合、この呼び出しは{@code null}を返す。
	 * </p>
	 */
	public Object getInvoker() {
		return object;
	}
	
	/**
	 * 拡張を行う前の本来のメソッドを返す。
	 * <p>
	 * 返される値は本来の呼び出し先を表現する{@link Method}型の値となる。
	 * </p>
	 * <p>
	 * {@link #proceed()}によって実行される実際のメソッドが、
	 * このメソッドが返す表現するメソッドと一致しない場合がある。
	 * そのため、このオブジェクトを利用してメソッド呼び出しを行うことは推奨されない。
	 * </p>
	 * @return 拡張を行う前の本来のメソッド、またはコンストラクタ
	 */
	public Method getTarget() {
		return originalTarget;
	}
	
	/**
	 * {@inheritDoc}
	 * @return 実際のメソッドを呼び出した結果、{@code void}型のメソッドの場合は{@code null}
	 */
	public Object proceed() throws InvocationTargetException {
		try {
			return actualTarget.invoke(object, arguments);
		} catch (IllegalAccessException e) {
			// May not occurr
			throw new AssertionError(e);
		}
	}
	
	/**
	 * このオブジェクトの文字列表現を返す。
	 * <p>
	 * 返される文字列は本来の呼び出し先に関する情報を含む。
	 * ただし、この形式は変更される可能性があるので、デバッグ用途意外に使ってはならない。
	 * </p>
	 * @return このオブジェクトのデバッグ用の文字列表現
	 */
	@Override
	public String toString() {
		return originalTarget.toString();
	}
}
