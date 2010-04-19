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
 * コンストラクタのテストに利用する。
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA
 */
@SuppressWarnings("all")
public class ConstructorTestTarget {
    
    private String value;
    
    public ConstructorTestTarget() {
        super();
    }
    
    public ConstructorTestTarget(Object _) {
        super();
    }
    
    public ConstructorTestTarget(String value) {
        super();
        this.value = value;
    }
    
    private ConstructorTestTarget(int a, int b) {
        super();
        this.value = String.format("%d + %d", a, b);
    }

    public <T extends Exception> ConstructorTestTarget(Class<T> exc) throws T {
        try {
            throw exc.newInstance();
        }
        catch (InstantiationException e) {
            throw new AssertionError(e);
        }
        catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
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
        ConstructorTestTarget other = (ConstructorTestTarget) obj;
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
