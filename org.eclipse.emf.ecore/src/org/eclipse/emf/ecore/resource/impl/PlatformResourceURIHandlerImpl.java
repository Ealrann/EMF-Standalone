/**
 * Copyright (c) 2007-2012 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.ecore.resource.impl;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;


public class PlatformResourceURIHandlerImpl extends URIHandlerImpl
{
  /**
   * An output stream that transfers its contents to an {@link IFile} upon closing.
   */
  public static class PlatformResourceOutputStream extends ByteArrayOutputStream
  {
    protected IFile file;
    protected boolean force;
    protected boolean keepHistory;
    protected IProgressMonitor progressMonitor;
    protected boolean previouslyFlushed;

    public PlatformResourceOutputStream(IFile file,  boolean force, boolean keepHistory, IProgressMonitor progressMonitor)
    {
      this.file = file;
      this.force = force;
      this.keepHistory = keepHistory;
      this.progressMonitor = progressMonitor;
    }

    protected void createContainer(IContainer container) throws IOException
    {
      if (!container.exists())
      {
        if (container.getType() == IResource.FOLDER)
        {
          createContainer(container.getParent());
          try
          {
            ((IFolder)container).create(force, keepHistory, progressMonitor);
          }
          catch (CoreException exception)
          {
            throw new ResourceImpl.IOWrappedException(exception);
          }
        }
      }
    }

    @Override
    public void close() throws IOException
    {
      flush();
      super.close();
    }

    @Override
    public void flush() throws IOException
    {
      super.flush();

      if (previouslyFlushed)
      {
        if (count == 0)
        {
          return;
        }
      }
      else
      {
        createContainer(file.getParent());
      }

      byte[] contents = toByteArray();
      InputStream inputStream = new ByteArrayInputStream(contents, 0, contents.length);

      try
      {
        if (previouslyFlushed)
        {
          file.appendContents(inputStream, force, false, progressMonitor);
        }
        else
        {
          if (!file.isSynchronized(IResource.DEPTH_ONE))
          {
            file.refreshLocal(IResource.DEPTH_ONE, progressMonitor);
          }

          if (!file.exists())
          {
            file.create(inputStream, false, null);
            previouslyFlushed = true;
          }
          else
          {
            file.setContents(inputStream, force, keepHistory, progressMonitor);
            previouslyFlushed = true;
          }
        }
        reset();
      }
      catch (CoreException exception)
      {
        throw new Resource.IOWrappedException(exception);
      }
    }
  }

  /**
   * Isolated Eclipse workbench utilities.
   */
  public static class WorkbenchHelper
  {
    /**
     * Creates an output stream for the given {@link IFile} path.
     * <p>
     * This implementation uses a {@link PlatformResourceURIHandlerImpl.PlatformResourceOutputStream}.
     * </p>
     * @return an open output stream.
     * @exception IOException if there is a problem obtaining an open output stream.
     * @see IWorkspaceRoot#getFile(org.eclipse.core.runtime.IPath)
     * @see PlatformResourceURIHandlerImpl.PlatformResourceOutputStream
     * @see IFile#setContents(InputStream, boolean, boolean, IProgressMonitor)
     */
    public static OutputStream createPlatformResourceOutputStream(String platformResourcePath, Map<?, ?> options) throws IOException
    {
      IFile file = getWorkspaceRoot().getFile(new Path(platformResourcePath));
      @SuppressWarnings("unchecked")
      final Map<Object, Object> response = options == null ? null : (Map<Object, Object>)options.get(URIConverter.OPTION_RESPONSE);
      return
        new PlatformResourceOutputStream(file, false, true, null)
        {
          @Override
          public void close() throws IOException
          {
            try
            {
              super.close();
            }
            finally
            {
              if (response != null)
              {
                response.put(URIConverter.RESPONSE_TIME_STAMP_PROPERTY, file.getLocalTimeStamp());
              }
            }
          }
        };
    }

    /**
     * Creates an input stream for the given {@link IFile} path.
     * <p>
     * This implementation uses {@link IFile#getContents() IFile.getContents}.
     * </p>
     * @return an open input stream.
     * @see IWorkspaceRoot#getFile(org.eclipse.core.runtime.IPath)
     * @see IFile#getContents()
     * @exception IOException if there is a problem obtaining an open input stream.
     */
    public static InputStream createPlatformResourceInputStream(String platformResourcePath, Map<?, ?> options) throws IOException
    {
      IFile file = getWorkspaceRoot().getFile(new Path(platformResourcePath));
      try
      {
        if (!file.isSynchronized(IResource.DEPTH_ONE))
        {
          file.refreshLocal(IResource.DEPTH_ONE, null);
        }
        InputStream result = file.getContents();
        if (options != null)
        {
          @SuppressWarnings("unchecked")
          Map<Object, Object> response = (Map<Object, Object>)options.get(URIConverter.OPTION_RESPONSE);
          if (response != null)
          {
            response.put(URIConverter.RESPONSE_TIME_STAMP_PROPERTY, file.getLocalTimeStamp());
          }
        }
        return result;
      }
      catch (CoreException exception)
      {
        throw new Resource.IOWrappedException(exception);
      }
    }

    public static void delete(String platformResourcePath, Map<?, ?> options) throws IOException
    {
      IFile file = getWorkspaceRoot().getFile(new Path(platformResourcePath));
      try
      {
        file.delete(true, null);
      }
      catch (CoreException exception)
      {
        throw new Resource.IOWrappedException(exception);
      }
    }

    public static boolean exists(String platformResourcePath, Map<?, ?> options)
    {
      IResource resource = getWorkspaceRoot().findMember(new Path(platformResourcePath));
      return resource != null && resource.getResourceAttributes() != null;
    }

    public static Map<String, ?> attributes(String platformResourcePath, Map<?, ?> options)
    {
      IResource resource = getWorkspaceRoot().findMember(new Path(platformResourcePath));
      Map<String, Object> result = new HashMap<String, Object>();
      if (resource != null)
      {
        @SuppressWarnings("unchecked")
        Set<String> requestedAttributes = options == null ? null : (Set<String>)options.get(URIConverter.OPTION_REQUESTED_ATTRIBUTES);

        if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_TIME_STAMP))
        {
          result.put(URIConverter.ATTRIBUTE_TIME_STAMP,  resource.getLocalTimeStamp());
        }
        ResourceAttributes resourceAttributes = null;
        if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_READ_ONLY))
        {
          resourceAttributes = resource.getResourceAttributes();
          if (resourceAttributes == null)
          {
            return result;
          }
          result.put(URIConverter.ATTRIBUTE_READ_ONLY,  resourceAttributes.isReadOnly());
        }
        if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_ARCHIVE))
        {
          if (resourceAttributes == null)
          {
            resourceAttributes = resource.getResourceAttributes();
            if (resourceAttributes == null)
            {
              return result;
            }
          }
          result.put(URIConverter.ATTRIBUTE_ARCHIVE,  resourceAttributes.isArchive());
        }
        if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_EXECUTABLE))
        {
          if (resourceAttributes == null)
          {
            resourceAttributes = resource.getResourceAttributes();
            if (resourceAttributes == null)
            {
              return result;
            }
          }
          result.put(URIConverter.ATTRIBUTE_EXECUTABLE,  resourceAttributes.isExecutable());
        }
        if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_HIDDEN))
        {
          if (resourceAttributes == null)
          {
            resourceAttributes = resource.getResourceAttributes();
            if (resourceAttributes == null)
            {
              return result;
            }
          }
          result.put(URIConverter.ATTRIBUTE_HIDDEN,  resourceAttributes.isHidden());
        }
        if (requestedAttributes == null || requestedAttributes.contains(URIConverter.ATTRIBUTE_DIRECTORY))
        {
          if (resourceAttributes == null)
          {
            resourceAttributes = resource.getResourceAttributes();
            if (resourceAttributes == null)
            {
              return result;
            }
          }
          result.put(URIConverter.ATTRIBUTE_DIRECTORY, resource instanceof IContainer);
        }
      }
      return result;
    }

    public static void updateAttributes(String platformResourcePath, Map<String, ?> attributes, Map<?, ?> options) throws IOException
    {
      IResource resource = getWorkspaceRoot().findMember(new Path(platformResourcePath));
      if (resource == null)
      {
        throw new FileNotFoundException("The resource " + platformResourcePath + " does not exist");
      }
      else
      {
        try
        {
          Long timeStamp = (Long)attributes.get(URIConverter.ATTRIBUTE_TIME_STAMP);
          if (timeStamp != null)
          {
            resource.setLocalTimeStamp(timeStamp);
          }

          ResourceAttributes resourceAttributes = null;
          Boolean readOnly = (Boolean)attributes.get(URIConverter.ATTRIBUTE_READ_ONLY);
          if (readOnly != null)
          {
            resourceAttributes = resource.getResourceAttributes();
            if (resourceAttributes == null)
            {
              return;
            }
            resourceAttributes.setReadOnly(readOnly);
          }
          Boolean archive = (Boolean)attributes.get(URIConverter.ATTRIBUTE_ARCHIVE);
          if (archive != null)
          {
            if (resourceAttributes == null)
            {
              resourceAttributes = resource.getResourceAttributes();
              if (resourceAttributes == null)
              {
                return;
              }
            }
            resourceAttributes.setArchive(archive);
          }
          Boolean executable =  (Boolean)attributes.get(URIConverter.ATTRIBUTE_EXECUTABLE);
          if (executable != null)
          {
            if (resourceAttributes == null)
            {
              resourceAttributes = resource.getResourceAttributes();
              if (resourceAttributes == null)
              {
                return;
              }
            }
            resourceAttributes.setExecutable(executable);
          }
          Boolean hidden = (Boolean)attributes.get(URIConverter.ATTRIBUTE_HIDDEN);
          if (hidden != null)
          {
            if (resourceAttributes == null)
            {
              resourceAttributes = resource.getResourceAttributes();
              if (resourceAttributes == null)
              {
                return;
              }
            }
            resourceAttributes.setHidden(hidden);
          }

          if (resourceAttributes != null)
          {
            resource.setResourceAttributes(resourceAttributes);
          }
        }
        catch (CoreException exception)
        {
          throw new Resource.IOWrappedException(exception);
        }
      }
    }

    public static IContentDescription getContentDescription(String platformResourcePath, Map<?, ?> options) throws IOException
    {
      IFile file = getWorkspaceRoot().getFile(new Path(platformResourcePath));
      try
      {
        return file.getContentDescription();
      }
      catch (CoreException exception)
      {
        throw new Resource.IOWrappedException(exception);
      }
    }

    /**
     * Returns the {@link IFile#getCharset() character set} for the file at the specified location.
     * @since 2.9
     */
    public static String getCharset(String platformResourcePath, Map<?, ?> options) throws IOException
    {
      IFile file = getWorkspaceRoot().getFile(new Path(platformResourcePath));
      try
      {
        return file.getCharset();
      }
      catch (CoreException exception)
      {
        throw new Resource.IOWrappedException(exception);
      }
    }

    @SuppressWarnings("deprecation")
    private static final InstanceScope INSTANCE_SCOPE = new InstanceScope();

    private static final String SYSTEM_PROPERTY_LINE_SEPARATOR = System.getProperty(Platform.PREF_LINE_SEPARATOR);

    /**
     * Returns the project or workspace line delimiter preference for a new workspace file at the specified location.
     * @since 2.9
     */
    public static String getLineDelimiter(String platformResourcePath, Map<?, ?> options)
    {
      IFile file = getWorkspaceRoot().getFile(new Path(platformResourcePath));
      IProject project = file.getProject();
      return 
        Platform.getPreferencesService().getString
          (Platform.PI_RUNTIME, 
           Platform.PREF_LINE_SEPARATOR, 
           SYSTEM_PROPERTY_LINE_SEPARATOR, 
           new IScopeContext[] { new ProjectScope(project), INSTANCE_SCOPE });
    }
  }

  /**
   * The cached Eclipse workspace root.
   * @deprecated use {@link #getWorkspaceRoot()} instead.
   */
  protected static IWorkspaceRoot workspaceRoot = getWorkspaceRoot();

  /**
   * Whether {@link #cachedWorkspaceRoot} is initialized.
   */
  private static boolean cachedWorkspaceRootInitialized;

  /**
   * The cached Eclipse workspace root returned by {@link #getWorkspaceRoot()}.
   */
  private static IWorkspaceRoot cachedWorkspaceRoot;

  /**
   * Returns the Eclipse workspace root,
   * except in the case that the resource bundle is not available or the platform has not yet initialized in instance location,
   * in which case it returns {@code null}.
   * @return the workspace root.
   * @since 2.21
   */
  protected static IWorkspaceRoot getWorkspaceRoot()
  {
    if (!cachedWorkspaceRootInitialized)
    {
      try
      {
        // If the resource bundle isn't available, we will always return null for this method.
        if (EMFPlugin.IS_RESOURCES_BUNDLE_AVAILABLE)
        {
          // This will throw an exception if the instance location is not yet initialized,
          // i.e., when EMF is used by some component before the the user chooses the workspace location.
          // In this case we will return null and try again later to initialize the cached workspace root instance.
          EcorePlugin.getPlugin().getStateLocation();
          cachedWorkspaceRoot = workspaceRoot = EcorePlugin.getWorkspaceRoot();
        }
        cachedWorkspaceRootInitialized = true;
      }
      catch (Exception exception)
      {
        // Ignore.
      }
    }
    return cachedWorkspaceRoot;
  }

  /**
   * Creates an instance.
   */
  public PlatformResourceURIHandlerImpl()
  {
    super();
  }

  @Override
  public boolean canHandle(URI uri)
  {
    return uri.isPlatformResource();
  }

  /**
   * Creates an output stream for the platform resource path and returns it.
   * <p>
   * This implementation does one of two things, depending on the runtime environment.
   * If there is an Eclipse workspace, it delegates to
   * {@link WorkbenchHelper#createPlatformResourceOutputStream WorkbenchHelper.createPlatformResourceOutputStream},
   * which gives the expected Eclipse behaviour.
   * Otherwise, the {@link EcorePlugin#resolvePlatformResourcePath resolved} URI
   * is delegated to {@link #createOutputStream createOutputStream}
   * for recursive processing.
   * @return an open output stream.
   * @exception IOException if there is a problem obtaining an open output stream or a valid interpretation of the path.
   * @see EcorePlugin#resolvePlatformResourcePath(String)
   */
  @Override
  public OutputStream createOutputStream(URI uri, Map<?, ?> options) throws IOException
  {
    String platformResourcePath = uri.toPlatformString(true);
    if (getWorkspaceRoot() != null)
    {
      return WorkbenchHelper.createPlatformResourceOutputStream(platformResourcePath, options);
    }
    else
    {
      URI resolvedLocation = EcorePlugin.resolvePlatformResourcePath(platformResourcePath);
      if (resolvedLocation != null)
      {
        return ((URIConverter)options.get(URIConverter.OPTION_URI_CONVERTER)).createOutputStream(resolvedLocation, options);
      }

      throw new IOException("The path '" + platformResourcePath + "' is unmapped");
    }
  }

  /**
   * Creates an input stream for the platform resource path and returns it.
   * <p>
   * This implementation does one of two things, depending on the runtime environment.
   * If there is an Eclipse workspace, it delegates to
   * {@link WorkbenchHelper#createPlatformResourceInputStream WorkbenchHelper.createPlatformResourceInputStream},
   * which gives the expected Eclipse behaviour.
   * Otherwise, the {@link EcorePlugin#resolvePlatformResourcePath resolved} URI
   * is delegated to {@link #createInputStream createInputStream}
   * for recursive processing.
   * @return an open input stream.
   * @exception IOException if there is a problem obtaining an open input stream or a valid interpretation of the path.
   * @see EcorePlugin#resolvePlatformResourcePath(String)
   */
  @Override
  public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException
  {
    String platformResourcePath = uri.toPlatformString(true);
    if (getWorkspaceRoot() != null)
    {
      return WorkbenchHelper.createPlatformResourceInputStream(platformResourcePath, options);
    }
    else
    {
      URI resolvedLocation = EcorePlugin.resolvePlatformResourcePath(platformResourcePath);
      if (resolvedLocation != null)
      {
        return getURIConverter(options).createInputStream(resolvedLocation, options);
      }

      throw new IOException("The path '" + platformResourcePath + "' is unmapped");
    }
  }

  @Override
  public void delete(URI uri, Map<?, ?> options) throws IOException
  {
    String platformResourcePath = uri.toPlatformString(true);
    if (getWorkspaceRoot() != null)
    {
      WorkbenchHelper.delete(platformResourcePath, options);
    }
    else
    {
      URI resolvedLocation = EcorePlugin.resolvePlatformResourcePath(platformResourcePath);
      if (resolvedLocation != null)
      {
        getURIConverter(options).delete(resolvedLocation, options);
      }
      else
      {
        throw new IOException("The path '" + platformResourcePath + "' is unmapped");
      }
    }
  }

  @Override
  public boolean exists(URI uri, Map<?, ?> options)
  {
    String platformResourcePath = uri.toPlatformString(true);
    if (getWorkspaceRoot() != null)
    {
      return WorkbenchHelper.exists(platformResourcePath, options);
    }
    else
    {
      URI resolvedLocation = EcorePlugin.resolvePlatformResourcePath(platformResourcePath);
      return resolvedLocation != null && getURIConverter(options).exists(resolvedLocation, options);
    }
  }

  @Override
  public Map<String, ?> getAttributes(URI uri, Map<?, ?> options)
  {
    String platformResourcePath = uri.toPlatformString(true);
    if (getWorkspaceRoot() != null)
    {
      return WorkbenchHelper.attributes(platformResourcePath, options);
    }
    else
    {
      URI resolvedLocation = EcorePlugin.resolvePlatformResourcePath(platformResourcePath);
      return resolvedLocation == null ? Collections.<String, Object>emptyMap() : getURIConverter(options).getAttributes(resolvedLocation, options);
    }
  }

  @Override
  public void setAttributes(URI uri, Map<String, ?> attributes, Map<?, ?> options) throws IOException
  {
    String platformResourcePath = uri.toPlatformString(true);
    if (getWorkspaceRoot() != null)
    {
      WorkbenchHelper.updateAttributes(platformResourcePath, attributes, options);
    }
    else
    {
      URI resolvedLocation = EcorePlugin.resolvePlatformResourcePath(platformResourcePath);
      if (resolvedLocation != null)
      {
        getURIConverter(options).setAttributes(resolvedLocation, attributes, options);
      }
      else
      {
        throw new IOException("The platform resource path '" + platformResourcePath + "' does not resolve");
      }
    }
  }
}
