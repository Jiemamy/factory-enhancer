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
 * 本来のメソッド呼び出しまたはインスタンス生成をフックして、処理内容を拡張するためのインターフェース。
 * <p>
 * このインターフェースは、クライアントが実装してエンハンサに登録することができる。
 * </p>
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public interface InvocationHandler {
	
	/**
	 * 本来のメソッド呼び出しまたはインスタンス生成をフックして、前後の処理を行うためのメソッド。
	 * <p>
	 * このインターフェースを実装したクラスをエンハンサに与えることで、
	 * メソッド呼び出しやインスタンス生成処理を拡張することができる。
	 * </p>
	 * <p>
	 * 次のようなプログラムを記述することができる。
	 * </p>
	 * <pre><code>
	 * public Object invocation(Invocation invocation) throws Throwable {
	 *     before(); // 前処理
	 *     try {
	 *         Object result = invocation.proceed(); // 実際の実行
	 *         after(result); // 実行結果の後処理
	 *         return result; // 戻り値
	 *     }
	 *     catch (InvocationTargetException e) {
	 *         throw e.getCause();
	 *     }
	 * }
	 * </code></pre>
	 * @param invocation 本来の実行をカプセル化したオブジェクト
	 * @return　本来の実行の結果の代わりに通知する戻り値
	 * @throws Throwable 本来の実行の結果の代わりに通知する例外
	 * @see Invocation
	 */
	Object handle(Invocation invocation) throws Throwable;
}
