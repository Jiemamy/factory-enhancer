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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javassist.CtBehavior;

import org.jiemamy.utils.enhancer.InvocationHandler;

/**
 * アドバイスの適用先とアドバイスの内容を保持するアスペクトを表現するクラス。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 * @param <T> 適用対象を表現する型
 */
public class Aspect<T extends CtBehavior> {
	
	private final T original;
	
	private final T actual;
	
	private final List<InvocationHandler> handlers;
	

	/**
	 * インスタンスを生成する。
	 * <p>
	 * {@code handlers}に含まれるハンドラは、その優先度の順に整列されている必要がある。
	 * </p>
	 * @param original アドバイスが適用される本来のメソッドまたはコンストラクタ
	 * @param actual アドバイスが適用される結果、実際に呼び出されるべきメソッドまたはコンストラクタ
	 * @param handlers アドバイスの処理を実現するハンドラの一覧
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public Aspect(T original, T actual, List<? extends InvocationHandler> handlers) {
		super();
		if (original == null) {
			throw new NullPointerException("original"); //$NON-NLS-1$
		}
		if (actual == null) {
			throw new NullPointerException("actual"); //$NON-NLS-1$
		}
		if (handlers == null) {
			throw new NullPointerException("handlers"); //$NON-NLS-1$
		}
		this.original = original;
		this.actual = actual;
		this.handlers = Collections.unmodifiableList(new ArrayList<InvocationHandler>(handlers));
	}
	
	/**
	 * アドバイスが適用されるメソッドまたはコンストラクタを返す。
	 * @return アドバイスが適用されるメソッドまたはコンストラクタ
	 */
	public T getOriginal() {
		return original;
	}
	
	/**
	 * アドバイスの適用によって実際に呼び出されるメソッドまたはコンストラクタを返す。
	 * @return 実際に呼び出されるメソッドまたはコンストラクタを返す
	 */
	public T getActual() {
		return actual;
	}
	
	/**
	 * アドバイスの処理を実現するハンドラの一覧を返す。
	 * <p>
	 * 返されるハンドラは、その優先度の順に整列されている。
	 * </p>
	 * <p>
	 * 返されるリストは変更できない。
	 * </p>
	 * @return アドバイスの処理を実現するハンドラの一覧
	 */
	public List<InvocationHandler> getHandlers() {
		return handlers;
	}
}
