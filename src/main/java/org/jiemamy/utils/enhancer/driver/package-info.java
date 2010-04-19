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

/**
 * 外部が提供する機構を利用して、ファクトリの拡張方法を記述するためのドライバ群。
 * <p>
 * なお、{@link org.jiemamy.utils.enhancer.driver.AopAllianceDriver}を利用する際には、
 * AOP Allianceが提供するそれぞれのインターフェースが必要となる。
 * </p>
 * @see org.jiemamy.utils.enhancer.Enhance
 * @see org.jiemamy.utils.enhancer.InvocationPointcut
 * @see org.jiemamy.utils.enhancer.InvocationHandler
 * @see <a href="http://aopalliance.sourceforge.net/">AOP Alliance</a>
 */
package org.jiemamy.utils.enhancer.driver;

