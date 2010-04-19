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
 * 拡張に失敗したことを表現する例外。
 * @version $Date: 2009-09-29 23:06:33 +0900 (火, 29  9 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class EnhanceException extends Exception {
	
	private static final long serialVersionUID = -6448820201405601488L;
	

	/**
	 * インスタンスを生成する。
	 * @param message メッセージ
	 * @param cause この例外の原因となった例外
	 */
	public EnhanceException(String message, Throwable cause) {
		super(message, cause);
	}
}
