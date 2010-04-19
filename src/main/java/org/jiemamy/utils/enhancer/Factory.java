/*
 * Copyright 2007-2009 Jiemamy Project and the Others.
 * Created on 2009/10/04
 *
 * This file is part of Jiemamy.
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
package org.jiemamy.utils.enhancer;

import java.lang.reflect.InvocationTargetException;

/**
 * 特定の型のオブジェクトを生成するファクトリ。
 * @version 0.2.0
 * @since 0.2.0
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 * @param <T> 生成するインスタンスの型
 */
public interface Factory<T> {
	
	/**
	 * このファクトリが生成するオブジェクトのクラスを返す。
	 * @return このファクトリが生成するオブジェクトのクラス
	 */
	Class<T> getTargetClass();
	
	/**
	 * 指定の引数を利用して、このファクトリが生成可能なオブジェクトを生成して返す。
	 * @param arguments オブジェクトの生成に利用する引数の一覧
	 * @return 生成したオブジェクト
	 * @throws IllegalArgumentException 指定された引数がオブジェクトの生成に適さない場合
	 * @throws InvocationTargetException オブジェクトの生成中に例外が発生した場合
	 */
	T newInstance(Object... arguments) throws InvocationTargetException;
}
