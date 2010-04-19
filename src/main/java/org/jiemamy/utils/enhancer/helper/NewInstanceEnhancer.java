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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.EnhanceException;
import org.jiemamy.utils.enhancer.InvocationHandler;

/**
 * ファクトリに含まれるインスタンス生成式を拡張する。
 * @version 0.2.0
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class NewInstanceEnhancer extends ExprEditor {
	
	private static final Logger LOG = LoggerFactory.getLogger(NewInstanceEnhancer.class);
	

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
	 * @param target アスペクトを埋め込む先のファクトリクラス
	 * @param enhanceManager 拡張の定義
	 * @param productsToBeEnhanced
	 *      拡張されるべきプロダクトクラスの一覧 ({@code base -> toBeEnhanced})
	 * @param allProductAspects
	 *      それぞれのプロダクトクラスに対するメソッドアスペクトの一覧
	 *      ({@code base -> aspect for each method})
	 * @return
	 *      ファクトリに実際に埋め込まれるべきアスペクトの一覧、ひとつも存在しない場合は{@code null}
	 * @throws EnhanceException 拡張に失敗した場合
	 */
	public static AspectList<CtConstructor> enhance(
			CtClass target,
			EnhanceManager enhanceManager,
			Map<? extends CtClass, ? extends CtClass> productsToBeEnhanced,
			Map<? extends CtClass, AspectList<CtMethod>> allProductAspects) throws EnhanceException {
		
		if (target == null) {
			throw new NullPointerException("target"); //$NON-NLS-1$
		}
		if (enhanceManager == null) {
			throw new NullPointerException("enhanceManager"); //$NON-NLS-1$
		}
		if (productsToBeEnhanced == null) {
			throw new NullPointerException("productsToBeEnhanced"); //$NON-NLS-1$
		}
		if (allProductAspects == null) {
			throw new NullPointerException("allProductAspects"); //$NON-NLS-1$
		}
		
		try {
			NewInstanceEnhancer enhancer =
					new NewInstanceEnhancer(target, enhanceManager, productsToBeEnhanced, allProductAspects);
			target.instrument(enhancer);
			if (enhancer.aspects.isEmpty()) {
				assert enhancer.holder == null;
				return null;
			} else {
				assert enhancer.holder != null;
				return new AspectList<CtConstructor>(enhancer.holder, enhancer.aspects);
			}
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat.format("Cannot compile {0}", target.getName()), e);
		}
	}
	

	private final CtClass target;
	
	private final EnhanceManager enhanceManager;
	
	private final Map<? extends CtClass, ? extends CtClass> productEnhanceMap;
	
	private final Map<CtClass, CtClass> reverseProductEnhanceMap;
	
	private final Map<? extends CtClass, AspectList<CtMethod>> allProductAspects;
	
	private final List<Aspect<CtConstructor>> aspects;
	
	private int enhanceIndex;
	
	private CtField holder;
	

	private NewInstanceEnhancer(CtClass target, EnhanceManager enhanceManager,
			Map<? extends CtClass, ? extends CtClass> productsToBeEnhanced,
			Map<? extends CtClass, AspectList<CtMethod>> allProductAspects) {
		super();
		assert target != null;
		assert enhanceManager != null;
		assert productsToBeEnhanced != null;
		assert allProductAspects != null;
		this.target = target;
		this.enhanceManager = enhanceManager;
		productEnhanceMap = productsToBeEnhanced;
		reverseProductEnhanceMap = reverse(productsToBeEnhanced);
		this.allProductAspects = allProductAspects;
		aspects = new ArrayList<Aspect<CtConstructor>>();
		enhanceIndex = 0;
		holder = null;
	}
	
	/**
	 * マップのキーと値を転置した新しいマップを返す。
	 * @param <K> キーの型
	 * @param <V> 値の型
	 * @param map 転置するマップ
	 * @return 転置後のマップ、値が重複していた場合の処理は保証しない
	 */
	private <K, V>Map<V, K> reverse(Map<? extends K, ? extends V> map) {
		assert map != null;
		Map<V, K> results = new HashMap<V, K>();
		for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
			results.put(entry.getValue(), entry.getKey());
		}
		return results;
	}
	
	/**
	 * それぞれのインスタンス生成式を必要ならば書き換える。
	 */
	@Override
	public void edit(NewExpr expr) throws CannotCompileException {
		LOG.trace("Pointcut candidate: new {}", expr.getClassName());
		try {
			CtConstructor constructor = expr.getConstructor();
			if (enhanceManager.isLegalJoinpoint(constructor) == false) {
				return;
			}
			CtClass provider = computeProvider(constructor);
			List<InvocationHandler> handlers = enhanceManager.findApplicableHandlers(provider, constructor);
			
			// 書き換えても挙動がまったく変化しないため、何も行わないで終了
			if (handlers.isEmpty() && productEnhanceMap.containsKey(provider) == false
					&& allProductAspects.containsKey(provider) == false) {
				return;
			}
			
			// 対象クラスに対する最初の拡張であれば、アドバイステーブルを作成
			if (enhanceIndex == 0) {
				holder = EnhanceManipulator.createAdviceTableField(target);
			}
			
			LOG.debug("Rewrite new[{}]: {} (at {}{}:{})", new Object[] {
				enhanceIndex,
				provider.getName(),
				expr.where().getName(),
				expr.where().getSignature(),
				expr.getLineNumber()
			});
			
			// new Hoge(...)の部分を、アドバイスの実行に置き換える
			EnhanceManipulator.replaceToPointcut(expr, provider, holder, enhanceIndex);
			
			if (productEnhanceMap.containsKey(provider)) {
				// newする対象のクラス自体がエンハンスされて別クラスになっている場合、
				// 呼び出し先のコンストラクタはエンハンス後のクラス上で定義されたものに変更
				CtClass actual = productEnhanceMap.get(provider);
				String descriptor = constructor.getSignature();
				CtConstructor actualCtor = actual.getConstructor(descriptor);
				aspects.add(new Aspect<CtConstructor>(constructor, actualCtor, handlers));
			} else {
				// newする対象自体が同一である場合、呼び出し先のコンストラクタは変更なし
				assert allProductAspects.containsKey(provider) == false;
				aspects.add(new Aspect<CtConstructor>(constructor, constructor, handlers));
			}
			
			enhanceIndex++;
		} catch (EnhanceException e) {
			throw new CannotCompileException(e);
		} catch (NotFoundException e) {
			throw new CannotCompileException(e);
		}
	}
	
	/**
	 * このコンストラクタの本来の提供者を返す。
	 * <p>
	 * 不明の場合、コンストラクタを宣言したクラスをそのまま返す。
	 * </p>
	 * @param constructor 対象のコンストラクタ
	 * @return このコンストラクタの本来の提供者
	 */
	private CtClass computeProvider(CtConstructor constructor) {
		assert constructor != null;
		CtClass declaring = constructor.getDeclaringClass();
		if (productEnhanceMap.containsKey(declaring)) {
			return declaring;
		}
		CtClass original = reverseProductEnhanceMap.get(declaring);
		if (original != null) {
			return original;
		}
		return declaring;
	}
}
