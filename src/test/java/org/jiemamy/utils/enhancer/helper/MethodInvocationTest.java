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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.jiemamy.utils.enhancer.helper.MethodInvocation;

/**
 * Test for {@link MethodInvocation}.
 * @version $Date: 2009-09-21 02:27:46 +0900 (月, 21  9 2009) $
 * @author Suguru ARAKAWA
 */
public class MethodInvocationTest {
    
    private Method get;
    private Method add;
    private Method sub;
    private Method factory;
    
    /**
     * Initializes the test.
     * @throws Exception if some errors were occurred
     */
    @Before
    public void setUp() throws Exception {
        get = MethodTestTarget.class.getMethod("getValue");
        add = MethodTestTarget.class.getMethod("add", int.class, int.class);
        sub = MethodTestTarget.class.getMethod("sub", int.class, int.class);
        factory = MethodTestTarget.class.getMethod("valueOf", String.class);
    }

    /**
     * Test method for {@link MethodInvocation#MethodInvocation(java.lang.reflect.Method, java.lang.reflect.Method, java.lang.Object, java.lang.Object[])}.
     * @throws Exception if occur
     */
    @Test
    public void testMethodInvocation() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        MethodInvocation inv = new MethodInvocation(add, add, obj, of(1, 2));
        assertThat(inv.proceed(), is((Object) 3));
    }

    /**
     * Test method for {@link MethodInvocation#MethodInvocation(java.lang.reflect.Method, java.lang.reflect.Method, java.lang.Object, java.lang.Object[])}.
     * @throws Exception if occur
     */
    @Test(expected = NullPointerException.class)
    public void testMethodInvocation_InstanceNull() throws Exception {
        new MethodInvocation(factory, add, null, of(1, 2));
    }

    /**
     * Test method for {@link MethodInvocation#MethodInvocation(java.lang.reflect.Method, java.lang.reflect.Method, java.lang.Object, java.lang.Object[])}.
     * @throws Exception if occur
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMethodInvocation_StaticNotNull() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        new MethodInvocation(add, factory, obj, of(1, 2));
    }

    /**
     * Test method for throws Exception {@link MethodInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_Actual() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        MethodInvocation inv = new MethodInvocation(add, sub, obj, of(1, 2));
        assertThat(inv.proceed(), is((Object) (-1)));
    }

    /**
     * Test method for throws Exception {@link MethodInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_Instance() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        obj.setValue("testProceed_Instance");
        MethodInvocation inv = new MethodInvocation(get, get, obj, of());
        assertThat(inv.proceed(), is((Object) "testProceed_Instance"));
    }

    /**
     * Test method for throws Exception {@link MethodInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_Static() throws Exception {
        MethodInvocation inv = new MethodInvocation(
            factory, factory, null, of("static"));
        Object result = inv.proceed();
        assertThat(result, is(MethodTestTarget.class));
        assertThat(
            ((MethodTestTarget) result).getValue(),
            is("static"));
    }

    /**
     * Test method for throws Exception {@link MethodInvocation#getTarget()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetTarget() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        MethodInvocation inv = new MethodInvocation(add, sub, obj, of(1, 2));
        assertThat(inv.getTarget().getName(), is("add"));
    }

    /**
     * Test method for throws Exception {@link MethodInvocation#getInvoker()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetObject() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        obj.setValue("Hello");
        MethodInvocation inv = new MethodInvocation(add, add, obj, of(1, 2));
        assertThat(inv.getInvoker(), is(MethodTestTarget.class));
        assertThat(
            ((MethodTestTarget) inv.getInvoker()).getValue(),
            is("Hello"));
    }
    
    /**
     * Test method for throws Exception {@link MethodInvocation#getInvoker()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetObject_Null() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        obj.setValue("Hello");
        MethodInvocation inv = new MethodInvocation(
            factory, factory, null, of("Hello"));
        assertThat(inv.getInvoker(), is(nullValue()));
    }

    /**
     * Test method for throws Exception {@link MethodInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetArguments() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        MethodInvocation inv = new MethodInvocation(add, add, obj, of(1, 2));
        assertThat(inv.getArguments(), is(of(1, 2)));
    }
    
    /**
     * Test method for throws Exception {@link MethodInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetArguments_Rewrite() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        MethodInvocation inv = new MethodInvocation(sub, sub, obj, of(1, 2));
        inv.getArguments()[0] = 10;
        assertThat(inv.proceed(), is((Object) 8));
    }
    
    /**
     * Test method for throws Exception {@link MethodInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetArguments_IllegalRewrite() throws Exception {
        MethodTestTarget obj = new MethodTestTarget();
        MethodInvocation inv = new MethodInvocation(add, add, obj, of(1, 2));
        inv.getArguments()[0] = "1";
        inv.proceed();
    }
    
    private Object[] of(Object...values) {
        return values;
    }
}
