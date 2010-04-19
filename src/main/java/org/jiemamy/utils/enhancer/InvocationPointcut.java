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

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * このエンハンサにおいて、拡張する対象のメソッド呼び出しおよび
 * インスタンス生成式を決定するためのインターフェース。
 * <p>
 * 複数のポイントカット定義を組み合わせる場合、{@link Pointcuts}が提供する
 * {@link Pointcuts#and(InvocationPointcut, InvocationPointcut) and},
 * {@link Pointcuts#or(InvocationPointcut, InvocationPointcut) or},
 * {@link Pointcuts#not(InvocationPointcut) not}
 * などを利用可能である。
 * </p>
 * <p>
 * このインターフェースは、クライアントが実装してエンハンサに登録することができる。
 * </p>
 * @version 0.2.0
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 * @see Pointcuts
 */
public interface InvocationPointcut {
	
	/**
	 * 指定のメソッド呼び出しまたはインスタンス生成をジョインポイントとして利用するかどうかを返す。
	 * <p>
	 * {@code self}に渡される値は、対象のメソッドまたはコンストラクタを公開するクラスである。
	 * メソッドは継承によって子クラスで親クラスのメソッドを公開可能であるため、
	 * {@code behavior}に指定されるメンバを実際に宣言するクラスとは異なる場合がある。
	 * コンストラクタの場合も同様に、対象のインスタンス生成対象がインターフェースであった場合、
	 * インターフェースにコンストラクタは定義できないため、元のインターフェースが{@code self}に渡される。
	 * 上記のため、 
	 * {@code CtBehavior#getDeclaringClass() behavior.getDeclaringClass()}
	 * の値を参考にせずに{@code self}を利用して対象を特定することが望ましい。
	 * </p>
	 * <p>
	 * メソッド呼び出しに対して検査を行う場合、引数の値は必ず
	 * {@link javassist.CtMethod CtMethod}型となる。
	 * そうでなく、インスタンス生成に対して検査を行う場合、引数の値は必ず
	 * {@link javassist.CtConstructor CtConstructor}型となる。
	 * </p>
	 * <p>
	 * なお、このポイントカットに対する{@link InvocationHandler ハンドラ}のメソッド
	 * {@link InvocationHandler#handle(Invocation)}は、次の要件を満たす必要がある。
	 * </p>
	 * <ul>
	 * <li> メソッド起動に対するハンドラ
	 *   <ul>
	 *   <li>
	 *   {@link CtMethod#getReturnType() ((CtMethod) behavior).getReturnType()}
	 *   が{@code void}を表現する場合、どのような制約も課されない。
	 *   </li>
	 *   <li>
	 *   {@link CtMethod#getReturnType() ((CtMethod) behavior).getReturnType()}
	 *   がプリミティブ型を表現する場合、ハンドラのメソッドはそのラッパ型の値を返す必要がある。
	 *   </li>
	 *   <li>
	 *   {@link CtMethod#getReturnType() ((CtMethod) behavior).getReturnType()}
	 *   がオブジェクト型を表現する場合、ハンドラのメソッドはその型、またはそのサブタイプの値を返す必要がある。
	 *   </li>
	 *   </ul>
	 * </li>
	 * <li> インスタンス生成に対するハンドラ
	 *   <ul>
	 *   <li> {@code self}が表現する型、またはそのサブタイプの値を返す必要がある。 </li>
	 *   </ul>
	 * </li>
	 * </ul>
	 * @param self このメソッドまたはコンストラクタを公開するクラス
	 * @param behavior 検査対象のメソッドまたはコンストラクタ
	 * @return ジョインポイントとして利用する場合は{@code true}、そうでない場合は{@code false}
	 */
	boolean isTarget(CtClass self, CtBehavior behavior);
}
