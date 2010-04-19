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
package org.jiemamy.utils.enhancer;

import static org.jiemamy.utils.enhancer.helper.EnhanceManipulator.createFactoryImplementation;
import static org.jiemamy.utils.enhancer.helper.EnhanceManipulator.createProductImplementation;
import static org.jiemamy.utils.enhancer.helper.EnhanceManipulator.install;
import static org.jiemamy.utils.enhancer.helper.EnhanceManipulator.weavePointcutIntoAllProducts;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.helper.AccessibilityValidator;
import org.jiemamy.utils.enhancer.helper.AspectList;
import org.jiemamy.utils.enhancer.helper.EnhanceManager;
import org.jiemamy.utils.enhancer.helper.JavassistConverter;
import org.jiemamy.utils.enhancer.helper.NewInstanceEnhancer;
import org.jiemamy.utils.enhancer.reflection.ReflectionFactory;
import org.jiemamy.utils.enhancer.reflection.ReflectionUtil;

/**
 * インターフェースのみが提供されるファクトリを拡張し、またそのファクトリが提供する
 * プロダクトオブジェクトについても拡張を行う。
 * <p>
 * このエンハンサは主に次のようなことができる。
 * </p>
 * <ul>
 *   <li>
 *     ファクトリインターフェースの各メソッドの戻り値型をプロダクトの型とみなし、
 *     そのプロダクトオブジェクトを生成するコードを自動的に生成する。
 *     ただし、それぞれのプロダクトはインターフェース型で指定される必要がある。
 *     (以下、これらを<em>プロダクトインターフェース</em>と呼ぶ)
 *   </li>
 *   <li>
 *     各プロダクトインターフェースの実装を自動的に生成する。
 *     その際に、親クラスを指定することができる。
 *   </li>
 *   <li>
 *     プロダクトインターフェースで宣言されるメソッドを拡張し、呼び出し時にその実行をインターセプトして
 *     前後に別の処理を記述することができる。
 *   </li>
 * </ul>
 * <p>
 * このエンハンサには次のような制約がある。
 * </p>
 * <ul>
 *   <li>
 *     ファクトリインターフェース、プロダクトインターフェース、およびプロダクトの親クラスは、
 *     いずれも{@code public}で宣言されなければならない。
 *   </li>
 *   <li>
 *     ファクトリインターフェースはインターフェースとして宣言されなければならない。
 *   </li>
 *   <li>
 *     ファクトリインターフェースの各メソッドは、その戻り値として常にインターフェース型をとらなければならない。
 *     これらはすべて、プロダクトインターフェースとして扱われる。
 *   </li>
 *   <li>
 *     プロダクトインターフェースの親に指定するクラスは、列挙でない具象クラスである。
 *     また、{@code final}で指定されていてはならず、引数をとらない公開コンストラクタを提供する必要がある。
 *   </li>
 * </ul>
 * <p>
 * なお、現在の実装ではパラメータ化されたインターフェースを利用したファクトリ、プロダクトの拡張に関する
 * いくつかの不具合が存在する。
 * このため、それらを親インターフェースにもつファクトリやプロダクトの使用時の挙動は保証されない。
 * </p>
 * @version 0.2.0
 * @since 0.2.0
 * @author Suguru ARAKAWA
 * @param <T> 実装するファクトリのインターフェース型
 */
public class InterfaceEnhancer<T> extends AbstractEnhancer<T> {
	
	private static final Logger LOG =
			LoggerFactory.getLogger(InterfaceEnhancer.class);
	
	private final Class<T> factoryInterface;
	
	private final Set<Class<?>> productInterfaces;
	
	private final Class<?> productSuperClass;
	
	private EnhanceManager enhanceManager;
	
	private JavassistConverter converter;
	
	private volatile Class<? extends T> factoryImplementation;
	

