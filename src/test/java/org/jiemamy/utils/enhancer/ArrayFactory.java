/*
 * Copyright 2007-2009 Jiemamy Project and the Others.
 * Created on 2009/05/16
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
 * 配列を作成するファクトリ。
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public interface ArrayFactory {
	
	/**
	 * @return {@code int[]}
	 */
	int[] newIntArray();
	
	/**
	 * @return {@code int[][]}
	 */
	int[][] newIntMultiArray();
	
	/**
	 * @return {@code String[]}
	 */
	String[] newObjectArray();
	
	/**
	 * @return {@code String[][]}
	 */
	String[][] newObjectMultiArray();
	
	/**
	 * @return {@code TargetComponent[]}
	 */
	TargetComponent[] newProductArray();
	
	/**
	 * @return {@code TargetComponent[][]}
	 */
	TargetComponent[][] newProductMultiArray();
	
	/**
	 * @return {@code null}
	 */
	TargetComponentOnlyType[] returnsNull();
}
