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


/**
 * Test target for {@link MethodInvocationTest}.
 * @version $Date$
 * @author Suguru ARAKAWA
 */
@SuppressWarnings("all")
public class OrthogonalMethodTestTarget {
    
    public static MethodTestTarget valueOf(String s) {
        MethodTestTarget target = new MethodTestTarget();
        target.setValue("Orthogonal" + s);
        return target;
    }
}
