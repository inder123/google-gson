package com.google.gson.functional;

import java.util.HashMap;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.RuntimeFieldNamingStrategy;
import com.google.gson.annotations.RuntimeSerializedName;
import com.google.gson.annotations.SerializedName;
import junit.framework.TestCase;

public final class RuntimeSerializedNameTest extends TestCase {
    private Gson gson;
    private HashMap<String, String> fieldNames;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fieldNames = new HashMap<String, String>();
        fieldNames.put("acceptanceCriteria", "customfield_10100");
        gson = createGson(null);
    }

    public void testIssueIsSerializedCorrectly() {
        Issue target = new Issue("dummyIssue", "ensure site starts up");
        assertEquals("{\"issueName\":\"dummyIssue\",\"customfield_10100\":\"ensure site starts up\"}", gson.toJson
                (target));
    }

    public void testIssueIsDeserializedCorrectly() {
        Issue target = gson.fromJson("{\"issueName\":\"dummyIssue\",\"customfield_10100\":\"ensure site starts " +
                "up\"}", Issue.class);
        assertEquals("dummyIssue", target.issueName);
        assertEquals("ensure site starts up", target.acceptanceCriteria);
    }

    public void testCanUseDifferentDefaultNamingPolicy() {
        Gson gson = createGson(FieldNamingPolicy.UPPER_CAMEL_CASE_WITH_SPACES);
        Issue target = new Issue("dummyIssue", "ensure site starts up");
        assertEquals("{\"Issue Name\":\"dummyIssue\",\"customfield_10100\":\"ensure site starts up\"}", gson.toJson
                (target));
    }

    public void testCanDeserializeUsingDifferentDefaultNamingPolicy() {
        Gson gson = createGson(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Issue target = gson.fromJson("{\"issue_name\":\"dummyIssue\",\"customfield_10100\":\"ensure site starts " +
                "up\"}", Issue.class);
        assertEquals("dummyIssue", target.issueName);
        assertEquals("ensure site starts up", target.acceptanceCriteria);
    }

    public void testSerializedNameAnnotationTakesPrecedence() {
        IssueWithSerializedNameAnnotation target = new IssueWithSerializedNameAnnotation("dummyIssue", "ensure site starts up");
        assertEquals("{\"issueName\":\"dummyIssue\",\"criteria\":\"ensure site starts up\"}", gson.toJson
                (target));
    }

    public void testIssueWillFailSerializationIfFieldNameNotFound() {
        try {
            gson.toJson(new IssueWithUnknownRuntimeField("dummyIssue", "ensure site starts up"));
            fail("Expected serialization to fail");
        } catch (IllegalArgumentException iae) {
            assertEquals("No field name mapping for runtime field name unknownFieldName", iae.getMessage());
        }
    }

    private Gson createGson(FieldNamingStrategy defaultStrategy) {
        GsonBuilder builder = new GsonBuilder();
        builder.setFieldNamingStrategy(new RuntimeFieldNamingStrategy(fieldNames, defaultStrategy));
        return builder.create();
    }

    private static final class Issue {
        String issueName;
        @RuntimeSerializedName("acceptanceCriteria")
        String acceptanceCriteria;

        public Issue(String issueName, String acceptanceCriteria) {
            this.issueName = issueName;
            this.acceptanceCriteria = acceptanceCriteria;
        }
    }

    private static final class IssueWithUnknownRuntimeField {
        String issueName;
        @RuntimeSerializedName("unknownFieldName")
        String acceptanceCriteria;

        public IssueWithUnknownRuntimeField(String issueName, String acceptanceCriteria) {
            this.issueName = issueName;
            this.acceptanceCriteria = acceptanceCriteria;
        }
    }

    private static final class IssueWithSerializedNameAnnotation {
        String issueName;
        @RuntimeSerializedName("acceptanceCriteria")
        @SerializedName("criteria")
        String acceptanceCriteria;

        public IssueWithSerializedNameAnnotation(String issueName, String acceptanceCriteria) {
            this.issueName = issueName;
            this.acceptanceCriteria = acceptanceCriteria;
        }
    }
}
