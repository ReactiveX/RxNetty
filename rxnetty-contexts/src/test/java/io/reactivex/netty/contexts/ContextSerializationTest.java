/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.netty.contexts;

import com.netflix.server.context.ContextSerializationException;
import com.netflix.server.context.ContextSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import static io.reactivex.netty.contexts.ContextSerializationHelper.CONTEXT_HEADER_NAMES_SEPARATOR;

/**
 * @author Nitesh Kant (nkant@netflix.com)
 */
public class ContextSerializationTest {

    private static final Pattern NAMES_SEPARATOR_PATTERN = Pattern.compile(CONTEXT_HEADER_NAMES_SEPARATOR);

    @Before
    public void setUp() throws Exception {
        TestContextSerializer.instanceCount = 0;
    }

    @Test
    public void testGetSerializedHeaderForStringContext() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "StringContext";
        String value = "stringAsIAm";
        container.addContext(name, value);
        Map<String, String> serializedContexts = getSerializedContexts(container);

        validateContextNamesHeaders(serializedContexts, name);
        try {
            validateSerializedContents(serializedContexts, new ExpectedContent[] {new ExpectedContent(name, value, StringSerializer.INSTANCE)});
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to serialize locally, strange. Error: " + e.getMessage());
        }
    }

    @Test
    public void testGetSerializedHeaderForCustomContext() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(name, value, serializer);
        Map<String, String> serializedContexts = getSerializedContexts(container);

        validateContextNamesHeaders(serializedContexts, name);
        try {
            validateSerializedContents(serializedContexts, new ExpectedContent[]{new ExpectedContent(name, value, serializer)});
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to serialize locally, strange. Error: " + e.getMessage());
        }
    }

    @Test
    public void testGetSerializedHeaderForMultipleContexts() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(name, value, serializer);

        String name1 = "CustomContext1";
        TestContext value1 = new TestContext("mynameiscustom1");
        container.addContext(name1, value1, serializer);

        String name2 = "StringContext1";
        String value2 = "stringasiam";
        container.addContext(name2, value2);

        Map<String, String> serializedContexts = getSerializedContexts(container);

        validateContextNamesHeaders(serializedContexts, name, name1, name2);
        try {
            validateSerializedContents(serializedContexts,
                    new ExpectedContent[]{new ExpectedContent(name, value, serializer),
                                          new ExpectedContent(name1, value1, serializer),
                                          new ExpectedContent(name2, value2, StringSerializer.INSTANCE)
                    });
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to serialize locally, strange. Error: " + e.getMessage());
        }
    }

    @Test
    public void testDeserialization() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "StringContext";
        String value = "stringAsIAm";
        container.addContext(name, value);
        Map<String, String> serializedContexts = getSerializedContexts(container);

        testDeserializedContexts(serializedContexts, new ExpectedContent(name, value, StringSerializer.INSTANCE));
    }

    @Test
    public void testDeserializationMultiContexts() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(name, value, serializer);

        String name1 = "CustomContext1";
        TestContext value1 = new TestContext("mynameiscustom1");
        container.addContext(name1, value1, serializer);

        String name2 = "StringContext1";
        String value2 = "stringasiam";
        container.addContext(name2, value2);

        Map<String, String> serializedContexts = getSerializedContexts(container);

        testDeserializedContexts(serializedContexts, new ExpectedContent(name, value, serializer),
                                                     new ExpectedContent(name1, value1, serializer),
                                                     new ExpectedContent(name2, value2, StringSerializer.INSTANCE));

    }

    @Test
    public void testDeserializeWithDifferentSerializer() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer origSerializer = new TestContextSerializer();
        container.addContext(name, value, origSerializer);

        ContextsContainer deserializedContainer = createDeserializedContext(getSerializedContexts(container));

        TestContextSerializer getSerializer = new TestContextSerializer();
        try {
            TestContext context = deserializedContainer.getContext(name, getSerializer);
            Assert.assertEquals(value, context);
            Assert.assertEquals("Get deserializer not called.", 1, getSerializer.deserializationCallReceived);
            Assert.assertEquals("Original deserializer called.", 0, origSerializer.deserializationCallReceived);
            Assert.assertEquals("More than one instance of original deserializer created.", 2,
                                TestContextSerializer.instanceCount);
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to get context from deserialized data. Error: " + e.getMessage());
        }
    }

    @Test
    public void testSerializerInstanceCache() throws Exception {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer origSerializer = new TestContextSerializer();
        container.addContext(name, value, origSerializer);

        TestContextSerializer.instanceCount = 0;

        ContextsContainer deserializedContainer = createDeserializedContext(getSerializedContexts(container));

        Assert.assertEquals("Serializer instance created prematurely.", 0, TestContextSerializer.instanceCount);


        TestContext context = deserializedContainer.getContext(name);
        Assert.assertEquals(value, context);

        Assert.assertEquals("More/less than 1 serializer instance created.", 1, TestContextSerializer.instanceCount);

        deserializedContainer.getContext(name);

        Assert.assertEquals("Serializer instance not cached, sucks!", 1, TestContextSerializer.instanceCount);
    }

    @Test
    public void testValueCache() throws Exception {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer origSerializer = new TestContextSerializer();
        container.addContext(name, value, origSerializer);

        ContextsContainerImpl deserializedContainer = createDeserializedContext(getSerializedContexts(container));
        TestContext context = deserializedContainer.getContext(name);
        Assert.assertEquals(value, context);

        deserializedContainer.getContext(name);

        @SuppressWarnings("rawtypes") ContextHolder contextHolder = deserializedContainer.getContextHolder(name);
        Assert.assertEquals("Context value not cached, deserialize called.", 1,
                            ((TestContextSerializer) contextHolder.getRawSerializer()).deserializationCallReceived);
    }

    @Test
    public void testValueCacheWithCustom() throws Exception {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer origSerializer = new TestContextSerializer();
        container.addContext(name, value, origSerializer);

        ContextsContainer deserializedContainer = createDeserializedContext(getSerializedContexts(container));

        TestContextSerializer getSerializer = new TestContextSerializer();
        try {
            TestContext context = deserializedContainer.getContext(name, getSerializer);
            Assert.assertEquals(value, context);

            deserializedContainer.getContext(name, getSerializer);
            Assert.assertEquals("Context value not cached, deserialize called.", 1,
                                getSerializer.deserializationCallReceived);
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to get context from deserialized data. Error: " + e.getMessage());
        }
    }

    @Test
    public void testOverwrite() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(name, value, serializer);

        getSerializedContexts(container); // To make sure its not just the value cache.

        TestContext newValue = new TestContext("mynameiscustomNew");
        container.addContext(name, newValue, serializer);

        Map<String, String> serializedContexts = getSerializedContexts(container);
        validateContextNamesHeaders(serializedContexts, name);

        try {
            validateSerializedContents(serializedContexts,
                                       new ExpectedContent[]{new ExpectedContent(name, newValue, serializer)});
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to serialize locally, strange. Error: " + e.getMessage());
        }
    }

    @Test
    public void testOverwriteDeserialize() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(name, value, serializer);

        getSerializedContexts(container); // To make sure its not just the value cache.

        TestContext newValue = new TestContext("mynameiscustomNew");
        TestContextSerializer newSerializer = new TestContextSerializer();
        container.addContext(name, newValue, newSerializer);

        Map<String, String> serializedContexts = getSerializedContexts(container);

        ContextsContainer deserializedContext = createDeserializedContext(serializedContexts);

        try {
            TestContext context = deserializedContext.getContext(name);
            Assert.assertEquals(context, newValue);
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to get context from deserialized data. Error: " + e.getMessage());
        }
    }

    @Test
    public void testNullContext() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        try {
            container.addContext("ctx", null);
            throw new AssertionError("Null context accepted.");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            container.addContext("ctx", null, null);
            throw new AssertionError("Null context & serializer accepted.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNullSerialized() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        ContextSerializer<String> nullSerializer = new NullContextSerializer<String>();
        container.addContext("ctx", "IamNullForAllPracticalPurposes", nullSerializer);
        try {
            container.getSerializedContexts();
            throw new AssertionError("Null serialized value accepted.");
        } catch (ContextSerializationException e) {
            // expected
        }
    }

    @Test
    public void testNullDeSerialized() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "StringContext";
        String value = "stringAsIAm";
        container.addContext(name, value);
        Map<String, String> serializedContexts = getSerializedContexts(container);
        ContextsContainer deserializedContext = createDeserializedContext(serializedContexts);
        ContextSerializer<String> nullSerializer = new NullContextSerializer<String>();
        try {
            deserializedContext.getContext(name, nullSerializer);
            throw new AssertionError("Null deserialized value accepted.");
        } catch (ContextSerializationException e) {
            // expected
        }
    }

    @Test
    public void testZeroLengthDeSerialized() throws ContextSerializationException {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "StringContext";
        String value = "";
        container.addContext(name, new TestContext(value), new TestContextSerializer());
        Map<String, String> serializedContexts = getSerializedContexts(container);
        ContextsContainer deserializedContext = createDeserializedContext(serializedContexts);
        TestContext context = deserializedContext.getContext(name);
        Assert.assertEquals("Empty string serialization failed.", context.getName(), value);
    }

    @Test
    public void testEmptyDeSerialized() throws ContextSerializationException {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "StringContext";
        String value = "   ";
        container.addContext(name, new TestContext(value), new TestContextSerializer());
        Map<String, String> serializedContexts = getSerializedContexts(container);
        ContextsContainer deserializedContext = createDeserializedContext(serializedContexts);
        TestContext context = deserializedContext.getContext(name);
        Assert.assertEquals("Empty string serialization failed.", context.getName(), value);
    }

    @Test
    public void testRemoveAll() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(name, value, serializer);

        getSerializedContexts(container); // To make sure its not just the value cache.

        Assert.assertTrue("Context remove failed.", container.removeContext(name));

        Map<String, String> serializedContexts = getSerializedContexts(container);

        Assert.assertTrue("Serialized contexts not empty after remove.", serializedContexts.isEmpty());
    }

    @Test
    public void testRemoveOne() {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "CustomContext";
        String name2 = "CustomContext2";
        TestContext value = new TestContext("mynameiscustom");
        TestContext value2 = new TestContext("mynameiscustom2");
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(name, value, serializer);
        container.addContext(name2, value2, serializer);

        getSerializedContexts(container); // To make sure its not just the value cache.

        Assert.assertTrue("Context remove failed.", container.removeContext(name));

        Map<String, String> serializedContexts = getSerializedContexts(container);

        // Size is 2 as there is an ALL_HEADER_NAMES header too.
        Assert.assertEquals("Serialized contexts size should be 2 after removing one context.", 2,
                            serializedContexts.size());
        validateContextNamesHeaders(serializedContexts, name2);
    }

    @Test
    public void testBidirectionalSer() throws Exception {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "BidirectionalCtx";
        BidirectionalTestContext bictx = new BidirectionalTestContext(name);
        String ctxName = "BICTX";
        TestContextSerializer serializer = new TestContextSerializer();
        container.addContext(ctxName, bictx, serializer);

        String name2 = "CustomContext";
        TestContext value = new TestContext("mynameiscustom");
        TestContextSerializer serializer2 = new TestContextSerializer();
        container.addContext(name2, value, serializer2);

        Map<String, String> serializedContexts = getSerializedContexts(container);

        validateContextNamesHeaders(serializedContexts, ctxName, name2);

        try {
            validateSerializedContents(serializedContexts, new ExpectedContent[]{new ExpectedContent(ctxName, bictx, serializer),
                                                                                 new ExpectedContent(name2, value, serializer2)});
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to serialize locally, strange. Error: " + e.getMessage());
        }
    }

    @Test
    public void testBidirectionalUpdate() throws Exception {
        ContextsContainerImpl container = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "BidirectionalCtx1";
        BidirectionalTestContext bictx = new BidirectionalTestContext(name);
        String ctxName = "BICTX";
        BidirectionalTestContextSerializer serializer = new BidirectionalTestContextSerializer();
        container.addContext(ctxName, bictx, serializer);

        String name2 = "BidirectionalCtx2";
        BidirectionalTestContext bictx2 = new BidirectionalTestContext(name2);
        String ctxName2 = "BICTX2";
        BidirectionalTestContextSerializer serializer2 = new BidirectionalTestContextSerializer();
        container.addContext(ctxName2, bictx2, serializer2);

        String biCtxUpdatedName = name + "Updated";
        BidirectionalTestContext bictxUpdated = new BidirectionalTestContext(biCtxUpdatedName);

        container.addContext(ctxName, bictxUpdated, serializer); // Update

        Map<String, String> modifiedBidirectionalContexts = container.getModifiedBidirectionalContexts();
        ContextKeySupplier modifiedContextSupplier = new MockContextKeySupplier(modifiedBidirectionalContexts);
        validateContextNamesHeaders(modifiedBidirectionalContexts, ctxName, ctxName2);

        try {
            validateSerializedContents(modifiedBidirectionalContexts, new ExpectedContent[]{new ExpectedContent(ctxName, bictxUpdated, serializer)});
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to serialize locally, strange. Error: " + e.getMessage());
        }

        ContextsContainerImpl containerNew = new ContextsContainerImpl(new MockContextKeySupplier());
        containerNew.consumeBidirectionalContextsFromResponse(modifiedContextSupplier);

        BidirectionalTestContext ctxAfterConsume = containerNew.getContext(ctxName);
        Assert.assertNotNull("Updated bi-directional ctx for consumed", ctxAfterConsume);
        Assert.assertEquals("Updated ctx value not obtained in new ctx", ctxAfterConsume.getName(), biCtxUpdatedName);

        BidirectionalTestContext addedCtxAfterConsume = containerNew.getContext(ctxName2);
        Assert.assertNotNull("Freshly added bi-directional ctx not serialized", addedCtxAfterConsume);
        Assert.assertEquals("Updated ctx value not obtained in new ctx", addedCtxAfterConsume.getName(), name2);
    }

    @Test
    public void testBidirectionalMerge() throws Exception {
        ContextsContainerImpl ctx = new ContextsContainerImpl(new MockContextKeySupplier());
        String name = "BidirectionalCtx1";
        MergeableBidirectionalTestContext bictx = new MergeableBidirectionalTestContext(name);
        String ctxName = "BICTX";
        MergeableBidirectionalTestContextSerializer serializer = new MergeableBidirectionalTestContextSerializer();
        ctx.addContext(ctxName, bictx, serializer);

        String biCtxUpdatedName = name + "Updated";
        MergeableBidirectionalTestContext bictxUpdated = new MergeableBidirectionalTestContext(biCtxUpdatedName);

        ctx.addContext(ctxName, bictxUpdated, serializer); // Update

        Map<String, String> modifiedBidirectionalContexts = ctx.getModifiedBidirectionalContexts();
        ContextKeySupplier modifiedContextSupplier = new MockContextKeySupplier(modifiedBidirectionalContexts);
        validateContextNamesHeaders(modifiedBidirectionalContexts, ctxName);

        try {
            validateSerializedContents(modifiedBidirectionalContexts, new ExpectedContent[]{new ExpectedContent(ctxName, bictxUpdated, serializer)});
        } catch (ContextSerializationException e) {
            throw new AssertionError("Failed to serialize locally, strange. Error: " + e.getMessage());
        }

        ContextsContainerImpl containerNew = new ContextsContainerImpl(new MockContextKeySupplier());
        MergeableBidirectionalTestContext subCtx = new MergeableBidirectionalTestContext(name);
        String addnName1 = "name1";
        String addnName2 = "name2";
        subCtx.addName(addnName1);
        subCtx.addName(addnName2);
        containerNew.addContext(ctxName, subCtx, serializer);
        containerNew.consumeBidirectionalContextsFromResponse(modifiedContextSupplier);

        MergeableBidirectionalTestContext ctxAfterConsume = containerNew.getContext(ctxName);
        Assert.assertNotNull("Updated bi-directional ctx for consumed", ctxAfterConsume);
        Assert.assertEquals("Updated ctx value not obtained in new ctx", ctxAfterConsume.getName(), biCtxUpdatedName);
    }

    @Test
    public void testDirectional() throws Exception {
        ContextsContainerImpl ctx = new ContextsContainerImpl(new MockContextKeySupplier());

        //CurrentRequestContext.set(ctx);
        String ctxName = "directionaware";
        TestContextDirectionalSerializer ser = new TestContextDirectionalSerializer();
        BidirectionalTestContext testContext = new BidirectionalTestContext("Blah");

        ctx.addContext(ctxName, testContext, ser);

        Map<String, String> serializedContexts = getSerializedContexts(ctx);
        ContextKeySupplier serializedSupplier = new MockContextKeySupplier(serializedContexts);

        Assert.assertEquals("Serializer did not get called", 1,
                            ((TestContextDirectionalSerializer) ctx.getContextHolder(ctxName)
                                                                   .getRawSerializer()).serializationOutboundCallReceived);

        ContextsContainerImpl newCtx = new ContextsContainerImpl(new MockContextKeySupplier());
        newCtx.consumeBidirectionalContextsFromResponse(serializedSupplier);
        newCtx.addContext(ctxName, new BidirectionalTestContext("updated"), ser);

        newCtx.getModifiedBidirectionalContexts();
        Assert.assertEquals("Serializer did not get called", 1,
                            ((TestContextDirectionalSerializer) newCtx.getContextHolder(ctxName)
                                                                      .getRawSerializer()).serializationInboundCallReceived);
    }

    /*********   UTILITY methods **********/
    private static ContextsContainerImpl createDeserializedContext(Map<String, String> serializedContexts) {
        MockContextKeySupplier keySupplier = new MockContextKeySupplier();
        for (Map.Entry<String, String> entry : serializedContexts.entrySet()) {
            keySupplier.put(entry.getKey(), entry.getValue());
        }

        return new ContextsContainerImpl(keySupplier);
    }


    private static void testDeserializedContexts(Map<String, String> serializedContexts,
                                                 ExpectedContent... expectedContents) {
        ContextsContainer deserializedContainer = createDeserializedContext(serializedContexts);

        StringBuilder errMsgBuilder = new StringBuilder();
        for (ExpectedContent expectedContent : expectedContents) {
            try {
                Object context = deserializedContainer.getContext(expectedContent.contextName);
                Assert.assertEquals(expectedContent.data, context);
            } catch (ContextSerializationException e) {
                errMsgBuilder.append("Failed to get the data for context: ");
                errMsgBuilder.append(expectedContent.contextName);
                errMsgBuilder.append(". Error: ");
                errMsgBuilder.append(e.getMessage());
                errMsgBuilder.append('\n');
            }
        }

        if (errMsgBuilder.length() != 0) {
            throw new AssertionError(errMsgBuilder.toString());
        }
    }

    private static Map<String, String> getSerializedContexts(ContextsContainer container) {
        Map<String, String> serializedContexts;
        try {
            serializedContexts = container.getSerializedContexts();
        } catch (ContextSerializationException e) {
            throw new AssertionError("Serialization of contexts failed." + e.getMessage());
        }
        return serializedContexts;
    }

    private static void validateSerializedContents(Map<String, String> serializedContexts,
                                                   ExpectedContent[] expectedContents)
            throws ContextSerializationException {
        for (ExpectedContent expectedContent : expectedContents) {
            String headerName = getHeaderName(expectedContent.contextName);
            Assert.assertTrue("Serialized context not found", serializedContexts.containsKey(headerName));
            Assert.assertEquals("Serialized context value not as expected.", getSerializedHeaderValue(expectedContent),
                                serializedContexts.get(headerName));
        }
    }

    @SuppressWarnings("unchecked")
    private static String getSerializedHeaderValue(ExpectedContent expected) throws ContextSerializationException {
        return ContextSerializationHelper.SERIALIZATION_FORMAT_VER + ContextSerializationHelper.CONTEXT_SERIALIZATION_PROPS_SEPARATOR +
               expected.serializer.getClass().getName() + ContextSerializationHelper.CONTEXT_SERIALIZATION_PROPS_SEPARATOR +
               expected.serializer.getVersion() +  ContextSerializationHelper.CONTEXT_SERIALIZATION_PROPS_SEPARATOR +
               expected.serializer.serialize(expected.data);
    }

    private static void validateContextNamesHeaders(Map<String, String> serializedContexts, String... contextNames) {
        String[] headerNames = new String[contextNames.length];
        for (int i = 0, contextNamesLength = contextNames.length; i < contextNamesLength; i++) {
            String contextName = contextNames[i];
            headerNames[i] = getHeaderName(contextName);
        }

        Assert.assertTrue("All context names header not found.",
                          serializedContexts.containsKey(ContextSerializationHelper.ALL_CONTEXTS_NAMES_KEY_NAME));
        String allContextHeaderNames = serializedContexts.get(ContextSerializationHelper.ALL_CONTEXTS_NAMES_KEY_NAME);
        Arrays.sort(headerNames);
        String[] headerNamesReceived = NAMES_SEPARATOR_PATTERN.split(allContextHeaderNames);
        Arrays.sort(headerNamesReceived);
        Assert.assertArrayEquals(headerNamesReceived, headerNames);
    }

    private static String getHeaderName(String contextName) {
        return ContextSerializationHelper.CONTEXT_PROTOCOL_KEY_NAME_PREFIX + contextName;
    }

    private static class NullContextSerializer<T> implements ContextSerializer<T> {
        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public String serialize(Object toSerialize) throws ContextSerializationException {
            return null;
        }

        @Override
        public T deserialize(String serialized, int version) throws ContextSerializationException {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static class ExpectedContent {

        private String contextName;
        private Object data;
        private ContextSerializer serializer;

        private ExpectedContent(String contextName, Object data, ContextSerializer serializer) {
            this.contextName = contextName;
            this.data = data;
            this.serializer = serializer;
        }
    }
}
