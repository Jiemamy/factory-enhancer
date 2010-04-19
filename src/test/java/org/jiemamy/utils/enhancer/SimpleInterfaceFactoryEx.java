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

/**
 * 単純なインターフェースファクトリ。
 * @version $Id: SimpleInterfaceFactoryEx.java 3734 2009-10-08 13:05:37Z ashigeru $
 * @author Suguru ARAKAWA
 */
public interface SimpleInterfaceFactoryEx extends SimpleInterfaceFactory {
	
	/**
	 * @return {@link InterfaceProductEx}
	 */
	InterfaceProductEx newProductEx();
}
