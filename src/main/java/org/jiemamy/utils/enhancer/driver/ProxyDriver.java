/*
 * Copyright 2009 the Seasar Foundation and the Others.
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
package org.jiemamy.utils.enhancer.driver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;

import org.jiemamy.utils.enhancer.Enhance;
import org.jiemamy.utils.enhancer.Invocation;
import org.jiemamy.utils.enhancer.InvocationHandler;
import org.jiemamy.utils.enhancer.InvocationPointcut;

/**
 * {@link java.lang.reflect.Proxy}のインスタンスを生成するための{@link Enhance}を作成する。
 * <p>
 * このクラスを利用して作成したエンハンスは、インターフェースのインスタンス生成に関するもののみである。
 * そのため、
 * {@link org.jiemamy.utils.enhancer.FactoryEnhancer}でこれを利用することはできず
 * {@link org.jiemamy.utils.enhancer.InterfaceEnhancer}のみで利用できる。
 * </p>
 * <p>
 * また、このクラスを利用して作成したエンハンスは、インスタンス生成時に既定のインスタンス生成プロセスを利用せず、
 * {@code java.lang.reflect.Proxy}を利用してインスタンスを直接生成する。
 * このため、コンストラクタ起動に対するエンハンスのチェインがあった場合においても、
 * このクラスを利用して作成したエンハンスは、後続するエンハンスのチェインを利用しない。
 * そのため、インスタンス生成に対してさらに拡張を行う場合、このエンハンスは
 * エンハンスチェインの末尾におかれる必要がある。
 * </p>
 * @version 0.2.0
 * @since 0.2.0
 * @author Suguru ARAKAWA
 */
public class ProxyDriver {
	
	/**
	 * {@link java.lang.reflect.Proxy}を利用したエンハンスを作成する。
	 * <p>
	 * 返されるエンハンスは、指定のインターフェースが表すインターフェースプロダクトを作成する際に、
	 * 本来のプロダクトインスタンスではなく{@link java.lang.reflect.Proxy}を利用した
	 * プロクシインスタンスを生成して返す。
	 * その際に利用する{@link java.lang.reflect.InvocationHandler}は
	 * 引数に指定したものを利用する。
	 * このハンドラオブジェクトは、同一のエンハンスが生成するすべてのプロクシインスタンスで共有する。
	 * <p>
	 * @param anInterface Proxyを作成する対象のインターフェース
	 * @param handler 指定のインターフェースに対するメソッド呼び出しをハンドルするハンドラ
	 *     これはfactory-enhancerの{@link InvocationHandler}ではなく、
	 *     標準の{@link java.lang.reflect.InvocationHandler}である
	 * @return 指定のインターフェースのメソッドを指定のハンドラでフックするProxyを生成するエンハンス
	 * @throws IllegalArgumentException 引数{@code anInterface}がProxyを作成できない型である場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static Enhance newEnhance(Class<?> anInterface, java.lang.reflect.InvocationHandler handler) {
		if (anInterface == null) {
			throw new NullPointerException("anInterface is null"); //$NON-NLS-1$
		}
		if (handler == null) {
			throw new NullPointerException("handler is null"); //$NON-NLS-1$
		}
		
		// Proxyクラスを作って、それをインスタンス化するコンストラクタを取り出す
		Class<?> proxyClass = Proxy.getProxyClass(anInterface.getClassLoader(), anInterface);
		Constructor<?> constructor;
		try {
			constructor = proxyClass.getConstructor(java.lang.reflect.InvocationHandler.class);
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
		return new Enhance(
				new ProxyPointcut(anInterface.getName()),
				new ProxyHandler(constructor, handler));
	}
	
	/**
	 * インスタンス生成の禁止。
	 */
	private ProxyDriver() {
		throw new AssertionError();
	}
	

	/**
	 * プロクシ作成の対象となるインターフェースプロダクト生成へのポイントカット。
	 * @version $Date$
	 * @author Suguru ARAKAWA
	 */
	private static class ProxyPointcut implements InvocationPointcut {
		
		/**
		 * Proxyインスタンス生成の対象とするインターフェースの名前
		 */
		private String targetName;
		

		/**
		 * インスタンスを生成する。
		 * @param targetName 対象インターフェースの名称
		 */
		public ProxyPointcut(String targetName) {
			assert targetName != null;
			this.targetName = targetName;
		}
		
		public boolean isTarget(CtClass self, CtBehavior behavior) {
			// インスタンス生成のみ
			if ((behavior instanceof CtConstructor) == false) {
				return false;
			}
			
			// 対象インターフェースのみ
			if (self.getName().equals(targetName) == false) {
				return false;
			}
			return true;
		}
	}
	
	private static class ProxyHandler implements InvocationHandler {
		
		/**
		 * Proxyインスタンスを生成するためのコンストラクタ。
		 */
		private Constructor<?> proxyCreator;
		
		/**
		 * Proxyインスタンスに渡す{@code java.lang.reflect.InvocationHandler}。
		 */
		private java.lang.reflect.InvocationHandler handler;
		

		/**
		 * インスタンスを生成する。
		 * @param proxyCreator 該当Proxyのインスタンスを生成するコンストラクタ
		 * @param handler 該当Proxyの各メソッド呼び出しに対するハンドラ
		 */
		public ProxyHandler(
				Constructor<?> proxyCreator,
				java.lang.reflect.InvocationHandler handler) {
			assert proxyCreator != null;
			assert handler != null;
			this.proxyCreator = proxyCreator;
			this.handler = handler;
		}
		
		/**
		 * この実装では{@code invocation.proceed()}を実行せずに、
		 * 代わりにProxyインスタンスを生成して返す。
		 */
		public Object handle(Invocation invocation) throws Throwable {
			assert invocation != null;
			return proxyCreator.newInstance(new Object[] {
				handler
			});
		}
	}
}