	/**
	 * インスタンスを生成する。
	 * <p>
	 * 指定する引数はそれぞれ下記の制約を同時に満たす必要がある。
	 * </p>
	 * <ul>
	 *   <li> {@code factoryInterface}は{@code public}の可視性を持つ </li>
	 *   <li> {@code factoryInterface}はインターフェース型を表現する </li>
	 *   <li> {@code factoryInterface}のすべてのメソッドは、インターフェース型の値を返す </li>
	 *   <li> {@code factoryInterface}のそれぞれのメソッドが返すインターフェースは、いずれも{@code public}で宣言される </li>
	 *   <li> {@code productSuperClass}は{@code public}の可視性を持つ </li>
	 *   <li> {@code productSuperClass}は具象クラスである </li>
	 *   <li> {@code productSuperClass}は継承可能である </li>
	 *   <li> {@code productSuperClass}は(列挙でない)クラス型を表現する </li>
	 *   <li> {@code productSuperClass}は引数をとらない{@code public}コンストラクタを提供する </li>
	 * </ul>
	 * <p>
	 * このエンハンサによって拡張されたファクトリは、次のような性質を持つことになる。
	 * </p>
	 * <ul>
	 * <li>
	 * 	 ファクトリインターフェースの各メソッド(以下、ファクトリメソッド)は、
	 *   その戻り値型に指定されたインターフェース(以下、プロダクトインターフェース)を
	 *   実装するクラスのインスタンス(以下、プロダクトインスタンス)を返す。
	 * </li>
	 * <li>
	 *   プロダクトインターフェースの各メソッドに対して、{@code enhanceList}に指定された拡張が存在する場合、
	 *   対応するプロダクトインスタンスの各メソッドはその拡張が適用された状態になっている。
	 * </li>
	 * <li>
	 *   各ファクトリメソッドは、プロダクトインスタンスの生成に関するジョインポイントを提供する。
	 *   これに対して拡張を行う場合、次のようなポイントカットを用意する。 
	 *   <ul>
	 *   <li>
	 *     {@link InvocationPointcut#isTarget(CtClass, javassist.CtBehavior)}の
	 *     第1引数に、対象のファクトリメソッドの戻り値の型(つまり、ファクトリインターフェース)が渡されることを許可する。
	 *   </li>
	 *   <li>
	 *     {@link InvocationPointcut#isTarget(CtClass, javassist.CtBehavior)}の
	 *     第2引数に、対象のファクトリメソッドと同じパラメータリストを持つコンストラクタが渡されることを許可する。
	 *   </li>
	 *   </ul>
	 *   このジョインポイントに対する{@link InvocationHandler}において、独自のクラスインスタンスを返した場合、
	 *   それらはプロダクトインスタンスとみなされない。
	 *   つまり、{@code enhanceList}に指定されたメソッドジョインポイントに関する拡張は、
	 *   それらのインスタンスに対して適用されない。
	 * </li>
	 * </ul>
	 * <p>
	 * 拡張対象の判定は、{@code enhanceList}に含まれるそれぞれの{@link Enhance}が返す、
	 * {@link InvocationPointcut}によって行われる。
	 * </p>
	 * <p>
	 * 単一のジョインポイント(メソッド起動やインスタンス生成)を複数のエンハンスが同時に拡張する場合、
	 * 引数{@code enhanceList}に指定された順にハンドラが実行される。
	 * たとえば、{@code enhanceList}に対してあるメソッド呼び出しに対して
	 * 同時に適用可能な拡張{@code [a, b, c]}が含まれる場合、
	 * {@code a, b, c}の順に含まれる{@link InvocationHandler}が実行された後、
	 * 最後の{@code c}から実際のメソッドが呼び出される。
	 * これらの途中で後続する処理を実行しなかった場合、それらの拡張は実際には利用されない。
	 * つまり、{@code b}の拡張がハンドラの内部で{@link Invocation#proceed()}を実行しなかった場合、
	 * それに続く{@code c}および実際のメソッドは実行されないことになる。
	 * </p>
	 * @param factoryInterface 実装を生成する対象のファクトリインターフェース
	 * @param productSuperClass それぞれのプロダクトが実装する親クラス
	 * @param enhanceList 拡張を定義するオブジェクトの一覧
	 * @throws IllegalArgumentException それぞれの引数が上記制約を満たさない場合
	 * @throws NullPointerException 引数に{@code null}が含まれる場合
	 */
	public InterfaceEnhancer(
			Class<T> factoryInterface,
			Class<?> productSuperClass,
			List<? extends Enhance> enhanceList) {
		if (factoryInterface == null) {
			throw new NullPointerException("factoryInterface is null"); //$NON-NLS-1$
		}
		if (productSuperClass == null) {
			throw new NullPointerException("productSuperClass is null"); //$NON-NLS-1$
		}
		if (enhanceList == null) {
			throw new NullPointerException("enhanceList is null"); //$NON-NLS-1$
		}
		checkFactoryInterfaceConstraint(factoryInterface);
		checkProductSuperClassConstraint(productSuperClass);
		this.factoryInterface = factoryInterface;
		this.productInterfaces = computeProductInterfaces(factoryInterface);
		this.productSuperClass = productSuperClass;
		this.enhanceManager = new EnhanceManager(enhanceList);
		this.converter = new JavassistConverter(factoryInterface);
	}
	
