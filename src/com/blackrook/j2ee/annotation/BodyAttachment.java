package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method. Should be used on Controllers.
 * <p>
 * This turns the method return body content into an "attachment" via <code>Content-Disposition</code> headers.
 * Most effective on File return types.
 * @author Matthew Tropiano
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BodyAttachment
{
}
