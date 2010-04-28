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

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.EnhanceException;

/**
 * Javassistを利用して{@link java.lang.Class}と{@link javassist.CtClass}を相互に
 * 変換するためのライブラリ。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class JavassistConverter {
	
	private static final Logger LOG = LoggerFactory.getLogger(JavassistConverter.class);
	
	/**
	 * CtClassで表現されたプリミティブ型をjava.lang.Classで表現された同一の型に
	 * 変換するためのテーブル。
	 */
	private static final Map<CtClass, Class<?>> PRIMITIVES;
	static {
		Map<CtClass, Class<?>> map = new TreeMap<CtClass, Class<?>>(CtClassComparator.INSTANCE);
		map.put(CtClass.voidType, void.class);
		map.put(CtClass.intType, int.class);
		map.put(CtClass.longType, long.class);
		map.put(CtClass.floatType, float.class);
		map.put(CtClass.doubleType, double.class);
		map.put(CtClass.shortType, short.class);
		map.put(CtClass.byteType, byte.class);
		map.put(CtClass.charType, char.class);
		map.put(CtClass.byteType, byte.class);
		map.put(CtClass.booleanType, boolean.class);
		PRIMITIVES = Collections.unmodifiableMap(map);
	}
	
	private final ClassPool pool;
	
	private final ClassLoader loader;
	

	/**
	 * インスタンスを生成する。
	 * <p>
	 * 生成されたインスタンスを利用してロードされるクラスは、
	 * {@code targetClass}の定義ローダ(JVMS2-5.3)を親ローダに持つような、
	 * クラスローダによって定義またはロードされる。
	 * そのクラスローダはこのクラスのインスタンスごとに新しく生成され、
	 * このクラスの同一のインスタンスを利用してロードされるクラスは、
	 * すべてこのインスタンスごとに生成されたクラスローダを利用する。
	 * </p>
	 * @param targetClass 基点とするクラス
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public JavassistConverter(Class<?> targetClass) {
		super();
		if (targetClass == null) {
			throw new NullPointerException("targetClass"); //$NON-NLS-1$
		}
		pool = new ClassPool();
		pool.appendClassPath(new ClassClassPath(targetClass));
		// targetClass.getClassLoader() をそのまま利用すると、
		// targetClassのリアルローダ上にエンハンスされたクラスが定義されてしまう。
		// これは、名前空間の衝突やクラスオブジェクトのリークの原因となる。
		// これを回避するため、エンハンサは自身ではクラスをロードしないEmptyClassLoaderを定義し、
		// その親ローダをtargetClassのリアルローダとする。
		// JavassistはEmptyClassLoader上のdefineClass(*)を呼び出すため、
		// エンハンスされたクラスはtargetClassのリアルローダ上ではなくEmptyClassLoader上で
		// 定義されることになる。
		// @see javassist.ClassPool#toClass(ClassLoader, ProtectionDomain)
		loader = new EmptyClassLoader(targetClass.getClassLoader());
	}
	
	/**
	 * このインスタンスが利用する{@link ClassPool}を返す。
	 * <p>
	 * 返されるインスタンスは、このインスタンスを生成する際にコンストラクタに渡したクラスの、
	 * 定義ローダによってロード可能なすべてのクラスを参照することができる。
	 * </p>
	 * @return このインスタンスが利用する{@link ClassPool}
	 */
	public ClassPool getClassPool() {
		return pool;
	}
	
	/**
	 * 指定の{@link java.lang.Class}オブジェクトに対応する{@link CtClass}をロードする。
	 * <p>
	 * 指定する{@link java.lang.Class}は、次のすべてを満たす必要がある。
	 * </p>
	 * <ul>
	 *   <li> クラス、インターフェース、列挙、注釈型のいずれかである </li>
	 *   <li> トップレベルの型宣言である </li>
	 * </ul>
	 * @param klass 対象の{@link java.lang.Class}オブジェクト
	 * @return 対応する{@link CtClass}
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @throws IllegalArgumentException {@link java.lang.Class}が不正である場合
	 * @throws EnhanceException ロードに失敗した場合
	 */
	public CtClass loadCtClass(Class<?> klass) throws EnhanceException {
		if (klass == null) {
			throw new NullPointerException("klass"); //$NON-NLS-1$
		}
		if (klass.isArray() || klass.isPrimitive() || klass.isMemberClass() || klass.isLocalClass()
				|| klass.isAnonymousClass()) {
			throw new IllegalArgumentException("klass"); //$NON-NLS-1$
		}
		String name = klass.getCanonicalName();
		assert name != null;
		try {
			return pool.get(name);
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Cannot load {0}", name), e);
		}
	}
	
	/**
	 * 指定の{@link CtClass}オブジェクトを対応する{@link java.lang.Class}オブジェクトへと
	 * 変換して返す。
	 * <p>
	 * この操作が成功した場合、対象の{@link CtClass}は凍結される。
	 * </p>
	 * @param klass 対象の{@link CtClass}オブジェクト
	 * @return 対応する{@link java.lang.Class}オブジェクト
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @throws EnhanceException 変換に失敗した場合
	 */
	public Class<?> toClass(CtClass klass) throws EnhanceException {
		if (klass == null) {
			throw new NullPointerException("klass"); //$NON-NLS-1$
		}
		LOG.trace("Loading java.lang.Class: {}", klass.getName());
		try {
			if (klass.isPrimitive()) {
				// プリミティブ型はクラスローダでロードできないため、別の処理を行う
				Class<?> prim = toPrimitiveClass(klass);
				assert prim != null;
				return prim;
			} else if (klass.isArray()) {
				// 配列型もクラスローダでロードできないため、別の処理
				CtClass current = klass;
				int dim = 0;
				while (current.isArray()) {
					dim++;
					current = current.getComponentType();
				}
				// ただし、要素型は先にロードされていないといけない
				Class<?> root = toClass(current);
				// FIXME to smart
				// Class<T>からClass<T[]>を作る方法が不明なので、
				// 一度 Array.newInstance で T[] を作ってから getClass() して実現。 
				Object array = Array.newInstance(root, new int[dim]);
				return array.getClass();
			} else {
				// 親ローダでロードできたり、すでにロード済みのものがあればそれを利用
				Class<?> loaded = findFromLoader(klass);
				if (loaded != null) {
					klass.freeze();
					return loaded;
				} else {
					// なければローダ上に定義する
					LOG.debug("Register to JVM: {}", klass.getName());
					return klass.toClass(loader, loader.getClass().getProtectionDomain());
				}
			}
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat.format("Cannot load class (compile error):{0}", klass.getName()),
					e);
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Cannot convert class (not found):{0}", klass.getName()), e);
		}
	}
	
	/**
	 * {@link #toClass(CtClass)}を配列に対して一斉適用する。
	 * <p>
	 * 返される配列に含まれるそれぞれの要素は、引数に渡した{@code CtClass}の一覧と同じ順序で
	 * 対応する{@link java.lang.Class}オブジェクトが格納される。
	 * </p>
	 * @param classes 変換対象の一覧
	 * @return 対応する変換結果の一覧
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @throws EnhanceException 変換に失敗した場合
	 */
	public Class<?>[] toClasses(CtClass[] classes) throws EnhanceException {
		if (classes == null) {
			throw new NullPointerException("classes"); //$NON-NLS-1$
		}
		Class<?>[] results = new Class<?>[classes.length];
		for (int i = 0; i < results.length; i++) {
			results[i] = toClass(classes[i]);
		}
		return results;
	}
	
	/**
	 * プリミティブ型を表現する指定の{@link CtClass}オブジェクトを
	 * 対応する{@link java.lang.Class}オブジェクトへと変換して返す。
	 * @param klass 対象のプリミティブ型を表現する{@link CtClass}オブジェクト
	 * @return 対応する{@link java.lang.Class}オブジェクト
	 */
	private Class<?> toPrimitiveClass(CtClass klass) {
		assert klass != null;
		assert klass.isPrimitive();
		Class<?> javaLangClass = PRIMITIVES.get(klass);
		assert javaLangClass != null : klass;
		return javaLangClass;
	}
	
	/**
	 * 指定の{@link CtClass}オブジェクトに対応する{@link java.lang.Class}オブジェクトが
	 * すでにロードされている場合、その値を返す。
	 * <p>
	 * エンハンサのクラスローダによって{@link java.lang.Class}オブジェクトがロードされていない場合、
	 * この呼び出しは{@code null}を返す。
	 * </p>
	 * <p>
	 * 引数に渡される{@link CtClass}は、プリミティブ型であってはならない。
	 * </p>
	 * @param klass 対象の{@link CtClass}オブジェクト
	 * @return
	 *      対応するロード済みの{@link java.lang.Class}オブジェクト、
	 *      ロードされていない場合は{@code null}
	 */
	private Class<?> findFromLoader(CtClass klass) {
		assert klass.isPrimitive() == false;
		try {
			if (klass.isArray()) {
				String descriptor = Descriptor.of(klass);
				return loader.loadClass(descriptor);
			} else {
				return loader.loadClass(klass.getName());
			}
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * メソッドに対するアスペクトの一覧を対応するアドバイステーブルに変換する。
	 * <p>
	 * 返される配列に含まれる各要素は、引数に渡したアスペクトと同じ順序で、
	 * それらの実行形式が格納される。
	 * </p>
	 * @param aspectList 変換するアスペクトの一覧
	 * @return 対応するアドバイステーブル
	 * @throws EnhanceException 変換に失敗した場合
	 */
	public AdviceTable toMethodAspects(AspectList<CtMethod> aspectList) throws EnhanceException {
		
		assert aspectList != null;
		LOG.trace("Creating advice table entries: {}", aspectList);
		
		List<AdviceApplier> results = new ArrayList<AdviceApplier>();
		for (Aspect<CtMethod> aspect : aspectList) {
			CtMethod original = aspect.getOriginal();
			CtMethod actual = aspect.getActual();
			CtClass[] params;
			try {
				params = original.getParameterTypes();
			} catch (NotFoundException e) {
				throw new EnhanceException(MessageFormat.format("Cannot resolve constructor {0}", original.getName()),
						e);
			}
			AdviceApplier helper =
					AdviceApplier.method(aspect.getHandlers(), toClass(original.getDeclaringClass()), original
						.getName(), toClass(actual.getDeclaringClass()), actual.getName(), toClasses(params));
			results.add(helper);
		}
		return new AdviceTable(aspectList, results);
	}
	
	/**
	 * インスタンス生成に対するアスペクトの一覧を対応するアドバイステーブルに変換する。
	 * <p>
	 * 返される配列に含まれる各要素は、引数に渡したアスペクトと同じ順序で、
	 * それらの実行形式が格納される。
	 * </p>
	 * @param aspectList 変換するアスペクトの一覧
	 * @return 対応するアドバイステーブル
	 * @throws EnhanceException 変換に失敗した場合
	 */
	public AdviceTable toConstructorAspects(AspectList<CtConstructor> aspectList) throws EnhanceException {
		
		assert aspectList != null;
		LOG.trace("Creating advice table entries: {}", aspectList);
		
		List<AdviceApplier> results = new ArrayList<AdviceApplier>();
		for (Aspect<CtConstructor> aspect : aspectList) {
			CtConstructor original = aspect.getOriginal();
			CtConstructor actual = aspect.getActual();
			CtClass[] params;
			try {
				params = original.getParameterTypes();
			} catch (NotFoundException e) {
				throw new EnhanceException(MessageFormat.format("Cannot resolve constructor {0}", original.getName()),
						e);
			}
			AdviceApplier helper =
					AdviceApplier.constructor(aspect.getHandlers(), toClass(original.getDeclaringClass()),
							toClass(actual.getDeclaringClass()), toClasses(params));
			results.add(helper);
		}
		return new AdviceTable(aspectList, results);
	}
}