	/**
	 * インスタンスを生成する。
	 * <p>
	 * このコンストラクタは引数{@code enhances}を{@link List}型に変換した後に、
	 * {@link #InterfaceEnhancer(Class, Class, List)}にすべての引数を委譲して実行する。
	 * </p>
	 * @param factoryInterface 実装を生成する対象のファクトリインターフェース
	 * @param productSuperClass それぞれのプロダクトが実装する親クラス
	 * @param enhances 拡張を定義するオブジェクトの一覧
	 * @throws NullPointerException
	 *      引数{@code enhances}に{@code null}が指定された場合
	 * @throws IllegalArgumentException
	 *      {@link #InterfaceEnhancer(Class, Class, List)}で同様の例外が発生した場合
	 * @throws NullPointerException
	 *      {@link #InterfaceEnhancer(Class, Class, List)}で同様の例外が発生した場合
	 * @see #InterfaceEnhancer(Class, Class, List)
	 */
	public InterfaceEnhancer(Class<T> factoryInterface, Class<?> productSuperClass, Enhance... enhances) {
		this(factoryInterface, productSuperClass, checkToList(enhances, "enhances")); //$NON-NLS-1$
	}
	
	private static <T>List<T> checkToList(T[] values, String name) {
		if (values == null) {
			throw new NullPointerException(name);
		}
		return Arrays.asList(values);
	}
	
	/**
	 * ファクトリインターフェースが満たすべき制約を検証する。
	 * <p>
	 * ただし、ここではファクトリメソッドに関する検査は行わない。
	 * これについては、{@link #computeProductInterfaces(Class)}の内部で行われる。
	 * </p>
	 * @param anInterface 対象のファクトリインターフェース
	 * @throws IllegalArgumentException 引数がファクトリインターフェースとしてのいずれかの制約を満たさない場合
	 * @see InterfaceEnhancer
	 */
	private static void checkFactoryInterfaceConstraint(Class<?> anInterface) {
		assert anInterface != null;
		int modifiers = anInterface.getModifiers();
		if (Modifier.isInterface(modifiers) == false) {
			throw new IllegalArgumentException(MessageFormat.format(
					"The factory ({0}) must be an interface",
					anInterface.getName()));
		}
		if (Modifier.isPublic(modifiers) == false) {
			throw new IllegalArgumentException(MessageFormat.format(
					"The factory ({0}) must be public",
					anInterface.getName()));
		}
	}
	
	/**
	 * ファクトリインターフェースの親クラスが満たすべき制約を検証する。
	 * @param aClass 対象のクラス
	 * @throws IllegalArgumentException 引数がファクトリインターフェースの親クラスとしてのいずれかの制約を満たさない場合
	 * @see InterfaceEnhancer
	 */
	private void checkProductSuperClassConstraint(Class<?> aClass) {
		assert aClass != null;
		if (ReflectionUtil.isNormalClass(aClass) == false) {
			throw new IllegalArgumentException(MessageFormat.format(
					"The product super class ({0}) must be a normal class",
					aClass.getName()));
		}
		int modifiers = aClass.getModifiers();
		if (Modifier.isPublic(modifiers) == false) {
			throw new IllegalArgumentException(MessageFormat.format(
					"The product super class ({0}) must be public",
					aClass.getName()));
		}
		if (Modifier.isAbstract(modifiers)) {
			throw new IllegalArgumentException(MessageFormat.format(
					"The product super class ({0}) must not be abstract class",
					aClass.getName()));
		}
		if (Modifier.isFinal(modifiers)) {
			throw new IllegalArgumentException(MessageFormat.format(
					"The product super class ({0}) must not be final class",
					aClass.getName()));
		}
		try {
			aClass.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(MessageFormat.format(
					"Product super class ({0}) must have a public non-parameters constructor",
					aClass.getName()),
					e);
		}
	}
	
