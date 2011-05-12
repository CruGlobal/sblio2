package org.ccci.framework.sblio.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field should be used to map Siebel field values to the corresponding java enum.
 * This annotation must only be present on enum fields that are not static. The annotated field must be of type
 * {@link String}.
 * 
 * For example: <code>
 * enum Gender 
 * {
 *   Male("M"),
 *   Female("F");
 *   
 *   Gender(String code)
 *   {
 *     this.code = code;
 *   }
 * 
 *   @SiebelFieldValue 
 *   String code; 
 * } 
 * </code> 
 * In this case, Siebel will store "M" and "F", but java code can always
 * use the typesafe Gender.Male and Gender.Female.
 * 
 * 
 * @author Matt Drees
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
@Documented
public @interface SiebelFieldValue {
}
