/*
 * Copyright 2007-2009 Jiemamy Project and the Others.
 * Created on 2009/10/04
 *
 * This file is part of Jiemamy.
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
package org.jiemamy.utils.enhancer.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * リフレクションに関するユーティリティ群。
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 * @version 0.2.0
 * @since 0.2.0
 */
public class ReflectionUtil {
	
	private static final Map<Class<?>, Class<?>> BOXING;
	static {
		Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
		map.put(Integer.TYPE, Integer.class);
		map.put(Long.TYPE, Long.class);
		map.put(Float.TYPE, Float.class);
		map.put(Double.TYPE, Double.class);
		map.put(Short.TYPE, Short.class);
		map.put(Byte.TYPE, Byte.class);
		map.put(Character.TYPE, Character.class);
		map.put(Boolean.TYPE, Boolean.class);
		BOXING = Collections.unmodifiableMap(map);
	}
	

	/**
	 * インスタンス生成の禁止。
	 */
	private ReflectionUtil() {
		throw new AssertionError();
	}
	
	/**
	 * 指定のオブジェクトが、列挙やインターフェース等でない通常のクラスを表現する場合のみ{@code true}を返す。
	 * @param aClass 対象のクラスオブジェクト
	 * @return 通常のクラスを表現する場合に{@code true}
	 * @throws IllegalArgumentException 引数に{@code null}が指定された場合
	 */
	public static boolean isNormalClass(Class<?> aClass) {
		if (aClass == null) {
			throw new IllegalArgumentException("aClass is null"); //$NON-NLS-1$
		}
		return aClass.isPrimitive() == false
				&& aClass.isArray() == false
				&& aClass.isInterface() == false
				&& aClass.isEnum() == false;
	}
	
	/**
	 * 指定のクラスがパッケージメンバ(トップレベルクラス)である場合のみ{@code true}を返す。
	 * @param aClass 対象のクラスオブジェクト
	 * @return 指定のクラスがパッケージメンバである場合に{@code true}
	 * @throws IllegalArgumentException 引数に{@code null}が指定された場合
	 */
	public static boolean isPackageMember(Class<?> aClass) {
		if (aClass == null) {
			throw new IllegalArgumentException("aClass is null"); //$NON-NLS-1$
		}
		return aClass.isPrimitive() == false
				&& aClass.isArray() == false
				&& aClass.isMemberClass() == false
				&& aClass.isLocalClass() == false
				&& aClass.isAnonymousClass() == false;
	}
	
	/**
	 * 引数の一覧をそのクラスの一覧に変換して返す。
	 * <p>
	 * 引数の要素に{@code null}が指定された場合、対応する結果のクラスも{@code null}となる。
	 * このクラスが提供するほかのメソッドでは、クラスに{@code null}が指定された場合に
	 * {@code null}型を表現するため、このメソッドの実行結果をそのまま利用できる。
	 * </p>
	 * @param arguments 実引数の一覧
	 * @return 対応する型引数の一覧
	 * @throws IllegalArgumentException 引数に{@code null}が指定された場合
	 */
	public static List<Class<?>> toParameterTypes(Object... arguments) {
		if (arguments == null) {
			throw new IllegalArgumentException("arguments is null"); //$NON-NLS-1$
		}
		List<Class<?>> results = new ArrayList<Class<?>>(arguments.length);
		for (Object argument : arguments) {
			if (argument != null) {
				results.add(argument.getClass());
			} else {
				results.add(null);
			}
		}
		return results;
	}
	
	/**
	 * 指定の名前と実引数の型で適用可能なコンストラクタを、指定の一覧の中から検出して返す。
	 * <p>
	 * 複数のメソッドが同時に適用可能である場合、その中から最大限に限定的であるものを計算して返す。
	 * ただし、その結果が単一のコンストラクタとなるとは限らない。
	 * </p>
	 * @param <T> それぞれのコンストラクタが生成するインスタンスの型
	 * @param constructors 候補のコンストラクタ一覧
	 * @param argumentTypes 起動時に利用する実引数の型一覧、この要素に{@code null}が
	 *     含まれる場合、それは{@code null}型を表現する
	 * @return 適用可能なコンストラクタのうち限定的であるもの、発見できない場合は空のリスト
	 * @throws IllegalArgumentException 引数に{@code null}が含まれる場合
	 */
	public static <T>Collection<Constructor<T>> findConstructor(
			Collection<Constructor<T>> constructors,
			List<Class<?>> argumentTypes) {
		if (constructors == null) {
			throw new IllegalArgumentException("constructors is null"); //$NON-NLS-1$
		}
		if (argumentTypes == null) {
			throw new IllegalArgumentException("argumentTypes is null"); //$NON-NLS-1$
		}
		
		Class<?>[] argTypeArray = argumentTypes.toArray(
			new Class<?>[argumentTypes.size()]);
		
		List<Executable<Constructor<T>>> results =
				findPotentiallyApplicables(constructors, argTypeArray);
		
		filterApplicables(results, argTypeArray);
		filterMaximallySpecifics(results);
		return unpack(results);
	}
	
