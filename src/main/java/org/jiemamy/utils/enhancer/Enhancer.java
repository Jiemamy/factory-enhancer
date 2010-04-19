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

/**
 * エンハンスされたクラスを参照するためのインターフェース。
 * @param <T> エンハンスする対象の種類
 * @version 0.2.0
 * @since 0.2.0
 * @author Suguru ARAKAWA
 */
public interface Enhancer<T> {
	
	/**
	 * エンハンスされたクラスのインスタンスを生成するファクトリを返す。
	 * @return エンハンスされたクラスのインスタンスを生成するファクトリ
	 * @throws EnhanceException 拡張に失敗した場合
	 * @since 0.2.0
	 */
	Factory<? extends T> getFactory() throws EnhanceException;
}
