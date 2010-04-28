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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import org.jiemamy.utils.enhancer.helper.AndInvocationPointcut;
import org.jiemamy.utils.enhancer.helper.NotInvocationPointcut;
import org.jiemamy.utils.enhancer.helper.OrInvocationPointcut;

/**
 * 一般的なポイントカット定義の一覧。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public enum Pointcuts implements InvocationPointcut {
	
	/**
	 * すべてを対象とするポイントカット定義。
	 */
	TRUE("true") { //$NON-NLS-1$
	
		public boolean isTarget(CtClass self, CtBehavior behavior) {
			return true;
		}
	},
	
	/**
	 * 何も対象としないポイントカット定義。
	 */
	FALSE("false") { //$NON-NLS-1$
	
		public boolean isTarget(CtClass self, CtBehavior behavior) {
			return false;
		}
	},
	
	/**
	 * メソッドのみを対象とするポイントカット定義。
	 */
	METHODS("methods") { //$NON-NLS-1$
	
		public boolean isTarget(CtClass self, CtBehavior behavior) {
			return behavior instanceof CtMethod;
		}
	},
	
	/**
	 * コンストラクタのみを対象とするポイントカット定義。
	 */
	CONSTRUCTORS("constructors") { //$NON-NLS-1$
	
		public boolean isTarget(CtClass self, CtBehavior behavior) {
			return behavior instanceof CtConstructor;
		}
	};
	
	/**
	 * 指定したポイントカット定義の論理積を取るような新しいポイントカット定義を返す。
	 * <p>
	 * 返されるオブジェクトは、
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * が呼び出された際に、引数に指定されたポイントカット定義についてそれぞれ
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * を実行する。
	 * この実行の結果としてすべてのポイントカット定義が{@code true}を返す場合のみ、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果が{@code true}となる。
	 * そうでなく、いずれかのポイントカット定義が{@code false}を返す場合、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果は{@code false}となる。
	 * </p>
	 * @param a 内包するポイントカット定義
	 * @param b 内包するポイントカット定義
	 * @return 指定されたポイントカット定義の論理積となるようなポイントカット定義
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static InvocationPointcut and(InvocationPointcut a, InvocationPointcut b) {
		return new AndInvocationPointcut(toList(a, b));
	}
	
	/**
	 * 指定したポイントカット定義の論理積を取るような新しいポイントカット定義を返す。
	 * <p>
	 * 返されるオブジェクトは、
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * が呼び出された際に、引数に指定されたポイントカット定義についてそれぞれ
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * を実行する。
	 * この実行の結果としてすべてのポイントカット定義が{@code true}を返す場合のみ、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果が{@code true}となる。
	 * そうでなく、いずれかのポイントカット定義が{@code false}を返す場合、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果は{@code false}となる。
	 * </p>
	 * @param a 内包するポイントカット定義
	 * @param b 内包するポイントカット定義
	 * @param rest 内包するポイントカット定義の一覧
	 * @return 指定されたポイントカット定義の論理積となるようなポイントカット定義
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @throws NullPointerException 引数{@code rest}に{@code null}が含まれる場合
	 */
	public static InvocationPointcut and(InvocationPointcut a, InvocationPointcut b, InvocationPointcut... rest) {
		return new AndInvocationPointcut(toList(a, b, rest));
	}
	
	/**
	 * 指定したポイントカット定義の論理否定を取るような新しいポイントカット定義を返す。
	 * <p>
	 * 返されるオブジェクトは、
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * が呼び出された際に、引数に指定されたポイントカット定義の
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * を実行し、その論理否定を自身の呼び出しの結果とする。
	 * </p>
	 * @param term 内包するポイントカット定義
	 * @return 指定されたポイントカット定義の論理和となるようなポイントカット定義
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static InvocationPointcut not(InvocationPointcut term) {
		if (term == null) {
			throw new NullPointerException("term"); //$NON-NLS-1$
		}
		return new NotInvocationPointcut(term);
	}
	
	/**
	 * 指定したポイントカット定義の論理和を取るような新しいポイントカット定義を返す。
	 * <p>
	 * 返されるオブジェクトは、
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * が呼び出された際に、引数に指定されたポイントカット定義についてそれぞれ
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * を実行する。
	 * この実行の結果としていずれかのポイントカット定義が{@code false}を返す場合のみ、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果が{@code true}となる。
	 * そうでなく、すべてのポイントカット定義が{@code false}を返す場合、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果は{@code false}となる。
	 * </p>
	 * @param a 内包するポイントカット定義
	 * @param b 内包するポイントカット定義
	 * @return 指定されたポイントカット定義の論理和となるようなポイントカット定義
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static InvocationPointcut or(InvocationPointcut a, InvocationPointcut b) {
		return new OrInvocationPointcut(toList(a, b));
	}
	
	/**
	 * 指定したポイントカット定義の論理和を取るような新しいポイントカット定義を返す。
	 * <p>
	 * 返されるオブジェクトは、
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * が呼び出された際に、引数に指定されたポイントカット定義についてそれぞれ
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)
	 * isTarget(self, declaring)}
	 * を実行する。
	 * この実行の結果としていずれかのポイントカット定義が{@code false}を返す場合のみ、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果が{@code true}となる。
	 * そうでなく、すべてのポイントカット定義が{@code false}を返す場合、
	 * 返されるオブジェクトに対する
	 * {@link InvocationPointcut#isTarget(javassist.CtClass, javassist.CtBehavior)}
	 * の結果は{@code false}となる。
	 * </p>
	 * @param a 内包するポイントカット定義
	 * @param b 内包するポイントカット定義
	 * @param rest 内包するポイントカット定義の一覧
	 * @return 指定されたポイントカット定義の論理積となるようなポイントカット定義
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @throws NullPointerException 引数{@code rest}に{@code null}が含まれる場合
	 */
	public static InvocationPointcut or(InvocationPointcut a, InvocationPointcut b, InvocationPointcut... rest) {
		return new OrInvocationPointcut(toList(a, b, rest));
	}
	
	private static List<InvocationPointcut> toList(InvocationPointcut a, InvocationPointcut b) {
		if (a == null) {
			throw new NullPointerException("a"); //$NON-NLS-1$
		}
		if (b == null) {
			throw new NullPointerException("b"); //$NON-NLS-1$
		}
		List<InvocationPointcut> terms = new ArrayList<InvocationPointcut>(2);
		terms.add(a);
		terms.add(b);
		return terms;
	}
	
	private static List<InvocationPointcut> toList(InvocationPointcut a, InvocationPointcut b, InvocationPointcut[] rest) {
		if (a == null) {
			throw new NullPointerException("a"); //$NON-NLS-1$
		}
		if (b == null) {
			throw new NullPointerException("b"); //$NON-NLS-1$
		}
		if (rest == null) {
			throw new NullPointerException("rest"); //$NON-NLS-1$
		}
		List<InvocationPointcut> terms = new ArrayList<InvocationPointcut>(2 + rest.length);
		terms.add(a);
		terms.add(b);
		for (int i = 0; i < rest.length; i++) {
			if (rest[i] == null) {
				throw new NullPointerException(MessageFormat.format("rest[{0}]", //$NON-NLS-1$
						String.valueOf(i)));
			}
			terms.add(rest[i]);
		}
		return terms;
	}
	

	private final String description;
	

	private Pointcuts(String description) {
		assert description != null;
		this.description = description;
	}
	
	/**
	 * このオブジェクトの文字列表現を返す。
	 * @return このオブジェクトの文字列表現
	 */
	@Override
	public String toString() {
		return description;
	}
}
