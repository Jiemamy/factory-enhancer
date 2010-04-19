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

import static org.jiemamy.utils.enhancer.helper.EnhanceManipulator.createCopyClass;
import static org.jiemamy.utils.enhancer.helper.EnhanceManipulator.createInheritedClass;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.helper.AccessibilityValidator;
import org.jiemamy.utils.enhancer.helper.AspectList;
import org.jiemamy.utils.enhancer.helper.CtClassComparator;
import org.jiemamy.utils.enhancer.helper.EnhanceManager;
import org.jiemamy.utils.enhancer.helper.EnhanceManipulator;
import org.jiemamy.utils.enhancer.helper.EnhanceTargetProductCollector;
import org.jiemamy.utils.enhancer.helper.JavassistConverter;
import org.jiemamy.utils.enhancer.helper.NewInstanceEnhancer;
import org.jiemamy.utils.enhancer.reflection.ReflectionFactory;

/**
 * ファクトリを拡張するエンハンサ。
 * <p>
 * このエンハンサは主に次のようなことができる。
 * </p>
 * <ul>
 *   <li>
 *     ファクトリクラス内でプロダクトクラスのインスタンスを生成する処理をインターセプトして、
 *     前後に別の処理を記述することができる。
 *   </li>
 *   <li>
 *     プロダクトクラスで宣言されるメソッドを拡張し、呼び出し時にその実行をインターセプトして
 *     前後に別の処理を記述することができる。
 *   </li>
 * </ul>
 * <p>
 * それぞれのインターセプト方法は、{@link Enhance}で定義することができる。
 * </p>
 * <p>
 * このエンハンサには次のような制約がある。
 * </p>
 * <ul>
 *   <li>
 *     拡張する対象のファクトリクラスは、必ず何らかのインターフェースを実装しなければならない。
 *     また、拡張されたファクトリクラスは元のクラスと同一でなく、またサブタイプ関係にもない。
 *     ただし、元のクラスが宣言するインターフェースは、
 *     拡張されたファクトリクラスにおいてもすべて実装されている。
 *   </li>
 *   <li>
 *     拡張する対象のファクトリクラスは、明示的な親クラスを持つことができない。
 *     つまり、{@link java.lang.Object}の直接の子クラスでなければならない。
 *   </li>
 *   <li>
 *     拡張する対象のファクトリクラスは、{@code public}で宣言される具象クラスでなければならない。
 *     つまり、{@code abstract}クラスや列挙型は除外される。
 *   </li>
 *   <li>
 *     拡張する対象のファクトリクラスは、{@code public}で宣言された型やメンバのみを参照できる。
 *     ただし、自身が宣言するメンバに関してはこの限りではない。
 *   </li>
 *   <li>
 *     拡張する対象のプロダクトクラスは、いずれもファクトリクラスの中でのみインスタンスを生成されなければならない。
 *     そうでない場合、拡張が適用されないプロダクトクラスのインスタンスが生成される場合がある。
 *   </li>
 *   <li>
 *     拡張する対象のプロダクトクラスは、{@code public}の公開性を持たなければならない。
 *     また、{@code final}で宣言されていてはならない。
 *   </li>
 *   <li>
 *     プロダクトクラスに含まれる拡張する対象のメソッドは、{@code public}の公開性を持たなければならない。
 *     また、{@code final}および{@code static}で宣言されていてはならない。
 *     さらに、コンパイラによって合成された特殊なメソッド(ブリッジメソッド等)は拡張対象とならない。
 *   </li>
 * </ul>
 * @version 0.2.0
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 * @param <T> 拡張するファクトリのインターフェース型
 */
public class FactoryEnhancer<T> extends AbstractEnhancer<T> {
	
	static final Logger LOG = LoggerFactory.getLogger(FactoryEnhancer.class);
	
	private final Class<T> factoryInterface;
	
	private final Class<? extends T> factoryImplementation;
	
	private EnhanceManager enhanceManager;
	
	private JavassistConverter converter;
	
