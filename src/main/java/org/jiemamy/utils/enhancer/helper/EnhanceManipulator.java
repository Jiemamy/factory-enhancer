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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.NewExpr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.EnhanceException;
import org.jiemamy.utils.enhancer.InvocationHandler;
import org.jiemamy.utils.enhancer.InvocationPointcut;

/**
 * エンハンスを行う際の定型的なクラス書き換えを行うためのライブラリ。
 * @version 0.2.0
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class EnhanceManipulator {
	
	private static final Logger LOG = LoggerFactory.getLogger(EnhanceManipulator.class);
	
	/**
	 * ブリッジメソッドに対する修飾子のフラグ。
	 */
	private static final int BRIDGE = 0x00000040;
	
	/**
	 * コンパイラ合成要素に対する修飾子のフラグ。
	 */
	private static final int SYNTHETIC = 0x00001000;
	
	private static final Pattern AVOID_NAME_PATTERN = Pattern.compile("(java\\.|javax\\.|sun\\.).*"); //$NON-NLS-1$
	
	private static final String ENHANCE_CLASS = "__ENHANCED__";
	
	private static final String BYPASS_METHOD = "__BYPASS__"; //$NON-NLS-1$
	
	private static final String ADVICE_TABLE_FIELD = "__ADVICE_TABLE__"; //$NON-NLS-1$
	
	
	/**
	 * 指定のクラスをコピーした新しいクラスを作成して返す。
	 * <p>
	 * 作成したクラスは元のクラスの名称を変更したクラスで、すべての内容は元のクラスと同一である。
	 * ただし、自身への参照は、コピー先への参照に貼りかえられる。
	 * </p>
	 * @param klass コピー元のクラス
	 * @return 作成したクラス
	 * @throws EnhanceException クラスのコピーに失敗した場合
	 * @throws IllegalArgumentException 引数に通常のクラスでない型が指定された場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static CtClass createCopyClass(CtClass klass) throws EnhanceException {
		if (klass == null) {
			throw new NullPointerException("klass is null"); //$NON-NLS-1$
		}
		if (klass.isInterface() || klass.isEnum()) {
			throw new IllegalArgumentException();
		}
		LOG.trace("Creating a copy: {}", klass);
		String name = klass.getName();
		try {
			CtClass copy = klass.getClassPool().getAndRename(name, getEnhanceClassName(name));
			copy.setModifiers(klass.getModifiers() | Modifier.FINAL);
			LOG.debug("Copy class: {} -> {}", klass.getName(), copy.getName());
			return copy;
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Cannot load {0}", name), e);
		}
	}
	
	/**
	 * 指定のクラスを継承した新しいクラスを作成して返す。
	 * <p>
	 * 生成されたクラスは、引数にとるクラスを親クラスに持ち、
	 * 親クラスで宣言されたコンストラクタのうちジョインポイントとして利用可能なコンストラクタに対する
	 * 移譲コンストラクタを宣言する。
	 * </p>
	 * @param klass 元となるクラス
	 * @return 対象のクラスを継承した新しいクラス
	 * @throws EnhanceException クラスの作成に失敗した場合
	 * @throws IllegalArgumentException 引数に通常のクラスでない型が指定された場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @see #createDelegateConstructors(CtClass)
	 */
	public static CtClass createInheritedClass(CtClass klass) throws EnhanceException {
		if (klass == null) {
			throw new NullPointerException("klass is null"); //$NON-NLS-1$
		}
		if (klass.isInterface() || klass.isEnum()) {
			throw new IllegalArgumentException();
		}
		LOG.trace("Creating an inherited class: {}", klass.getName());
		CtClass copy = klass.getClassPool().makeClass(getEnhanceClassName(klass.getName()));
		copy.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
		try {
			copy.setSuperclass(klass);
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat.format("Cannot inherit {0}", klass.getName()), e);
		}
		LOG.debug("Inherit class: {} -> {}", klass.getName(), copy.getName());
		createDelegateConstructors(copy);
		return copy;
	}
	
	/**
	 * 指定のインターフェースを実装した新しいクラスを作成して返す。
	 * <p>
	 * このメソッドでは、主に次の3つが行われる。
	 * </p>
	 * <ul>
	 * <li> ファクトリインターフェースを実装した新しいファクトリ実装クラスの作成 </li>
	 * <li> ファクトリインターフェース、およびその親インターフェースに含まれるすべてのメソッドについて
	 *   <ul>
	 *   <li> そのメソッドが生成するプロダクトの実装に対し、そのメソッドと同一の引数リストを取るコンストラクタの追加 </li>
	 *   <li> 追加したコンストラクタを起動するメソッドをファクトリ実装クラス内に生成 </li>
	 *   </ul>
	 * </li>
	 * </ul>
	 * @param factoryInterface 実装を生成する対象のファクトリインターフェース 
	 * @param targetProducts プロダクトインターフェースと、その実装のマッピング
	 * @return 生成したファクトリ実装クラス
	 * @throws EnhanceException ファクトリ実装クラスの作成に失敗した場合
	 * @since 0.2.0
	 */
	public static CtClass createFactoryImplementation(
			CtClass factoryInterface,
			Map<CtClass, CtClass> targetProducts) throws EnhanceException {
		if (factoryInterface == null) {
			throw new IllegalArgumentException("factoryInterface is null"); //$NON-NLS-1$
		}
		if (targetProducts == null) {
			throw new IllegalArgumentException("targetProducts is null"); //$NON-NLS-1$
		}
		LOG.trace("Creating a factory implementation: {}", factoryInterface.getName());
		ClassPool pool = factoryInterface.getClassPool();
		CtClass implementation = pool.makeClass(getEnhanceClassName(factoryInterface.getName()));
		implementation.setModifiers(Modifier.PUBLIC);
		implementation.setInterfaces(new CtClass[] {
			factoryInterface
		});
		
		LOG.debug("A factory implementation: {} implements {}",
				implementation.getName(), factoryInterface.getName());
		
		// ファクトリインターフェースが提供するすべてのメソッドを実装
		for (MethodGroup group : collectInterfaceMethods(factoryInterface)) {
			implementFactoryMethod(implementation, group, targetProducts);
		}
		return implementation;
	}
	
	private static void implementFactoryMethod(
			CtClass implementation,
			MethodGroup factoryMethodGroup,
			Map<CtClass, CtClass> targetProducts) throws EnhanceException {
		assert implementation != null;
		assert factoryMethodGroup != null;
		assert targetProducts != null;
		LOG.trace("Implementing a factory method: {}", factoryMethodGroup);
		
		if (factoryMethodGroup.hasMostSpecificMethod() == false) {
			throw new EnhanceException(MessageFormat.format(
					"{0} has ambigous return type",
					factoryMethodGroup),
					null);
		}
		
		CtMethod factoryMethod = factoryMethodGroup.getMostSpecificMethod();
		CtClass productType;
		try {
			productType = factoryMethod.getReturnType();
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format(
					"Cannot detect product interface for {0}",
					factoryMethodGroup),
					e);
		}
		CtClass productImpl = targetProducts.get(productType);
		if (productImpl == null) {
			throw new EnhanceException(MessageFormat.format(
					"{0} is not a valid product interface",
					productType.getName()),
					null);
		}
		createProductConstructor(productImpl, factoryMethod);
		createFactoryMethod(implementation, factoryMethod, productImpl);
		createBridgeMethods(implementation, factoryMethodGroup);
	}
	
	/**
	 * 指定のファクトリメソッドに定義された仮引数と同じ仮引数を有するコンストラクタを指定のクラス上に生成する。
	 * <p>
	 * 生成されたコンストラクタは、親クラスの引数をとらないコンストラクタを起動するのみである。
	 * このため、{@code productImplementation}の親クラスは引数をとらないコンストラクタを
	 * 子に対して公開している必要がある。
	 * なお、このメソッドによって生成されるコンストラクタは{@code public}となる。
	 * </p>
	 * <p>
	 * 生成しようとするコンストラクタがすでに師弟のクラス上に存在する場合、この呼び出しは何も行わない。
	 * </p>
	 * @param productImplementation コンストラクタを生成する対象のクラス
	 * @param factoryMethod 生成するコンストラクタと同様の仮引数リストを有するメソッド
	 * @throws EnhanceException コンストラクタの生成に失敗した場合
	 */
	private static void createProductConstructor(
			CtClass productImplementation,
			CtBehavior factoryMethod) throws EnhanceException {
		assert productImplementation != null;
		assert factoryMethod != null;
		LOG.trace("Creating product constructor: {}", factoryMethod);
		try {
			if (isConstructorExist(productImplementation, factoryMethod.getParameterTypes())) {
				LOG.debug("The product constructor already exists: {}<-{}",
						productImplementation.getName(), factoryMethod.getSignature());
				return;
			}
			CtConstructor constructor = new CtConstructor(
					factoryMethod.getParameterTypes(),
					productImplementation);
			constructor.setModifiers(Modifier.PUBLIC);
			constructor.setBody(null);
			productImplementation.addConstructor(constructor);
			LOG.debug("Product constructor: {}{}",
					productImplementation.getName(), constructor.getSignature());
		} catch (CannotCompileException e) {
			throw new EnhanceException(
					MessageFormat.format(
					"Cannot create constructor {1}{2} to {0}",
					productImplementation.getName(),
					factoryMethod.getName(),
					factoryMethod.getSignature()),
					e);
		} catch (NotFoundException e) {
			throw new EnhanceException(
					MessageFormat.format(
					"Cannot create constructor {1}{2} to {0}",
					productImplementation.getName(),
					factoryMethod.getName(),
					factoryMethod.getSignature()),
					e);
		}
	}
	
	/**
	 * 指定のクラス上に指定の型をとる仮引数リストを持つようなコンストラクタが存在する場合にのみ{@code true}を返す。
	 * @param aClass 対象コンストラクタを宣言するクラス
	 * @param parameterTypes 対象コンストラクタの仮引数型一覧
	 * @return コンストラクタがすでに宣言されていれば{@code true}
	 */
	private static boolean isConstructorExist(CtClass aClass, CtClass[] parameterTypes) {
		assert aClass != null;
		assert parameterTypes != null;
		try {
			aClass.getDeclaredConstructor(parameterTypes);
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	/**
	 * ファクトリインターフェースで定義されたファクトリメソッドをファクトリクラス上に実装する。
	 * @param factoryImplementation ファクトリクラス
	 * @param factoryMethod インターフェースで定義されたファクトリメソッド
	 * @param productImplementation ファクトリが生成するプロダクトの実装クラス
	 * @throws EnhanceException ファクトリメソッドの実装に失敗した場合
	 */
	private static void createFactoryMethod(
			CtClass factoryImplementation,
			CtMethod factoryMethod,
			CtClass productImplementation) throws EnhanceException {
		assert factoryImplementation != null;
		assert factoryMethod != null;
		assert productImplementation != null;
		LOG.trace("Creating factory method: {} -> new {}", factoryMethod, productImplementation.getName());
		try {
			CtMethod factoryMethodImpl = new CtMethod(
					factoryMethod.getReturnType(),
					factoryMethod.getName(),
					factoryMethod.getParameterTypes(),
					factoryImplementation);
			factoryMethodImpl.setModifiers(Modifier.PUBLIC);
			factoryMethodImpl.setBody(String.format("return new %s($$);", //$NON-NLS-1$
					productImplementation.getName()));
			factoryImplementation.addMethod(factoryMethodImpl);
			LOG.trace("Factory method: {}{}{} -> new {}", new Object[] {
				factoryImplementation.getName(),
				factoryMethodImpl.getName(),
				factoryMethodImpl.getSignature(),
				productImplementation.getName()
			});
		} catch (CannotCompileException e) {
			throw new EnhanceException(
					MessageFormat.format(
					"Cannot create factory method {1}{2} to {0}",
					factoryImplementation.getName(),
					factoryMethod.getName(),
					factoryMethod.getSignature()),
					e);
		} catch (NotFoundException e) {
			throw new EnhanceException(
					MessageFormat.format(
					"Cannot create factory method {1}{2} to {0}",
					factoryImplementation.getName(),
					factoryMethod.getName(),
					factoryMethod.getSignature()),
					e);
		}
	}
	
	/**
	 * 指定のクラス上に対象のメソッドグループが提供すべきブリッジメソッドを生成する。
	 * @param aClass 対象のクラス
	 * @param methodGroup 対象のメソッドグループ
	 * @throws EnhanceException ブリッジメソッドの生成に失敗した場合
	 */
	private static void createBridgeMethods(
			CtClass aClass,
			MethodGroup methodGroup) throws EnhanceException {
		assert aClass != null;
		assert methodGroup != null;
		assert methodGroup.hasMostSpecificMethod();
		Collection<CtMethod> less = methodGroup.getLessSpecificMethods();
		if (less.isEmpty()) {
			return;
		}
		LOG.trace("Creating bridge methods: {}", methodGroup);
		CtMethod most = methodGroup.getMostSpecificMethod();
		CtClass ownerOfMost = most.getDeclaringClass();
		for (CtMethod source : less) {
			try {
				// すでに本体が定義されたメソッドについては何もしない
				if (isBodyDeclared(aClass, source)) {
					continue;
				}
				CtMethod bridge = new CtMethod(source, aClass, null);
				bridge.setModifiers(bridge.getModifiers() & ~(Modifier.ABSTRACT) | BRIDGE | SYNTHETIC);
				// FIXME ちょっと怪しい。反例を探すこと
				assert isStatic(most) == false;
				assert bridge.getReturnType().equals(CtClass.voidType) == false;
				bridge.setBody(String.format(
						"return ((%s) this).%s($$);",
						ownerOfMost.getName(),
						bridge.getName()));
				aClass.addMethod(bridge);
			} catch (CannotCompileException e) {
				throw new EnhanceException(MessageFormat.format(
						"Cannot create bridge method {0}#{1}{2}",
						aClass.getName(),
						source.getName(),
						source.getSignature()),
						e);
			} catch (NotFoundException e) {
				throw new EnhanceException(MessageFormat.format(
						"Cannot create bridge method {0}#{1}{2}",
						aClass.getName(),
						source.getName(),
						source.getSignature()),
						e);
			}
		}
	}
	
	/**
	 * 指定のクラスに指定のメソッドと同じシグネチャを有するメソッドが、本体ごと宣言されている場合のみ{@code true}を返す。
	 * @param aClass 対象のクラス
	 * @param method 対象のメソッド
	 * @return 本体が宣言されていれば{@code true}
	 */
	private static boolean isBodyDeclared(CtClass aClass, CtMethod method) {
		assert aClass != null;
		assert method != null;
		String name = method.getName();
		String descriptor = method.getSignature();
		try {
			for (CtClass current = aClass; current != null; current = current.getSuperclass()) {
				for (CtMethod declared : aClass.getDeclaredMethods()) {
					if (declared.getName().equals(name) == false) {
						continue;
					}
					if (declared.getSignature().equals(descriptor) == false) {
						continue;
					}
					int modifiers = declared.getModifiers();
					if (Modifier.isPrivate(modifiers)) {
						return false;
					}
					if (Modifier.isAbstract(modifiers)) {
						return false;
					}
					return true;
				}
			}
			return false;
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	/**
	 * インターフェースプロダクトの実装クラスを生成して返す。
	 * <p>
	 * 生成されるクラスは、指定された基本クラスとインターフェースをそれぞれ親にもち。
	 * コンストラクタは定義されない(最後まで定義されないと、Javassistがデフォルトコンストラクタを生成する)。
	 * </p>
	 * @param baseInterface 生成するクラスの親インターフェース
	 * @param baseClass 生成するクラスの親クラス
	 * @return 生成したクラス
	 * @throws EnhanceException クラスの生成に失敗した場合
	 * @since 0.2.0
	 */
	public static CtClass createProductImplementation(
			CtClass baseInterface,
			CtClass baseClass) throws EnhanceException {
		if (baseInterface == null) {
			throw new NullPointerException("baseInterface is null"); //$NON-NLS-1$
		}
		if (baseClass == null) {
			throw new NullPointerException("baseClass is null"); //$NON-NLS-1$
		}
		LOG.trace("Creating product implementation: implements {} extends {}",
				baseInterface.getName(), baseClass.getName());
		
		ClassPool pool = baseInterface.getClassPool();
		CtClass implementation = pool.makeClass(getEnhanceClassName(baseInterface.getName()));
		implementation.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
		try {
			implementation.setSuperclass(baseClass);
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat.format(
					"Cannot inherit {0}",
					baseClass.getName()),
					e);
		}
		implementation.addInterface(baseInterface);
		
		LOG.debug("Implementation: {} extends {} implements {}", new Object[] {
			implementation.getName(),
			baseClass.getName(),
			baseInterface.getName()
		});
		
		// http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#13.5.3
		// Adding a method to an interface does not break compatibility with pre-existing binaries. 
		// とのことで、ここでは baseInterface を初期状態でメソッドを持たないインターフェースとみなし、
		// そこに現在の baseInterface に対して現在のメソッドが追加されたものとする。
		// そう考えた場合、作成したクラス implementation はいくつかのメソッドを実装しない具象クラスとなってソースコード的には不正であるものの
		// バイナリ的には問題なくリンクできる。
		
		// ただし、ブリッジメソッドは自分で作らないといけない模様...
		for (MethodGroup group : collectInterfaceMethods(baseInterface)) {
			if (group.hasMostSpecificMethod() == false) {
				throw new EnhanceException(MessageFormat.format(
						"{0} has ambigous return type",
						group),
						null);
			}
			createBridgeMethods(implementation, group);
		}
		
		return implementation;
	}
	
	/**
	 * 指定のインターフェースが公開するインターフェースメソッドの一覧を返す。
	 * <p>
	 * インターフェースメソッドがオーバーライドされている場合、そのうち一つだけを返す。
	 * ただし、現時点ではオーバーライド時の共変戻り値型による戻り値の変更を許していない。
	 * これは将来のバージョンで改善されるかもしれない。
	 * </p>
	 * @param anInterface 対象のインターフェース
	 * @return 対象のインターフェースに含まれる公開メソッドの一覧
	 * @throws EnhanceException メソッドの計算に失敗した場合
	 * @since 0.2.0
	 */
	private static Set<MethodGroup> collectInterfaceMethods(CtClass anInterface) throws EnhanceException {
		assert anInterface != null;
		assert anInterface.isInterface();
		Map<NameAndParameter, MethodGroup> results = new HashMap<NameAndParameter, MethodGroup>();
		
		LinkedList<CtClass> work = new LinkedList<CtClass>();
		work.addFirst(anInterface);
		Set<String> saw = new HashSet<String>();
		while (work.isEmpty() == false) {
			CtClass targetInterface = work.removeFirst();
			if (saw.contains(targetInterface.getName())) {
				continue;
			}
			saw.add(targetInterface.getName());
			try {
				for (CtMethod method : targetInterface.getDeclaredMethods()) {
					NameAndParameter target = new NameAndParameter(method);
					
					// 名前とパラメータが一致するメソッドがすでに追加されているか
					MethodGroup group = results.get(target);
					if (group == null) {
						// 追加されていなければ、新しいグループを作成
						results.put(target, new MethodGroup(method));
					} else {
						// 追加されていれば、そのグループにメソッドの追加を試みる
						group.add(method);
					}
				}
				for (CtClass superInterface : targetInterface.getInterfaces()) {
					work.addFirst(superInterface);
				}
			} catch (NotFoundException e) {
				throw new EnhanceException(
						MessageFormat.format(
						"Cannot resolve super interfaces for {0}",
						targetInterface.getName()),
						e);
			}
		}
		
		return new HashSet<MethodGroup>(results.values());
	}
	
	/**
	 * 親クラスで宣言されたコンストラクタに処理を委譲するコンストラクタを拡張クラス上に宣言する。
	 * <p>
	 * 拡張クラス上に宣言される移譲コンストラクタは、移譲先のコンストラクタと同一の引数リストを持ち、
	 * かつ与えられた引数リストをそのまま移譲先に渡すようなコンストラクタである。
	 * </p>
	 * <p>
	 * この呼び出しによって作成される移譲コンストラクタは、
	 * 移譲先のコンストラクタがジョインポイントとして適切である必要がある。
	 * ジョインポイントとして適切でないコンストラクタについては、拡張クラス上に移譲コンストラクタを作成しない。
	 * </p>
	 * @param enhanced 宣言する対象のクラス
	 * @throws EnhanceException コンストラクタの宣言に失敗した場合
	 */
	private static void createDelegateConstructors(CtClass enhanced) throws EnhanceException {
		assert enhanced != null;
		LOG.trace("Creating delegate constructors: {}", enhanced.getName());
		CtClass base;
		try {
			base = enhanced.getSuperclass();
		} catch (NotFoundException e) {
			// may not occur
			throw new EnhanceException(MessageFormat.format("Cannot resolve superclass for {0}", enhanced.getName()), e);
		}
		for (CtConstructor constructor : base.getConstructors()) {
			if (Modifier.isPublic(constructor.getModifiers()) == false) {
				LOG.debug("Skip delegate constructor: {}{}", base.getName(), constructor.getSignature());
				continue;
			}
			if (isClassInitializer(constructor)) {
				continue;
			}
			try {
				CtConstructor delegate = new CtConstructor(constructor.getParameterTypes(), enhanced);
				delegate.setExceptionTypes(constructor.getExceptionTypes());
				delegate.setModifiers(Modifier.PUBLIC);
				delegate.setBody("super($$);");
				enhanced.addConstructor(delegate);
				
				LOG.debug("Delegate constructor: {}{}", enhanced.getName(), delegate.getSignature());
			} catch (NotFoundException e) {
				throw new EnhanceException(MessageFormat.format("Cannot inspect constructor ({0})", constructor
					.getLongName()), e);
			} catch (CannotCompileException e) {
				throw new EnhanceException(MessageFormat.format("Cannot delegate constructor ({0})", constructor
					.getLongName()), e);
			}
		}
	}
	
	/**
	 * 指定のオブジェクトが、コンストラクタではなくクラス初期化子を表現する場合にのみ{@code true}を返す。
	 * @param behavior 対象のコンストラクタまたはクラス初期化子
	 * @return 引数がクラス初期化子である場合に{@code true}
	 */
	private static boolean isClassInitializer(CtConstructor behavior) {
		assert behavior != null;
		return behavior.getName().charAt(0) == '<';
	}
	
	/**
	 * {@code method}に指定されたメソッドを呼び出すバイパスメソッドを拡張クラス上に新しく作成して返す。
	 * <p>
	 * バイパスメソッドは、拡張元のクラスが公開するメソッドを安全に呼び出すための方法を提供する。
	 * 親クラスで定義されたメソッドは、アドバイスを実行するためのメソッドで上書きしてしまうため
	 * 上書きされたメソッドを迂回して元のメソッドを呼び出す別のメソッドを定義している。
	 * </p>
	 * <pre><code>
	 * // original class
	 * public class Hoge {
	 *   public int add(int a, int b) {
	 *     return a + b;
	 *   }
	 * }
	 * </code></pre>
	 * <p>
	 * 上記のクラスは、拡張された際に下記のようなバイパスメソッドを追加される。
	 * ただし、バイパスメソッドの名前は他のメソッドの名称と衝突しないようなものが自動的に選択される。
	 * </p>
	 * <pre><code>
	 * // enhanced class (extends original class)
	 * public class Hoge__Enhanced__ extends Hoge {
	 *   ...
	 *   // 上書きしてアスペクトを処理する
	 *   &#64;Override public int add(int a, int b) {
	 *     return (invoke advice chain);
	 *   }
	 *   // 親クラス(Hoge)のaddメソッドを呼び出すためのバイパス
	 *   public int bypass(int a, int b) {
	 *     return super.add(int a, int b);
	 *   }
	 * }
	 * </code></pre>
	 * <p>
	 * このとき、バイパス対象がインターフェースメソッドである場合、親のメソッドを呼び出そうとすると
	 * リンクエラーになってしまう。
	 * そのため、かわりに{@link AbstractMethodError}をスローする。
	 * </p>
	 * <p>
	 * なお、このメソッドでは拡張するクラスにバイパスメソッドを追加するだけで、
	 * アスペクトを処理するメソッドについては
	 * {@link #createPointcutMethod(CtClass, CtMethod, CtField, int)}
	 * で作成する。
	 * </p>
	 * @param enhance 拡張対象のクラス
	 * @param method バイパスメソッドが呼び出す対象のメソッド
	 * @param index 拡張メソッドの番号
	 * @return 作成したバイパスメソッド
	 * @throws EnhanceException バイパスメソッドの作成に失敗した場合
	 * @see #createPointcutMethod(CtClass, CtMethod, CtField, int)
	 */
	private static CtMethod createBypassMethod(CtClass enhance, CtMethod method, int index) throws EnhanceException {
		assert enhance != null;
		assert method != null;
		assert index >= 0;
		assert isSubtype(enhance, method.getDeclaringClass());
		assert isStatic(method) == false;
		
		LOG.trace("Creating bypass method: {}#{}", method.getDeclaringClass().getName(), method.getName());
		
		try {
			CtMethod bypass =
					new CtMethod(method.getReturnType(), getBypassMethodName(method, index),
					method.getParameterTypes(), enhance);
			
			LOG.debug("Bypass method: {}#{}{} -> {}#{}{}", new Object[] {
				bypass.getDeclaringClass().getName(),
				bypass.getName(),
				bypass.getSignature(),
				method.getDeclaringClass().getName(),
				method.getName(),
				method.getSignature()
			});
			bypass.setModifiers(Modifier.PUBLIC);
			if (isDeclaredInClass(method, enhance.getSuperclass()) == false) {
				LOG.debug("Bypass target {}{} is not declared in {}; this throws {}", new Object[] {
					method.getName(),
					method.getSignature(),
					enhance.getSuperclass().getName(),
					AbstractMethodError.class.getName()
				});
				bypass.setBody(String.format("throw new %s(\"%s\");", AbstractMethodError.class.getName(), method
					.getName()));
			} else {
				LOG.debug("Bypass target {}{} is in {}; this just invokes it", new Object[] {
					method.getName(),
					method.getSignature(),
					enhance.getSuperclass().getName()
				});
				if (isVoid(method)) {
					bypass.setBody(String.format("super.%s($$);", method.getName()));
				} else {
					bypass.setBody(String.format("return super.%s($$);", method.getName()));
				}
			}
			enhance.addMethod(bypass);
			return bypass;
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat
				.format("Cannot create bypass method for {0}", method.getLongName()), e);
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat
				.format("Cannot create bypass method for {0}", method.getLongName()), e);
		}
	}
	
	/**
	 * 指定のメソッドが指定のクラスで実際に宣言されている場合のみ{@code true}を返す。
	 * @param method 対象のメソッド
	 * @param aClass 対象のクラス
	 * @return 実際に宣言されている場合に{@code true}
	 */
	private static boolean isDeclaredInClass(CtMethod method, CtClass aClass) {
		assert method != null;
		if (aClass == null) {
			return false;
		}
		try {
			aClass.getMethod(method.getName(), method.getSignature());
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	private static boolean isVoid(CtMethod method) {
		assert method != null;
		try {
			return method.getReturnType() == CtClass.voidType;
		} catch (NotFoundException e) {
			// 解決できない時点でプリミティブでない
			return false;
		}
	}
	
	/**
	 * {@code method}の動作をフックするポイントカットメソッドを拡張クラス上に作成して返す。
	 * <p>
	 * ポイントカットメソッドは、拡張元のクラスが公開するメソッドを上書きして
	 * 登録されたアドバイスを実行するための機能を提供する。
	 * ポイントカットメソッドが実行するアドバイスは、
	 * {@link #createAdviceTableField(CtClass)}によって作成されるフィールドに含まれている。
	 * </p>
	 * @param enhance 拡張対象のクラス
	 * @param method フックする対象のメソッド
	 * @param holder アドバイスを保持するフィールド
	 * @param index 拡張メソッドの番号
	 * @return 作成したポイントカットメソッド
	 * @throws EnhanceException ポイントカットメソッドの作成に失敗した場合
	 * @see #createAdviceTableField(CtClass)
	 * @see #createPointcutMethod(CtClass, CtMethod, CtField, int)
	 */
	public static CtMethod createPointcutMethod(CtClass enhance, CtMethod method, CtField holder, int index)
			throws EnhanceException {
		
		assert enhance != null;
		assert method != null;
		assert isSubtype(enhance, method.getDeclaringClass());
		assert holder != null;
		assert enhance.equals(holder.getDeclaringClass());
		assert index >= 0;
		assert isStatic(method) == false;
		
		LOG.trace("Creating pointcut method: {}#{}", method.getDeclaringClass().getName(), method.getName());
		
		try {
			CtMethod pointcut =
					new CtMethod(method.getReturnType(), method.getName(), method.getParameterTypes(), enhance);
			
			LOG.debug("Pointcut method[{}]: {}#{}{}", new Object[] {
				index,
				pointcut.getDeclaringClass().getName(),
				pointcut.getName(),
				pointcut.getSignature()
			});
			
			pointcut.setModifiers(Modifier.PUBLIC);
			if (isVoid(method)) {
				pointcut.setBody(String.format("%s[%d].invoke(this, $args);", holder.getName(), index));
			} else {
				pointcut.setBody(String.format("return ($r) %s[%d].invoke(this, $args);", holder.getName(), index));
			}
			enhance.addMethod(pointcut);
			return pointcut;
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Cannot create pointcut method for {0}", method
				.getLongName()), e);
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat.format("Cannot create pointcut method for {0}", method
				.getLongName()), e);
		}
	}
	
	/**
	 * ポイントカットメソッドが参照するアドバイステーブルを保持するフィールド拡張クラス上に作成して返す。
	 * <p>
	 * アドバイステーブルは、それぞれのポイントカットメソッドが利用するアドバイスを提供する。
	 * このメソッドによって作成されるフィールドは、このアドバイステーブルを保持するために
	 * それぞれのクラス上に自動的に宣言される。
	 * それぞれのアドバイスは{@link AdviceApplier}クラスによって定義され、
	 * フィールドにはこの配列型の値が格納される。
	 * </p>
	 * <p>
	 * それぞれのポイントカットメソッドには「拡張メソッド番号」というものが{@code 0}から順に
	 * 一意に割り当てられ、その番号と同一のインデックスを利用してこのフィールドの
	 * 配列に含まれるアドバイスを参照する。
	 * </p>
	 * @param target 拡張対象のクラス
	 * @return 作成した
	 * @throws EnhanceException フィールドの作成に失敗した場合
	 * @see #createPointcutMethod(CtClass, CtMethod, CtField, int)
	 */
	public static CtField createAdviceTableField(CtClass target) throws EnhanceException {
		
		assert target != null;
		LOG.trace("Creating advice table field: {}", target.getName());
		try {
			CtField field = new CtField(getAdviceTableType(target), getAdviceTableFieldName(target), target);
			field.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
			target.addField(field);
			LOG.debug("Advice table: {}#{}", field.getDeclaringClass().getName(), field.getName());
			return field;
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Cannot create advice table field for {0}", target
				.getName()), e);
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat.format("Cannot create advice table field for {0}", target
				.getName()), e);
		}
	}
	
	/**
	 * 指定のクラス名に対する拡張クラスの名称を返す。
	 * @param name 対象のクラス名
	 * @return 拡張クラスの名称
	 */
	private static String getEnhanceClassName(String name) {
		assert name != null;
		if (AVOID_NAME_PATTERN.matcher(name).matches()) {
			return ENHANCE_CLASS + name;
		}
		return name + ENHANCE_CLASS;
	}
	
	/**
	 * 指定のメソッドに対するバイパスメソッドの名称を返す。
	 * @param method 対象のメソッド
	 * @param index 拡張メソッド番号
	 * @return バイパスメソッドの名称
	 */
	private static String getBypassMethodName(CtMethod method, int index) {
		assert method != null;
		assert index >= 0;
		return String.format("%08d%s", index, BYPASS_METHOD);
	}
	
	/**
	 * 指定のクラスに対するアドバイステーブルフィールドの名称を返す。
	 * @param target 対象のクラス
	 * @return アドバイステーブルフィールドの名称
	 */
	private static String getAdviceTableFieldName(CtClass target) {
		assert target != null;
		return ADVICE_TABLE_FIELD;
	}
	
	/**
	 * アドバイステーブルの型を計算して返す。
	 * @param klass 対象の型
	 * @return アドバイステーブルの型
	 * @throws NotFoundException アドバイステーブルの型をロードできなかった場合
	 */
	private static CtClass getAdviceTableType(CtClass klass) throws NotFoundException {
		assert klass != null;
		CtClass helper = klass.getClassPool().get(AdviceApplier.class.getName() + "[]"); //$NON-NLS-1$
		return helper;
	}
	
	/**
	 * {@code target}の動作をフックするポイントカット実行式を、
	 * 元の{@code target}の処理の代わりに上書きする。
	 * <p>
	 * ポイントカット実行式は、拡張元のクラスが実行するインスタンス生成を上書きして、
	 * 登録されたアドバイスを実行するための機能を提供する。
	 * ポイントカットメソッドが実行するアドバイスは、
	 * {@link #createAdviceTableField(CtClass)}によって作成されるフィールドに含まれている。
	 * </p>
	 * @param target フックする対象のインスタンス生成式
	 * @param resultType 生成するインスタンス型の上限境界
	 * 		(本来実行するインスタンス生成式に対して、拡張前のプログラムが要求する型がインスタンス型のスーパータイプである可能性がある)
	 * @param holder アドバイスを保持するフィールド
	 * @param index 拡張番号
	 * @throws CannotCompileException コンパイルに失敗した場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @since 0.2.0
	 */
	public static void replaceToPointcut(
			NewExpr target,
			CtClass resultType,
			CtField holder,
			int index) throws CannotCompileException {
		if (target == null) {
			throw new NullPointerException("target is null"); //$NON-NLS-1$
		}
		if (resultType == null) {
			throw new NullPointerException("resultType is null"); //$NON-NLS-1$
		}
		if (holder == null) {
			throw new NullPointerException("holder is null"); //$NON-NLS-1$
		}
		CtBehavior contextBehaviour = target.where();
		if (isStatic(contextBehaviour)) {
			target.replace(String.format("$_ = (%s) %s[%d].invoke(%s, $args);", //$NON-NLS-1$
					resultType.getName(),
					holder.getName(),
					index,
					contextBehaviour.getDeclaringClass().getName() + ".class")); //$NON-NLS-1$
		} else {
			target.replace(String.format("$_ = (%s) %s[%d].invoke(this, $args);", //$NON-NLS-1$
					resultType.getName(),
					holder.getName(),
					index));
		}
	}
	
	/**
	 * 指定のプロダクトクラス一覧に、それぞれアスペクト用のフックを織り込む。
	 * <p>
	 * 実際に織り込まれるアスペクトの一覧は、{@code enhanceManager}を元に計算される。
	 * このメソッドでは、{@code enhanceList}に規定される拡張の一覧を
	 * 実際に適用するためのフックのみを織り込む。
	 * </p>
	 * @param enhanceManager
	 * 		エンハンスに関する情報を一意に管理するオブジェクト
	 * @param productsToBeEnhanced
	 *      拡張されるべきプロダクトクラスの一覧 ({@code base -> toBeEnhanced})
	 * @return
	 *      計算されたそれぞれのプロダクトクラスに対するメソッドアスペクトの一覧
	 *      ({@code base -> aspect for each method})
	 * @throws EnhanceException 拡張に失敗した場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static Map<CtClass, AspectList<CtMethod>> weavePointcutIntoAllProducts(
			EnhanceManager enhanceManager,
			Map<CtClass, CtClass> productsToBeEnhanced) throws EnhanceException {
		if (enhanceManager == null) {
			throw new NullPointerException("enhanceManager is null"); //$NON-NLS-1$
		}
		if (productsToBeEnhanced == null) {
			throw new NullPointerException("productsToBeEnhanced is null"); //$NON-NLS-1$
		}
		Map<CtClass, AspectList<CtMethod>> allProductAspects = new HashMap<CtClass, AspectList<CtMethod>>();
		for (Map.Entry<CtClass, CtClass> entry : productsToBeEnhanced.entrySet()) {
			CtClass base = entry.getKey();
			CtClass enhanced = entry.getValue();
			AspectList<CtMethod> aspects = weavePointcutIntoSingleProduct(enhanceManager, base, enhanced);
			if (aspects != null) {
				allProductAspects.put(base, aspects);
			}
		}
		return allProductAspects;
	}
	
	/**
	 * 単一のプロダクトクラスにアスペクト用のフックを織り込む。
	 * <p>
	 * {@code base}は{@link InvocationPointcut#isTarget(CtClass, CtBehavior)}の第一引数に利用される(であろう)型で、
	 * {@code enhance}のスーパータイプである必要がある。
	 * </p>
	 * @param enhanceManager エンハンスに関する情報を一意に管理するオブジェクト
	 * @param base 拡張される前のプロダクトクラス定義
	 * @param enhance 拡張対象となるプロダクトクラス定義
	 * @return 対象のプロダクトクラスに適用すべきアスペクトの一覧、ひとつも存在しない場合は{@code null}
	 * @throws EnhanceException 拡張に失敗した場合
	 */
	private static AspectList<CtMethod> weavePointcutIntoSingleProduct(
			EnhanceManager enhanceManager,
			CtClass base,
			CtClass enhance) throws EnhanceException {
		assert enhanceManager != null;
		assert base != null;
		assert enhance != null;
		assert isSubtype(enhance, base);
		
		LOG.trace("Weaving pointcuts: {}", enhance.getName());
		List<Aspect<CtMethod>> results = new ArrayList<Aspect<CtMethod>>();
		CtField holder = null;
		int enhanceIndex = 0;
		// FIXME override check
		for (CtMethod method : base.getMethods()) {
			if (enhanceManager.isLegalJoinpoint(method) == false) {
				continue;
			}
			List<InvocationHandler> handlers = enhanceManager.findApplicableHandlers(base, method);
			if (handlers.isEmpty()) {
				continue;
			}
			
			if (enhanceIndex == 0) {
				// 最初の拡張メソッドを発見したら、参照するアドバイステーブルフィールドも作成する
				holder = createAdviceTableField(enhance);
			}
			assert holder != null;
			CtMethod bypass = createBypassMethod(enhance, method, enhanceIndex);
			createPointcutMethod(enhance, method, holder, enhanceIndex);
			results.add(new Aspect<CtMethod>(method, bypass, handlers));
			enhanceIndex++;
		}
		
		if (results.isEmpty()) {
			assert holder == null;
			return null;
		} else {
			assert holder != null;
			return new AspectList<CtMethod>(holder, results);
		}
	}
	
	private static boolean isSubtype(CtClass subtype, CtClass supertype) {
		assert subtype != null;
		assert supertype != null;
		try {
			return subtype.subtypeOf(supertype);
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	/**
	 * 指定のメンバが{@code static}として宣言されている場合のみ{@code true}を返す。
	 * @param member 対象のメンバ
	 * @return
	 *      {@code static}として宣言されている場合に{@code true}、
	 *      そうでない場合は{@code false}
	 */
	private static boolean isStatic(CtMember member) {
		assert member != null;
		return Modifier.isStatic(member.getModifiers());
	}
	
	/**
	 * 指定のファクトリクラスをVM上にインストールする。
	 * @param converter {@link java.lang.Class}と{@link javassist.CtClass}を相互に変換する
	 * @param targetFactory ファクトリクラスの実装
	 * @param targetProducts 拡張されるべきプロダクトクラスの一覧 ({@code base -> toBeEnhanced})
	 * @param factoryAspects ファクトリに実際に埋め込まれるべきアスペクトの一覧、ひとつも存在しない場合は{@code null}
	 * @param allProductAspects それぞれのプロダクトクラスに対するメソッドアスペクトの一覧 ({@code base -> aspect for each method})、
	 * @return ロードしたファクトリクラス
	 * @throws EnhanceException いずれかのロードに失敗した場合 
	 * @since 0.2.0
	 */
	public static Class<?> install(
			JavassistConverter converter,
			CtClass targetFactory,
			Map<CtClass, CtClass> targetProducts,
			AspectList<CtConstructor> factoryAspects, // can be null
			Map<CtClass, AspectList<CtMethod>> allProductAspects) throws EnhanceException {
		if (converter == null) {
			throw new NullPointerException("converter is null"); //$NON-NLS-1$
		}
		if (targetFactory == null) {
			throw new NullPointerException("targetFactory is null"); //$NON-NLS-1$
		}
		if (targetProducts == null) {
			throw new NullPointerException("targetProducts is null"); //$NON-NLS-1$
		}
		if (allProductAspects == null) {
			throw new NullPointerException("allProductAspects is null"); //$NON-NLS-1$
		}
		
		LOG.trace("Installing: {}", targetFactory.getName());
		Map<CtClass, CtClass> restProducts = loadAndInitializeProducts(converter, targetProducts, allProductAspects);
		for (CtClass enhanced : restProducts.values()) {
			LOG.debug("Installing an enhanced product: {}", enhanced.getName());
			converter.toClass(enhanced);
		}
		
		LOG.debug("Installing the enhanced factory: {}", targetFactory.getName());
		Class<?> result = converter.toClass(targetFactory);
		if (factoryAspects != null) {
			registerAdviceTable(result, converter.toConstructorAspects(factoryAspects));
		}
		return result;
	}
	
	/**
	 * 拡張されるプロダクトの一覧をロードしたのち、それぞれを初期化する。
	 * <p>
	 * {@code productAspects.base IN productsToBeEnhanced.base}
	 * </p>
	 * @param converter Javassistを利用してCtClassとClassを相互に変換するオブジェクト
	 * @param productsToBeEnhanced
	 *      拡張されるべきプロダクトクラスの一覧 ({@code base -> toBeEnhanced})
	 * @param allProductAspects
	 *      それぞれのプロダクトクラスに対するメソッドアスペクトの一覧
	 *      ({@code base -> aspect for each method})
	 * @return
	 *      アスペクトが適用されなかった拡張されるべきプロダクトクラスの一覧
	 *      ({@code base -> toBeEnhanced})
	 * @throws EnhanceException 拡張に失敗した場合
	 */
	private static Map<CtClass, CtClass> loadAndInitializeProducts(
			JavassistConverter converter,
			Map<CtClass, CtClass> productsToBeEnhanced,
			Map<CtClass, AspectList<CtMethod>> allProductAspects) throws EnhanceException {
		assert productsToBeEnhanced != null;
		assert allProductAspects != null;
		
		Map<CtClass, CtClass> rest = new HashMap<CtClass, CtClass>();
		rest.putAll(productsToBeEnhanced);
		for (Map.Entry<CtClass, AspectList<CtMethod>> entry : allProductAspects.entrySet()) {
			CtClass orig = entry.getKey();
			AspectList<CtMethod> aspects = entry.getValue();
			CtClass enhanced = rest.remove(orig);
			LOG.debug("Installing an enhanced product: {}", enhanced.getName());
			AdviceTable table = converter.toMethodAspects(aspects);
			registerAdviceTable(converter.toClass(enhanced), table);
		}
		return rest;
	}
	
	/**
	 * 指定のクラスにアドバイステーブルの値を設定する。
	 * @param klass 設定対象のクラス
	 * @param adviceTable 設定するアドバイステーブル
	 * @throws EnhanceException 設定に失敗した場合
	 */
	private static void registerAdviceTable(Class<?> klass, AdviceTable adviceTable) throws EnhanceException {
		
		assert klass != null;
		assert adviceTable != null;
		
		AspectList<?> aspects = adviceTable.getAspects();
		AdviceApplier[] appliers = adviceTable.toAppliers();
		LOG.debug("Initialize advice table: {}", adviceTable);
		try {
			Field field = klass.getField(aspects.getAdviceTableHolder().getName());
			field.set(null, appliers);
		} catch (SecurityException e) {
			throw new EnhanceException(MessageFormat.format("Cannot access to enhanced field for ({0})", klass
				.getName()), e);
		} catch (NoSuchFieldException e) {
			// may not occur
			throw new AssertionError(e);
		} catch (IllegalArgumentException e) {
			// may not occur
			throw new AssertionError(e);
		} catch (IllegalAccessException e) {
			// may not occur
			throw new AssertionError(e);
		}
	}
	

	/**
	 * 同じ名前と仮引数型のリストを持つメソッドのグループ。
	 * @version $Id: EnhanceManipulator.java 3739 2009-10-09 14:08:04Z ashigeru $
	 * @author Suguru ARAKAWA
	 */
	private static class MethodGroup {
		
		private final Map<CtClass, CtMethod> returnTypeAndMethods;
		
		private final Set<CtClass> maximallySpecificTypes;
		
		final String name;
		
		final String parameters;
		

		/**
		 * インスタンスを生成する。
		 * @param method グループの最初のメンバとなるメソッド
		 * @throws NotFoundException 型の解決に失敗した場合
		 */
		public MethodGroup(CtMethod method) throws NotFoundException {
			assert method != null;
			returnTypeAndMethods = new HashMap<CtClass, CtMethod>();
			maximallySpecificTypes = new HashSet<CtClass>();
			name = method.getName();
			parameters = Descriptor.getParamDescriptor(method.getSignature());
			CtClass returnType = method.getReturnType();
			returnTypeAndMethods.put(returnType, method);
			maximallySpecificTypes.add(returnType);
		}
		
		/**
		 * 指定のメソッドをこのグループに追加する。
		 * <p>
		 * すでに同じシグネチャを持つメソッドがグループに存在する場合、この呼び出しは何も行わない。
		 * </p>
		 * @param method 追加するメソッド
		 * @return このグループに追加したら{@code true}、すでに同じシグネチャを持つものがいれば{@code false}
		 * @throws NotFoundException 型の解決に失敗した場合
		 */
		public boolean add(CtMethod method) throws NotFoundException {
			assert method != null;
			assert name.equals(method.getName());
			assert parameters.equals(Descriptor.getParamDescriptor(method.getSignature()));
			CtClass type = method.getReturnType();
			// already exists
			if (returnTypeAndMethods.containsKey(type)) {
				return false;
			}
			
			returnTypeAndMethods.put(type, method);
			updateMaximallySpecific(type);
			return true;
		}
		
		/**
		 * 最も限定的なメソッドがこのグループに存在する場合のみ{@code true}を返す。
		 * <p>
		 * 最大<em>限</em>に限定的なメソッドが複数存在する場合、このメソッドは{@code false}を返す。
		 * </p>
		 * @return 最大に限定的なメソッドがこのグループに存在する場合のみ{@code true}
		 */
		public boolean hasMostSpecificMethod() {
			return maximallySpecificTypes.size() == 1;
		}
		
		/**
		 * このグループの最も限定的な限定的なメソッドを返す。
		 * @return このグループの最も限定的な限定的なメソッド
		 * @throws IllegalStateException 最も限定的なメソッドを一意に特定できない場合
		 * @see #hasMostSpecificMethod()
		 */
		public CtMethod getMostSpecificMethod() {
			if (hasMostSpecificMethod() == false) {
				throw new IllegalStateException();
			}
			assert maximallySpecificTypes.size() == 1;
			CtClass returnType = maximallySpecificTypes.iterator().next();
			CtMethod theMost = returnTypeAndMethods.get(returnType);
			assert theMost != null;
			return theMost;
		}
		
		/**
		 * このグループの最も限定的な限定的なメソッドを除く、すべてのメソッド返す。
		 * @return このグループの最も限定的な限定的なメソッドを除く、すべてのメソッド
		 * @throws IllegalStateException 最も限定的なメソッドを一意に特定できない場合
		 * @see #hasMostSpecificMethod()
		 */
		public Collection<CtMethod> getLessSpecificMethods() {
			if (hasMostSpecificMethod() == false) {
				throw new IllegalStateException();
			}
			assert maximallySpecificTypes.size() == 1;
			if (returnTypeAndMethods.size() == 1) {
				assert returnTypeAndMethods.keySet().equals(maximallySpecificTypes);
				return Collections.emptySet();
			}
			Collection<CtMethod> results = new HashSet<CtMethod>();
			for (Map.Entry<CtClass, CtMethod> entry : returnTypeAndMethods.entrySet()) {
				if (maximallySpecificTypes.contains(entry.getKey())) {
					continue;
				}
				results.add(entry.getValue());
			}
			return results;
		}
		
		/**
		 * 戻り値型が最大限に限定的なものを探すため、現時点で最大限に限定的な型のうち、
		 * 指定の型のほうが限定的であるものを探して全体から除去する。
		 * <p>
		 * 現時点で最大限に限定的な型のいずれかよりも指定の型が限定的である場合、
		 * 最大限に限定的な型の集合に、指定の型を追加する。
		 * </p>
		 * @param type 対象の型
		 * @throws NotFoundException 型の解決に失敗した場合
		 */
		private void updateMaximallySpecific(CtClass type) throws NotFoundException {
			assert type != null;
			assert maximallySpecificTypes.isEmpty() == false;
			if (maximallySpecificTypes.contains(type)) {
				return;
			}
			boolean specific = false;
			for (Iterator<CtClass> iter = maximallySpecificTypes.iterator(); iter.hasNext();) {
				CtClass currentSpecific = iter.next();
				assert type.equals(currentSpecific) == false;
				if (type.subtypeOf(currentSpecific)) {
					iter.remove();
					specific = true;
				}
			}
			if (specific) {
				maximallySpecificTypes.add(type);
			}
		}
		
		@Override
		public String toString() {
			return MessageFormat.format(
					"{0}{1}:{2}",
					name, parameters, returnTypeAndMethods.keySet());
		}
	}
	
	/**
	 * 名前とデスクリプタのペア。主にオーバーロード判定に利用する。
	 * @version $Id: EnhanceManipulator.java 3739 2009-10-09 14:08:04Z ashigeru $
	 * @author Suguru ARAKAWA
	 */
	private static class NameAndParameter {
		
		final String name;
		
		final String parameter;
		

		/**
		 * インスタンスを生成する。
		 * @param method 対象のメソッド
		 */
		public NameAndParameter(CtMethod method) {
			assert method != null;
			name = method.getName();
			String paramAndReturn = method.getSignature();
			parameter = Descriptor.getParamDescriptor(paramAndReturn);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + parameter.hashCode();
			result = prime * result + name.hashCode();
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			NameAndParameter other = (NameAndParameter) obj;
			if (parameter.equals(other.parameter) == false) {
				return false;
			}
			if (name.equals(other.name) == false) {
				return false;
			}
			return true;
		}
	}
	

	/**
	 * インスタンス生成の禁止。
	 */
	private EnhanceManipulator() {
		throw new AssertionError();
	}
}
