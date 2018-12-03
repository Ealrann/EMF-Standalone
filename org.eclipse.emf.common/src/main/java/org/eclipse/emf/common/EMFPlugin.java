/**
 * Copyright (c) 2002-2007 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.common;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

import org.eclipse.emf.common.util.DelegatingResourceLocator;
import org.eclipse.emf.common.util.Logger;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.common.util.URI;


/**
 * EMF must run 
 * within an Eclipse workbench,
 * within a headless Eclipse workspace,
 * or just stand-alone as part of some other application.
 * To support this, all resource access (e.g., NL strings, images, and so on) is directed to the resource locator methods,
 * which can redirect the service as appropriate to the runtime.
 * During Eclipse invocation, the implementation delegates to a plugin implementation.
 * During stand-alone invocation, no plugin initialization takes place,
 * so the implementation delegates to a resource JAR on the CLASSPATH.
 * The resource jar will typically <b>not</b> be on the CLASSPATH during Eclipse invocation.
 * It will contain things like the icons and the .properties,  
 * which are available in a different way during Eclipse invocation.
 * @see DelegatingResourceLocator
 * @see ResourceLocator
 * @see Logger
 */
public abstract class EMFPlugin extends DelegatingResourceLocator implements ResourceLocator, Logger
{
  public static final boolean IS_ECLIPSE_RUNNING = false;

  public static final boolean IS_RESOURCES_BUNDLE_AVAILABLE = false;

  protected ResourceLocator [] delegateResourceLocators;

  public EMFPlugin(ResourceLocator [] delegateResourceLocators)
  {
    this.delegateResourceLocators = delegateResourceLocators;
  }

  @Override
  final protected ResourceLocator getPrimaryResourceLocator()
  {
    return null;
  }
  
  @Override
  protected ResourceLocator[] getDelegateResourceLocators()
  {
    return delegateResourceLocators;
  }

  public String getSymbolicName()
  {
    String result = getClass().getName();
    return result.substring(0, result.lastIndexOf('.'));
  }

  /*
   * Javadoc copied from interface.
   */
  public void log(Object logEntry)
  {
    if (logEntry instanceof Throwable)
    {
      ((Throwable)logEntry).printStackTrace(System.err);
    }
    else
    {
      System.err.println(logEntry);
    }
  }

  /**
   * This just provides a common interface for the Eclipse plugins supported by EMF.
   * It is not considered API and should not be used by clients.
   */
  public static interface InternalEclipsePlugin
  {
    String getSymbolicName();
  }

  public static void main(String[] args)
  {
    try
    {
      String [] relativePath = { "META-INF", "MANIFEST.MF" };
      Class<?> theClass =  args.length > 0 ? Class.forName(args[0]) : EMFPlugin.class;

      String className = theClass.getName();
      int index = className.lastIndexOf(".");
      URL classURL = theClass.getResource((index == -1 ? className : className.substring(index + 1)) + ".class");
      URI uri = URI.createURI(classURL.toString());

      // Trim off the segments corresponding to the package nesting.
      //
      int count = 1;
      for (int i = 0; (i = className.indexOf('.', i)) != -1; ++i)
      {
        ++count;
      }
      uri = uri.trimSegments(count);

      URL manifestURL = null;
  
      // For an archive URI, check for the path in the archive.
      //
      if (URI.isArchiveScheme(uri.scheme()))
      {
        try
        {
          // If we can open  an input stream, then the path is there, and we have a good URL.
          //
          String manifestURI = uri.appendSegments(relativePath).toString();
          InputStream inputStream =  new URL(manifestURI).openStream();
          inputStream.close();
          manifestURL = new URL(manifestURI);
        }
        catch (IOException exception)
        {
          // If the path isn't within the root of the archive, 
          // create a new URI for the folder location of the archive, 
          // so we can look in the folder that contains it.
          //
          uri = URI.createURI(uri.authority()).trimSegments(1);
        }
      }
              
      // If we didn't find the path in the usual place nor in the archive...
      //
      if (manifestURL == null)
      {
        // Trim off the "bin" or "runtime" segment.
        //
        String lastSegment = uri.lastSegment();
        if ("bin".equals(lastSegment) || "runtime".equals(lastSegment))
        {
          uri = uri.trimSegments(1);
        }
        uri = uri.appendSegments(relativePath);
        manifestURL = new URL(uri.toString());
      }
              
      Manifest manifest = new Manifest(manifestURL.openStream());
      String symbolicName =  manifest.getMainAttributes().getValue("Bundle-SymbolicName");
      if (symbolicName != null)
      {
        int end = symbolicName.indexOf(";");
        if (end != -1)
        {
          symbolicName = symbolicName.substring(0, end);
        }
        System.out.println("Bundle-SymbolicName=" + symbolicName + " Bundle-Version=" + manifest.getMainAttributes().getValue("Bundle-Version"));
        return;
      }
    }
    catch (Exception exception)
    {
      // Just print an error message.
    }
    
    System.err.println("No Bundle information found");
  }
}
