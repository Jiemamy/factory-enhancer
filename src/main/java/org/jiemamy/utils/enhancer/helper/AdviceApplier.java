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
package org.jiemamy.utils.enhancer.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.List;

import org.jiemamy.utils.enhancer.EnhanceException;
import org.jiemamy.utils.enhancer.Invocation;
import org.jiemamy.utils.enhancer.InvocationHandler;

/**
 * {@link InvocationHandler}によるアドバイスを適用する。
 * <p>
 * アドバイスは対象のポイントカットに対する{@link InvocationHandler}の列からなる。
 * アドバイスを適用する際には、実際のメソッド呼び出しやインスタンス生成の処理を
 * {@link Invocation}オブジェクトにカプセル化し、対応する{@link InvocationHandler}の列の
 * {@link InvocationHandler#handle(Invocation)}に与えることで実現する。
 * </p>
 * <p>
 * アドバイステーブルはこのオブジェクトの配列として定義される。
 * それぞれのポイントカットにおいて、アドバイステーブルから対応するこのクラスのオブジェクトを取り出し、
 * それぞれのオブジェクトに対して{@link #invoke(Object, Object[])}メソッドを呼び出すことで
 * インターセプトを実現する。
 * </p>
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class AdviceApplier {
	
	private static class ConstructorDefinition implements Definition {
		
		private final Constructor<?> original;
		
		private final Constructor<?> actual;
		

		/**
		 * インスタンスを生成する。
		 * @param original
		 *      本来呼び出されるべきコンストラクタ。
		 *      {@link #newInvocation(Object, Object[])}によって返されるオブジェクトは
		 *      本来このコンストラクタを呼び出そうとしていたことをユーザに通知することができる。
		 *      ただし、このコンストラクタが実際に呼び出されるかどうかは適用される拡張による
		 * @param actual
		 *      実際に呼び出されるコンストラクタ。
		 *      {@link #newInvocation(Object, Object[])}によって返されるオブジェクトは
		 *      {@link Invocation#proceed()}が呼び出された際に
		 *      このコンストラクタを呼び出す
		 */
		public ConstructorDefinition(Constructor<?> original, Constructor<?> actual) {
			super();
			assert original != null;
			assert actual != null;
			this.original = original;
			this.actual = actual;
		}
		
		public Invocation newInvocation(Object object, Object[] arguments) {
			return new ConstructorInvocation(original, actual, object, arguments);
		}
		
		@Override
		public String toString() {
			return MessageFormat.format(
					"{0}#{1}({2})", //$NON-NLS-1$
					original.getDeclaringClass().getName(), original.getDeclaringClass().getSimpleName(),
					toParams(original.getParameterTypes()));
		}
	}
	
	/**
	 * メソッドまたはコンストラクタの宣言を表現するインターフェース。
	 * <p>
	 * 種類にあった{@link Invocation}オブジェクトを透過的に生成するために利用される。
	 * </p>
	 * @version $Date$
	 * @author Suguru ARAKAWA
	 * @see MethodInvocation
	 * @see ConstructorInvocation
	 */
	private interface Definition {
		
		/**
		 * 指定の起動オブジェクト、起動引数を持つ{@link Invocation}オブジェクトを
		 * 新しく生成して返す。
		 * @param invoker 起動オブジェクト
		 * @param arguments 引数リスト
		 * @return 生成した{@link Invocation}オブジェクト
		 */
		Invocation newInvocation(Object invoker, Object[] arguments);
	}
	
	/**
	 * 特定のメソッドの宣言を表現するクラス。
	 * @version $Date$
	 * @author Suguru ARAKAWA
	 * @see MethodInvocation
	 */
	private static class MethodDefinition implements Definition {
		
		private final Method original;
		
		private final Method actual;
		

		/**
		 * インスタンスを生成する。
		 * @param original
		 *      本来呼び出されるべきメソッド。
		 *      {@link #newInvocation(Object, Object[])}によって返されるオブジェクトは
		 *      本来このメソッドを呼び出そうとしていたことをユーザに通知することができる。
		 *      ただし、このメソッドが実際に呼び出されるかどうかは適用される拡張による
		 * @param actual
		 *      実際に呼び出されるメソッド。
		 *      {@link #newInvocation(Object, Object[])}によって返されるオブジェクトは
		 *      {@link Invocation#proceed()}が呼び出された際に
		 *      このメソッドを呼び出す
		 */
		public MethodDefinition(Method original, Method actual) {
			super();
			assert original != null;
			assert actual != null;
			this.original = original;
			this.actual = actual;
		}
		
		public Invocation newInvocation(Object object, Object[] arguments) {
			return new MethodInvocation(original, actual, object, arguments);
		}
		
		@Override
		public String toString() {
			return MessageFormat.format("{0}#{1}({2})", //$NON-NLS-1$
					original.getDeclaringClass().getName(), original.getName(), toParams(original.getParameterTypes()));
		}
	}
	

	/**
	 * メソッドに関するこのクラスのインスタンスを生成して返す。
	 * <p>
	 * {@code handlers}に含まれるハンドラの個数を{@code n}とおくと、
	 * 返されるオブジェクトの{@link #invoke(Object, Object[])}を呼び出した際に
	 * {@code handlers[n - 1]}の
	 * {@link InvocationHandler#handle(Invocation) handleメソッド}がまず呼び出される。
	 * そこに渡された{@link Invocation}の{@link Invocation#proceed() proceed()}を
	 * 実行すると、{@code handlers[n - 2]}の
	 * {@link InvocationHandler#handle(Invocation) handleメソッド}が呼び出される。
	 * 一般的には、{@code handlers[i]}に渡された{@link Invocation}に対して、
	 * {@link Invocation#proceed() proceed()}を実行すると、
	 * {@code handlers[i - 1]}に渡された
	 * {@link InvocationHandler#handle(Invocation)}が呼びだされる。
	 * また、{@code handlers[0]}に渡される{@link Invocation}は、
	 * {@link Invocation#proceed() proceed()}を実行することで
	 * この呼び出し時に指定した実際のコンストラクタを実行することができる。
	 * それぞれの{@link Invocation}に対する{@link Invocation#getArguments()}の値は
	 * これらの一連の連鎖に対して伝搬される。
	 * </p>
	 * <p>
	 * また、{@code handlers}に一つもハンドラが指定されなかった場合、
	 * 返されるオブジェクトの{@link #invoke(Object, Object[])}は
	 * この呼び出し時に指定した実際のコンストラクタを直接実行する。
	 * </p>
	 * @param handlers ハンドラ一覧
	 * @param originalType オリジナルのコンストラクタが定義された型
	 * @param actualType 実際にコンストラクタが定義された型
	 * @param parameterTypes 仮引数の型一覧
	 * @return このクラスのインスタンス
	 * @throws EnhanceException コンストラクタの検出に失敗した場合
	 * @throws NullPointerException いずれかの引数に{@code null}が指定された場合
	 */
	public static AdviceApplier constructor(List<? extends InvocationHandler> handlers, Class<?> originalType,
			Class<?> actualType, Class<?>[] parameterTypes) throws EnhanceException {
		
		if (handlers == null) {
			throw new NullPointerException("targetType"); //$NON-NLS-1$
		}
		if (originalType == null) {
			throw new NullPointerException("originalType"); //$NON-NLS-1$
		}
		if (actualType == null) {
			throw new NullPointerException("actualType"); //$NON-NLS-1$
		}
		if (parameterTypes == null) {
			throw new NullPointerException("parameterTypes"); //$NON-NLS-1$
		}
		Constructor<?> original = load(originalType, parameterTypes);
		Constructor<?> actual = load(actualType, parameterTypes);
		Definition definition = new ConstructorDefinition(original, actual);
		return new AdviceApplier(definition, handlers);
	}
	
	private static Constructor<?> load(Class<?> type, Class<?>[] params) throws EnhanceException {
		try {
			return type.getConstructor(params);
		} catch (Exception e) {
			throw new EnhanceException(MessageFormat.format("Cannot load constructor {0}#{1}({2})", type, type
				.getSimpleName(), toParams(params)), e);
		}
	}
	
	private static Method load(Class<?> type, String name, Class<?>[] params) throws EnhanceException {
		try {
			return type.getMethod(name, params);
		} catch (Exception e) {
			throw new EnhanceException(MessageFormat.format("Cannot load method {0}#{1}({2})", type, name,
					toParams(params)), e);
		}
	}
	
	/**
	 * メソッドに関するこのクラスのインスタンスを生成して返す。
	 * <p>
	 * {@code handlers}に含まれるハンドラの個数を{@code n}とおくと、
	 * 返されるオブジェクトの{@link #invoke(Object, Object[])}を呼び出した際に
	 * {@code handlers[n - 1]}の
	 * {@link InvocationHandler#handle(Invocation) handleメソッド}がまず呼び出される。
	 * そこに渡された{@link Invocation}の{@link Invocation#proceed() proceed()}を
	 * 実行すると、{@code handlers[n - 2]}の
	 * {@link InvocationHandler#handle(Invocation) handleメソッド}が呼び出される。
	 * 一般的には、{@code handlers[i]}に渡された{@link Invocation}に対して、
	 * {@link Invocation#proceed() proceed()}を実行すると、
	 * {@code handlers[i - 1]}に渡された
	 * {@link InvocationHandler#handle(Invocation)}が呼びだされる。
	 * また、{@code handlers[0]}に渡される{@link Invocation}は、
	 * {@link Invocation#proceed() proceed()}を実行することで
	 * この呼び出し時に指定した実際のメソッドを呼びだすことができる。
	 * それぞれの{@link Invocation}に対する{@link Invocation#getArguments()}の値は
	 * これらの一連の連鎖に対して伝搬される。
	 * </p>
	 * <p>
	 * また、{@code handlers}に一つもハンドラが指定されなかった場合、
	 * 返されるオブジェクトの{@link #invoke(Object, Object[])}は
	 * この呼び出し時に指定した実際のメソッドを直接実行する。
	 * </p>
	 * @param handlers ハンドラ一覧
	 * @param originalType オリジナルのメソッドを定義するクラス
	 * @param originalName オリジナルのメソッド名
	 * @param actualType 実際に呼び出すメソッドを定義するクラス
	 * @param actualName 実際に呼び出すメソッド名
	 * @param parameterTypes 仮引数の型一覧
	 * @return このクラスのインスタンス
	 * @throws EnhanceException メソッドの検出に失敗した場合
	 * @throws NullPointerException いずれかの引数に{@code null}が指定された場合
	 */
	public static AdviceApplier method(List<? extends InvocationHandler> handlers, Class<?> originalType,
			String originalName, Class<?> actualType, String actualName, Class<?>[] parameterTypes)
			throws EnhanceException {
		
		if (handlers == null) {
			throw new NullPointerException("targetType"); //$NON-NLS-1$
		}
		if (originalType == null) {
			throw new NullPointerException("originalType"); //$NON-NLS-1$
		}
		if (originalName == null) {
			throw new NullPointerException("originalName"); //$NON-NLS-1$
		}
		if (actualType == null) {
			throw new NullPointerException("actualType"); //$NON-NLS-1$
		}
		if (actualName == null) {
			throw new NullPointerException("actualName"); //$NON-NLS-1$
		}
		if (parameterTypes == null) {
			throw new NullPointerException("parameterTypes"); //$NON-NLS-1$
		}
		Method original = load(originalType, originalName, parameterTypes);
		Method actual = load(actualType, actualName, parameterTypes);
		Definition definition = new MethodDefinition(original, actual);
		return new AdviceApplier(definition, handlers);
	}
	
	/**
	 * 型の一覧をカンマ区切りで列挙した文字列を返す。
	 * @param params 型の一覧
	 * @return カンマ区切りで列挙した文字列
	 */
	static String toParams(Class<?>[] params) {
		assert params != null;
		if (params.length == 0) {
			return "()"; //$NON-NLS-1$
		}
		StringBuilder buf = new StringBuilder();
		buf.append('(');
		buf.append(params[0].getName());
		for (int i = 1; i < params.length; i++) {
			buf.append(',');
			buf.append(params[i].getName());
		}
		buf.append(')');
		return buf.toString();
	}
	

	private final Definition definition;
	
	private final List<? extends InvocationHandler> handlers;
	

	private AdviceApplier(Definition definition, List<? extends InvocationHandler> handlers) {
		super();
		assert definition != null;
		assert handlers != null;
		this.definition = definition;
		this.handlers = handlers;
	}
	
	/**
	 * このヘルパを利用してメソッドまたはコンストラクタを起動する。
	 * @param object 呼び出し用のオブジェクト (インスタンスメソッド以外では{@code null})
	 * @param arguments 実引数リスト
	 * @return ハンドラと実際の呼び出しの実行結果
	 * @throws Throwable 呼び出し先、またはハンドラの処理で例外が発生した場合
	 */
	public Object invoke(Object object, Object[] arguments) throws Throwable {
		Invocation current = definition.newInvocation(object, arguments);
		for (InvocationHandler h : handlers) {
			current = new DelegateInvocation(current, h);
		}
		try {
			return current.proceed();
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	/**
	 * このオブジェクトの文字列表現を返す。
	 * <p>
	 * 返される文字列はハンドラに関する情報と、呼び出し先に関する情報を含む。
	 * ただし、形式は変更される可能性があるので、デバッグ用途意外に使ってはならない。
	 * </p>
	 * @return このオブジェクトのデバッグ用の文字列表現
	 */
	@Override
	public String toString() {
		return MessageFormat.format("{0}->{1}", //$NON-NLS-1$
				handlers, definition);
	}
}
