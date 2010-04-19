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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.EnhanceException;

/**
 * 拡張対象のプロダクトを検出する。
 * @version $Date: 2009-10-09 12:59:48 +0900 (金, 09 10 2009) $
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class EnhanceTargetProductCollector extends ExprEditor {
	
	private static final Logger LOG = LoggerFactory.getLogger(EnhanceTargetProductCollector.class);
	

	/**
	 * 指定のファクトリクラスの定義を分析し、拡張するプロダクトの一覧を抽出して返す。
	 * @param target 対象のファクトリクラス
	 * @param enhanceManager 利用する拡張の一覧
	 * @return 検出した拡張対象のプロダクト一覧
	 * @throws EnhanceException 指定のクラスからプロダクトを抽出するのに失敗した場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public static List<CtClass> collect(CtClass target, EnhanceManager enhanceManager) throws EnhanceException {
		if (target == null) {
			throw new NullPointerException("target"); //$NON-NLS-1$
		}
		if (enhanceManager == null) {
			throw new NullPointerException("enhanceManager"); //$NON-NLS-1$
		}
		LOG.trace("Collecting products: {}", target.getName());
		try {
			EnhanceTargetProductCollector inspector = new EnhanceTargetProductCollector(enhanceManager);
			target.instrument(inspector);
			return inspector.results;
		} catch (CannotCompileException e) {
			throw new EnhanceException(MessageFormat.format(
					"Cannot collect target products from {0}",
					target.getName()),
					e);
		}
	}
	

	private EnhanceManager enhanceManager;
	
	private final List<CtClass> results;
	
	private Set<String> saw;
	

	/**
	 * インスタンスを生成する。
	 * @param enhanceManager 利用する拡張の一覧
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	private EnhanceTargetProductCollector(EnhanceManager enhanceManager) {
		super();
		if (enhanceManager == null) {
			throw new NullPointerException("enhanceManager"); //$NON-NLS-1$
		}
		this.enhanceManager = enhanceManager;
		results = new ArrayList<CtClass>();
		saw = new HashSet<String>();
	}
	
	/**
	 * 対象クラスに含まれるインスタンス生成式についての検査を行う。
	 */
	@Override
	public void edit(NewExpr expr) throws CannotCompileException {
		LOG.trace("Inspecting new: {}", expr.getClassName());
		CtClass declaringClass;
		try {
			CtConstructor constructor = expr.getConstructor();
			declaringClass = constructor.getDeclaringClass();
			if (declaringClass != expr.getEnclosingClass()) {
				declaringClass.freeze();
			}
			if (enhanceManager.canEnhanceProduct(constructor) == false) {
				return;
			}
			if (saw.contains(declaringClass.getName())) {
				return;
			}
			saw.add(declaringClass.getName());
			if (enhanceManager.hasApplicableMethod(declaringClass)) {
				LOG.debug("Product found: {}", declaringClass.getName());
				declaringClass.freeze();
				results.add(declaringClass);
			}
		} catch (NotFoundException e) {
			throw new CannotCompileException(e);
		}
	}
}
