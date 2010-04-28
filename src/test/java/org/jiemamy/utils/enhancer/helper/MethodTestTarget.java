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

import java.io.IOException;

/**
 * Test target for {@link MethodInvocationTest}.
 * @version $Date$
 * @author Suguru ARAKAWA
 */
@SuppressWarnings("all")
public class MethodTestTarget {
    
    private String value;
    
    public MethodTestTarget() {
        this.value = "init";
    }
    
    public MethodTestTarget(String value) {
        super();
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public int add(int a, int b) {
        return a + b;
    }
    
    public int sub(int a, int b) {
        return a - b;
    }
    
    public String ident(String value) {
        return value;
    }
    
    public void raise() throws IOException {
        throw new IOException();
    }

    private String privateIdent(String value) {
        return value;
    }
    
    public static MethodTestTarget valueOf(String s) {
        MethodTestTarget target = new MethodTestTarget();
        target.setValue(s);
        return target;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MethodTestTarget other = (MethodTestTarget) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        }
        else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
}