	private Class<? extends T> enhancedFactory;
	

	/**
	 * インスタンスを生成する。
	 * <p>
	 * 指定する引数はそれぞれ下記の制約を同時に満たす必要がある。
	 * </p>
	 * <ul>
	 *   <li> {@code factoryInterface}は(注釈でない)インターフェース型を表現する </li>
	 *   <li> {@code factoryImplementation}は(列挙でない)クラス型を表現する </li>
	 *   <li> {@code factoryImplementation}は具象クラスである </li>
	 *   <li>
	 *     {@code factoryImplementation}は{@code factoryInterface}を実装する
	 *   </li>
	 *   <li>
	 *     {@code factoryImplementation}は
	 *     {@code java.lang.Object}の直接のサブクラスである
	 *   </li>
	 * </ul>
	 * <p>
	 * また、このクラスによって拡張されるファクトリは、つぎのそれぞれの条件をすべて満たすもののみである。
	 * </p>
	 * <ul>
	 *   <li> 拡張されるインスタンス生成式 <ul>
	 *     <li>
	 *       {@code factoryImplementation}に直接記述されたクラスインスタンス生成式である
	 *     </li>
	 *     <li> 対象のクラスが{@code public}で宣言されている </li>
	 *     <li> 実行するコンストラクタが{@code public}で宣言されている </li>
	 *     <li>
	 *       実行するコンストラクタが{@code enhanceList}に含まれるいずれかの拡張対象となる
	 *     </li> </ul>
	 *   </li>
	 *   <li> 拡張されるプロダクトクラス <ul>
	 *     <li> 下記のすべてを満たすいずれかのインスタンス生成式の対象にとるクラスである : <ul>
	 *       <li>
	 *         {@code factoryImplementation}に直接記述されたインスタンス生成式である
	 *       </li>
	 *       <li> 対象のクラスが{@code public}で宣言されている </li>
	 *       <li> 対象のクラスが{@code final}で宣言されて<b>いない</b> </li>
	 *       <li> 実行対象のコンストラクタが{@code public}で宣言されている </li> </ul>
	 *     </li>
	 *     <li> クラスが公開するいずれかのメソッドが次の条件をすべて満たす
	 *          (つまり、このクラスが拡張されるプロダクトメソッドを含む): <ul>
	 *       <li> {@code public}で宣言されている </li>
	 *       <li> {@code final}で宣言されて<b>いない</b> </li>
	 *       <li> {@code static}で宣言されて<b>いない</b> </li>
	 *       <li>
	 *         {@code enhanceList}に含まれるいずれかの拡張対象となる
	 *       </li> </ul>
	 *     </li> </ul>
	 *   </li>
	 *   <li> 拡張されるプロダクトメソッド <ul>
	 *     <li>
	 *       上記&quot;拡張されるプロダクトクラス&quot;の対象となったクラスが公開するメソッドである
	 *       (継承したメソッドを含む)
	 *     </li>
	 *     <li> {@code public}で宣言されている </li>
	 *     <li> {@code final}で宣言されて<b>いない</b> </li>
	 *     <li> {@code static}で宣言されて<b>いない</b> </li>
	 *     <li> ブリッジメソッドでない </li>
	 *     <li> コンパイラによって合成された({@code synthetic})メソッドでない </li>
	 *     <li>
	 *       {@code enhanceList}に含まれるいずれかの拡張対象となる
	 *     </li> </ul>
	 *   </li>
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
	 * <p>
	 * なお、プロダクトクラスが拡張される場合、このオブジェクトが作成するファクトリは
	 * 拡張されたプロダクトクラスをインスタンス化して返す。
	 * ただし、拡張対象となるプロダクトクラスのうち、{@code public}でない
	 * コンストラクタを起動するようなインスタンス生成式は拡張されていない通常のプロダクトクラスの
	 * インスタンスを生成する。
	 * </p>
	 * @param factoryInterface 拡張するファクトリのインターフェース型
	 * @param factoryImplementation 拡張するファクトリの実装クラス
	 * @param enhanceList 拡張を定義するオブジェクトの一覧
	 * @throws NullPointerException
	 *     引数に{@code null}が指定された場合、または
	 *     引数{@code enhanceList}に{@code null}が含まれる場合
	 * @throws IllegalArgumentException
	 *     {@code factoryInterface}がインターフェース型でない場合
	 * @throws IllegalArgumentException
	 *     {@code factoryImplementation}がクラス型でない場合
	 * @throws IllegalArgumentException
	 *     {@code factoryImplementation}が抽象クラスである場合
	 * @throws IllegalArgumentException
	 *     {@code factoryImplementation}が{@code factoryInterface}を
	 *     実装しない場合
	 * @throws IllegalArgumentException
	 *     {@code factoryImplementation}が{@code java.lang.Object}の
	 *     直接のサブクラスでない場合
	 */
	public FactoryEnhancer(
			Class<T> factoryInterface,
			Class<? extends T> factoryImplementation,
			List<? extends Enhance> enhanceList) {
		super();
		if (factoryInterface == null) {
			throw new NullPointerException("factoryInterface");
		}
		if (factoryImplementation == null) {
			throw new NullPointerException("factoryImplementation");
		}
		if (enhanceList == null) {
			throw new NullPointerException("enhanceList");
		}
		LOG.debug("factoryInterface={}", factoryInterface);
		LOG.debug("factoryImplementation={}", factoryImplementation);
		LOG.debug("enhanceList={}", enhanceList);
		
		if (factoryInterface.isInterface() == false || factoryInterface.isAnnotation()) {
			throw new IllegalArgumentException(MessageFormat.format("{0} must be a interface", factoryInterface
				.getName()));
		}
		if (factoryImplementation.isInterface() || factoryImplementation.isEnum()) {
			throw new IllegalArgumentException(MessageFormat.format("{0} must be a class", factoryImplementation
				.getName()));
		}
		if (factoryInterface.isAssignableFrom(factoryImplementation) == false) {
			throw new IllegalArgumentException(MessageFormat.format("{0} does not implement {1}", factoryImplementation
				.getName(), factoryInterface.getName()));
		}
		if (factoryImplementation.getSuperclass() != Object.class) {
			throw new IllegalArgumentException(MessageFormat.format("{0} is not a direct subclass of {1}",
					factoryImplementation.getName(), Object.class.getName()));
		}
		if (Modifier.isPublic(factoryImplementation.getModifiers()) == false) {
			throw new IllegalArgumentException(MessageFormat.format("{0} is not a public class", factoryImplementation
				.getName()));
		}
		if (Modifier.isAbstract(factoryImplementation.getModifiers())) {
			throw new IllegalArgumentException(MessageFormat.format("{0} is an abstract class", factoryImplementation
				.getName()));
		}
		this.factoryInterface = factoryInterface;
		this.factoryImplementation = factoryImplementation;
		this.converter = new JavassistConverter(factoryImplementation);
		this.enhanceManager = new EnhanceManager(enhanceList);
	}
	
