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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Before;
import org.junit.Test;

import org.jiemamy.utils.enhancer.helper.ConstructorInvocation;

/**
 * Test for {@link ConstructorInvocation}.
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA
 */
public class ConstructorInvocationTest {
    
    private Constructor<?> stringEmpty;
    private Constructor<?> builderEmpty;
    private Constructor<?> stringString;
    private Constructor<?> errorException;
    
    /**
     * Initializes the test.
     * @throws Exception if some errors were occurred
     */
    @Before
    public void setUp() throws Exception {
        stringEmpty = String.class.getConstructor();
        stringString = String.class.getConstructor(String.class);
        builderEmpty = StringBuilder.class.getConstructor();
        errorException = ConstructorTestTarget.class.getConstructor(Class.class);
    }

   /**
     * Test method for {@link ConstructorInvocation#ConstructorInvocation(Constructor, Constructor, Object, Object[])}.
     * @throws Exception if occur
     */
    @Test
    public void testConstructorInvocation() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringEmpty,
            stringEmpty,
            ConstructorTestTarget.class,
            new Object[0]);
        Object result = inv.proceed();
        assertThat(result, is(String.class));
        assertThat((String) result, equalTo(""));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_Simple() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringEmpty,
            stringEmpty,
            new Object(),
            new Object[] {});

        assertThat(inv.proceed(), is(String.class));
        assertThat((String) inv.proceed(), is(""));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_WithArgs() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringString,
            stringString,
            new Object(),
            new Object[] {"Hello"});

        assertThat(inv.proceed(), is(String.class));
        assertThat((String) inv.proceed(), is("Hello"));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_Delegate() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringEmpty,
            builderEmpty,
            new Object(),
            new Object[] {});

        assertThat(inv.proceed(), is(StringBuilder.class));
        assertThat(inv.proceed().toString(), is(""));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test(expected = IllegalArgumentException.class)
    public void testProceed_InvalidArgs() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringEmpty,
            stringEmpty,
            new Object(),
            new Object[] {"extra"});
        inv.proceed();
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_Exception() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            errorException,
            errorException,
            new Object(),
            new Object[] {IOException.class});

        try {
            inv.proceed();
            fail();
        }
        catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(IOException.class));
        }
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test(expected = IllegalArgumentException.class)
    public void testProceed_RewriteInvalid() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringString,
            stringString,
            new Object(),
            new Object[] { "Hello" });
        inv.getArguments()[0] = 1;
        inv.proceed();
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#getTarget()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetTarget() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            builderEmpty,
            stringEmpty,
            new Object(),
            new Object[] {});
        assertThat(
            inv.getTarget().getDeclaringClass(),
            equalTo((Object) StringBuilder.class));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#getInvoker()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetObject() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringEmpty,
            builderEmpty,
            "",
            new Object[] {});

        assertThat(inv.getInvoker(), is((Object) ""));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetArguments() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringEmpty,
            stringEmpty,
            new Object(),
            new Object[] {});
        
        assertThat(
            inv.getArguments(),
            is(new Object[] {}));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetArguments_Single() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringString,
            stringString,
            new Object(),
            new Object[] { "Hello" });
        
        assertThat(
            inv.getArguments(),
            is(new Object[] {"Hello"}));
    }

    /**
     * Test method for throws Exception {@link ConstructorInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetArguments_Rewrite() throws Exception {
        ConstructorInvocation inv = new ConstructorInvocation(
            stringString,
            stringString,
            new Object(),
            new Object[] { "Hello" });
        
        inv.getArguments()[0] = "world";
        assertThat(
            inv.getArguments(),
            is(new Object[] {"world"}));
        assertThat(inv.proceed(), is((Object) "world"));
    }
}