	/**
	 * 生成したファクトリをインスタンスかするためのメタファクトリを返す。
	 * @throws EnhanceException ファクトリの拡張に失敗した場合
	 */
	@Override
	protected Factory<? extends T> createFactory() throws EnhanceException {
		/* 
		 * わざわざ別メソッドにしているのは、getEnhance()が返すクラスが
		 * ? extends T であり、これをキャプチャして名前のある型変数にする必要があるため。
		 * Javaの言語仕様では、クラスインスタンス生成時にパラメータ化型を利用する場合、
		 * その実型引数は型式であってはならない(ワイルドカードが使えない)。
		 */
		return createMetaFactory(getImplemtation());
	}
	
	private synchronized Class<? extends T> getImplemtation() throws EnhanceException {
		if (factoryImplementation == null) {
			this.factoryImplementation = createImplementation();
			
			// prune javassist information
			enhanceManager = null;
			converter = null;
		}
		return this.factoryImplementation;
	}
	
	/**
	 * 指定のファクトリインターフェースが利用する、プロダクトインターフェースの一覧を返す。
	 * @param factoryInterface 対象のファクトリインターフェース
	 * @return 対応するプロダクトインターフェースの一覧
	 * @throws IllegalArgumentException 引数のファクトリインターフェースが参照するいずれかのファクトリメソッドが、
	 * 		制約を満たさないプロダクトインターフェースを戻り値に持つ場合
	 * @see InterfaceEnhancer
	 */
	private static Set<Class<?>> computeProductInterfaces(Class<?> factoryInterface) {
		assert factoryInterface != null;
		Set<Class<?>> results = new HashSet<Class<?>>();
		for (Method method : factoryInterface.getMethods()) {
			// java.lang.Objectで定義されたメソッドは無視
			if (method.getDeclaringClass() == Object.class) {
				continue;
			}
			assert Modifier.isAbstract(method.getModifiers()) : method;
			
			Class<?> product = method.getReturnType();
			if (product.isInterface() == false) {
				throw new IllegalArgumentException(MessageFormat.format(
						"All product must be declared as an interface ({0})",
						method));
			}
			if (Modifier.isPublic(product.getModifiers()) == false) {
				throw new IllegalArgumentException(MessageFormat.format(
						"All product interface must be public ({0})",
						product));
			}
			results.add(product);
		}
		return Collections.unmodifiableSet(results);
	}
	
	/**
	 * このクラスのインスタンス生成時に指定された、ファクトリインターフェースを実装したクラスオブジェクトを生成して返す。
	 * @return 生成したクラスオブジェクト
	 * @throws EnhanceException ファクトリインターフェースの実装や、クラスオブジェクトの生成に失敗した場合
	 */
	private Class<? extends T> createImplementation() throws EnhanceException {
		assert enhanceManager != null;
		assert converter != null;
		LOG.trace("Creating an implementation of factory: {}", factoryImplementation);
		
		Map<CtClass, CtClass> targetProducts = createProductMap();
		CtClass implementation = createFactory(targetProducts);
		
		Map<CtClass, AspectList<CtMethod>> allProductAspects =
				weavePointcutIntoAllProducts(enhanceManager, targetProducts);
		
		AccessibilityValidator.validate(implementation);
		
		AspectList<CtConstructor> factoryAspects =
				weavePointcutIntoFactory(implementation, targetProducts, allProductAspects);
		Class<?> installedFactory =
				install(converter, implementation, targetProducts, factoryAspects, allProductAspects);
		
		return installedFactory.asSubclass(factoryInterface);
	}
	
