/*
 * Copyright 2007-2009 Jiemamy Project and the Others.
 * Created on 2009/10/08
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
package org.jiemamy.utils.enhancer.driver;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.junit.Before;

import org.jiemamy.utils.enhancer.Invocation;
import org.jiemamy.utils.enhancer.InvocationHandler;
import org.jiemamy.utils.enhancer.InvocationPointcut;

/**
 * このパッケージのテストで利用するテスト基底クラス。
 * @version $Id$
 * @author Suguru ARAKAWA
 */
public class DriverTestRoot {
	
	/**
	 * 現在のクラスローダを元に構築したクラスプール。
	 */
	protected ClassPool pool;
	

	/**
	 * テストを初期化する。
	 * @throws Exception if occur
	 */
	@Before
	public void setUp() throws Exception {
		pool = new ClassPool();
		pool.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));
	}
	
	/**
	 * コンストラクタ起動を指定のハンドラにハンドルさせる。
	 * @param handler 対象のハンドラ
	 * @param constructor 対象のコンストラクタ
	 * @param arguments 引数リスト
	 * @return {@code handler.handle()}の実行結果
	 * @throws Throwable {@code handler.handle()}の実行時に例外が発生した場合
	 */
	protected Object construct(
			InvocationHandler handler,
			final Constructor<?> constructor,
			final Object... arguments) throws Throwable {
		Invocation invocation = new Invocation() {
			
			public Object proceed() throws InvocationTargetException {
				try {
					return constructor.newInstance(arguments);
				} catch (InstantiationException e) {
					throw new AssertionError(e);
				} catch (IllegalAccessException e) {
					throw new AssertionError(e);
				}
			}
			
			public Member getTarget() {
				return constructor;
			}
			
			public Object getInvoker() {
				return constructor.getDeclaringClass();
			}
			
			public Object[] getArguments() {
				return arguments;
			}
		};
		return handler.handle(invocation);
	}
	
	/**
	 * コンストラクタ起動を指定のハンドラにハンドルさせる。
	 * @param handler 対象のハンドラ
	 * @param method 対象のメソッド
	 * @param self 対象のコンテキスト(may be {@code this} keyword)
	 * @param arguments 引数リスト
	 * @return {@code handler.handle()}の実行結果
	 * @throws Throwable {@code handler.handle()}の実行時に例外が発生した場合
	 */
	protected Object invoke(
			InvocationHandler handler,
			final Method method,
			final Object self,
			final Object... arguments) throws Throwable {
		Invocation invocation = new Invocation() {
			
			public Object proceed() throws InvocationTargetException {
				try {
					return method.invoke(self, arguments);
				} catch (IllegalAccessException e) {
					throw new AssertionError();
				}
			}
			
			public Member getTarget() {
				return method;
			}
			
			public Object getInvoker() {
				return self;
			}
			
			public Object[] getArguments() {
				return arguments;
			}
		};
		return handler.handle(invocation);
	}
	
	/**
	 * 対象のポイントカットに対し、指定のメソッドが対象となるかどうかを表明する。
	 * @param targetOrNot {@code true}ならば対象であることを表明する
	 * @param pointcut テスト対象のポイントカット
	 * @param selfClass メソッドを公開するクラス
	 * @param declaringClass メソッドを実際に宣言するクラス
	 * @param name メソッド名
	 * @param descriptor メソッドデスクリプタ
	 */
	protected void assertTargetMethod(
			boolean targetOrNot,
			InvocationPointcut pointcut,
			Class<?> selfClass,
			Class<?> declaringClass,
			String name,
			String descriptor) {
		try {
			CtClass ctSelfClass = pool.get(selfClass.getName());
			CtClass ctDeclaringClass = pool.get(declaringClass.getName());
			CtMethod method = findMethod(ctDeclaringClass, name, descriptor);
			assertThat(
					ctSelfClass.getName() + "." + name + descriptor,
					pointcut.isTarget(ctSelfClass, method),
					is(targetOrNot));
		} catch (NotFoundException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * 対象のポイントカットに対し、指定のメソッドが対象となるかどうかを表明する。
	 * @param targetOrNot {@code true}ならば対象であることを表明する
	 * @param pointcut テスト対象のポイントカット
	 * @param selfClass コンストラクタを公開するクラス
	 * @param declaringClass コンストラクタを実際に宣言するクラス
	 * @param descriptor メソッドデスクリプタ
	 */
	protected void assertTargetConstructor(
			boolean targetOrNot,
			InvocationPointcut pointcut,
			Class<?> selfClass,
			Class<?> declaringClass,
			String descriptor) {
		try {
			CtClass ctSelfClass = pool.get(selfClass.getName());
			CtClass ctDeclaringClass = pool.get(declaringClass.getName());
			CtConstructor ctor = findConstructor(ctDeclaringClass, descriptor);
			assertThat(
					ctSelfClass.getName() + descriptor,
					pointcut.isTarget(ctSelfClass, ctor),
					is(targetOrNot));
		} catch (NotFoundException e) {
			throw new AssertionError(e);
		}
	}
	
	private CtMethod findMethod(
			CtClass declaring,
			String name,
			String descriptor) {
		for (CtMethod method : declaring.getDeclaredMethods()) {
			if (method.getName().equals(name) == false) {
				continue;
			}
			if (method.getSignature().equals(descriptor) == false) {
				continue;
			}
			return method;
		}
		throw new AssertionError(declaring.getName() + "." + name + descriptor);
	}
	
	private CtConstructor findConstructor(
			CtClass declaring,
			String descriptor) {
		for (CtConstructor ctor : declaring.getDeclaredConstructors()) {
			if (ctor.isClassInitializer()) {
				continue;
			}
			if (ctor.getSignature().equals(descriptor) == false) {
				continue;
			}
			return ctor;
		}
		throw new AssertionError(declaring.getName() + "." + descriptor);
	}
}
