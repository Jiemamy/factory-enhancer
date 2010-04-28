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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.Enhance;
import org.jiemamy.utils.enhancer.InvocationHandler;

/**
 * {@link Enhance}を管理する。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class EnhanceManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(EnhanceManager.class);
	
	/**
	 * ブリッジメソッドを表現するビットマスク。
	 */
	private static final int BRIDGE_METHOD = 0x00000040;
	
	/**
	 * コンパイラによって合成されたメソッドを表現するビットマスク。
	 */
	private static final int SYNTHETIC_METHOD = 0x00001000;
	
	private final List<Enhance> enhanceList;
	

	/**
	 * インスタンスを生成する。
	 * @param enhanceList 保持する拡張の一覧
	 * @throws NullPointerException
	 *      引数に{@code null}が指定された場合、または
	 *      引数のリストに{@code null}が含まれる場合
	 */
	public EnhanceManager(List<? extends Enhance> enhanceList) {
		super();
		if (enhanceList == null) {
			throw new NullPointerException("enhanceList contains null"); //$NON-NLS-1$
		}
		if (enhanceList.contains(null)) {
			throw new NullPointerException("enhanceList contains null"); //$NON-NLS-1$
		}
		this.enhanceList = new ArrayList<Enhance>(enhanceList);
	}
	
	/**
	 * 指定のメソッドまたはコンストラクタに対して、このエンハンサに登録された拡張から
	 * 適用可能なハンドラの一覧を検出して返す。
	 * <p>
	 * 返されるハンドラの一覧は、かならずエンハンサに登録された拡張と同じ順序を保持する。
	 * ただし、拡張のポイントカット定義に該当しないメソッドやコンストラクタが指定された場合
	 * その拡張のハンドラは戻り値のリストに含まれない。
	 * </p>
	 * <p>
	 * 適用可能なハンドラが一つも存在しない場合、この呼び出しは空のリストを返す。
	 * </p>
	 * @param self このメソッドまたはコンストラクタを公開するクラス
	 * @param behavior 対象のメソッドまたはコンストラクタ
	 * @return 適用可能なハンドラの一覧
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public List<InvocationHandler> findApplicableHandlers(CtClass self, CtBehavior behavior) {
		if (self == null) {
			throw new NullPointerException("self"); //$NON-NLS-1$
		}
		if (behavior == null) {
			throw new NullPointerException("behavior"); //$NON-NLS-1$
		}
		
		LOG.trace("Detecting applicable handlers: {}#{}", self.getName(), behavior.getName() + behavior.getSignature());
		
		List<InvocationHandler> results = new ArrayList<InvocationHandler>();
		for (Enhance e : enhanceList) {
			if (e.getPointcut().isTarget(self, behavior)) {
				results.add(e.getHandler());
			}
		}
		return results;
	}
	
	/**
	 * 指定の型が公開するメソッドのいずれかが、拡張を適用可能である場合のみ{@code true}を返す。
	 * <p>
	 * このメソッドでは、実際に対象のメソッドが拡張されるかどうかは気にせず、
	 * 対象のクラスが公開するいずれかのメソッドが、いずれかのポイントカット定義に合致することだけを検査する。
	 * すべてのメソッドがいずれのポイントカット定義にも合致しない場合、この呼び出しは{@code null}を返す。
	 * </p>
	 * @param klass 検査対象のクラス
	 * @return
	 *      拡張対象であるメソッドを公開する場合に{@code true}、
	 *      そうでない場合は{@code false}
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public boolean hasApplicableMethod(CtClass klass) {
		if (klass == null) {
			throw new NullPointerException("klass"); //$NON-NLS-1$
		}
		for (CtMethod method : klass.getMethods()) {
			for (Enhance enhance : enhanceList) {
				if (isLegalJoinpoint(method) && enhance.getPointcut().isTarget(klass, method)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 指定のコンストラクタを保持するクラスが、拡張の対象として適切である場合のみ
	 * {@code true}を返す。
	 * @param target 対象のコンストラクタ
	 * @return 拡張対象として適切である場合に{@code true}、そうでない場合は{@code false}
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public boolean canEnhanceProduct(CtConstructor target) {
		if (target == null) {
			throw new NullPointerException("target"); //$NON-NLS-1$
		}
		int constructorModifiers = target.getModifiers();
		int classModifiers = target.getDeclaringClass().getModifiers();
		return Modifier.isPublic(constructorModifiers) && Modifier.isPublic(classModifiers)
				&& Modifier.isFinal(classModifiers) == false;
	}
	
	/**
	 * 指定のコンストラクタを利用したインスタンス生成式が、ジョインポイントとして適切である場合のみ
	 * {@code true}を返す。
	 * <p>
	 * ジョインポイントとして適切であるインスタンス生成式は、アドバイスを適用する対象にとることができる。
	 * そうでない場合はアドバイス適用の対象にとることができず、この呼び出しは{@code false}を返す。
	 * </p>
	 * @param target 対象のコンストラクタ
	 * @return ジョインポイントとして適切である場合に{@code true}、そうでない場合は{@code false}
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public boolean isLegalJoinpoint(CtConstructor target) {
		if (target == null) {
			throw new NullPointerException("target"); //$NON-NLS-1$
		}
		int constructorModifiers = target.getModifiers();
		int classModifiers = target.getDeclaringClass().getModifiers();
		return Modifier.isPublic(constructorModifiers) && Modifier.isPublic(classModifiers);
	}
	
	/**
	 * 指定のメソッドをが持つメソッド本体が、ジョインポイントとして適切である場合のみ{@code true}を返す。
	 * <p>
	 * ジョインポイントとして適切であるメソッド本体は、アドバイスを適用する対象にとることができる。
	 * そうでない場合はアドバイス適用の対象にとることができず、この呼び出しは{@code false}を返す。
	 * </p>
	 * @param target 対象のメソッド
	 * @return ジョインポイントとして適切である場合に{@code true}、そうでない場合は{@code false}
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 */
	public boolean isLegalJoinpoint(CtMethod target) {
		if (target == null) {
			throw new NullPointerException("target"); //$NON-NLS-1$
		}
		int methodModifiers = target.getModifiers();
		int classModifiers = target.getDeclaringClass().getModifiers();
		return Modifier.isPublic(methodModifiers) && Modifier.isStatic(methodModifiers) == false
				&& Modifier.isFinal(methodModifiers) == false && is(methodModifiers, BRIDGE_METHOD) == false
				&& is(methodModifiers, SYNTHETIC_METHOD) == false && Modifier.isPublic(classModifiers)
				&& Modifier.isFinal(classModifiers) == false;
	}
	
	/**
	 * 指定されたフラグの一覧が指定のマスクパターンを有する場合のみ{@code true}を返す。
	 * @param flags 対象のフラグ一覧
	 * @param mask 検査するマスクパターン
	 * @return マスクパターンをすべて有する場合に{@code true}、そうでない場合は{@code false}
	 */
	private static boolean is(int flags, int mask) {
		return (flags & mask) == mask;
	}
}
