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
package org.jiemamy.utils.enhancer;

/**
 * テストに利用するファクトリのインターフェース。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public interface TargetFactory {
	
	/**
	 * @return product1
	 */
	TargetProduct1 newProduct1();
	
	/**
	 * @return product2
	 */
	TargetProduct2 newProduct2();
	
	/**
	 * @return product3
	 */
	TargetProduct3 newProduct3();
	
	/**
	 * @param value initial value
	 * @return product1
	 */
	TargetProduct1 newProduct1(String value);
	
	/**
	 * @param value initial value
	 * @return product2
	 */
	TargetProduct2 newProduct2(String value);
	
	/**
	 * @param value initial value
	 * @return product3
	 */
	TargetProduct3 newProduct3(String value);
	
	/**
	 * @param value initial value
	 * @return product final
	 */
	TargetProductFinal newProductFinal(String value);
	
	/**
	 * @param value initial value
	 * @return {@link String}
	 */
	String newString(String value);
}
