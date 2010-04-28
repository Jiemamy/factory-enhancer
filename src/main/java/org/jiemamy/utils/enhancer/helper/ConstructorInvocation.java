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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jiemamy.utils.enhancer.Invocation;

/**
 * コンストラクタの起動を表現する。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class ConstructorInvocation implements Invocation {
	
	private final Constructor<?> originalTarget;
	
	private final Constructor<?> actualTarget;
	
	private Object factory;
	
	private final Object[] arguments;
	

	/**
	 * インスタンスを生成する。
	 * @param originalTarget 本来のコンストラクタ
	 * @param actualTarget 実際に実行するコンストラクタ
	 * @param factory コンストラクタを実行しようとしているファクトリ (省略可)
	 * @param arguments 実行に利用する実引数の一覧、なおこの配列は破壊される場合がある
	 * @throws NullPointerException　いずれかの引数に{@code null}が指定された場合
	 */
	public ConstructorInvocation(Constructor<?> originalTarget, Constructor<?> actualTarget, Object factory,
			Object[] arguments) {
		super();
		if (originalTarget == null) {
			throw new NullPointerException("originalTarget"); //$NON-NLS-1$
		}
		if (actualTarget == null) {
			throw new NullPointerException("actualTarget"); //$NON-NLS-1$
		}
		if (factory == null) {
			throw new NullPointerException("factory"); //$NON-NLS-1$
		}
		if (arguments == null) {
			throw new NullPointerException("arguments"); //$NON-NLS-1$
		}
		this.originalTarget = originalTarget;
		this.actualTarget = actualTarget;
		this.factory = factory;
		this.arguments = arguments;
	}
	
	/**
	 * このオブジェクトが表現するインスタンス生成式に渡された引数の一覧を返す。
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
	 * このインスタンス生成を行おうとしているファクトリのインスタンス、またはクラスを返す。
	 * <p>
	 * このインスタンス生成式がファクトリのインスタンスメソッド内で実行されている場合、
	 * この呼び出しは常にファクトリのインスタンスを返す。
	 * そうでなく、ファクトリのクラスメソッド内で実行されている場合はファクトリのクラスを返す。
	 * </p>
	 */
	public Object getInvoker() {
		return factory;
	}
	
	/**
	 * 拡張を行う前の本来のコンストラクタを返す。
	 * <p>
	 * 返される値は、インスタンス生成時に本来実行されるべき{@link Constructor}型の値となる。
	 * </p>
	 * <p>
	 * {@link #proceed()}によって実行される実際のコンストラクタが
	 * このメソッドが返すコンストラクタと一致しない場合がある。
	 * そのため、このオブジェクトを利用してインスタンス生成を行うことは推奨されない。
	 * </p>
	 * @return 拡張を行う前の本来のコンストラクタ
	 */
	public Constructor<?> getTarget() {
		return originalTarget;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * この実装では、実際のコンストラクタを呼び出して生成されたインスタンスを返す。
	 * </p>
	 */
	public Object proceed() throws InvocationTargetException, IllegalArgumentException {
		try {
			return actualTarget.newInstance(arguments);
		} catch (InstantiationException e) {
			// may not occur
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			// may not occur
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
