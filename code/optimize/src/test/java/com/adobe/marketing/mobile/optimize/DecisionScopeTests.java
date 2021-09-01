/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.optimize;

import android.util.Base64;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Base64.class)
public class DecisionScopeTests {
    @Before
    public void setup() {
        PowerMockito.mockStatic(Base64.class);
        Mockito.when(Base64.encodeToString((byte[]) any(), anyInt())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                return java.util.Base64.getEncoder().encodeToString((byte[]) invocation.getArguments()[0]);
            }
        });
        Mockito.when(Base64.decode((byte[]) any(), anyInt())).thenAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                return java.util.Base64.getDecoder().decode((byte[]) invocation.getArguments()[0]);
            }
        });
    }

    @Test
    public void testConstructor_validName() {
        // test
        final DecisionScope scope = new DecisionScope("myMbox");
        assertNotNull(scope);
        assertEquals("myMbox", scope.getName());
    }

    @Test
    public void testConstructor_emptyName() {
        // test
        final DecisionScope scope = new DecisionScope("");
        assertNotNull(scope);
        assertEquals("", scope.getName());
    }

    @Test
    public void testConstructor_nullName() {
        // test
        final DecisionScope scope = new DecisionScope("");
        assertNotNull(scope);
        assertEquals("", scope.getName());
    }

    @Test
    public void testConvenienceConstructor_defaultItemCount() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111");
        assertNotNull(scope);
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", scope.getName());
    }

    @Test
    public void testConvenienceConstructor_itemCount() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 10);
        assertNotNull(scope);
        assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEiLCJpdGVtQ291bnQiOjEwfQ==", scope.getName());
    }

    @Test
    public void testConvenienceConstructor_emptyActivityId() {
        // test
        final DecisionScope scope = new DecisionScope("", "xcore:offer-placement:1111111111111111");
        assertNotNull(scope);
        assertEquals("", scope.getName());
    }

    @Test
    public void testConvenienceConstructor_nullActivityId() {
        // test
        final DecisionScope scope = new DecisionScope(null, "xcore:offer-placement:1111111111111111");
        assertNotNull(scope);
        assertEquals("", scope.getName());
    }

    @Test
    public void testConvenienceConstructor_emptyPlacementId() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "");
        assertNotNull(scope);
        assertEquals("", scope.getName());
    }

    @Test
    public void testConvenienceConstructor_nullPlacementId() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", null);
        assertNotNull(scope);
        assertEquals("", scope.getName());
    }

    @Test
    public void testConvenienceConstructor_zeroItemCount() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 0);
        assertNotNull(scope);
        assertEquals("", scope.getName());
    }

    @Test
    public void testIsValid_scopeWithValidName() {
        // test
        final DecisionScope scope = new DecisionScope("myMbox");
        assertTrue(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithNullName() {
        // test
        final DecisionScope scope = new DecisionScope(null);
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithEmptyName() {
        // test
        final DecisionScope scope = new DecisionScope("");
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithDefaultItemCount() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111");
        assertTrue(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithItemCount() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 10);
        assertTrue(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithEmptyActivityId() {
        // test
        final DecisionScope scope = new DecisionScope("", "xcore:offer-placement:1111111111111111");
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithNullActivityId() {
        // test
        final DecisionScope scope = new DecisionScope(null, "xcore:offer-placement:1111111111111111");
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithEmptyPlacementId() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "");
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithNullPlacementId() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", null);
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_scopeWithZeroItemCount() {
        // test
        final DecisionScope scope = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 0);
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_encodedScopeWithDefaultItemCount() {
        // test
        final DecisionScope scope = new DecisionScope( "eyJ4ZG06YWN0aXZpdHlJZCI6Inhjb3JlOm9mZmVyLWFjdGl2aXR5OjExMTExMTExMTExMTExMTEiLCJ4ZG06cGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9");
        assertTrue(scope.isValid());
    }

    @Test
    public void testIsValid_encodedScopeWithItemCount() {
        // test
        final DecisionScope scope = new DecisionScope( "eyJ4ZG06YWN0aXZpdHlJZCI6Inhjb3JlOm9mZmVyLWFjdGl2aXR5OjExMTExMTExMTExMTExMTEiLCJ4ZG06cGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSIsInhkbTppdGVtQ291bnQiOjEwMH0=");
        assertTrue(scope.isValid());
    }

    @Test
    public void testIsValid_encodedScopeWithEmptyActivityId() {
        // test
        final DecisionScope scope = new DecisionScope( "eyJ4ZG06YWN0aXZpdHlJZCI6IiIsInhkbTpwbGFjZW1lbnRJZCI6Inhjb3JlOm9mZmVyLXBsYWNlbWVudDoxMTExMTExMTExMTExMTExIn0=");
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_encodedScopeWithEmptyPlacementId() {
        // test
        final DecisionScope scope = new DecisionScope( "eyJ4ZG06YWN0aXZpdHlJZCI6Inhjb3JlOm9mZmVyLWFjdGl2aXR5OjExMTExMTExMTExMTExMTEiLCJ4ZG06cGxhY2VtZW50SWQiOiIifQ==");
        assertFalse(scope.isValid());
    }

    @Test
    public void testIsValid_encodedScopeWithZeroItemCount() {
        // test
        final DecisionScope scope = new DecisionScope( "eyJ4ZG06YWN0aXZpdHlJZCI6Inhjb3JlOm9mZmVyLWFjdGl2aXR5OjExMTExMTExMTExMTExMTEiLCJ4ZG06cGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSIsInhkbTppdGVtQ291bnQiOjB9");
        assertFalse(scope.isValid());
    }

    @Ignore
    public void testIsValid_invalidEncodedScope() {
        // test
        final DecisionScope scope = new DecisionScope( "eyJ4ZG06YWN0aXZpdHlJZCI6Inhjb3JlOm9mZmVyLWFjdGl2aXR5OjExMTExMTExMTExMTExMTEiLCJ4ZG06cGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSwieGRtOml0ZW1Db3VudCI6MzB9");
        assertFalse(scope.isValid());
    }

    @Test
    public void testEquals() {
        // test
        final DecisionScope scope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final DecisionScope scope2 = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111");
        assertEquals(scope1, scope2);
    }

    @Test
    public void testEquals_decisionScopeObjectContainsItemCount() {
        // test
        final DecisionScope scope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEiLCJpdGVtQ291bnQiOjEwMH0=");
        final DecisionScope scope2 = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111", 100);
        assertEquals(scope1, scope2);
    }

    @Test
    public void testEquals_decisionScopeObjectsNotEqual() {
        // test
        final DecisionScope scope1 = new DecisionScope("mymbox");
        final DecisionScope scope2 = new DecisionScope("xcore:offer-activity:1111111111111111", "xcore:offer-placement:1111111111111111");
        assertNotEquals(scope1, scope2);
    }

    @Test
    public void testEquals_otherDecisionScopeObjectIsNull() {
        // test
        final DecisionScope scope1 = new DecisionScope("mymbox");
        final DecisionScope scope2 = null;
        assertNotEquals(scope1, scope2);
    }
}