	/**
	 * プロダクトインターフェースの一覧に対応する、プロダクトクラスの一覧を生成して返す。
	 * <p>
	 * このメソッドが生成する各プロダクトクラスは、特にコンストラクタやメソッドが生成されていない状態である。
	 * 親クラスと親インターフェースのみが、プロダクトクラスの規約に従って設定される。
	 * </p>
	 * @return プロダクトインターフェースと、対応するプロダクトクラスのペア一覧
	 * @throws EnhanceException プロダクトクラスの生成に失敗した場合
	 */
	private Map<CtClass, CtClass> createProductMap() throws EnhanceException {
		LOG.trace("Creating each product map: {}", productInterfaces);
		Map<CtClass, CtClass> results = new HashMap<CtClass, CtClass>();
		CtClass baseClass = converter.loadCtClass(productSuperClass);
		for (Class<?> productInterface : productInterfaces) {
			CtClass baseInterface = converter.loadCtClass(productInterface);
			CtClass productClass = createProductImplementation(baseInterface, baseClass);
			results.put(baseInterface, productClass);
		}
		LOG.debug("Product map: {}", results);
		return results;
	}
	
	/**
	 * ファクトリインターフェースに対するファクトリクラスの実装を生成して返す。
	 * <p>
	 * 引数に指定したプロダクトインターフェースとプロダクトクラスのマッピングは、
	 * ファクトリインターフェースがファクトリメソッドを実装する際に使用する。
	 * ファクトリメソッドを実装する際には、次の順で作業が行われるはずである。
	 * </p>
	 * <ul>
	 * <li> ファクトリメソッドに対応したコンストラクタを、該当のファクトリクラス上に生成する </li>
	 * <li> ファクトリメソッドに対応したメソッドを、ファクトリクラス上に生成する </li>
	 * <li> ファクトリクラス上のファクトリメソッドが、ファクトリクラス上の対応するコンストラクタを起動してその値を返すように実装する </li>
	 * </ul>
	 * <p>
	 * この時点では、まだ{@link Enhance}は利用しない。
	 * </p>
	 * @param targetProductMap {@link #createProductMap()}で生成したプロダクトマップ。
	 * 		それぞれの実装に対し、ファクトリメソッドに対応したコンストラクタが自動生成される
	 * @return 生成したファクトリクラス
	 * @throws EnhanceException ファクトリクラスの生成や、プロダクトクラス上のコンストラクタ生成に失敗した場合
	 */
	private CtClass createFactory(Map<CtClass, CtClass> targetProductMap) throws EnhanceException {
		assert targetProductMap != null;
		CtClass factory = converter.loadCtClass(factoryInterface);
		CtClass factoryImpl = createFactoryImplementation(factory, targetProductMap);
		return factoryImpl;
	}
	
	/**
	 * ファクトリクラスで行われるプロダクトインスタンスの生成に対し、{@link Enhance}による拡張を挿入する。
	 * @param implementation 対象のファクトリクラス
	 * @param targetProductMap {@link #createProductMap()}で生成したプロダクトマップ
	 * @param allProductAspects それぞれのプロダクトクラスに追加したアスペクトの一覧
	 * @return ファクトリクラスのプロダクトインスタンス生成に対する、アスペクトの一覧
	 * @throws EnhanceException ファクトリクラスの拡張に失敗した場合
	 */
	private AspectList<CtConstructor> weavePointcutIntoFactory(
			CtClass implementation,
			Map<CtClass, CtClass> targetProductMap,
			Map<CtClass, AspectList<CtMethod>> allProductAspects) throws EnhanceException {
		assert implementation != null;
		assert targetProductMap != null;
		assert allProductAspects != null;
		
		LOG.trace("Weaving pointcuts: {}", implementation.getName());
		return NewInstanceEnhancer.enhance(implementation, enhanceManager, targetProductMap, allProductAspects);
	}
	
	/**
	 * メタファクトリのインスタンスを作成する。
	 * @param <F> メタファクトリが生成するファクトリの種類
	 * @param aClass メタファクトリが生成するファクトリのクラス
	 * @return 生成したメタファクトリ
	 */
	private static <F>Factory<F> createMetaFactory(Class<F> aClass) {
		return new ReflectionFactory<F>(aClass);
	}
}
