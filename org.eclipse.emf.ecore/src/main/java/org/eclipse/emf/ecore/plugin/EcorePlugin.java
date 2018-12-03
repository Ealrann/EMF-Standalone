/**
 * Copyright (c) 2002-2012 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.ecore.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.URIConverter;


/**
 * A collection of platform-neutral static utilities
 * as well as Eclipse support utilities.
 */
public class EcorePlugin  extends EMFPlugin
{
  /**
   * The singleton instance of the plugin.
   */
  public static final EcorePlugin INSTANCE = new EcorePlugin();

  /**
   * Creates the singleton instance.
   */
  private EcorePlugin()
  {
    super(new ResourceLocator[] {});
  }

  /**
   * Returns a map from {@link EPackage#getNsURI() package namespace URI} (represented as a String) 
   * to the location of the GenModel containing a GenPackage for the package (represented as a {@link URI URI}).
   * <p>
   * It's implemented like this:
   *<pre>
   *  return getEPackageNsURIToGenModelLocationMap(false);
   *</pre>
   * </p>
   * @return a map from package namespace to GenModel location.
   * @deprecated since 2.9;
   * use {@link #getEPackageNsURIToGenModelLocationMap(boolean) getEPackageNsURItoGenModelLocationMap(true)}
   * to get the locations in the target platform, 
   * or use {@link #getEPackageNsURIToGenModelLocationMap(boolean) getEPackageNsURItoGenModelLocationMap(false)} to get the legacy behavior, i.e., the locations in the installed environment.
   * It's generally expected that all clients, will migrate to use the target platform.
   */
  @Deprecated
  public static Map<String, URI> getEPackageNsURIToGenModelLocationMap()
  {
    return getEPackageNsURIToGenModelLocationMap(false);
  }

  /**
   * Returns a map from {@link EPackage#getNsURI() package namespace URI} (represented as a String) 
   * to the location of the GenModel containing a GenPackage for the package (represented as a {@link URI URI})
   * for either the target platform or the environment itself.
   * If there is no target platform, i.e., if the PDE is not installed, it defaults back to the environment.
   * It's generally expected that an application using these URIs will also {@link URIConverter#getURIMap() register} the mappings returned by {@link #computePlatformURIMap(boolean)}.
   * @param targetPlatform whether to get locations for the target platform or for the environment itself; the former is preferred.
   * @return a map from package namespace to GenModel location.
   * @see #computePlatformURIMap(boolean)
   * @since 2.9
   */
  public static Map<String, URI> getEPackageNsURIToGenModelLocationMap(boolean targetPlatform)
  {
    if (ePackageNsURIToGenModelLocationMap == null)
    {
      ePackageNsURIToGenModelLocationMap = new HashMap<String, URI>();
    }
    return ePackageNsURIToGenModelLocationMap;
  }

  private static Pattern bundleSymbolNamePattern;
  private static byte [] NO_BYTES = new byte [0];

  /**
   * The map from package namespace URIs to the location of the GenModel for that package.
   * @see #getPlatformResourceMap
   */
  private static Map<String, URI> ePackageNsURIToGenModelLocationMap;