	/**
	 * 指定の名前と実引数の型で適用可能なメソッドを、指定の一覧の中から検出して返す。
	 * <p>
	 * 複数のメソッドが同時に適用可能である場合、その中から最大限に限定的であるものを計算して返す。
	 * ただし、その結果が単一のメソッドとなるとは限らない。
	 * </p>
	 * @param methods 候補のメソッド一覧
	 * @param name 起動するメソッドの名前
	 * @param argumentTypes 起動時に利用する実引数の型一覧、この要素に{@code null}が
	 *     含まれる場合、それは{@code null}型を表現する
	 * @return 適用可能なメソッドのうち限定的であるもの、発見できない場合は空のリスト
	 * @throws IllegalArgumentException 引数に{@code null}が含まれる場合
	 */
	public static Collection<Method> findMethod(
			Collection<Method> methods,
			String name,
			List<Class<?>> argumentTypes) {
		if (methods == null) {
			throw new IllegalArgumentException("methods is null"); //$NON-NLS-1$
		}
		if (name == null) {
			throw new IllegalArgumentException("name is null"); //$NON-NLS-1$
		}
		if (argumentTypes == null) {
			throw new IllegalArgumentException("argumentTypes is null"); //$NON-NLS-1$
		}
		
		Class<?>[] argTypeArray = argumentTypes.toArray(
			new Class<?>[argumentTypes.size()]);
		
		LinkedList<Executable<Method>> results =
				findPotentiallyApplicables(methods, name, argTypeArray);
		
		filterApplicables(results, argTypeArray);
		filterMaximallySpecifics(results);
		return unpack(results);
	}
	
	private static <T extends Member>List<T> unpack(List<Executable<T>> packed) {
		assert packed != null;
		List<T> results = new ArrayList<T>();
		for (Executable<T> exec : packed) {
			results.add(exec.target);
		}
		return results;
	}
	
