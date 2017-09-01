package com.gline.orm.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ISchema {
	String schema() default "com.gline.orm";
	String name() default "default";
	int version() default 1;
}
