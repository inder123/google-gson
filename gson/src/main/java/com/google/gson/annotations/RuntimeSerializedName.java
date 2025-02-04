package com.google.gson.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that is used to determine the serialized field name at runtime.
 * This works in conjunction with {@link com.google.gson.RuntimeFieldNamingStrategy}.
 *
 * {@link com.google.gson.annotations.SerializedName} will take precedence over this annotation.
 * This annotation is only intended for use cases where you want the ability to set the serialized name dynamically.
 *
 * <p>Here is an example of how this annotation is meant to be used:</p>
 * <pre>
 * public class Issue {
 *   String name;
 *   &#64RuntimeSerializedName("acceptanceCriteria") String acceptanceCriteria;
 *
 *   public Issue(String name, String acceptanceCriteria) {
 *     this.name = name;
 *     this.acceptanceCriteria = acceptanceCriteria;
 *   }
 * }
 * </pre>
 *
 * <p>The following shows the output that is generated when serializing an instance of the
 * above example class:</p>
 * <pre>
 * Issue target = new Issue("dummyIssue", "ensure site starts up");
 * Map<String, String> fieldNames = new HashMap<>();
 * fieldNames.put("acceptanceCriteria", "customfield_10100");
 * GsonBuilder builder = new GsonBuilder();
 * builder.setFieldNamingStrategy(new RuntimeFieldNamingStrategy(fieldNames));
 * Gson gson = builder.create();
 * String json = gson.toJson(target);
 * System.out.println(json);
 *
 * ===== OUTPUT =====
 * {"name":"dummyIssue","customfield_10100":"ensure site starts up"}
 * </pre>
 *
 * While deserializing, the annotation is used as well
 * For example:
 * <pre>
 *   Issue target = gson.fromJson("{'name':'dummyIssue','customfield_10100':'ensure site starts up'}", Issue.class);
 *   assertEquals("dummyIssue", target.name);
 *   assertEquals("ensure site starts up", target.acceptanceCriteria);
 * </pre>
 *
 *
 * @author Damien Biggs
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface RuntimeSerializedName {

    /**
     * @return the name used to look up the real serialized name for the property.
     */
    String value();
}