	/**
	 * インスタンスを生成する。
	 * <p>
	 * このコンストラクタは引数{@code enhances}を{@link List}型に変換した後に、
	 * {@link #FactoryEnhancer(Class, Class, List)}にすべての引数を委譲して実行する。
	 * </p>
	 * @param factoryInterface 拡張するファクトリのインターフェース型
	 * @param factoryImplementation 拡張するファクトリの実装クラス
	 * @param enhances 拡張を定義するオブジェクトの一覧
	 * @throws NullPointerException
	 *      引数{@code enhances}に{@code null}が指定された場合
	 * @throws IllegalArgumentException
	 *      {@link #FactoryEnhancer(Class, Class, List)}で同様の例外が発生した場合
	 * @throws NullPointerException
	 *      {@link #FactoryEnhancer(Class, Class, List)}で同様の例外が発生した場合
	 * @see #FactoryEnhancer(Class, Class, List)
	 */
	public FactoryEnhancer(Class<T> factoryInterface, Class<? extends T> factoryImplementation, Enhance... enhances) {
		this(factoryInterface, factoryImplementation, checkToList(enhances, "enhances")); //$NON-NLS-1$
	}
	
	private static <T>List<T> checkToList(T[] values, String name) {
		if (values == null) {
			throw new NullPointerException(name);
		}
		return Arrays.asList(values);
	}
	
