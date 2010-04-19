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
import java.util.Iterator;
import java.util.List;

import javassist.CtBehavior;
import javassist.CtField;

/**
 * アドバイスの適用先とアドバイスの適用方法の一覧を表す。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 * @param <T> 適用対象を表現する型
 */
public class AspectList<T extends CtBehavior> implements Iterable<Aspect<T>> {
	
	private CtField adviceTable;
	
	private List<Aspect<T>> aspectList;
	

	/**
	 * インスタンスを生成する。
	 * @param adviceTableHolder アスペクトを保持するフィールドの定義
	 * @param aspectList 適用すべきアスペクトの一覧
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public AspectList(CtField adviceTableHolder, List<? extends Aspect<T>> aspectList) {
		super();
		if (adviceTableHolder == null) {
			throw new NullPointerException("adviceTableHolder"); //$NON-NLS-1$
		}
		if (aspectList == null) {
			throw new NullPointerException("aspectList"); //$NON-NLS-1$
		}
		this.adviceTable = adviceTableHolder;
		this.aspectList = new ArrayList<Aspect<T>>(aspectList);
	}
	
	/**
	 * このアスペクト一覧が表現するアドバイスの情報を保持するフィールドを返す。
	 * @return アドバイスの情報を保持するフィールド
	 */
	public CtField getAdviceTableHolder() {
		return adviceTable;
	}
	
	/**
	 * アドバイステーブルに保持されるべきアドバイスの個数を返す。
	 * @return アドバイステーブルに保持されるべきアドバイスの個数
	 * @see #getAdviceTableHolder()
	 */
	public int getAdviceTableSize() {
		return aspectList.size();
	}
	
	/**
	 * このリストに含まれるアスペクトオブジェクトを走査する反復子を返す。
	 * @return このリストに含まれるアスペクトオブジェクトを走査する反復子
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Aspect<T>> iterator() {
		return aspectList.iterator();
	}
	
	/**
	 * このオブジェクトの文字列表現を返す。
	 * <p>
	 * ただし、形式は変更される可能性があるので、デバッグ用途意外に使ってはならない。
	 * </p>
	 * @return このオブジェクトのデバッグ用の文字列表現
	 */
	@Override
	public String toString() {
		return MessageFormat.format("{0}#{1}[{2}]", //$NON-NLS-1$
				adviceTable.getDeclaringClass().getName(), adviceTable.getName(), String.valueOf(aspectList.size()));
	}
}