  /**
   * Determine all the available plugin.xml resources.
   */
  private static List<URI> getPluginXMLs(ClassLoader classLoader)
  {
    List<URI> result = new ArrayList<URI>();

    String classpath = null;
    try
    {
      // Try to get the classpath from the class loader.
      //
      Method method = classLoader.getClass().getMethod("getClassPath");
      if (method != null)
      {
        classpath = (String) method.invoke(classLoader);
      }
    }
    catch (Throwable throwable)
    {
      // Failing that, get it from the system properties.
      //
      classpath = System.getProperty("java.class.path");
    }

    // Keep track of whether we find any plugin.xml in the parent of a folder on the classpath, i.e., whether we're in development mode with bin folders on the classpath.
    //
    boolean nonClasspathXML = false;

    // If we have a classpath to use...
    //
    if (classpath != null)
    {
      // Split out the entries on the classpath.
      //
      for (String classpathEntry: classpath.split(File.pathSeparator))
      {
        classpathEntry = classpathEntry.trim();

        // Determine if the entry is a folder or an archive file.
        //
        File file = new File(classpathEntry);
        if (file.isDirectory())
        {
          // Determine if there is a plugin.xml at the root of the folder.
          //
          File pluginXML = new File(file, "plugin.xml");
          if (!pluginXML.exists())
          {
            // If not, check if there is one in the parent folder.
            //
            File parentFile = file.getParentFile();
            pluginXML = new File(parentFile, "plugin.xml");
            if (pluginXML.isFile())
            {
              // If there is, then we have plugin.xml files that aren't on the classpath.
              //
              nonClasspathXML = true;
            }
            else if (parentFile != null)
            {
              // The parent has a parent, check if there is one in the parent's parent folder.
              //
              pluginXML = new File(parentFile.getParentFile(), "plugin.xml");
              if (pluginXML.isFile())
              {
                // If there is, then we have plugin.xml files that aren't on the classpath.
                //
                nonClasspathXML = true;
              }
              else
              {
                // Otherwise this is bogus too.
                //
                pluginXML = null;
              }
            }
            else
            {
              // Otherwise this is bogus too.
              //
              pluginXML = null;
            }
          }

          // If we found a plugin.xml, create a URI for it.
          //
          if (pluginXML != null)
          {
            result.add(URI.createFileURI(pluginXML.getPath()));
          }
        }
        else if (file.isFile())
        {
          // The file must be a jar...
          //
          JarFile jarFile = null;
          try
          {
            // Look for a plugin.xml entry...
            //
            jarFile = new JarFile(classpathEntry);
            ZipEntry entry = jarFile.getEntry("plugin.xml");
            if (entry != null)
            {
              // If we find one, create a URI for it.
              //
              result.add(URI.createURI("archive:" + URI.createFileURI(classpathEntry) + "!/" + entry));
            }
          }
          catch (IOException exception)
          {
            // Ignore.
          }
          finally
          {
            if (jarFile != null)
            {
              try
              {
                jarFile.close();
              }
              catch (IOException exception)
              {
                INSTANCE.log(exception);
              }
            }
          }
        }
      }
    }

    // If we didn't find any non-classpath plugin.xml files, use the class loader to enumerate all the plugin.xml files.
    // This is more reliable given the possibility of specialized class loader behavior.
    //
    if (!nonClasspathXML)
    {
      result.clear();
      try
      {
        for (Enumeration<URL> resources = classLoader.getResources("plugin.xml"); resources.hasMoreElements(); )
        {
          // Create a URI for each plugin.xml found by the class loader.
          //
          URL url = resources.nextElement();
          result.add(URI.createURI(url.toURI().toString()));
        }
      }
      catch (IOException exception)
      {
        INSTANCE.log(exception);
      }
      catch (URISyntaxException exception)
      {
        INSTANCE.log(exception);
      }
    }

    return result;
  }

  @Override
  public String getSymbolicName()
  {
    return "org.eclipse.emf.ecore";
  }	

  /**
   * The default registry implementation singleton.
   */
  private static EPackage.Registry defaultRegistryImplementation; 

  /**
   * Returns the default registry implementation singleton.
   * @return the default registry implementation singleton.
   */
  public static EPackage.Registry getDefaultRegistryImplementation()
  {
    return defaultRegistryImplementation;
  }

  public static final String DYNAMIC_PACKAGE_PPID = "dynamic_package";
  public static final String GENERATED_PACKAGE_PPID = "generated_package";
  public static final String FACTORY_OVERRIDE_PPID = "factory_override";
  public static final String EXTENSION_PARSER_PPID = "extension_parser";
  public static final String PROTOCOL_PARSER_PPID = "protocol_parser";
  public static final String CONTENT_PARSER_PPID = "content_parser";
  public static final String CONTENT_HANDLER_PPID = "content_handler";
  public static final String SCHEME_PARSER_PPID = "scheme_parser";
  public static final String URI_MAPPING_PPID = "uri_mapping";
  public static final String PACKAGE_REGISTRY_IMPLEMENTATION_PPID = "package_registry_implementation";
  public static final String VALIDATION_DELEGATE_PPID = "validation_delegate";
  public static final String SETTING_DELEGATE_PPID = "setting_delegate";
  public static final String INVOCATION_DELEGATE_PPID = "invocation_delegate";
  public static final String QUERY_DELEGATE_PPID = "query_delegate";
  public static final String CONVERSION_DELEGATE_PPID = "conversion_delegate";

  /**
   * Since 2.14
   */
  public static final String ANNOTATION_VALIDATOR_PPID = "annotation_validator";
}