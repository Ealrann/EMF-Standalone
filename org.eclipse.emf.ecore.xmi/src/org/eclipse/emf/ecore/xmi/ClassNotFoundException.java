/**
 * Copyright (c) 2002-2006 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.ecore.xmi;

import org.eclipse.emf.ecore.EFactory;

public class ClassNotFoundException extends XMIException 
{
  private static final long serialVersionUID = 1L;

  protected String className;
  protected transient EFactory factory;

  public ClassNotFoundException(String name, EFactory factory, String location, int line, int column) 
  {
    super("Class '" + name + "' is not found or is abstract.", location, line, column);
    className = name;
    this.factory = factory;
  }
  
  public String getName() 
  {
    return className;
  }  
  
  public EFactory getFactory() 
  {
    return factory;
  }  
}
