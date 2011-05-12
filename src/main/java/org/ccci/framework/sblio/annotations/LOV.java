package org.ccci.framework.sblio.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface LOV {
	public String type() default "";
	public int low() default -1;
	public int high() default -1;
	public String orderBy() default "";
}