	/**
	 * メソッドの一覧から、潜在的に適用可能な起動候補の一覧を返す。
	 * <p>
	 * 潜在的に適用可能とは、名前、および実引数の数と仮引数の数が一致することを指す。
	 * </p>
	 * @param candidates 起動候補の一覧
	 * @param name 起動するメソッドの名前
	 * @param argumentTypes 実引数型の一覧
	 * @return 潜在的に適用可能な起動候補の一覧
	 */
	private static LinkedList<Executable<Method>> findPotentiallyApplicables(
			Collection<Method> candidates,
			String name,
			Class<?>[] argumentTypes) {
		assert candidates != null;
		assert name != null;
		assert argumentTypes != null;
		LinkedList<Executable<Method>> results =
				new LinkedList<Executable<Method>>();
		for (Method m : candidates) {
			// 名前が一致すること
			if (m.getName().equals(name) == false) {
				continue;
			}
			// 実引数と仮引数の個数が一致すること
			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != argumentTypes.length) {
				continue;
			}
			results.add(new Executable<Method>(m, paramTypes));
		}
		return results;
	}
	
	/**
	 * コンストラクタの一覧から、潜在的に適用可能な起動候補の一覧を返す。
	 * <p>
	 * 潜在的に適用可能とは、実引数の数と仮引数の数が一致することを指す。
	 * </p>
	 * @param <T> コンストラクタが対象とするインスタンスの種類
	 * @param candidates 起動候補の一覧
	 * @param argumentTypes 実引数型の一覧
	 * @return 潜在的に適用可能な起動候補の一覧
	 */
	private static <T>LinkedList<Executable<Constructor<T>>> findPotentiallyApplicables(
			Collection<Constructor<T>> candidates,
			Class<?>[] argumentTypes) {
		assert candidates != null;
		assert argumentTypes != null;
		LinkedList<Executable<Constructor<T>>> results =
				new LinkedList<Executable<Constructor<T>>>();
		for (Constructor<T> c : candidates) {
			// 実引数と仮引数の個数が一致すること
			Class<?>[] parameterTypes = c.getParameterTypes();
			if (argumentTypes.length != parameterTypes.length) {
				continue;
			}
			// 引数を適用できるコンストラクタがひとつとは限らないので全部列挙
			results.add(new Executable<Constructor<T>>(c, parameterTypes));
		}
		return results;
	}
	
	/**
	 * 起動対象の候補の中から、指定の実引数を適用できないものを除外する。
	 * @param <T> 起動候補の種類
	 * @param candidates 対象の起動候補
	 * @param argumentTypes 実引数型の一覧
	 */
	private static <T extends Member>void filterApplicables(
			Collection<Executable<T>> candidates,
			Class<?>[] argumentTypes) {
		assert candidates != null;
		assert argumentTypes != null;
		for (Iterator<Executable<T>> iter = candidates.iterator(); iter
			.hasNext();) {
			Executable<T> exec = iter.next();
			// 実引数を仮引数に適用できること
			if (isApplicable(argumentTypes, exec.parameterTypes) == false) {
				iter.remove();
			}
		}
	}
	
	/**
	 * 実引数の型を仮引数の型に適用できる場合のみ{@code true}を返す。
	 * <p>
	 * これは、実引数と仮引数の個数が一致し、それぞれの実引数の型が
	 * 対応する仮引数の型のサブタイプであることを検査する。
	 * </p>
	 * @param argumentTypes 実引数の型一覧
	 * @param parameterTypes 仮引数の型一覧
	 * @return 実引数の型を仮引数の型に適用できる場合に{@code true}
	 */
	private static boolean isApplicable(
			Class<?>[] argumentTypes,
			Class<?>[] parameterTypes) {
		assert argumentTypes != null;
		assert parameterTypes != null;
		// 引数の個数が一致していないと失敗
		if (argumentTypes.length != parameterTypes.length) {
			return false;
		}
		// ひとつでも適用できない実引数があれば失敗
		for (int i = 0; i < argumentTypes.length; i++) {
			if (isApplicable(argumentTypes[i], parameterTypes[i]) == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 指定の型を持つ実引数を、指定の型を持つ仮引数に適用できる場合のみ{@code true}を返す。
	 * <p>
	 * 実引数、仮引数のいずれか一方<em>のみ</em>がプリミティブ型を表現する場合、
	 * その型を対応するラッパー形に変換した後、
	 * </p>
	 * @param argumentType 実引数の型
	 * @param parameterType 仮引数の型
	 * @return 実引数を駆り引数に適用できる場合のみ{@code true}
	 */
	private static boolean isApplicable(Class<?> argumentType, Class<?> parameterType) {
		// 引数型がnullやvoidであることはありえない
		assert parameterType != null;
		assert parameterType != void.class;
		
		// SomeType v = null; は常にOK
		if (isNullType(argumentType)) {
			return true;
		}
		// Class.isAssignbleFrom(Class) で簡易検査
		if (parameterType.isAssignableFrom(argumentType)) {
			return true;
		}
		
		boolean primitiveArgument = argumentType.isPrimitive();
		boolean primitiveParameter = parameterType.isPrimitive();
		if (primitiveArgument && primitiveParameter == false) {
			Class<?> boxed = BOXING.get(argumentType);
			assert boxed != null;
			return isApplicable(boxed, parameterType);
		}
		if (primitiveParameter && primitiveArgument == false) {
			Class<?> boxed = BOXING.get(parameterType);
			assert boxed != null;
			return isApplicable(argumentType, boxed);
		}
		
		return false;
	}
	
	/**
	 * 指定の値が{@code null}型を表現する場合にのみ{@code true}を返す。
	 * @param aClass 対象の値
	 * @return 指定の値が{@code null}型を表現する場合にのみ{@code true}
	 */
	private static boolean isNullType(Class<?> aClass) {
		return aClass == null;
	}
	
	/**
	 * 起動候補から最大限に限定的でないものを除外する。
	 * @param <T> 起動候補の種類
	 * @param candidates 対象の起動候補
	 */
	private static <T extends Member>void filterMaximallySpecifics(
			List<Executable<T>> candidates) {
		assert candidates != null;
		
		// 引数リストのテーブルを作る
		Class<?>[][] table = new Class<?>[candidates.size()][];
		for (int i = 0; i < table.length; i++) {
			table[i] = candidates.get(i).parameterTypes;
		}
		
		for (int i = 0; i < table.length; i++) {
			if (table[i] == null) {
				continue; // less applicableですでに候補から外れてる
			}
			// ms[i]のほうがms[j]よりもmore applicableなら、ms[j]を候補から消す
			// 本来は「厳密に限定的」である必要があるが、ここでは省略
			// FIXME　for f(.., int, ..), f(.., Integer, ..)
			for (int j = 0; j < table.length; j++) {
				if (i == j) {
					continue; // 自分自身はスルー
				}
				if (table[j] == null) {
					continue; // less applicableですでに候補から外れてる
				}
				if (isMoreSpecific(table[i], table[j])) {
					table[j] = null; // less applicable は除去
				}
			}
		}
		
		// 限定的でないものを除外
		int index = 0;
		for (Iterator<Executable<T>> iter = candidates.iterator(); iter.hasNext();) {
			iter.next();
			if (table[index] == null) {
				iter.remove();
			}
			index++;
		}
	}
	
	/**
	 * 第1引数に含まれるクラスの一覧が、第2引数に含まれるクラスの一覧よりも限定的である場合に
	 * {@code true}を返す。
	 * <p>
	 * クラスの列{@code a}がクラスの列{@code b}よりも限定的であるとは、
	 * 次の条件をすべて満たす場合に限られる。
	 * </p>
	 * <ul>
	 * <li> すべての{@code i}において、{@code a[i] <: b[i]}である </li>
	 * <li> {@code a[i] < b[i]}であるような{@code i}が1つ以上存在する </li>
	 * </ul>
	 * <p>
	 * このとき、{@code a}, {@code b}の要素数は同一でなければならない。
	 * また、いずれかの配列に含まれる{@code null}は、{@code null}型を表現するものとする。
	 * </p>
	 * @param a 限定的であることが検査される型の一覧
	 * @param b 限定的であることを検査する型の一覧
	 * @return {@code a}が{@code b}よりも限定的である場合に{@code true}
	 */
	private static boolean isMoreSpecific(Class<?>[] a, Class<?>[] b) {
		assert a != null;
		assert b != null;
		assert a.length == b.length;
		boolean result = false;
		for (int i = 0; i < a.length; i++) {
			if (a[i] == b[i]) {
				// 同じ型なら次の引数で判定
			} else if (isMoreSpecific(a[i], b[i])) {
				// a[i]とb[i]が違う型で、a[i]をb[i]に適用できると、aのほうが限定的になる可能性がある
				result = true;
			} else {
				// それ以外では、aとbは関係ない型なので、aはbよりも限定的とはいえない
				return false;
			}
		}
		return result;
	}
	
	/**
	 * {@code a}が{@code b}よりも限定的である場合にのみ{@code true}を返す。
	 * <p>
	 * このメソッドはプリミティブ型の取り扱いが{@link #isApplicable(Class, Class)}と多少異なる。
	 * ボクシング変換が利用されるのは{@code a}のみで、{@code b}には適用されない。
	 * </p>
	 * <ul>
	 * <li> {@code a <: b} </li>
	 * <li> {@code boxing(a) <: b} </li>
	 * </ul>
	 * @param a 比較される型
	 * @param b 比較する型
	 * @return {@code a}が{@code b}よりも限定的である場合に{@code true}
	 */
	private static boolean isMoreSpecific(Class<?> a, Class<?> b) {
		assert a != null;
		assert b != null;
		// Class.isAssignbleFrom(Class) で簡易検査
		if (b.isAssignableFrom(a)) {
			return true;
		}
		if (a.isPrimitive()) {
			Class<?> aBoxed = BOXING.get(a);
			assert aBoxed != null;
			return b.isAssignableFrom(aBoxed);
		}
		return false;
	}
	

	/**
	 * 任意の起動候補を表す。
	 * <p>
	 * {@link Method}と{@link Constructor}を同時に取り扱うための型。
	 * </p>
	 * @version $Date: 2009-10-09 19:03:38 +0900 (金, 09 10 2009) $
	 * @author Suguru ARAKAWA
	 * @param <T> 内包するメンバ
	 */
	private static final class Executable<T extends Member> {
		
		/**
		 * 実行対象のオブジェクト。
		 */
		final T target;
		
		/**
		 * 引数の型一覧。
		 */
		final Class<?>[] parameterTypes;
		

		/**
		 * インスタンスを生成する。
		 * @param target 実行対象
		 * @param parameterTypes 引数リスト
		 */
		Executable(T target, Class<?>[] parameterTypes) {
			assert target != null;
			assert parameterTypes != null;
			this.target = target;
			this.parameterTypes = parameterTypes;
		}
		
		@Override
		public String toString() {
			return target.toString();
		}
	}
}
