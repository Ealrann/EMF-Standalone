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
package org.eclipse.emf.ecore.resource.impl;


import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;


/**
 * A trivial implementation of a resource factory.
 */
public class ResourceFactoryImpl implements Resource.Factory
{
  /**
   * Creates an instance.
   */
  public ResourceFactoryImpl()
  {
    super();
  }
  
  /**
   * Returns a newly allocated default resource {@link org.eclipse.emf.ecore.resource.impl.ResourceImpl#ResourceImpl(URI) implementation}.
   * @param uri the URI.
   * @return a new resource for the URI.
   */
  public Resource createResource(URI uri)
  {
    return new ResourceImpl(uri);
  }
}
