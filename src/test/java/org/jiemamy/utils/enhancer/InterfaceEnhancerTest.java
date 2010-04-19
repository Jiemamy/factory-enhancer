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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;

import org.junit.Ignore;
import org.junit.Test;

import org.jiemamy.utils.enhancer.aspect.StringParameterPointcut;
import org.jiemamy.utils.enhancer.aspect.StringResultPointcut;
import org.jiemamy.utils.enhancer.aspect.ThroughHandler;

/**
 * Test for {@link InterfaceEnhancer}.
 * @version $Id: InterfaceEnhancerTest.java 3739 2009-10-09 14:08:04Z ashigeru $
 * @author Suguru ARAKAWA
 */
public class InterfaceEnhancerTest {
	
	/**
	 * Test method for {@link AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_NoEnhance() throws Exception {
		// 何もエンハンスしない
		InterfaceEnhancer<SimpleInterfaceFactory> enhancer = new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				Object.class,
				enhances());
		Factory<? extends SimpleInterfaceFactory> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactory factory = metaFactory.newInstance();
		
		// getMessage()が実装されないので、AbstractMethodErrorになる
		InterfaceProduct product = factory.newProduct();
		try {
			product.getMessage();
			fail();
		} catch (AbstractMethodError e) {
			// ok.
		}
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_EnhanceMethod() throws Exception {
		// Stringを返すメソッドをフックして、かわりに "Hello" を返す。
		Enhance enhance = new Enhance(new StringResultPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				return "Hello";
			}
		});
		InterfaceEnhancer<SimpleInterfaceFactory> enhancer = new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				Object.class,
				enhances(enhance));
		Factory<? extends SimpleInterfaceFactory> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactory factory = metaFactory.newInstance();
		InterfaceProduct product = factory.newProduct();
		
		// インターフェースメソッドだったgetMessage()が実装される
		assertThat(product.getMessage(), is("Hello"));
		
		// ついでにStringを返すtoString()が上書きされる
		assertThat(product.toString(), is("Hello"));
	}
	
	/**
	 * Test method for {@link AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_Inherit() throws Exception {
		// 何もエンハンスしないが、かわりに getMessage() を実装する親クラス ConcreteProduct を指定してやる
		InterfaceEnhancer<SimpleInterfaceFactory> enhancer = new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				ConcreteProduct.class,
				enhances());
		Factory<? extends SimpleInterfaceFactory> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactory factory = metaFactory.newInstance();
		
		// InterfaceProduct.getMessage() が ConcreteProduct のメソッドに束縛される
		InterfaceProduct product = factory.newProduct();
		assertThat(product.getMessage(), is("Concrete"));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_EnhanceNew() throws Exception {
		// InterfaceProduct を生成するファクトリインターフェースに対して行う
		Class<SimpleInterfaceFactory> factoryInterface = SimpleInterfaceFactory.class;
		
		// 全てのプロダクトにPropertiesを継承させる
		Class<?> productSuperClass = Properties.class;
		
		Enhance enhance = new Enhance(new InvocationPointcut() {
			
			public boolean isTarget(CtClass self, CtBehavior behavior) {
				// InterfaceProduct に対するコンストラクタ呼び出しだけを対象にする
				if ((behavior instanceof CtConstructor) == false) {
					return false;
				}
				// InterfaceProductと同じ名前？
				return self.getName().equals(InterfaceProduct.class.getName());
			}
		}, new InvocationHandler() {
			
			public Object handle(Invocation invocation) throws Throwable {
				// InterfaceProductの実装を生成するときに呼び出される
				try {
					// とりあえずプロダクトのインスタンスを作る
					Object result = invocation.proceed();
					
					// プロダクトの親クラスはProperties
					assertThat(result, instanceOf(Properties.class));
					Properties p = (Properties) result;
					
					// Hello -> world! を追加して初期化
					p.put("Hello", "world!");
					
					return p;
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
		});
		InterfaceEnhancer<SimpleInterfaceFactory> enhancer = new InterfaceEnhancer<SimpleInterfaceFactory>(
				factoryInterface,
				productSuperClass,
				enhances(enhance));
		Factory<? extends SimpleInterfaceFactory> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactory factory = metaFactory.newInstance();
		InterfaceProduct product = factory.newProduct();
		
		// プロダクトの親クラスはProperties
		assertThat(product, instanceOf(Properties.class));
		
		// プロダクトのequalsはオーバーライドしてないうえ、Mapの規約で中身が同じなら一致する
		Map<String, String> map = new HashMap<String, String>();
		map.put("Hello", "world!");
		assertThat(product, is((Object) map));
	}
	
	/**
	 * Test method for {@link AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_ParameterConflict() throws Exception {
		Enhance enhance = new Enhance(new StringResultPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				return "Hello";
			}
		});
		InterfaceEnhancer<ParameterConflictInterfaceFactory> enhancer =
				new InterfaceEnhancer<ParameterConflictInterfaceFactory>(
				ParameterConflictInterfaceFactory.class,
				Object.class,
				enhances(enhance));
		Factory<? extends ParameterConflictInterfaceFactory> metaFactory = enhancer.getFactory();
		ParameterConflictInterfaceFactory factory = metaFactory.newInstance();
		
		InterfaceProduct p1 = factory.new1(1);
		InterfaceProduct p2 = factory.new1(2);
		assertThat(p1.getMessage(), is("Hello"));
		assertThat(p2.getMessage(), is("Hello"));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_Through() throws Exception {
		Enhance enhance = new Enhance(new StringResultPointcut(), new ThroughHandler());
		InterfaceEnhancer<SimpleInterfaceFactory> enhancer = new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				ConcreteProduct.class,
				enhances(enhance));
		Factory<? extends SimpleInterfaceFactory> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactory factory = metaFactory.newInstance();
		InterfaceProduct product = factory.newProduct();
		assertThat(product.getMessage(), is("Concrete"));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_Inherited() throws Exception {
		Enhance enhance = new Enhance(new StringResultPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				if (invocation.getInvoker() instanceof InterfaceProductEx) {
					return "inherited";
				}
				if (invocation.getInvoker() instanceof InterfaceProduct) {
					return "base";
				}
				throw new AssertionError();
			}
		});
		InterfaceEnhancer<SimpleInterfaceFactoryEx> enhancer = new InterfaceEnhancer<SimpleInterfaceFactoryEx>(
				SimpleInterfaceFactoryEx.class,
				Object.class,
				enhances(enhance));
		Factory<? extends SimpleInterfaceFactoryEx> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactoryEx factory = metaFactory.newInstance();
		
		InterfaceProduct product = factory.newProduct();
		assertThat(product.getMessage(), is("base"));
		
		InterfaceProductEx productEx = factory.newProductEx();
		assertThat(productEx.getMessage(), is("inherited"));
		assertThat(productEx.getMessageEx(), is("inherited"));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_OverrideWithReturnSubType() throws Exception {
		Enhance enhance = new Enhance(new StringResultPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				if (invocation.getInvoker() instanceof InterfaceProductEx) {
					return "inherited";
				}
				if (invocation.getInvoker() instanceof InterfaceProduct) {
					return "base";
				}
				throw new AssertionError();
			}
		});
		InterfaceEnhancer<SimpleInterfaceFactoryOverride> enhancer =
				new InterfaceEnhancer<SimpleInterfaceFactoryOverride>(
				SimpleInterfaceFactoryOverride.class,
				Object.class,
				enhances(enhance));
		Factory<? extends SimpleInterfaceFactoryOverride> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactoryOverride factory = metaFactory.newInstance();
		
		InterfaceProductEx productEx = factory.newProduct();
		assertThat(productEx.getMessage(), is("inherited"));
		assertThat(productEx.getMessageEx(), is("inherited"));
		
		InterfaceProduct product = productEx;
		assertThat(product.getMessage(), is("inherited"));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_OverrideConflictReturnSubType() throws Exception {
		Enhance enhance = new Enhance(new StringResultPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				if (invocation.getInvoker() instanceof InterfaceProductEx) {
					return "inherited";
				}
				if (invocation.getInvoker() instanceof InterfaceProduct) {
					return "base";
				}
				throw new AssertionError();
			}
		});
		InterfaceEnhancer<SimpleInterfaceFactoryConflict> enhancer =
				new InterfaceEnhancer<SimpleInterfaceFactoryConflict>(
				SimpleInterfaceFactoryConflict.class,
				Object.class,
				enhances(enhance));
		Factory<? extends SimpleInterfaceFactoryConflict> metaFactory = enhancer.getFactory();
		SimpleInterfaceFactoryConflict factory = metaFactory.newInstance();
		
		InterfaceProductEx productEx = factory.newProduct();
		assertThat(productEx.getMessage(), is("inherited"));
		assertThat(productEx.getMessageEx(), is("inherited"));
		
		InterfaceProduct product = ((SimpleInterfaceFactory) factory).newProduct();
		assertThat(product.getMessage(), is("inherited"));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * @throws Exception if occur
	 */
	@Test
	public void testGetFactory_ProductConflictReturnSubType() throws Exception {
		Enhance enhance = new Enhance(new StringResultPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				return "Hello";
			}
		});
		InterfaceEnhancer<InterfaceFactoryProductConflict> enhancer =
				new InterfaceEnhancer<InterfaceFactoryProductConflict>(
				InterfaceFactoryProductConflict.class,
				Object.class,
				enhances(enhance));
		Factory<? extends InterfaceFactoryProductConflict> metaFactory = enhancer.getFactory();
		InterfaceFactoryProductConflict factory = metaFactory.newInstance();
		
		InterfaceProductString productString = factory.newProduct();
		InterfaceProductCharSequence productCharSequence = productString;
		assertThat(productString.getMessage(), is("Hello"));
		assertThat(productCharSequence.getMessage(), is((CharSequence) "Hello"));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * TODO 仮引数型に型引数を直接とる(パラメータ化型ではなく、型変数をそのまま使う)インターフェースを継承し、
	 * そのメソッドをオーバーライドしなかった場合にブリッジメソッドを作るかどうか。
	 * @throws Exception if occur
	 */
	@Ignore
	@Test
	public void testGetFactory_ProductTypeVariableReificationImplicit() throws Exception {
		Enhance enhance = new Enhance(new StringParameterPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				return false;
			}
		});
		InterfaceEnhancer<InterfaceListFactory> enhancer =
				new InterfaceEnhancer<InterfaceListFactory>(
				InterfaceListFactory.class,
				Object.class,
				enhances(enhance));
		Factory<? extends InterfaceListFactory> metaFactory = enhancer.getFactory();
		InterfaceListFactory factory = metaFactory.newInstance();
		
		ExtendsListImplicit product = factory.newImplicit();
		assertThat(product.add("Hello"), is(false));
		
		List<String> list = factory.newImplicit();
		assertThat(list.add("Hello"), is(false));
	}
	
	/**
	 * Test method for {@link org.jiemamy.utils.enhancer.AbstractEnhancer#getFactory()}.
	 * TODO 仮引数型に型引数を直接とる(パラメータ化型ではなく、型変数をそのまま使う)インターフェースを継承し、
	 * そのメソッドをオーバーライドした場合にブリッジメソッドを作る。
	 * @throws Exception if occur
	 */
	@Ignore
	@Test
	public void testGetFactory_ProductTypeVariableReificationExplicit() throws Exception {
		Enhance enhance = new Enhance(new StringParameterPointcut(), new InvocationHandler() {
			
			public Object handle(Invocation invocation) {
				return true;
			}
		});
		InterfaceEnhancer<InterfaceListFactory> enhancer =
				new InterfaceEnhancer<InterfaceListFactory>(
				InterfaceListFactory.class,
				Object.class,
				enhances(enhance));
		Factory<? extends InterfaceListFactory> metaFactory = enhancer.getFactory();
		InterfaceListFactory factory = metaFactory.newInstance();
		
		ExtendsListExplicit product = factory.newExplicit();
		assertThat(product.add("Hello"), is(true));
		
		List<String> list = product;
		assertThat(list.add("Hello"), is(true));
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_FactoryNotPublic() {
		new InterfaceEnhancer<InterfaceNotPublic>(
				InterfaceNotPublic.class,
				Object.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_FactoryNotInterface() {
		new InterfaceEnhancer<Object>(
				Object.class,
				Object.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_FactoryNotReturnsInterface() {
		new InterfaceEnhancer<InterfaceReturnsNotInterface>(
				InterfaceReturnsNotInterface.class,
				Object.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_FactoryNotReturnsPublic() {
		new InterfaceEnhancer<InterfaceReturnsNotPublic>(
				InterfaceReturnsNotPublic.class,
				Object.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_ProductNotPublic() {
		new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				ClassNotPublic.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_ProductNotConcrete() {
		new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				EmptyFactoryAbstract.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_ProductNotInheritable() {
		new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				EmptyFactoryFinal.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_ProductNotClass() {
		new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				SimpleInterfaceFactory.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_ProductNotNormalClass() {
		new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				EmptyFactoryEnum.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_ProductDefaultConstructor() {
		new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				ClassNoDefaultConstructor.class,
				enhances());
	}
	
	/**
	 * Test method for {@link InterfaceEnhancer#InterfaceEnhancer(java.lang.Class, java.lang.Class, java.util.List)}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInterfaceEnhancer_ProductConstructorNotPublic() {
		new InterfaceEnhancer<SimpleInterfaceFactory>(
				SimpleInterfaceFactory.class,
				ClassNoPublicConstructor.class,
				enhances());
	}
	
	private static Enhance[] enhances(Enhance... enhances) {
		return enhances;
	}
}
