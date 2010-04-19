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

import java.text.MessageFormat;

import javassist.CtBehavior;
import javassist.CtClass;

import org.jiemamy.utils.enhancer.InvocationPointcut;

/**
 * 他のポイントカットの論理否定を結果とするポイントカット定義。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class NotInvocationPointcut implements InvocationPointcut {
	
	private final InvocationPointcut term;
	

	/**
	 * インスタンスを生成する。
	 * @param term 内包するポイントカット
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public NotInvocationPointcut(InvocationPointcut term) {
		super();
		if (term == null) {
			throw new NullPointerException("term"); //$NON-NLS-1$
		}
		this.term = term;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * この実装では、コンストラクタに指定されたポイントカット定義について
	 * {@link InvocationPointcut#isTarget(CtClass, CtBehavior)}を呼び出し、
	 * その結果の論理否定を返すのみである。
	 * </p>
	 */
	public boolean isTarget(CtClass self, CtBehavior klass) {
		return term.isTarget(self, klass) == false;
	}
	
	/**
	 * このオブジェクトの文字列表現を返す。
	 * <p>
	 * 返される文字列は項にとるポイントカット定義に関する情報を含む。
	 * ただし、形式は変更される可能性があるので、デバッグ用途意外に使ってはならない。
	 * </p>
	 * @return このオブジェクトのデバッグ用の文字列表現
	 */
	@Override
	public String toString() {
		return MessageFormat.format("Not[{0}]", term);
	}
}
