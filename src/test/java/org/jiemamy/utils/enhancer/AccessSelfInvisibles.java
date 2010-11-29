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

/**
 * 自身のメンバを様々な可視性で参照するテスト。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class AccessSelfInvisibles implements SingularFactory {
	
	/** private field. */
	private String privateField = "";
	
	/** package access field. */
	String packageField = "";
	
	/** protected field. */
	protected String protectedField = "";
	
	/** public field. */
	public String publicField = "";
	

	/**
	 * public constructor
	 */
	public AccessSelfInvisibles() {
		super();
	}
	
	/**
	 * protected constructor
	 * @param _ dummy parameter
	 */
	protected AccessSelfInvisibles(boolean _) {
		super();
	}
	
	/**
	 * package constructor
	 * @param _ dummy parameter
	 */
	AccessSelfInvisibles(char _) {
		super();
	}
	
	/**
	 * private constructor
	 * @param _ dummy parameter
	 */
	private AccessSelfInvisibles(byte _) {
		super();
	}
	
	/**
	 * Test visibility.
	 */
	@SuppressWarnings("unused")
	public Object newInstance() {
		new AccessSelfInvisibles(false);
		new AccessSelfInvisibles('a');
		new AccessSelfInvisibles((byte) 1);
		AccessSelfInvisibles instance = new AccessSelfInvisibles();
		instance.privateField.toString();
		instance.packageField.toString();
		instance.protectedField.toString();
		instance.publicField.toString();
		instance.privateMethod();
		instance.packageMethod();
		instance.protectedMethod();
		instance.publicMethod();
		return null;
	}
	
	/** public method */
	public void publicMethod() {
		return;
	}
	
	/** protected method */
	protected void protectedMethod() {
		return;
	}
	
	/** package access method */
	void packageMethod() {
		return;
	}
	
	/** private method */
	private void privateMethod() {
		return;
	}
}
