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

import java.util.Comparator;

import javassist.CtClass;

/**
 * {@link CtClass}オブジェクトを比較する。
 * <p>
 * 比較は{@link CtClass}の提供する完全限定名によって行う。
 * </p>
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA
 */
public class CtClassComparator implements Comparator<CtClass> {
	
	/**
	 * このクラスのインスタンス。
	 */
	public static final CtClassComparator INSTANCE = new CtClassComparator();
	

	/**
	 * 引数に渡された2つの型の、完全限定名を辞書式順序に従って比較した結果を返す。
	 * @param o1 比較される型
	 * @param o2 比較する型
	 * @return 完全限定名を辞書式順序に従って比較した結果
	 */
	public int compare(CtClass o1, CtClass o2) {
		return o1.getName().compareTo(o2.getName());
	}
}
