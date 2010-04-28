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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.jiemamy.utils.enhancer.Pointcuts.and;
import static org.jiemamy.utils.enhancer.Pointcuts.or;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.jiemamy.utils.enhancer.aspect.AfterIntIncrementHandler;
import org.jiemamy.utils.enhancer.aspect.AfterStringAppendHandler;
import org.jiemamy.utils.enhancer.aspect.BeforeStringInsertHandler;
import org.jiemamy.utils.enhancer.aspect.ClassSuffixPointcut;
import org.jiemamy.utils.enhancer.aspect.IntResultPointcut;
import org.jiemamy.utils.enhancer.aspect.StringParameterPointcut;
import org.jiemamy.utils.enhancer.aspect.StringResultPointcut;
import org.jiemamy.utils.enhancer.aspect.ThroughHandler;

/**
 * Test for {@link FactoryEnhancer}.
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class FactoryEnhancerTest {
	
	private static List<Enhance> enhances(Enhance... enhances) {
		return Arrays.asList(enhances);
	}
	
	private static Object p1(String value) {
		return new TargetProduct1(value);
	}
	
	private static Object p2(String value) {
		return new TargetProduct2(value);
	}
	
	private static Object p3(String value) {
		return new TargetProduct3(value);
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言される具象クラスでなければならない。
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_ImplementationIsAbstract() throws Exception {
		new FactoryEnhancer<EmptyFactory>(EmptyFactory.class, EmptyFactoryAbstract.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言される具象クラスでなければならない。
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_ImplementationIsEnum() throws Exception {
		new FactoryEnhancer<EmptyFactory>(EmptyFactory.class, EmptyFactoryEnum.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_ImplementationIsInterface() throws Exception {
		new FactoryEnhancer<EmptyFactory>(EmptyFactory.class, EmptyFactoryInterface.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_ImplementationIsNoExtended() throws Exception {
		new FactoryEnhancer<EmptyFactory>(EmptyFactory.class, EmptyFactoryExtended.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言される具象クラスでなければならない。
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_ImplementationIsPackagePrivate() throws Exception {
		new FactoryEnhancer<EmptyFactory>(EmptyFactory.class, EmptyFactoryPackagePrivate.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_InterfaceIsAnnotation() throws Exception {
		Override override = new Override() {
			
			public Class<? extends Annotation> annotationType() {
				return Override.class;
			}
		};
		new FactoryEnhancer<Override>(Override.class, override.getClass(), enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_InterfaceIsClass() throws Exception {
		new FactoryEnhancer<EmptyFactoryClass>(EmptyFactoryClass.class, EmptyFactoryClass.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#FactoryEnhancer(Class, Class, List)}.
	 * 拡張する対象のファクトリクラスは、明示的な親クラスを持つことができない
	 * @throws Exception if occur
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testFactoryEnhancer_NoExplicitSupertype() throws Exception {
		new FactoryEnhancer<EmptyFactory>(EmptyFactory.class, EmptyFactoryExtended.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * 拡張する対象のプロダクトクラスは、 publicの公開性を持たなければならない。
	 * @throws Exception if occur
	 */
	@Test(expected = EnhanceException.class)
	public void testGetEnhanced_AccessToNoPublicProductClass() throws Exception {
		Enhance enhance = new Enhance(new StringParameterPointcut(), new BeforeStringInsertHandler("!"));
		FactoryEnhancer<SingularFactory> enhancer =
				new FactoryEnhancer<SingularFactory>(SingularFactory.class, SingularFactoryNoPublicProduct.class,
				enhance);
		SingularFactory factory = enhancer.getFactory().newInstance();
		factory.newInstance();
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * @throws Exception if occur
	 */
	@Test(expected = EnhanceException.class)
	public void testGetEnhanced_AccessToPackageProductConstructor() throws Exception {
		invalidAccessibility(AccessPackageConstructor.class);
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * @throws Exception if occur
	 */
	@Test(expected = EnhanceException.class)
	public void testGetEnhanced_AccessToPackageProductField() throws Exception {
		invalidAccessibility(AccessPackageField.class);
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * @throws Exception if occur
	 */
	@Test(expected = EnhanceException.class)
	public void testGetEnhanced_AccessToPackageProductMethod() throws Exception {
		invalidAccessibility(AccessPackageMethod.class);
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * @throws Exception if occur
	 */
	@Test(expected = EnhanceException.class)
	public void testGetEnhanced_AccessToProtectedProductConstructor() throws Exception {
		invalidAccessibility(AccessProtectedConstructor.class);
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * @throws Exception if occur
	 */
	@Test(expected = EnhanceException.class)
	public void testGetEnhanced_AccessToProtectedProductField() throws Exception {
		invalidAccessibility(AccessProtectedField.class);
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * @throws Exception if occur
	 */
	@Test(expected = EnhanceException.class)
	public void testGetEnhanced_AccessToProtectedProductMethod() throws Exception {
		invalidAccessibility(AccessProtectedMethod.class);
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_AllConstructor() throws Exception {
		Enhance enhance = new Enhance(new StringParameterPointcut(), new BeforeStringInsertHandler("!"));
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, enhance);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		assertThat(factory.newProduct1("a"), is(p1("!a")));
		assertThat(factory.newProduct2("b"), is(p2("!b")));
		assertThat(factory.newProduct3("c"), is(p3("!c")));
		assertThat(factory.newString("s"), is("!s"));
		assertThat(factory.newProduct1("a").getValue(), is("!a1"));
		assertThat(factory.newProduct2("b").getValue(), is("!b2"));
		assertThat(factory.newProduct3("c").getValue(), is("!c3"));
		assertThat(factory.newString("s"), is("!s"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_Arrays() throws Exception {
		Enhance enhance = new Enhance(Pointcuts.TRUE, new ThroughHandler());
		FactoryEnhancer<ArrayFactory> enhancer =
				new FactoryEnhancer<ArrayFactory>(ArrayFactory.class, ArrayFactoryImpl.class, enhance);
		enhancer.getEnhanced(); // ok.
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * プロダクトクラスに含まれる拡張する対象のメソッドは、...
	 * さらに、コンパイラによって合成された特殊なメソッド(ブリッジメソッド等)は拡張対象とならない。
	 * @throws Exception if occur
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetEnhanced_BridgeProductMethod() throws Exception {
		Enhance enhance = new Enhance(new IntResultPointcut(), new AfterIntIncrementHandler());
		FactoryEnhancer<SingularFactory> enhancer =
				new FactoryEnhancer<SingularFactory>(SingularFactory.class, HasBridgeFactory.class, enhance);
		SingularFactory factory = enhancer.getEnhanced().newInstance();
		HasBridge bridge = (HasBridge) factory.newInstance();
		assertThat(bridge.compareTo(bridge), is(1));
		
		// bareComparable.compareTo
		// --> HasBridge.compareTo(Object)    -- Compiler Synthetic
		//   --> HasBridge.compareTo(Bridge)  -- User Declared
		Comparable bareComparable = bridge;
		assertThat(bareComparable.compareTo(bridge), is(1));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張されたファクトリクラスは元のクラスと同一でなく、またサブタイプ関係にもない。
	 * ただし、元のクラスが宣言するインターフェースは、 拡張されたファクトリクラスにおいてもすべて実装されている。
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_CheckInherit() throws Exception {
		FactoryEnhancer<SingularFactory> enhancer =
				new FactoryEnhancer<SingularFactory>(SingularFactory.class, PluralFactoryImpl.class, enhances());
		Class<?> enhanced = enhancer.getEnhanced();
		
		assertThat(enhanced, is(not((Object) PluralFactoryImpl.class)));
		assertThat(enhanced.isAssignableFrom(PluralFactoryImpl.class), is(false));
		assertThat(PluralFactoryImpl.class.isAssignableFrom(enhanced), is(false));
		
		assertThat(EmptyFactory.class.isAssignableFrom(enhanced), is(true));
		assertThat(SingularFactory.class.isAssignableFrom(enhanced), is(true));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_Empty() throws Exception {
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, enhances());
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		assertThat(factory.newProduct1("a"), is(p1("a")));
		assertThat(factory.newProduct2("b"), is(p2("b")));
		assertThat(factory.newProduct3("c"), is(p3("c")));
		assertThat(factory.newString("s"), is("s"));
		assertThat(factory.newProduct1("a").getValue(), is("a1"));
		assertThat(factory.newProduct2("b").getValue(), is("b2"));
		assertThat(factory.newProduct3("c").getValue(), is("c3"));
		assertThat(factory.newString("s"), is("s"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_FinalFactory() throws Exception {
		FactoryEnhancer<EmptyFactory> enhancer =
				new FactoryEnhancer<EmptyFactory>(EmptyFactory.class, EmptyFactoryFinal.class, enhances());
		enhancer.getEnhanced(); // ok.
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のプロダクトクラスは、 publicの公開性を持たなければならない。
	 * また、<b>finalで宣言されていてはならない</b>。
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_FinalProductClass() throws Exception {
		Enhance enhance =
				new Enhance(and(or(new ClassSuffixPointcut("Product1"), new ClassSuffixPointcut("ProductFinal")),
				new StringParameterPointcut()), new BeforeStringInsertHandler("!"));
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, enhance);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		
		TargetProduct1 p1 = factory.newProduct1("a");
		assertThat(p1.getValue(), is("!a1"));
		p1.setValue("a");
		assertThat(p1.getValue(), is("!a1"));
		
		TargetProductFinal pf = factory.newProductFinal("a");
		assertThat(pf.getValue(), is("!a"));
		pf.setValue("a");
		assertThat(pf.getValue(), is("a"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * プロダクトクラスに含まれる拡張する対象のメソッドは、...
	 * また、finalおよびstaticで宣言されていてはならない。
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_FinalProductMethod() throws Exception {
		TargetFactory factory = enhancedProduct1();
		TargetProduct1 p1 = factory.newProduct1("a");
		assertThat(p1.getValue(), is("a1!"));
		assertThat(p1.getFinal(), is("a1"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、必ず何らかのインターフェースを実装しなければならない
	 * @throws Exception if occur
	 */
	@SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
	public void testGetEnhanced_HasInterface() throws Exception {
		new FactoryEnhancer(EmptyFactory.class, IndependentFactoryImpl.class, enhances());
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_InheritedProductMethod() throws Exception {
		TargetFactory factory = enhancedProduct1();
		TargetProduct1 p1 = factory.newProduct1("a");
		assertThat(p1.getValue(), is("a1!"));
		assertThat(p1.getBase(), is("base!"));
		assertThat(p1.getBaseOverride(), is("a1!"));
		assertThat(p1.getBaseFinal(), is("base"));
		assertThat(p1.getBaseProtected(), is("base"));
		assertThat(p1.getBasePackage(), is("base"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * プロダクトクラスに含まれる拡張する対象のメソッドは、publicの公開性を持たなければならない。
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_PackageProductMethod() throws Exception {
		TargetFactory factory = enhancedProduct1();
		TargetProduct1 p1 = factory.newProduct1("a");
		assertThat(p1.getValue(), is("a1!"));
		assertThat(p1.getPackage(), is("a1"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_PassInvoker() throws Exception {
		final AtomicReference<Object> holder = new AtomicReference<Object>();
		Enhance enhance = new Enhance(new ClassSuffixPointcut("Product1"), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				holder.set(invocation.getInvoker());
				try {
					return invocation.proceed();
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}
		});
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, enhance);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		
		assertThat(holder.get(), is(nullValue()));
		TargetProduct1 p1 = factory.newProduct1();
		assertThat(holder.get(), is((Object) factory));
		assertThat(p1.getValue(), is("1"));
		assertThat(holder.get(), is((Object) p1));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_PassInvokerStatic() throws Exception {
		final AtomicReference<Object> holder = new AtomicReference<Object>();
		Enhance enhance = new Enhance(new ClassSuffixPointcut("Product1"), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				holder.set(invocation.getInvoker());
				try {
					return invocation.proceed();
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}
		});
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, enhance);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		
		assertThat(holder.get(), is(nullValue()));
		TargetProduct1 p1 = (TargetProduct1) enhanced.getMethod("newProduct1Static").invoke(null);
		assertThat(holder.get(), is((Object) enhanced));
		assertThat(p1.getValue(), is("1"));
		assertThat(holder.get(), is((Object) p1));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_ProductMethodArguments() throws Exception {
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, new Enhance(
				new StringParameterPointcut(), new BeforeStringInsertHandler("!")));
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		TargetProduct1 p1 = factory.newProduct1();
		TargetProduct2 p2 = factory.newProduct2();
		TargetProduct3 p3 = factory.newProduct3();
		p1.setValue("a");
		p2.setValue("b");
		p3.setValue("c");
		assertThat(p1.getValue(), is("!a1"));
		assertThat(p2.getValue(), is("!b2"));
		assertThat(p3.getValue(), is("!c3"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_ProductMethodResults() throws Exception {
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, new Enhance(
				new StringResultPointcut(), new AfterStringAppendHandler("!")));
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		assertThat(factory.newProduct1("a"), is(p1("a")));
		assertThat(factory.newProduct2("b"), is(p2("b")));
		assertThat(factory.newProduct3("c"), is(p3("c")));
		assertThat(factory.newString("s"), is("s"));
		assertThat(factory.newProduct1("a").getValue(), is("a1!"));
		assertThat(factory.newProduct2("b").getValue(), is("b2!"));
		assertThat(factory.newProduct3("c").getValue(), is("c3!"));
		
		// java.lang.String cannot become an enhance target
		assertThat(factory.newString("s"), is("s"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * プロダクトクラスに含まれる拡張する対象のメソッドは、publicの公開性を持たなければならない。
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_ProtectedProductMethod() throws Exception {
		TargetFactory factory = enhancedProduct1();
		TargetProduct1 p1 = factory.newProduct1("a");
		assertThat(p1.getValue(), is("a1!"));
		assertThat(p1.getProtected(), is("a1"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_SelectedMethod() throws Exception {
		Enhance before =
				new Enhance(and(new ClassSuffixPointcut("Product2"), new StringParameterPointcut()),
				new BeforeStringInsertHandler("!"));
		Enhance after =
				new Enhance(and(new ClassSuffixPointcut("Product3"), new StringResultPointcut()),
				new AfterStringAppendHandler("!"));
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, before, after);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		TargetProduct1 p1 = factory.newProduct1();
		TargetProduct2 p2 = factory.newProduct2();
		TargetProduct3 p3 = factory.newProduct3();
		p1.setValue("a");
		p2.setValue("b");
		p3.setValue("c");
		assertThat(p1.getValue(), is("a1"));
		assertThat(p2.getValue(), is("!b2"));
		assertThat(p3.getValue(), is("c3!"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * 拡張する対象のファクトリクラスは、 publicで宣言された型やメンバのみを参照できる。
	 * <b>ただし、自身が宣言するメンバに関してはこの限りではない。</b>
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_SelfVisibility() throws Exception {
		Enhance enhance = new Enhance(new StringParameterPointcut(), new BeforeStringInsertHandler("!"));
		FactoryEnhancer<SingularFactory> enhancer =
				new FactoryEnhancer<SingularFactory>(SingularFactory.class, AccessSelfInvisibles.class, enhance);
		SingularFactory factory = enhancer.getEnhanced().newInstance();
		factory.newInstance();
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_Series() throws Exception {
		InvocationPointcut pointcut = and(new ClassSuffixPointcut("Product1"), new StringParameterPointcut());
		Enhance a = new Enhance(pointcut, new BeforeStringInsertHandler("a"));
		Enhance b = new Enhance(pointcut, new BeforeStringInsertHandler("b"));
		Enhance c = new Enhance(pointcut, new BeforeStringInsertHandler("c"));
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, a, b, c);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		TargetProduct1 p1 = factory.newProduct1();
		TargetProduct2 p2 = factory.newProduct2();
		TargetProduct3 p3 = factory.newProduct3();
		p1.setValue("a");
		p2.setValue("b");
		p3.setValue("c");
		assertThat(p1.getValue(), is("abca1"));
		assertThat(p2.getValue(), is("b2"));
		assertThat(p3.getValue(), is("c3"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_SingleConstructor() throws Exception {
		Enhance enhance =
				new Enhance(and(new ClassSuffixPointcut("Product1"), new StringParameterPointcut()),
				new BeforeStringInsertHandler("!"));
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, enhance);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		factory.newProduct1();
		assertThat(factory.newProduct1("a"), is(p1("!a")));
		assertThat(factory.newProduct2("b"), is(p2("b")));
		assertThat(factory.newProduct3("c"), is(p3("c")));
		assertThat(factory.newString("s"), is("s"));
		assertThat(factory.newProduct1("a").getValue(), is("!a1"));
		assertThat(factory.newProduct2("b").getValue(), is("b2"));
		assertThat(factory.newProduct3("c").getValue(), is("c3"));
		assertThat(factory.newString("s"), is("s"));
	}
	
	/**
	 * Test method for {@link FactoryEnhancer#getEnhanced()}.
	 * プロダクトクラスに含まれる拡張する対象のメソッドは、...
	 * また、finalおよびstaticで宣言されていてはならない。
	 * @throws Exception if occur
	 */
	@Test
	public void testGetEnhanced_StaticProductMethod() throws Exception {
		TargetFactory factory = enhancedProduct1();
		TargetProduct1 p1 = factory.newProduct1("a");
		assertThat(p1.getValue(), is("a1!"));
		assertThat(p1.getClass().getMethod("getStatic").invoke(null), is((Object) "1"));
	}
	
	private TargetFactory enhancedProduct1() throws Exception {
		Enhance enhance =
				new Enhance(and(new ClassSuffixPointcut("Product1"), new StringResultPointcut()),
				new AfterStringAppendHandler("!"));
		FactoryEnhancer<TargetFactory> enhancer =
				new FactoryEnhancer<TargetFactory>(TargetFactory.class, TargetFactoryImpl.class, enhance);
		Class<? extends TargetFactory> enhanced = enhancer.getEnhanced();
		TargetFactory factory = enhanced.newInstance();
		return factory;
	}
	
	private void invalidAccessibility(Class<? extends SingularFactory> impl) throws Exception {
		Enhance enhance = new Enhance(new StringParameterPointcut(), new BeforeStringInsertHandler("!"));
		FactoryEnhancer<SingularFactory> enhancer =
				new FactoryEnhancer<SingularFactory>(SingularFactory.class, impl, enhance);
		Class<? extends SingularFactory> enhanced = enhancer.getEnhanced();
		enhanced.newInstance().newInstance();
		fail("Expected EnhanceException");
	}
}
