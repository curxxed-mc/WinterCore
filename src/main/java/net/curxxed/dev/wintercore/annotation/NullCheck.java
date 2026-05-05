package net.curxxed.dev.wintercore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a method parameter should be checked for null values.
 * This can be used to automatically generate null checks for method parameters
 * improving code safety and reducing the likelihood of NullPointerExceptions.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface NullCheck {
}
