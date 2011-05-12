package org.ccci.framework.sblio.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface MvgField {
    public String name() default "";
    public Class clazz();
    public boolean manyToMany() default false;
    public boolean cascadeLoad() default false;
}
