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

/**
 * 自身ではクラスをロードすることができないクラスローダ。
 * <p>
 * このクラスローダは、外部から
 * {@link #defineClass(String, byte[], int, int) ClassLoader.defineClass(*)}
 * が呼び出されることを前提としている。
 * この分割により、次の利点が望める。
 * </p>
 * <ul>
 * 	 <li>
 *     動的に生成されたクラスを、通常のクラスローダ上にではなくこの{@link EmptyClassLoader}上に
 *     {@link #defineClass(String, byte[], int, int) 定義}させることができる。
 *     これにより、通常のローダによるクラスの定義に影響することなく、動的生成されたクラスをロードすることができる。
 *   </li>
 * 	 <li>
 *     クラスを動的に生成する機構ごとにこのローダの異なるインスタンスを与えることで、
 *     それぞれの機構で生成されたクラスの定義が衝突することを防げる。
 *   </li>
 * 	 <li>
 *     動的に生成されたクラスがすべて不要となった場合、このローダをGCの対象とすることができる。
 *     クラスローダがGCの対象となった場合、それによってロードされたクラスをアンロード(JVMS2-2.17.8)
 *     することができ、動的生成されたクラスによるメモリリークを防げる可能性がある。
 *   </li>
 * </ul>
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class EmptyClassLoader extends ClassLoader {
	
	/**
	 * インスタンスを生成する。
	 * @param parent 親クラスローダ
	 */
	public EmptyClassLoader(ClassLoader parent) {
		super(parent);
	}
}
