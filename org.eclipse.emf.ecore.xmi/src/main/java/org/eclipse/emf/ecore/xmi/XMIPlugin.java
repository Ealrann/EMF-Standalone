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


import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.ResourceLocator;


/**
 * The <b>Plugin</b> for the EMF.Ecore.XMI library.
 */
public final class XMIPlugin extends EMFPlugin 
{
  /**
   * The singleton instance of the plugin.
   */
  public static final XMIPlugin INSTANCE = new XMIPlugin();

  /**
   * Creates the singleton instance.
   */
  private XMIPlugin()
  {
    super(new ResourceLocator[] {});
  }
}
