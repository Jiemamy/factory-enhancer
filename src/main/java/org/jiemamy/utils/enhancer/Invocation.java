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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

/**
 * {@link InvocationHandler}が利用する本来の呼び出しを表現するオブジェクト。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public interface Invocation {
	
	/**
	 * 拡張を行う前の本来のメソッド起動、またはインスタンス生成を続行する。
	 * <p>
	 * このメソッドが返す値は、本来のメソッドを起動した際の戻り値である。
	 * ただし、戻り値の型がプリミティブ型である場合はそのラッパー型での表現を返し、
	 * {@code void}型である場合には{@code null}を常に返す。
	 * また、このオブジェクトがインスタンス生成を表す場合には、この呼び出しは実際に生成されたインスタンスを返す。
	 * </p>
	 * <p>
	 * 本来の起動を実行した際にその呼び出し先で何らかの例外が発生した場合、
	 * このメソッドはその例外を{@link InvocationTargetException}でラップした例外をスローする。
	 * </p>
	 * <p>
	 * なお、起動する対象が存在しない場合、{@link InvocationTargetException}がスローされるが
	 * その原因となる例外は{@link IncompatibleClassChangeError}を親とするクラスのインスタンスである。
	 * たとえば、実装されてないインターフェースメソッドを起動した場合、{@link AbstractMethodError}が該当する。
	 * </p>
	 * @return
	 *      メソッド呼び出し、またはインスタンス生成の結果。
	 *      ただし、{@code void}型のメソッドを呼び出した場合には{@code null}、または
	 *      結果がプリミティブ型の値となるメソッドを呼び出した場合にはそのラッパー型の値
	 * @throws InvocationTargetException
	 *     呼び出し先の実行で例外が発生した場合
	 * @throws IllegalArgumentException
	 *     実引数リストが{@link #getArguments() 変更}された結果として、
	 *     このメソッドを呼び出すために必要な実引数リストでなくなってしまった場合
	 */
	Object proceed() throws InvocationTargetException, IllegalArgumentException;
	
	/**
	 * 拡張を行う前の本来のメソッド、またはコンストラクタを返す。
	 * <p>
	 * このオブジェクトがメソッド呼び出しを表現するものであれば、返される値は本来の呼び出し先を表現する
	 * {@link java.lang.reflect.Method Method}型の値となる。
	 * そうでなく、このオブジェクトがインスタンス生成を表現するものであれば、返される値はインスタンス生成時に
	 * 本来実行されるべき{@link java.lang.reflect.Constructor Constructor}型の値となる。
	 * </p>
	 * <p>
	 * いずれの場合も、{@link #proceed()}によって実行される実際のメソッドやコンストラクタが、
	 * このメソッドが返す表現するメソッドやコンストラクタと一致しない場合がある。
	 * そのため、このオブジェクトを利用してメソッド呼び出しやインスタンス生成を行うことは推奨されない。
	 * </p>
	 * @return 拡張を行う前の本来のメソッド、またはコンストラクタ
	 */
	Member getTarget();
	
	/**
	 * この呼び出しを行おうとしているオブジェクトを返す。
	 * <p>
	 * この呼び出しが返す値は{@code a.method()}のようなメソッド呼び出し式の{@code a}の値で、
	 * その{@code method()}は{@code a}のクラスで定義されたインスタンスメソッドである。
	 * </p>
	 * このオブジェクトがインスタンス生成を表現する場合、
	 * この呼び出しはインスタンスを生成しようとするファクトリを返す。
	 * ただし、ファクトリのクラスメソッド({@code static})からインスタンス生成式が実行される場合、
	 * この呼び出しはファクトリのクラスを表現する{@link java.lang.Class}型のインスタンスを返す。
	 * </p>
	 * @return この呼び出しを行おうとしているオブジェクト
	 */
	Object getInvoker();
	
	/**
	 * 本来のメソッドおよびコンストラクタを実行する際に与えられた実引数の一覧を返す。
	 * <p>
	 * 実引数にプリミティブ型の値が含まれる場合、その値は対応するラッパー型の値として
	 * 返される配列に含まれる。
	 * </p>
	 * <p>
	 * この配列を直接操作することで、呼び出しの実引数を変更することができる。
	 * ただし、その結果として呼び出し先の引数に適合しなくなる場合、
	 * {@link #proceed()}の呼び出しが失敗する場合がある。
	 * </p>
	 * @return 実行に利用する実引数の一覧
	 */
	Object[] getArguments();
}
