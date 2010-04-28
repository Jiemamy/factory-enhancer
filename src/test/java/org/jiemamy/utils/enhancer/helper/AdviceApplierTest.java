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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jiemamy.utils.enhancer.EnhanceException;
import org.jiemamy.utils.enhancer.Invocation;
import org.jiemamy.utils.enhancer.InvocationHandler;
import org.jiemamy.utils.enhancer.aspect.AfterIntIncrementHandler;
import org.jiemamy.utils.enhancer.aspect.AfterStringAppendHandler;
import org.jiemamy.utils.enhancer.aspect.BeforeStringInsertHandler;
import org.jiemamy.utils.enhancer.helper.AdviceApplier;

import org.junit.Test;

/**
 * Test for {@link AdviceApplier}.
 * @version $Date$
 * @author Suguru ARAKAWA
 */
public class AdviceApplierTest {
    
    private Class<?>[] INT_INT = new Class<?>[] {int.class, int.class};
    private Class<?>[] STRING = new Class<?>[] {String.class};
    private Class<?>[] CLASS = new Class<?>[] {Class.class};
    private Class<?>[] OBJECT = new Class<?>[] {Object.class};

    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testMethod_Direct() throws Throwable {
        AdviceApplier helper = AdviceApplier.method(
            handlers(),
            MethodTestTarget.class, "add",
            MethodTestTarget.class, "add",
            INT_INT);
        MethodTestTarget target = new MethodTestTarget();
        Object result = helper.invoke(target, of(1, 2));
        assertThat(result, is((Object) 3));
    }

    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testMethod_OrthogonalOriginal() throws Throwable {
        AdviceApplier helper = AdviceApplier.method(
            handlers(),
            OrthogonalMethodTestTarget.class, "valueOf",
            MethodTestTarget.class, "valueOf",
            STRING);
        Object result = helper.invoke(null, of("Hello"));
        assertThat(result, is((Object) new MethodTestTarget("Hello")));
    }

    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testMethod_OrthogonalActual() throws Throwable {
        AdviceApplier helper = AdviceApplier.method(
            handlers(),
            MethodTestTarget.class, "valueOf",
            OrthogonalMethodTestTarget.class, "valueOf",
            STRING);
        Object result = helper.invoke(null, of("Hello"));
        assertThat(result, is((Object) new MethodTestTarget("OrthogonalHello")));
    }

    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testMethod_SingleHandler() throws Throwable {
        AdviceApplier helper = AdviceApplier.method(
            handlers(new AfterIntIncrementHandler()),
            MethodTestTarget.class, "add",
            MethodTestTarget.class, "add",
            INT_INT);
        MethodTestTarget target = new MethodTestTarget();
        Object result = helper.invoke(target, of(1, 2));
        assertThat(result, is((Object) 4));
    }

    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testMethod_Handlers() throws Throwable {
        AdviceApplier helper = AdviceApplier.method(
            handlers(
                new BeforeStringInsertHandler("a"),
                new BeforeStringInsertHandler("b"),
                new AfterStringAppendHandler("d"),
                new AfterStringAppendHandler("e")),
            MethodTestTarget.class, "ident",
            MethodTestTarget.class, "ident",
            STRING);
        MethodTestTarget target = new MethodTestTarget();
        Object result = helper.invoke(target, of("c"));
        assertThat(result, is((Object) "abcde"));
    }

    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testMethod_Capture() throws Throwable {
        final AtomicReference<Invocation> h = new AtomicReference<Invocation>();
        AdviceApplier helper = AdviceApplier.method(
            handlers(new InvocationHandler() {
                public Object handle(Invocation invocation) throws Throwable {
                    h.set(invocation);
                    return invocation.proceed();
                }
            }),
            MethodTestTarget.class, "ident",
            MethodTestTarget.class, "ident",
            STRING);
        MethodTestTarget target = new MethodTestTarget();
        Object result = helper.invoke(target, of("a"));
        assertThat(result, is((Object) "a"));
        assertThat(h.get(), is(not(nullValue())));
        Invocation inv = h.get();
        assertThat(inv.getInvoker(), is((Object) target));
        assertThat(inv.getArguments(), is(of("a")));
        assertThat(inv.getTarget().getName(), is("ident"));
    }

    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testMethod_MissingOriginal() throws Throwable {
        AdviceApplier.method(
            handlers(),
            MethodTestTarget.class, "MISSING",
            MethodTestTarget.class, "ident",
            STRING);
    }
    
    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testMethod_MissingActual() throws Throwable {
        AdviceApplier.method(
            handlers(),
            MethodTestTarget.class, "ident",
            MethodTestTarget.class, "MISSING",
            STRING);
    }
    
    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testMethod_HiddenOriginal() throws Throwable {
        AdviceApplier.method(
            handlers(),
            MethodTestTarget.class, "privateIdent",
            MethodTestTarget.class, "ident",
            STRING);
    }
    
