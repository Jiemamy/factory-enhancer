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
import java.util.ArrayList;
import java.util.List;

import javassist.CtBehavior;
import javassist.CtClass;

import org.jiemamy.utils.enhancer.InvocationPointcut;

/**
 * 複数のフィルタの論理積を結果とするポイントカット定義。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class AndInvocationPointcut implements InvocationPointcut {
	
	private final List<InvocationPointcut> terms;
	

	/**
	 * インスタンスを生成する。
	 * @param terms 内包するポイントカットの一覧
	 * @throws IllegalArgumentException 空のリストが指定された場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public AndInvocationPointcut(List<? extends InvocationPointcut> terms) {
		super();
		if (terms == null) {
			throw new NullPointerException("terms"); //$NON-NLS-1$
		}
		if (terms.isEmpty()) {
			throw new IllegalArgumentException("terms is empty"); //$NON-NLS-1$
		}
		this.terms = new ArrayList<InvocationPointcut>(terms);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * この実装では、コンストラクタに指定されたポイントカット定義についてそれぞれ
	 * {@link InvocationPointcut#isTarget(CtClass, CtBehavior)}を呼び出し、
	 * そのうちひとつでも{@code false}を返すものがあれば以降の計算を行わない。
	 * </p>
	 */
	public boolean isTarget(CtClass self, CtBehavior klass) {
		List<InvocationPointcut> list = terms;
		for (int i = 0, n = list.size(); i < n; i++) {
			if (list.get(i).isTarget(self, klass) == false) {
				return false;
			}
		}
		return true;
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
		return MessageFormat.format("And{0}", //$NON-NLS-1$
				terms);
	}
}
