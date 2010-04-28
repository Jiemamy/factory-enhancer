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
package org.jiemamy.utils.enhancer.aspect;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

import org.jiemamy.utils.enhancer.InvocationPointcut;

/**
 * {@code ...(T[])}を対象とする。
 * @version $Date$
 * @author Suguru ARAKAWA (Gluegent, Inc.)
 */
public class ArrayParameterPointcut implements InvocationPointcut {
	
	/**
	 * @return true if target is {@code *.*(T[])}
	 */
	public boolean isTarget(CtClass self, CtBehavior behavior) {
		try {
			CtClass[] types = behavior.getParameterTypes();
			return types.length == 1 && types[0].isArray() && types[0].getComponentType().isPrimitive() == false;
		} catch (NotFoundException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * @return {@code *.*(T[])}
	 */
	@Override
	public String toString() {
		return "*.*(T[])";
	}
}
