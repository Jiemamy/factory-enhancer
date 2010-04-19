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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.ConstructorInvocation;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.jiemamy.utils.enhancer.Invocation;
import org.jiemamy.utils.enhancer.InvocationHandler;

/**
 * AOP Allianceが提供するインターセプタの定義を、Jiemamyが提供する{@link InvocationHandler}に変換するドライバ。
 * <p>
 * それぞれのインターセプタに渡される{@link org.aopalliance.intercept.Invocation}は
 * 本来の仕様と多少異なる場合がある。
 * たとえば、 {@link org.aopalliance.intercept.Invocation#getStaticPart()}は
 * インターセプタのチェインがインストールされた実体を正確に指さない場合がある。
 * また、その値を利用する
 * {@link MethodInvocation#getMethod()}, {@link ConstructorInvocation#getConstructor()}は
 * なども同様に正しい値を返さない場合があるので、注意が必要である。
 * </p>
 * 
 * @version 0.2.0
 * @since 0.2.0
 * @author Suguru ARAKAWA
 * @see <a href="http://aopalliance.sourceforge.net/">AOP Alliance</a>
 */
public class AopAllianceDriver {
	
	/**
	 * 指定の{@link MethodInterceptor}に処理を委譲する{@link InvocationHandler}を生成して返す。
	 * <p>
	 * 返されるハンドラがメソッド起動をハンドルした場合、
	 * 引数に渡された {@link MethodInterceptor#invoke(MethodInvocation)}に対して処理を委譲する。
	 * この引数に渡される{@link MethodInvocation}オブジェクトは、
	 * {@link InvocationHandler}が受け取った{@link Invocation}オブジェクトをラップしたものである。
	 * </p>
	 * <p>
	 * 返されるハンドラがインスタンス生成処理をハンドルした場合、
	 * そのハンドラは次のハンドラまたは実際の処理を直接実行する ({@link Invocation#proceed()})。
	 * </p>
	 * 
	 * @param interceptor 委譲先のインターセプタ
	 * @return 引数に渡されたインターセプタ
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static InvocationHandler toHandler(MethodInterceptor interceptor) {
		if (interceptor == null) {
			throw new NullPointerException("interceptor is null"); //$NON-NLS-1$
		}
		return new MethodInterceptorDriver(interceptor);
	}
	
	/**
	 * 指定の{@link ConstructorInterceptor}に処理を委譲する{@link InvocationHandler}を生成して返す。
	 * <p>
	 * 返されるハンドラがインスタンス生成処理をハンドルした場合、
	 * 引数に渡された {@link ConstructorInterceptor#construct(ConstructorInvocation)}に対して処理を委譲する。
	 * この引数に渡される{@link ConstructorInvocation}オブジェクトは、
	 * {@link InvocationHandler}が受け取った{@link Invocation}オブジェクトをラップしたものである。
	 * </p>
	 * <p>
	 * 返されるハンドラがメソッド起動をハンドルした場合、
	 * そのハンドラは次のハンドラまたは実際の処理を直接実行する ({@link Invocation#proceed()})。
	 * </p>
	 * 
	 * @param interceptor 委譲先のインターセプタ
	 * @return 引数に渡されたインターセプタ
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static InvocationHandler toHandler(ConstructorInterceptor interceptor) {
		if (interceptor == null) {
			throw new IllegalArgumentException("interceptor is null"); //$NON-NLS-1$
		}
		return new ConstructorInterceptorDriver(interceptor);
	}
	
	/**
	 * {@link Invocation#proceed() invocation.proceed()}を実行する。
	 * @param invocation {@link Invocation#proceed()}を実行する対象
	 * @return {@code invocation.proceed()}の実行結果
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @throws IllegalArgumentException {@code invocation.proceed()}が同例外をスローする場合
	 * @throws Throwable {@code invocation.proceed()}が例外を返す場合
	 */
	static Object doProceed(Invocation invocation) throws Throwable {
		if (invocation == null) {
			throw new NullPointerException("invocation is null"); //$NON-NLS-1$
		}
		try {
			return invocation.proceed();
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
	
	/**
	 * インスタンス生成の禁止。
	 */
	private AopAllianceDriver() {
		throw new AssertionError();
	}
	

	/**
	 * {@link MethodInterceptor}をラップして{@link InvocationHandler}としての振る舞いを提供する。
	 * 
	 * @version $Id: AopAllianceDriver.java 3738 2009-10-09 10:03:38Z ashigeru $
	 * @author Suguru ARAKAWA
	 */
	private static class MethodInterceptorDriver implements InvocationHandler {
		
		private MethodInterceptor interceptor;
		

		/**
		 * インスタンスを生成する。
		 * 
		 * @param interceptor ラップする {@link MethodInterceptor}
		 */
		public MethodInterceptorDriver(MethodInterceptor interceptor) {
			assert interceptor != null;
			this.interceptor = interceptor;
		}
		
		public Object handle(Invocation invocation) throws Throwable {
			if ((invocation.getTarget() instanceof Method) == false) {
				return doProceed(invocation);
			}
			return interceptor.invoke(new MethodInvocationDriver(invocation));
		}
	}
	
	/**
	 * {@link ConstructorInterceptor}をラップして{@link InvocationHandler}としての振る舞いを提供する。
	 * 
	 * @version $Id: AopAllianceDriver.java 3738 2009-10-09 10:03:38Z ashigeru $
	 * @author Suguru ARAKAWA
	 */
	private static class ConstructorInterceptorDriver implements InvocationHandler {
		
		private ConstructorInterceptor interceptor;
		

		/**
		 * インスタンスを生成する。
		 * 
		 * @param interceptor ラップする {@link ConstructorInterceptor}
		 */
		public ConstructorInterceptorDriver(ConstructorInterceptor interceptor) {
			assert interceptor != null;
			this.interceptor = interceptor;
		}
		
		public Object handle(Invocation invocation) throws Throwable {
			if ((invocation.getTarget() instanceof Constructor<?>) == false) {
				return doProceed(invocation);
			}
			return interceptor.construct(new ConstructorInvocationDriver(invocation));
		}
	}
	
	/**
	 * {@link org.jiemamy.utils.enhancer.Invocation}をラップして、
	 * {@link org.aopalliance.intercept.Invocation}としての振る舞いを提供する。
	 * <p>
	 * ただし、AOP AllienceのAPIでは、
	 * {@code org.aopalliance.intercept.Invocation}インターフェースを直接実装する
	 * クラスは存在せず、それぞれ{@link MethodInvocation}や{@link ConstructorInvocation}
	 * として提供しているようである。
	 * このため、このクラスは抽象クラスとして提供し、そのサブクラスでそれぞれのインターフェースを実装している。
	 * </p>
	 * @version $Id: AopAllianceDriver.java 3738 2009-10-09 10:03:38Z ashigeru $
	 * @author Suguru ARAKAWA
	 */
	private abstract static class AbstractInvocationDriver implements org.aopalliance.intercept.Invocation {
		
		/**
		 * ラップする{@link Invocation}。
		 */
		protected final Invocation invocation;
		

		/**
		 * インスタンスを生成する。
		 * @param invocation 呼び出しを表現するオブジェクト
		 */
		public AbstractInvocationDriver(Invocation invocation) {
			assert invocation != null;
			this.invocation = invocation;
		}
		
		public Object[] getArguments() {
			return invocation.getArguments();
		}
		
		public AccessibleObject getStaticPart() {
			return (AccessibleObject) invocation.getTarget();
		}
		
		public Object proceed() throws Throwable {
			return doProceed(invocation);
		}
	}
	
	/**
	 * {@link org.jiemamy.utils.enhancer.Invocation}をラップして、
	 * {@link MethodInvocation}としての振る舞いを提供する。
	 * 
	 * @version $Id: AopAllianceDriver.java 3738 2009-10-09 10:03:38Z ashigeru $
	 * @author Suguru ARAKAWA
	 */
	private static class MethodInvocationDriver extends AbstractInvocationDriver implements MethodInvocation {
		
		private Method executable;
		

		/**
		 * インスタンスを生成する。
		 * @param invocation 呼び出しを表現するオブジェクト
		 */
		public MethodInvocationDriver(Invocation invocation) {
			super(invocation);
			Member target = invocation.getTarget();
			assert target instanceof Method;
			executable = (Method) target;
		}
		
		public Object getThis() {
			if (Modifier.isStatic(executable.getModifiers()) == false) {
				return invocation.getInvoker();
			}
			return null;
		}
		
		public Method getMethod() {
			return executable;
		}
	}
	
	/**
	 * {@link org.jiemamy.utils.enhancer.Invocation}をラップして、
	 * {@link ConstructorInvocation}としての振る舞いを提供する。
	 * 
	 * @version $Id: AopAllianceDriver.java 3738 2009-10-09 10:03:38Z ashigeru $
	 * @author Suguru ARAKAWA
	 */
	private static class ConstructorInvocationDriver extends AbstractInvocationDriver implements ConstructorInvocation {
		
		private Constructor<?> executable;
		

		/**
		 * インスタンスを生成する。
		 * @param invocation 呼び出しを表現するオブジェクト
		 */
		public ConstructorInvocationDriver(Invocation invocation) {
			super(invocation);
			Member target = invocation.getTarget();
			assert target instanceof Constructor<?>;
			executable = (Constructor<?>) target;
		}
		
		public Object getThis() {
			return null;
		}
		
		public Constructor<?> getConstructor() {
			return executable;
		}
	}
}
