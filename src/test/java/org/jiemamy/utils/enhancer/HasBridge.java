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
 * ブリッジを持つ(可能性のある)メソッド。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA
 */
public class HasBridge implements Comparable<HasBridge> {
	
	/**
	 * 常に0を返す。
	 * このメソッドの宣言によって、柿のようなブリッジメソッドが用意されるはず。
	 * <pre><code>
	 * public int compareTo(Object o) {
	 *     return compareTo((HasBridge) o);
	 * }
	 * </code></pre>
	 */
	public int compareTo(HasBridge o) {
		return 0;
	}
}