    /**
     * Test method for {@link AdviceApplier#method(java.util.List, Class, String, Class, String, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testMethod_HiddenActual() throws Throwable {
        AdviceApplier.method(
            handlers(),
            MethodTestTarget.class, "ident",
            MethodTestTarget.class, "privateIdent",
            STRING);
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testConstructor_Direct() throws Throwable {
        AdviceApplier helper = AdviceApplier.constructor(
            handlers(),
            ConstructorTestTarget.class,
            ConstructorTestTarget.class,
            STRING);
        Object result = helper.invoke(ConstructorTestTarget.class, of("Hello"));
        assertThat(result, is((Object) new ConstructorTestTarget("Hello")));
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testConstructor_SingleHandler() throws Throwable {
        AdviceApplier helper = AdviceApplier.constructor(
            handlers(new BeforeStringInsertHandler("Hello")),
            ConstructorTestTarget.class,
            ConstructorTestTarget.class,
            STRING);
        Object result = helper.invoke(ConstructorTestTarget.class, of("World"));
        assertThat(result, is((Object) new ConstructorTestTarget("HelloWorld")));
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testConstructor_Handlers() throws Throwable {
        AdviceApplier helper = AdviceApplier.constructor(
            handlers(
                new BeforeStringInsertHandler("A"),
                new BeforeStringInsertHandler("B"),
                new BeforeStringInsertHandler("C"),
                new BeforeStringInsertHandler("D")),
            ConstructorTestTarget.class,
            ConstructorTestTarget.class,
            STRING);
        Object result = helper.invoke(ConstructorTestTarget.class, of("E"));
        assertThat(result, is((Object) new ConstructorTestTarget("ABCDE")));
    }
    
    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testConstructor_Capture() throws Throwable {
        final AtomicReference<Invocation> h = new AtomicReference<Invocation>();
        AdviceApplier helper = AdviceApplier.constructor(
            handlers(new InvocationHandler() {
                public Object handle(Invocation invocation) throws Throwable {
                    h.set(invocation);
                    return invocation.proceed();
                }
            }),
            ConstructorTestTarget.class,
            ConstructorTestTarget.class,
            STRING);
        Object result = helper.invoke(ConstructorTestTarget.class, of("a"));
        assertThat(result, is((Object) new ConstructorTestTarget("a")));
        assertThat(h.get(), is(not(nullValue())));
        Invocation inv = h.get();
        assertThat(inv.getInvoker(), is((Object) ConstructorTestTarget.class));
        assertThat(inv.getArguments(), is(of("a")));
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testConstructor_OrthogonalOriginal() throws Throwable {
        AdviceApplier helper = AdviceApplier.constructor(
            handlers(),
            ExtConstructorTestTarget.class,
            ConstructorTestTarget.class,
            STRING);
        Object result = helper.invoke(ConstructorTestTarget.class, of("Hello"));
        assertThat(result, is((Object) new ConstructorTestTarget("Hello")));
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test
    public void testConstructor_OrthogonalActual() throws Throwable {
        AdviceApplier helper = AdviceApplier.constructor(
            handlers(),
            ConstructorTestTarget.class,
            ExtConstructorTestTarget.class,
            STRING);
        Object result = helper.invoke(ConstructorTestTarget.class, of("Hello"));
        assertThat(result, is((Object) new ConstructorTestTarget("ExtHello")));
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testConstructor_MissingOriginal() throws Throwable {
        AdviceApplier.constructor(
            handlers(),
            ExtConstructorTestTarget.class,
            ConstructorTestTarget.class,
            OBJECT);
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testConstructor_MissingActual() throws Throwable {
        AdviceApplier.constructor(
            handlers(),
            ConstructorTestTarget.class,
            ExtConstructorTestTarget.class,
            OBJECT);
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testConstructor_HiddenOriginal() throws Throwable {
        AdviceApplier.constructor(
            handlers(),
            ExtConstructorTestTarget.class,
            ConstructorTestTarget.class,
            CLASS);
    }

    /**
     * Test method for throws Throwable {@link AdviceApplier#constructor(java.util.List, Class, Class, Class[])}.
     * @throws Throwable if occur
     */
    @Test(expected = EnhanceException.class)
    public void testConstructor_HiddenActual() throws Throwable {
        AdviceApplier.constructor(
            handlers(),
            ConstructorTestTarget.class,
            ExtConstructorTestTarget.class,
            CLASS);
    }
    
    private List<InvocationHandler> handlers(InvocationHandler...handlers) {
        return Arrays.asList(handlers);
    }
    
    private Object[] of(Object...values) {
        return values;
    }
}
