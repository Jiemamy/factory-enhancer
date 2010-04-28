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
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jiemamy.utils.enhancer.EnhanceException;

/**
 * ファクトリの実装に含まれるすべての参照を検証する。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class AccessibilityValidator extends ExprEditor {
	
	private static final Logger LOG = LoggerFactory.getLogger(AccessibilityValidator.class);
	
	private final CtClass thisClass;
	
	private final ConstPool constPool;
	
	private final ClassPool classPool;
	
	private final Set<String> yetVerifiedTypes;
	

	private AccessibilityValidator(CtClass klass) {
		super();
		assert klass != null;
		assert klass.isFrozen() == false;
		thisClass = klass;
		constPool = klass.getClassFile().getConstPool();
		classPool = klass.getClassPool();
		yetVerifiedTypes = new HashSet<String>();
	}
	
	/**
	 * 指定のクラスがファクトリとして可能な参照のみを持つことを検証する。
	 * @param klass 対象のクラス
	 * @throws EnhanceException 利用できない参照を持つ場合
	 * @throws NullPointerException 引数に{@code null}が指定された場合
	 * @throws IllegalArgumentException 引数が凍結されたクラスである場合
	 */
	public static void validate(CtClass klass) throws EnhanceException {
		if (klass == null) {
			throw new NullPointerException("klass"); //$NON-NLS-1$
		}
		if (klass.isFrozen()) {
			throw new IllegalArgumentException(MessageFormat.format("{0} is frozen", klass.getName()));
		}
		AccessibilityValidator validator = new AccessibilityValidator(klass);
		validator.verify();
	}
	
	/**
	 * エントリポイント。
	 * <p>
	 * このメソッドを起点にして、次のような処理が行われる。
	 * </p>
	 * <ul>
	 *   <li> {@link #verifyConstantPool()}の実行 </li>
	 *   <li> それぞれのフィールドに対して{@link #verifyField(CtField)}の実行</li>
	 *   <li> それぞれのコンストラクタに対して{@link #verifyConstructor(CtConstructor)}の実行</li>
	 *   <li> それぞれのメソッドに対して{@link #verifyMethod(CtMethod)}の実行</li>
	 * </ul>
	 * @throws EnhanceException 正当でない場合
	 */
	private void verify() throws EnhanceException {
		LOG.debug("Verifying accessibility: {}", thisClass.getName());
		
		verifyConstantPool();
		for (CtField field : thisClass.getFields()) {
			verifyField(field);
		}
		for (CtConstructor constructor : thisClass.getConstructors()) {
			verifyConstructor(constructor);
		}
		for (CtMethod method : thisClass.getMethods()) {
			verifyMethod(method);
		}
	}
	
	/**
	 * コンスタントプールに含まれるすべてのクラスおよびメンバの参照を検証する。
	 * <p>
	 * コンスタントプールには次のような情報が含まれる
	 * </p>
	 * <ul>
	 *   <li>
	 *   {@code CONSTANT_ClassInfo} -
	 *   現在のクラスが利用するほかのクラスの情報 ({@link #verifyType(String)})。
	 *   これは主に次を表現する。
	 *   <ul>
	 *     <li> 親クラスや親インターフェース </li>
	 *     <li> このクラスが参照するメンバを宣言した型 </li>
	 *     <li> クラスリテラルとして参照する型({@code >= Java 5}) </li>
	 *     <li> キャスト式、{@code instanceof}式で利用される型 </li>
	 *     <li> クラスインスタンス生成式、配列生成式で利用される型 </li>
	 *     <li> 利用する例外型 </li>
	 *     <li> 現在のクラスに含まれるインナークラス </li>
	 *     <li> 現在のクラスを含む(アウター)クラス </li>
	 *   </ul>
	 *   注意すべき点として、参照するメンバの宣言型はこれに含まれるものの、
	 *   参照するメソッドの戻り値型や、引数型などはここに含まれないという点である。
	 *   これらは、別の情報を元に検証を行う必要がある。
	 *   </li>
	 *   <li>
	 *   CONSTANT_FieldrefInfo - 現在のクラスが参照するフィールドの情報
	 *   ({@link #verifyFieldref(String, String, String)})
	 *   </li>
	 *   <li>
	 *   CONSTANT_MethodrefInfo - 現在のクラスが参照するメソッドの情報
	 *   ({@link #verifyMethodref(String, String, String)})
	 *   </li>
	 *   <li>
	 *   CONSTANT_InterfaceMethodrefInfo - 現在のクラスが参照するフィールドの情報
	 *   ({@link #verifyMethodref(String, String, String)})
	 *   </li>
	 *   <li>
	 *   CONSTANT_LongInfo, CONSTANT_DoubleInfo -
	 *   現在のクラスが利用する一部の{@code long}型の値、または{@code double}型の値。
	 *   この値は直接の検証の対象ではないが、コンスタンとプールのスロットを2個分占有するため
	 *   次の要素をスキップする処理が必要である。
	 *   </li>
	 * </ul>
	 * @throws EnhanceException 正当でない場合
	 */
	private void verifyConstantPool() throws EnhanceException {
		LOG.trace("Verifying constant pool: {}", thisClass.getName());
		ConstPool cp = constPool;
		for (int index = 1, size = cp.getSize(); index < size; index++) {
			switch (cp.getTag(index)) {
				case ConstPool.CONST_Class:
					verifyType(cp.getClassInfo(index));
					break;
				case ConstPool.CONST_Fieldref:
					verifyFieldref(cp.getFieldrefClassName(index), cp.getFieldrefName(index), cp.getFieldrefType(index));
					break;
				case ConstPool.CONST_Methodref:
					verifyMethodref(cp.getMethodrefClassName(index), cp.getMethodrefName(index), cp
						.getMethodrefType(index));
					break;
				case ConstPool.CONST_InterfaceMethodref:
					verifyMethodref(cp.getInterfaceMethodrefClassName(index), cp.getInterfaceMethodrefName(index), cp
						.getInterfaceMethodrefType(index));
					break;
				
				case ConstPool.CONST_Double:
				case ConstPool.CONST_Long:
					// Double, Longはコンスタントプールを2スロット占有するため、次をスキップ
					index++;
					break;
				
				default:
					// next..
			}
		}
	}
	
	/**
	 * 指定の名前を持つ型を検証する。
	 * @param name 対象の名前
	 * @return 検証結果の型
	 * @throws EnhanceException 正当でない場合
	 * @see #verifyType(CtClass)
	 */
	private CtClass verifyType(String name) throws EnhanceException {
		assert name != null;
		CtClass klass = loadType(name);
		verifyType(klass);
		return klass;
	}
	
	/**
	 * 指定の情報によって特定されるフィールドを検証する。
	 * @param declaring フィールドを宣言する型
	 * @param name フィールドの名前
	 * @param descriptor フィールドデスクリプタ
	 * @throws EnhanceException 正当でない場合
	 * @see #verifyField(CtField)
	 */
	private void verifyFieldref(String declaring, String name, String descriptor) throws EnhanceException {
		assert declaring != null;
		assert name != null;
		assert descriptor != null;
		CtField ref = loadField(declaring, name);
		assert ref.getSignature().equals(descriptor);
		verifyField(ref);
	}
	
	/**
	 * 指定の情報によって特定されるメソッドを検証する。
	 * <p>
	 * メソッドの名前に{@code <init>}を指定することによって、この呼び出しは
	 * 対象のクラスで宣言されるコンストラクタについての検証を行う。
	 * 同様に、メソッドの名前に{@code <clinit>}を指定した場合は、
	 * 対象のクラスに記述されたクラス初期化子についての検証を行う。
	 * </p>
	 * @param declaring メソッドを宣言する型
	 * @param name メソッドの名前
	 * @param descriptor メソッドデスクリプタ
	 * @throws EnhanceException 正当でない場合
	 * @see #loadMethod(String, String, String)
	 * @see #loadConstructor(String, String)
	 * @see #verifyMethod(CtMethod)
	 * @see #verifyConstructor(CtConstructor)
	 */
	private void verifyMethodref(String declaring, String name, String descriptor) throws EnhanceException {
		assert declaring != null;
		assert name != null;
		assert descriptor != null;
		if (name.equals(MethodInfo.nameClinit)) {
			// クラス初期化子は常に正当である
			// void <init>() {...} という形式なので、常に参照解決できる
		} else if (name.equals(MethodInfo.nameInit)) {
			CtConstructor ref = loadConstructor(declaring, descriptor);
			verifyConstructor(ref);
		} else {
			CtMethod ref = loadMethod(declaring, name, descriptor);
			verifyMethod(ref);
		}
	}
	
	/**
	 * 指定した型を現在のクラスから参照できることを検証する。
	 * <p>
	 * この呼び出しは、次のものを検証の対象とする。
	 * </p>
	 * <ul>
	 *   <li> 対象の型のアクセス修飾子 </li>
	 *   <li> (メンバ型のみ) 対象のメンバ型を宣言した型 </li>
	 * </ul>
	 * @param klass 検査対象の型
	 * @throws EnhanceException 正当でない場合
	 * @see #isAccessible(CtClass, CtClass)
	 */
	private void verifyType(CtClass klass) throws EnhanceException {
		assert klass != null;
		String className = klass.getName();
		if (yetVerifiedTypes.contains(className)) {
			return; // already verified
		}
		if (isAccessible(klass, thisClass) == false) {
			throw new EnhanceException(MessageFormat.format("Not visible type {0} from {1}", className, thisClass
				.getName()), null);
		}
		yetVerifiedTypes.add(className);
	}
	
	/**
	 * 引数に指定した型の一覧を、現在のクラスからすべて参照できることを検証する。
	 * <p>
	 * このメソッドは、引数の配列に含まれるすべての型を{@link #verifyType(CtClass)}によって
	 * 検証するのみである。
	 * </p>
	 * @param types 検査対象の型の一覧
	 * @throws EnhanceException いずれかの型が正当でない場合
	 * @see #verifyType(CtClass)
	 */
	private void verifyTypes(CtClass[] types) throws EnhanceException {
		assert types != null;
		for (CtClass t : types) {
			verifyType(t);
		}
	}
	
	/**
	 * 引数に指定したフィールドを、現在のクラスから参照できることを検証する。
	 * <p>
	 * この呼び出しは、次のものを検証の対象とする。
	 * </p>
	 * <ul>
	 *   <li> 対象のフィールドのアクセス修飾子 </li>
	 *   <li> 対象のフィールドを宣言したクラス </li>
	 *   <li> 対象のフィールドの型 </li>
	 * </ul>
	 * @param field 検査対象のフィールド宣言
	 * @throws EnhanceException 利用するいずれかの型が正当でない場合
	 * @see #isAccessible(javassist.CtMember, CtClass)
	 * @see #verifyType(CtClass)
	 */
	private void verifyField(CtField field) throws EnhanceException {
		assert field != null;
		try {
			verifyType(field.getDeclaringClass());
			verifyType(field.getType());
		} catch (NotFoundException e) {
			throw new EnhanceException("Type not found", e);
		}
		if (isAccessible(field, thisClass) == false) {
			throw new EnhanceException(MessageFormat.format("Not visible field {0}#{1} from {2}", field
				.getDeclaringClass().getName(), field.getName(), thisClass.getName()), null);
		}
	}
	
	/**
	 * 引数に指定したコンストラクタを、現在のクラスから参照できることを検証する。
	 * <p>
	 * この呼び出しは、次のものを検証の対象とする。
	 * </p>
	 * <ul>
	 *   <li> 対象のコンストラクタのアクセス修飾子 </li>
	 *   <li> 対象のコンストラクタを宣言したクラス </li>
	 *   <li> 対象のコンストラクタの引数型 </li>
	 *   <li> 対象のコンストラクタの例外型 </li>
	 * </ul>
	 * @param constructor 検査対象のコンストラクタ宣言
	 * @throws EnhanceException 利用するいずれかの型が正当でない場合
	 * @see #isAccessible(javassist.CtMember, CtClass)
	 * @see #verifyType(CtClass)
	 */
	private void verifyConstructor(CtConstructor constructor) throws EnhanceException {
		
		assert constructor != null;
		
		try {
			verifyType(constructor.getDeclaringClass());
			verifyTypes(constructor.getParameterTypes());
			verifyTypes(constructor.getExceptionTypes());
		} catch (NotFoundException e) {
			throw new EnhanceException("Type not found", e);
		}
		if (isAccessible(constructor, thisClass) == false) {
			throw new EnhanceException(MessageFormat.format("Not visible constructor {0}#{1}{2} from {3}", constructor
				.getDeclaringClass().getName(), constructor.getDeclaringClass().getSimpleName(), Descriptor
				.getParamDescriptor(constructor.getSignature()), thisClass.getName()), null);
		}
	}
	
	/**
	 * 引数に指定したメソッドを、現在のクラスから参照できることを検証する。
	 * <p>
	 * この呼び出しは、次のものを検証の対象とする。
	 * </p>
	 * <ul>
	 *   <li> 対象のメソッドのアクセス修飾子 </li>
	 *   <li> 対象のメソッドを宣言したクラス </li>
	 *   <li> 対象のメソッドの戻り値型 </li>
	 *   <li> 対象のメソッドの引数型 </li>
	 *   <li> 対象のメソッドの例外型 </li>
	 * </ul>
	 * @param method 検査対象のメソッド宣言
	 * @throws EnhanceException 利用するいずれかの型が正当でない場合
	 * @see #isAccessible(javassist.CtMember, CtClass)
	 * @see #verifyType(CtClass)
	 */
	private void verifyMethod(CtMethod method) throws EnhanceException {
		assert method != null;
		try {
			verifyType(method.getDeclaringClass());
			verifyType(method.getReturnType());
			verifyTypes(method.getParameterTypes());
			verifyTypes(method.getExceptionTypes());
		} catch (NotFoundException e) {
			throw new EnhanceException("Type not found", e);
		}
		if (isAccessible(method, thisClass) == false) {
			throw new EnhanceException(MessageFormat.format("Not visible method {0}#{1}{2} from {3}", method
				.getDeclaringClass().getName(), method.getName(), Descriptor.getParamDescriptor(method.getSignature()),
					thisClass.getName()), null);
		}
	}
	
	/**
	 * 指定の名前を持つ型を{@link CtClass}型のオブジェクトとしてロードする。
	 * @param name 対象の型の名前
	 * @return ロードしたオブジェクト
	 * @throws EnhanceException ロードに失敗した場合
	 */
	private CtClass loadType(String name) throws EnhanceException {
		assert name != null;
		LOG.trace("Loading type: {}", name);
		CtClass klass;
		try {
			klass = classPool.get(name);
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Type not found {0}", name), e);
		}
		return klass;
	}
	
	/**
	 * 指定の情報によって特定されるフィールドを{@link CtField}型のオブジェクトとしてロードする。
	 * @param declaring 対象のフィールドを宣言する型の名前
	 * @param name 対象のフィールドの名前
	 * @return ロードしたオブジェクト
	 * @throws EnhanceException ロードに失敗した場合
	 */
	private CtField loadField(String declaring, String name) throws EnhanceException {
		
		assert declaring != null;
		assert name != null;
		LOG.trace("Loading field: {}#{}", declaring, name);
		
		CtClass declaringType = verifyType(declaring);
		CtField ref;
		try {
			ref = declaringType.getDeclaredField(name);
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Field not found {0}#{1} from {2}", declaring, name,
					thisClass.getName()), e);
		}
		return ref;
	}
	
	/**
	 * 指定の情報によって特定されるコンストラクタを{@link CtConstructor}型のオブジェクトとしてロードする。
	 * @param declaring 対象のコンストラクタを宣言する型の名前
	 * @param descriptor 対象のコンストラクタの引数および戻り値を表現するメソッドデスクリプタ
	 * @return ロードしたオブジェクト
	 * @throws EnhanceException ロードに失敗した場合
	 */
	private CtConstructor loadConstructor(String declaring, String descriptor) throws EnhanceException {
		
		assert declaring != null;
		assert descriptor != null;
		LOG.trace("Loading constructor: {}{}", declaring, descriptor);
		
		CtClass declaringType = verifyType(declaring);
		CtConstructor ref;
		try {
			ref = declaringType.getConstructor(descriptor);
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Method not found {0}#{1}{2} from {3}", declaringType
				.getName(), declaringType.getSimpleName(), Descriptor.getParamDescriptor(descriptor), thisClass
				.getName()), e);
		}
		return ref;
	}
	
	/**
	 * 指定の情報によって特定されるメソッドを{@link CtMethod}型のオブジェクトとしてロードする。
	 * @param declaring 対象のメソッドを宣言する型、またはそのサブタイプの名前
	 * @param name 対象のメソッドの名前
	 * @param descriptor 対象のメソッドの引数および戻り値を表現するメソッドデスクリプタ
	 * @return ロードしたオブジェクト
	 * @throws EnhanceException ロードに失敗した場合
	 */
	private CtMethod loadMethod(String declaring, String name, String descriptor) throws EnhanceException {
		
		assert declaring != null;
		assert name != null;
		assert descriptor != null;
		assert name.equals(MethodInfo.nameClinit) == false;
		assert name.equals(MethodInfo.nameInit) == false;
		LOG.trace("Loading method: {}#{}", name, name + descriptor);
		
		CtClass declaringType = verifyType(declaring);
		CtMethod ref;
		try {
			ref = declaringType.getMethod(name, descriptor);
			// ref.getDeclaringClass() can != declaringType
		} catch (NotFoundException e) {
			throw new EnhanceException(MessageFormat.format("Method not found {0}#{1}{2} from {3}", declaring, name,
					Descriptor.getParamDescriptor(descriptor), thisClass.getName()), e);
		}
		return ref;
	}
	
	/**
	 * 指定の型が参照可能である場合のみ{@code true}を返す。
	 * <p>
	 * 参照可能である型は、次の条件を満たす型である。
	 * </p>
	 * <ul>
	 *   <li> プリミティブ型、または{@code void}型である </li>
	 *   <li> 配列型であり、その要素型が参照可能である </li>
	 *   <li> 宣言型であり、次の条件のいずれかを満たす <ul>
	 *     <li> 参照元と同一の型である </li>
	 *     <li> {@code protected}で宣言され、かつ参照元のスーパータイプである </li>
	 *     <li> {@code public}で宣言される </li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 * <p>
	 * ただし、対象の型がいずれかの型の内部型である場合、
	 * その型を宣言する型についても再帰的に同様の検証が行われる。
	 * </p>
	 * <p>
	 * なお、参照元と同一のパッケージに属する宣言型が
	 * パッケージプライベートや{@code protected}で宣言されている場合であっても、
	 * このメソッドは参照可能であるとみなさない。
	 * </p>
	 * @param target 対象の型
	 * @param from 参照元のクラス
	 * @return 対象の型が参照可能である場合に{@code true}、そうでない場合は{@code false}
	 */
	private static boolean isAccessible(CtClass target, CtClass from) {
		assert target != null;
		assert from != null;
		try {
			if (target.isPrimitive()) {
				return true;
			} else if (target.isArray()) {
				CtClass component = target.getComponentType();
				while (component.isArray()) {
					component = component.getComponentType();
				}
				return isAccessible(component, from);
			}
			CtClass klass = target;
			while (klass != null) {
				int modifiers = klass.getModifiers();
				if (isSame(klass, from)) {
					// ok.
				} else if (Modifier.isProtected(modifiers) && from.subclassOf(klass)) {
					// ok.
				} else if (Modifier.isPublic(modifiers)) {
					// ok.
				} else {
					return false;
				}
				klass = klass.getDeclaringClass();
			}
		} catch (NotFoundException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * 指定のメンバが参照可能である場合のみ{@code true}を返す。
	 * <p>
	 * 参照可能であるメンバは、次の条件を満たすメンバである。
	 * </p>
	 * <ul>
	 *   <li> 参照元の型で宣言される </li>
	 *   <li> {@code protected}で宣言され、かつ参照元のスーパータイプで宣言される </li>
	 *   <li> {@code public}で宣言される </li>
	 * </ul>
	 * <p>
	 * なお、参照元と同一のパッケージに属する宣言型が
	 * パッケージプライベートや{@code protected}で宣言されている場合であっても、
	 * このメソッドは参照可能であるとみなさない。
	 * </p>
	 * <p>
	 * また、このメソッドでは対象のメンバを宣言した型の参照可能性については検証を行わない。
	 * </p>
	 * @param target 対象のメンバ
	 * @param from 参照元のクラス
	 * @return メンバが参照可能である場合に{@code true}、そうでない場合は{@code false}
	 */
	private static boolean isAccessible(CtMember target, CtClass from) {
		assert target != null;
		assert from != null;
		CtClass declaring = target.getDeclaringClass();
		int modifiers = target.getModifiers();
		if (Modifier.isPublic(modifiers)) {
			return true;
		}
		if (isSame(from, declaring)) {
			return true;
		} else if (Modifier.isProtected(modifiers) && from.subclassOf(declaring)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 2つの型が同一である場合にのみ{@code true}を返す。
	 * @param a 比較される型
	 * @param b 比較する型
	 * @return 2つの型が同一である場合に{@code true}、そうでない場合は{@code false}
	 */
	private static boolean isSame(CtClass a, CtClass b) {
		assert a != null;
		assert b != null;
		if (a == b) {
			return true;
		}
		return b.getName().equals(a.getName());
	}
}