	/**
	 * 拡張されたファクトリのクラスオブジェクトを返す。
	 * <p>
	 * 拡張されたファクトリは、元の実装クラスのサブクラスとは限らない。
	 * ただし、コンストラクタに指定されたファクトリインターフェースの実装であることが保証される。
	 * </p>
	 * <p>
	 * すでにこのオブジェクトが拡張したファクトリクラスを生成している場合、この呼び出しは
	 * 前回の呼び出しと同一の結果を返す。
	 * </p>
	 * @return 拡張されたファクトリのクラスオブジェクト
	 * @throws EnhanceException 拡張に失敗した場合
	 * @see Enhancer#getFactory()
	 */
	public synchronized Class<? extends T> getEnhanced() throws EnhanceException {
		if (enhancedFactory == null) {
			enhancedFactory = prepareEnhanced();
			converter = null;
			enhanceManager = null;
		}
		return enhancedFactory;
	}
	
	/**
	 * {@link #getEnhanced()}を利用して拡張されたファクトリクラスに対する、オブジェクトファクトリを返す。
	 * @throws EnhanceException {@link #getEnhanced()}の実行に失敗した場合
	 * @since 0.2.0
	 */
	@Override
	protected Factory<? extends T> createFactory() throws EnhanceException {
		/* 
		 * わざわざ別メソッドにしているのは、getEnhance()が返すクラスが
		 * ? extends T であり、これをキャプチャして名前のある型変数にする必要があるため。
		 * Javaの言語仕様では、クラスインスタンス生成時にパラメータ化型を利用する場合、
		 * その実型引数は型式であってはならない(ワイルドカードが使えない)。
		 */
		return createMetaFactory(getEnhanced());
	}
	
	/**
	 * メタファクトリのインスタンスを作成する。
	 * @param <F> メタファクトリが生成するファクトリの種類
	 * @param aClass メタファクトリが生成するファクトリのクラス
	 * @return 生成したメタファクトリ
	 */
	private static <F>ReflectionFactory<F> createMetaFactory(Class<F> aClass) {
		return new ReflectionFactory<F>(aClass);
	}
	
	/**
	 * ファクトリクラスを拡張し、拡張したクラスを表現する{@link java.lang.Class}オブジェクトを返す。
	 * @return 拡張したクラスを表現する{@link java.lang.Class}オブジェクト
	 * @throws EnhanceException　拡張に失敗した場合
	 */
	private Class<? extends T> prepareEnhanced() throws EnhanceException {
		LOG.debug("Start factory enhancer: {}", factoryImplementation);
		
		CtClass original = converter.loadCtClass(factoryImplementation);
		CtClass enhance = createCopyClass(original);
		
		AccessibilityValidator.validate(enhance);
		
		Map<CtClass, CtClass> targetProducts =
				createProductMap(enhance);
		Map<CtClass, AspectList<CtMethod>> allProductAspects =
				EnhanceManipulator.weavePointcutIntoAllProducts(enhanceManager, targetProducts);
		AspectList<CtConstructor> factoryAspects =
				weavePointcutIntoFactory(enhance, targetProducts, allProductAspects);
		Class<?> installedFactory =
				EnhanceManipulator.install(converter, enhance, targetProducts, factoryAspects, allProductAspects);
		
		return installedFactory.asSubclass(factoryInterface);
	}
	
