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
import java.util.List;

/**
 * アドバイスの一覧を保持するテーブル。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class AdviceTable {
	
	private final AspectList<?> aspects;
	
	private final List<AdviceApplier> appliers;
	

	/**
	 * インスタンスを生成する。
	 * @param aspects アスペクトの定義一覧
	 * @param appliers 定義一覧に対応してアドバイスを実行するオブジェクトの一覧
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public AdviceTable(AspectList<?> aspects, List<AdviceApplier> appliers) {
		super();
		if (aspects == null) {
			throw new NullPointerException("aspects"); //$NON-NLS-1$
		}
		if (appliers == null) {
			throw new NullPointerException("appliers"); //$NON-NLS-1$
		}
		this.appliers = appliers;
		this.aspects = aspects;
	}
	
	/**
	 * このテーブルが保持するアスペクトの定義一覧を返す。
	 * @return アスペクトの定義一覧
	 */
	public AspectList<?> getAspects() {
		return aspects;
	}
	
	/**
	 * このテーブルが保持するアドバイス適用のための配列を返す。
	 * @return アドバイス適用のための配列
	 */
	public AdviceApplier[] toAppliers() {
		return appliers.toArray(new AdviceApplier[appliers.size()]);
	}
	
	/**
	 * このオブジェクトの文字列表現を返す。
	 * @return このオブジェクトのデバッグ用の文字列表現
	 */
	@Override
	public String toString() {
		return MessageFormat.format("{0} = {1}", //$NON-NLS-1$
				aspects, appliers);
	}
}
