/**
 * Copyright (c) 2010 Kenn Hussey and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 *   Kenn Hussey - Initial API and implementation
 */
package org.eclipse.emf.ecore.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.emf.ecore.EClassifier;


/**
 * An interface for delegating query execution.
 * 
 * @since 2.7
 */
public interface QueryDelegate
{

  /**
   * A factory for creating query delegates.
   */
  interface Factory
  {
    /**
     * Creates a query delegate for the specified <tt>expression</tt>.
     * 
     * @param context the type of object against which the query will be executed
     * @param parameters a map of parameter names to types
     * @param expression the query text
     * @return a query delegate
     */
    QueryDelegate createQueryDelegate(EClassifier context, Map<String, EClassifier> parameters, String expression);

    /**
     * A <code>Factory</code> wrapper that is used by the {@link Factory.Registry}.
     */
    interface Descriptor
    {
      Factory getFactory();
    }
  }

  /**
   * Prepares (e.g., parses) the query for execution.
   * 
   * @throws InvocationTargetException in case of failure to prepare (e.g., parse) the
   *    query, usually because of an exception
   */
  void prepare() throws InvocationTargetException;

  /**
   * Executes the query against the specified <tt>target</tt> object.
   * 
   * @param target the object against which to execute the query; must be an instance of the context
   *    with which the delegate was created
   * @param arguments a map of parameter names to values; must correspond to the parameters
   *    with which the delegate was created
   * @return the query's result
   * @throws InvocationTargetException in case of failure to execute the
   *    query, usually because of an exception
   */
  Object execute(Object target, Map<String, ?> arguments) throws InvocationTargetException;

}