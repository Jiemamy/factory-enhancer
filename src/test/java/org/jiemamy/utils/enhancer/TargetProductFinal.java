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
 * {@link TargetFactoryImpl}によって生成されるプロダクト(final)。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public final class TargetProductFinal {
	
	private String value;
	

	/**
	 * インスタンスを生成する。
	 * @param value 保持する値
	 */
	public TargetProductFinal(String value) {
		super();
		this.value = value;
	}
	
	/**
	 * オブジェクトが同値である場合のみ{@code true}を返す。
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		TargetProductFinal other = (TargetProductFinal) obj;
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
		return value;
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
