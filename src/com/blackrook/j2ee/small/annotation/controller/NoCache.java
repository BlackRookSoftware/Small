package com.blackrook.j2ee.small.annotation.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.blackrook.j2ee.small.annotation.filter.FilterEntry;

/**
 * If placed on a Controller method, headers are written in the response for
 * hinting at the fact that the returned content should not be cached by the browser.
 * @author Matthew Tropiano
 * @see ControllerEntry
 * @see FilterEntry
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoCache
{

}