	/**
	 * 指定の(拡張された)ファクトリに対し、必要なプロダクトクラスを拡張したクラスを生成して返す。
	 * <p>
	 * 拡張されたプロダクトクラスは、親クラスにもとのプロダクトクラスを持ち、
	 * 全ての(公開)コンストラクタが移譲コンストラクタとして宣言されている。
	 * ただし、返されるマップに含まれるプロダクトクラスは、メソッド呼び出しのポイントカットが存在するものに限られる。
	 * つまり、該当するジョインポイントが存在しないクラスは、このメソッド呼び出しの戻り値に含まれない。
	 * </p>
	 * @param enhance 対象の拡張されたファクトリクラス
	 * @return 拡張対象となった本来のプロダクトクラスと、それに対応する拡張プロダクトクラスのペア一覧
	 * @throws EnhanceException 拡張プロダクトクラスの生成に失敗した場合
	 */
	private Map<CtClass, CtClass> createProductMap(CtClass enhance) throws EnhanceException {
		assert enhance != null;
		List<CtClass> targets = EnhanceTargetProductCollector.collect(enhance, enhanceManager);
		Map<CtClass, CtClass> result = newMap();
		for (CtClass product : targets) {
			CtClass enhanced = createInheritedClass(product);
			result.put(product, enhanced);
		}
		return result;
	}
	
	/**
	 * 指定のファクトリクラスに、アスペクト用のフックを織り込む。
	 * <p>
	 * 実際に織り込まれるアスペクトの一覧は、{@code enhanceList}と
	 * プロダクトの拡張方法を元に計算される。
	 * このメソッドでは、{@code enhanceList}に規定される拡張の一覧を
	 * 実際に適用するためのフックのみを織り込む。
	 * </p>
	 * <p>
	 * {@code productAspects.base IN productsToBeEnhanced.base}
	 * </p>
	 * @param factoryClass アスペクトを埋め込む先のファクトリクラス
	 * @param productsToBeEnhanced
	 *      拡張されるべきプロダクトクラスの一覧 ({@code base -> toBeEnhanced})
	 * @param allProductAspects
	 *      それぞれのプロダクトクラスに対するメソッドアスペクトの一覧
	 *      ({@code base -> aspect for each method})
	 * @return
	 *      ファクトリに実際に埋め込まれるべきアスペクトの一覧、ひとつも存在しない場合は{@code null}
	 * @throws EnhanceException 拡張に失敗した場合
	 * @see #testAllWeaveTargetWillBeEnhanced(Map, Map)
	 */
	private AspectList<CtConstructor> weavePointcutIntoFactory(CtClass factoryClass,
			Map<CtClass, CtClass> productsToBeEnhanced, Map<CtClass, AspectList<CtMethod>> allProductAspects)
			throws EnhanceException {
		
		assert factoryClass != null;
		assert productsToBeEnhanced != null;
		assert allProductAspects != null;
		assert testAllWeaveTargetWillBeEnhanced(productsToBeEnhanced, allProductAspects);
		
		LOG.trace("Weaving pointcuts: {}", factoryClass.getName());
		return NewInstanceEnhancer.enhance(factoryClass, enhanceManager, productsToBeEnhanced, allProductAspects);
	}
	
	/**
	 * {@code productAspects.base IN productsToBeEnhanced.base}
	 * @param productsToBeEnhanced
	 *      拡張されるべきプロダクトクラスの一覧 ({@code base -> toBeEnhanced})
	 * @param allProductAspects
	 *      それぞれのプロダクトクラスに対するメソッドアスペクトの一覧
	 *      ({@code base -> aspect for each method})
	 * @return 制約を満たせば{@code true}
	 */
	private static boolean testAllWeaveTargetWillBeEnhanced(Map<CtClass, CtClass> productsToBeEnhanced,
			Map<CtClass, AspectList<CtMethod>> allProductAspects) {
		for (CtClass c : allProductAspects.keySet()) {
			if (productsToBeEnhanced.containsKey(c) == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * {@link CtClass}を安全に取り扱うマップを作成して返す。
	 * @param <V> マップの値が持つ型
	 * @return 作成したマップ
	 */
	private static <V>Map<CtClass, V> newMap() {
		return new TreeMap<CtClass, V>(CtClassComparator.INSTANCE);
	}
}
