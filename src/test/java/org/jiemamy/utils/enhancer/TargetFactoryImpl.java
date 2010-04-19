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
 * テスト対象のファクトリ。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class TargetFactoryImpl implements TargetFactory {
	
	/**
	 * @return static method
	 */
	public static TargetProduct1 newProduct1Static() {
		return new TargetProduct1();
	}
	
	/**
	 * @return {@link TargetProduct1}
	 */
	public TargetProduct1 newProduct1() {
		return new TargetProduct1();
	}
	
	/**
	 * @return {@link TargetProduct1} with value
	 */
	public TargetProduct1 newProduct1(String value) {
		return new TargetProduct1(value);
	}
	
	/**
	 * @return {@link TargetProduct2}
	 */
	public TargetProduct2 newProduct2() {
		return new TargetProduct2();
	}
	
	/**
	 * @return {@link TargetProduct2} with value
	 */
	public TargetProduct2 newProduct2(String value) {
		return new TargetProduct2(value);
	}
	
	/**
	 * @return {@link TargetProduct3}
	 */
	public TargetProduct3 newProduct3() {
		return new TargetProduct3();
	}
	
	/**
	 * @return {@link TargetProduct3} with value
	 */
	public TargetProduct3 newProduct3(String value) {
		return new TargetProduct3(value);
	}
	
	/**
	 * @return {@link TargetProductFinal}
	 */
	public TargetProductFinal newProductFinal(String value) {
		return new TargetProductFinal(value);
	}
	
	/**
	 * @return {@link String}
	 */
	public String newString(String value) {
		return new String(value);
	}
}
