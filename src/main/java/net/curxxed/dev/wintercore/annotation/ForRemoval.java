package net.curxxed.dev.wintercore.annotation;

/**
 * This is only because Deprecated does not have a forRemoval element in java 8,
 * and we want to be able to specify the reason for removal.
 */
public @interface ForRemoval {
        String value() default "";
}
