package com.blackrook.j2ee.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method parameter. Should be used on Controllers.
 * <p>
 * Parameter type must be String, and will be set to the "filename" portion of the request path.
 * @author Matthew Tropiano
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathFile
{

}
