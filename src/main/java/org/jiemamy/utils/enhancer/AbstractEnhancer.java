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

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Enhancer}の骨格実装。
 * @since 0.2.0
 * @version 0.2.0
 * @author Suguru ARAKAWA
 * @param <T> エンハンスする対象の種類
 */
public abstract class AbstractEnhancer<T> implements Enhancer<T> {
	
	private AtomicReference<Factory<? extends T>> factoryCache = new AtomicReference<Factory<? extends T>>();
	

	/**
	 * エンハンスされたクラスのインスタンスを生成するファクトリを返す。
	 * <p>
	 * この実装では、{@link #createFactory()}でファクトリを生成し、そのキャッシュを保持する。
	 * </p>
	 * @return エンハンスされたクラスのインスタンスを生成するファクトリ
	 * @throws EnhanceException 拡張に失敗した場合
	 */
	public Factory<? extends T> getFactory() throws EnhanceException {
		Factory<? extends T> cached = factoryCache.get();
		if (cached != null) {
			return cached;
		}
		Factory<? extends T> factory = createFactory();
		if (factoryCache.compareAndSet(null, factory)) {
			return factory;
		}
		cached = factoryCache.get();
		assert cached != null;
		return cached;
	}
	
	/**
	 * 指定のクラスのインスタンスを生成するファクトリを返す。
	 * @return 生成したファクトリ
	 * @throws EnhanceException ファクトリの生成に失敗した場合
	 */
	protected abstract Factory<? extends T> createFactory() throws EnhanceException;
}
