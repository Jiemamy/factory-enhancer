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
 * {@link ArrayFactory}の実装。
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class ArrayFactoryImpl implements ArrayFactory {
	
	public int[] newIntArray() {
		return new int[0];
	}
	
	public int[][] newIntMultiArray() {
		return new int[0][];
	}
	
	public String[] newObjectArray() {
		return new String[0];
	}
	
	public String[][] newObjectMultiArray() {
		return new String[0][];
	}
	
	@SuppressWarnings("unused")
	public TargetComponent[] newProductArray() {
		new TargetComponent();
		return new TargetComponent[0];
	}
	
	public TargetComponent[][] newProductMultiArray() {
		return new TargetComponent[0][];
	}
	
	public TargetComponentOnlyType[] returnsNull() {
		return null;
	}
}
