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
import org.jiemamy.utils.enhancer.InvocationHandler;

/**
 * {@link InvocationHandler}のチェインを作成するための{@link Invocation}。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class DelegateInvocation implements Invocation {
	
	private static final Object[] EMPTY = new Object[0];
	
	private Invocation delegateInvocation;
	
	private InvocationHandler delegateHandler;
	
	private Object[] arguments;
	

	/**
	 * インスタンスを生成する。
	 * @param delegateInvocation 移譲する呼び出し
	 * @param delegateHandler 移譲先のハンドラ
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public DelegateInvocation(Invocation delegateInvocation, InvocationHandler delegateHandler) {
		super();
		if (delegateInvocation == null) {
			throw new NullPointerException("delegateInvocation"); //$NON-NLS-1$
		}
		if (delegateHandler == null) {
			throw new NullPointerException("delegateHandler"); //$NON-NLS-1$
		}
		this.delegateInvocation = delegateInvocation;
		this.delegateHandler = delegateHandler;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * この実装では、委譲先の呼び出しオブジェクトが持つ引数一覧を返す。
	 * 返される配列の内容を変更した場合、{@link #proceed()}の実行に先立って
	 * 委譲先の呼び出しオブジェクトに変更内容が伝播される。
	 * </p>
	 */
	public Object[] getArguments() {
		if (arguments == null) {
			Object[] next = delegateInvocation.getArguments();
			if (next.length >= 1) {
				arguments = next.clone();
			} else {
				arguments = EMPTY;
			}
		}
		return arguments;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * この実装では、委譲先の呼び出しオブジェクトが表現する対象を
	 * 呼び出そうとしたオブジェクトを返す。
	 * </p>
	 */
	public Object getInvoker() {
		return delegateInvocation.getInvoker();
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * この実装では、委譲先の呼び出しオブジェクトが表現する対象を返す。
	 * </p>
	 */
	public Member getTarget() {
		return delegateInvocation.getTarget();
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * この実装では、委譲先のハンドラに委譲する呼び出しオブジェクトを渡した結果を返す。
	 * </p>
	 */
	public Object proceed() throws InvocationTargetException {
		if (arguments != null && arguments.length >= 1) {
			System.arraycopy(arguments, 0, delegateInvocation.getArguments(), 0, arguments.length);
		}
		try {
			return delegateHandler.handle(delegateInvocation);
		} catch (Throwable t) {
			throw new InvocationTargetException(t);
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
		return delegateInvocation.toString();
	}
}
