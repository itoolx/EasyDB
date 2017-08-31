package com.gline.db.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IColumnName {

	String name();

	ColumnType type();

	int length() default -1;

	boolean autoIncrement() default false;
	
}
