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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jiemamy.utils.enhancer.Invocation;
import org.jiemamy.utils.enhancer.InvocationHandler;
import org.jiemamy.utils.enhancer.aspect.ThroughHandler;
import org.jiemamy.utils.enhancer.helper.DelegateInvocation;

import org.junit.Test;

/**
 * Test for {@link DelegateInvocation}.
 * @version $Date$
 * @author Suguru ARAKAWA
 */
public class DelegateInvocationTest {

    /**
     * Test method for {@link DelegateInvocation#DelegateInvocation(Invocation, InvocationHandler)}.
     * @throws Exception if occur
     */
    @Test
    public void testDelegateInvocation() throws Exception {
        DelegateInvocation invocation = new DelegateInvocation(
            new MockInvocation() {
                @Override
                public Object proceed() {
                    return Boolean.TRUE;
                }
            },
            new ThroughHandler()
        );
        assertThat(invocation.proceed(), is((Object) true));
    }

    /**
     * Test method for throws Exception {@link DelegateInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed() throws Exception {
        DelegateInvocation invocation = new DelegateInvocation(
            new MockInvocation() {
                @Override
                public Object proceed() {
                    return Boolean.TRUE;
                }
            },
            new InvocationHandler() {
                public Object handle(Invocation _) {
                    return Boolean.FALSE;
                }
            }
        );
        assertThat(invocation.proceed(), is((Object) false));
    }

    /**
     * Test method for throws Exception {@link DelegateInvocation#proceed()}.
     * @throws Exception if occur
     */
    @Test
    public void testProceed_Exception() throws Exception {
        DelegateInvocation invocation = new DelegateInvocation(
            new MockInvocation(),
            new InvocationHandler() {
                public Object handle(Invocation _) throws Exception {
                    throw new IOException();
                }
            }
        );
        try {
            invocation.proceed();
            fail();
        }
        catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(IOException.class));
        }
    }

    /**
     * Test method for throws Exception {@link DelegateInvocation#getTarget()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetTarget() throws Exception {
        MockInvocation original = new MockInvocation() {
            @Override
            public Object proceed() {
                return Boolean.TRUE;
            }
        };
        DelegateInvocation invocation = new DelegateInvocation(
            original,
            new InvocationHandler() {
                public Object handle(Invocation _) {
                    return Boolean.FALSE;
                }
            }
        );
        assertThat(invocation.getTarget(), is(Method.class));
        assertThat(
            ((Method) invocation.getTarget()).invoke(original),
            is((Object) true));
    }

    /**
     * Test method for throws Exception {@link DelegateInvocation#getInvoker()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetObject() throws Exception {
        DelegateInvocation invocation = new DelegateInvocation(
            new MockInvocation(), new ThroughHandler());
        assertThat(invocation.getInvoker(), is(MockInvocation.class));
    }

    /**
     * Test method for throws Exception {@link DelegateInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetArguments() throws Exception {
        DelegateInvocation invocation = new DelegateInvocation(
            new MockInvocation(new Object[] {"a"}), new ThroughHandler());
        assertThat(invocation.getArguments(), is(new Object[] {"a"}));
    }

    /**
     * Test method for throws Exception {@link DelegateInvocation#getArguments()}.
     * @throws Exception if occur
     */
    @Test
    public void testGetArguments_Rewrite() throws Exception {
        MockInvocation original = new MockInvocation(new Object[] {"a"}) {
            @Override
            public Object proceed() {
                return getArguments();
            }
        };
        DelegateInvocation invocation = new DelegateInvocation(
            original,
            new InvocationHandler() {
                public Object handle(Invocation nest) throws Exception {
                    return nest.proceed();
                }
            }
        );
        invocation.getArguments()[0] = "b";
        Object[] result = (Object[]) invocation.proceed();
        assertThat(result, is(new Object[] {"b"}));
    }
}
