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

import static org.jiemamy.utils.enhancer.reflection.ReflectionUtil.findConstructor;
import static org.jiemamy.utils.enhancer.reflection.ReflectionUtil.toParameterTypes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jiemamy.utils.enhancer.Factory;

/**
 * Reflection APIを利用してオブジェクトを生成するファクトリの実装。
 * @version 0.2.0
 * @since 0.2.0
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 * @param <T> 生成するオブジェクトの型
 */
public class ReflectionFactory<T> implements Factory<T> {
	
	private Class<T> targetClass;
	

	/**
	 * インスタンスを生成する。
	 * <p>
	 * 対象のクラスは、次のようなクラスでなければならない。
	 * </p>
	 * <ul>
	 * <li> 通常の具象クラスである(インターフェース、列挙、抽象クラスではない) </li>
	 * <li> パッケージメンバである(トップレベルクラス) </li>
	 * <li> {@code public}で宣言されている </li>
	 * </ul>
	 * @param targetClass ファクトリが生成するインスタンスのクラス
	 * @throws IllegalArgumentException 引数のクラスが不正である場合
	 */
	public ReflectionFactory(Class<T> targetClass) {
		if (targetClass == null) {
			throw new IllegalArgumentException("targetClass is null"); //$NON-NLS-1$
		}
		if (ReflectionUtil.isNormalClass(targetClass) == false) {
			throw new IllegalArgumentException(MessageFormat.format(
					"{0} is not a normal class",
					targetClass));
		}
		if (Modifier.isAbstract(targetClass.getModifiers())) {
			throw new IllegalArgumentException(MessageFormat.format(
					"{0} is not a concrete class",
					targetClass));
		}
		if (ReflectionUtil.isPackageMember(targetClass) == false) {
			throw new IllegalArgumentException(MessageFormat.format(
					"{0} is not a package member",
					targetClass));
		}
		if (Modifier.isPublic(targetClass.getModifiers()) == false) {
			throw new IllegalArgumentException(MessageFormat.format(
					"{0} is not public",
					targetClass));
		}
		this.targetClass = targetClass;
	}
	
	public Class<T> getTargetClass() {
		return targetClass;
	}
	
	public T newInstance(Object... arguments) throws InvocationTargetException {
		if (arguments == null) {
			throw new IllegalArgumentException("arguments is null"); //$NON-NLS-1$
		}
		List<Constructor<T>> constructors = new ArrayList<Constructor<T>>();
		for (Constructor<?> ctor : targetClass.getConstructors()) {
			@SuppressWarnings("unchecked")
			Constructor<T> tCtor = (Constructor<T>) ctor;
			constructors.add(tCtor);
		}
		List<Class<?>> parameterTypes = toParameterTypes(arguments);
		Collection<Constructor<T>> targets =
				findConstructor(constructors, parameterTypes);
		if (targets.isEmpty()) {
			throw new IllegalArgumentException(MessageFormat.format(
					"No applicable constructors in {0} for {1}",
					targetClass.getName(),
					parameterTypes));
		}
		if (targets.size() >= 2) {
			throw new IllegalArgumentException(MessageFormat.format(
					"Ambiguous target constructors ({0}) for {1}",
					targets,
					parameterTypes));
		}
		Constructor<T> target = targets.iterator().next();
		try {
			return target.newInstance(arguments);
		} catch (IllegalAccessException e) {
			// may not occur
			throw new AssertionError(e);
		} catch (InstantiationException e) {
			// may not occur
			throw new AssertionError(e);
		}
	}
}
