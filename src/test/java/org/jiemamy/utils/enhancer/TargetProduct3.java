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
 * {@link TargetFactoryImpl}によって生成されるプロダクト(3)。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class TargetProduct3 {
	
	private String value;
	

	/**
	 * インスタンスを生成する。
	 */
	public TargetProduct3() {
		super();
		value = "";
	}
	
	/**
	 * インスタンスを生成する。
	 * @param value 保持する値
	 */
	public TargetProduct3(String value) {
		super();
		this.value = value;
	}
	
	/**
	 * オブジェクトが同値である場合のみ{@code true}を返す。
	 * ただし、エンハンス時にサブクラスを作られるので、型の比較はそれを考慮して行っている。
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if ((obj instanceof TargetProduct3) == false) {
			return false;
		}
		TargetProduct3 other = (TargetProduct3) obj;
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}
	
	/**
	 * 値を返す。
	 * @return 値
	 */
	public String getValue() {
		return value + 3;
	}
	
	/**
	 * このオブジェクトのハッシュ値を返す。
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	/**
	 * 値を設定する。
	 * @param value 設定する値
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
