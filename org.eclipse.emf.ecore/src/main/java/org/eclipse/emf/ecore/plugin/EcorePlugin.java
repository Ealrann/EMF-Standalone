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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;


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

  /**
   * The map from package namespace URIs to the location of the GenModel for that package.
   * @see #getPlatformResourceMap
   */
  private static Map<String, URI> ePackageNsURIToGenModelLocationMap;

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