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


import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.emf.common.util.ResourceLocator;


/**
 * The <b>Plugin</b> for the model EMF.Common library.
 * EMF must run 
 * within an Eclipse workbench,
 * within a headless Eclipse workspace,
 * or just stand-alone as part of some other application.
 * To support this, all resource access should be directed to the resource locator,
 * which can redirect the service as appropriate to the runtime.
 * During stand-alone invocation no plugin initialization takes place.
 * In this case, common.resources.jar must be on the CLASSPATH.
 * @see #INSTANCE
 */
public final class CommonPlugin extends EMFPlugin 
{
  /**
   * The singleton instance of the plugin.
   */
  public static final CommonPlugin INSTANCE = new CommonPlugin();

  /**
   * Creates the singleton instance.
   */
  private CommonPlugin()
  {
    super(new ResourceLocator[] {});
  }

  /**
   * Use the platform, if available, to load the named class using the right class loader.
   */
  public static Class<?> loadClass(String pluginID, String className) throws ClassNotFoundException
  {
    return Class.forName(className);
  }

  private static final Method COLLATOR_GET_INSTANCE_METHOD;
  static
  {
    Method collatorGetInstanceMethod = null;
    try
    {
      Class<?> collatorClass = loadClass("com.ibm.icu", "com.ibm.icu.text.Collator");
      collatorGetInstanceMethod = collatorClass.getMethod("getInstance", Locale.class);
    }
    catch (Throwable throwable)
    {
      // Assume the class is not available.
    }
    COLLATOR_GET_INSTANCE_METHOD = collatorGetInstanceMethod;
  }

  /**
   * Returns a string comparator appropriate for collating strings for the {@link Locale#getDefault() current locale}.
   * @return a string comparator appropriate for collating strings for the {@link Locale#getDefault() current locale}.
   */
  public Comparator<String> getComparator()
  {
    return getComparator(Locale.getDefault());
  }

  /**
   * Returns a string comparator appropriate for collating strings for the give locale.
   * This will use ICU, when available that plug-in is available, or {@link Collator} otherwise.
   * @param locale the locale for which a comparator is needed.
   * @return a string comparator appropriate for collating strings for the give locale.
   */
  @SuppressWarnings("unchecked")
  public Comparator<String> getComparator(Locale locale)
  {
    if (COLLATOR_GET_INSTANCE_METHOD != null)
    {
      try
      {
        return (Comparator<String>)COLLATOR_GET_INSTANCE_METHOD.invoke(null, locale);
      }
      catch (Throwable eception)
      {
        // Just return the default.
      }
    }
    return (Comparator<String>)(Comparator<?>)Collator.getInstance(locale);
  }

  /**
   * A specialized {@link HashMap} map that supports {@link #getTargetPlatformValues(String,String) computing} information from the target platform, if the PDE is available.
   * It is abstract because the {@link #createKey(String)} method must be specialized to convert each attribute's string value to a value of the map's key type.
   * 
   * @param <K> the type of the key.
   * @param <V> the type of the value.
   *
   * @since 2.14
   */
  public static abstract class SimpleTargetPlatformRegistryImpl<K, V> extends HashMap<K, V>
  {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an instance.
     */
    public SimpleTargetPlatformRegistryImpl()
    {
    }

    /**
     * Returns the set of values computed by {@link CommonPlugin#getTargetPlatformExtensionPoints(Set) fetching} all the given extension points 
     * and {@link ElementRecord#getAttributes() looking up} the given attribute name in each {@link ElementRecord#getChildren() element} of that extension point.
     * Each attribute value is {@link #createKey(String) converted} to a value of the map's key type.
     * @param extensionPoint the qualified extension point name.
     * @param attributeName the attribute name to query.
     * @return the values computed from the target platform, if the Plug-in Development Environment is available, or a copy of the {@link #keySet()} otherwise.
     * @see #createKey(String)
     */
    protected Set<K> getTargetPlatformValues(String extensionPoint, String attributeName)
    {
      return new LinkedHashSet<K>(keySet());
    }

    /**
     * Returns the attribute value converted to a value of the key type.
     * @param attribute the attribute value.
     * @return the attribute value converted to a value of the key type.
     */
    protected abstract K createKey(String attribute);
  }

  /**
   * A simple representation of an element in a {@code plugin.xml}.
   * It has the obvious things you'd expect of an element, i.e., a name, attributes, and children.
   *
   * @since 2.14
   */
  public static final class ElementRecord
  {
    /**
     * The element name.
     */
    private final String name;

    /**
     * The attributes of the element.
     */
    private final Map<String, String> attributes = new TreeMap<String, String>();

    /**
     * The children of the element.
     */
    private final List<ElementRecord> children = new ArrayList<ElementRecord>();

    /**
     * Creates an element with the given name.
     * 
     * @param name the name of the element.
     */
    private ElementRecord(String name)
    {
      this.name = name;
    }

    /**
     * Returns the name of the element.
     * @return the name of the element.
     */
    public String getName()
    {
      return name;
    }

    /**
     * Returns the attributes of the element.
     * @return the attributes of the element.
     */
    public Map<String, String> getAttributes()
    {
      return Collections.unmodifiableMap(attributes);
    }

    /**
     * Returns the children elements.
     * @return the children elements.
     */
    public List<ElementRecord> getChildren()
    {
      return Collections.unmodifiableList(children);
    }

    /**
     * A helpful override for debugging.
     */
    @Override
    public String toString()
    {
      return "" + name + "attributes= " + attributes + " children=" + children;
    }
  }
}
