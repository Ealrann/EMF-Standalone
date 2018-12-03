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


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ContentHandler;


/**
 * An implementation of a content handler.
 */
public class ContentHandlerImpl implements ContentHandler
{
  /**
   * Creates a map with a single entry from {@link ContentHandler#VALIDITY_PROPERTY} to the given validity value.
   * @param validity the value of the validity property.
   * @return a map with a single entry from {@link ContentHandler#VALIDITY_PROPERTY} to the given validity value.
   */
  public static Map<String, Object> createContentDescription(Validity validity)
  {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(VALIDITY_PROPERTY, validity);
    return result;
  }

  /**
   * Creates an instance.
   */
  public ContentHandlerImpl()
  {
    super();
  }

  /**
   * Returns the value of {@link ContentHandler#OPTION_REQUESTED_PROPERTIES} in the options map.
   * @param options the options in which to look up the property.
   * @return value of {@link ContentHandler#OPTION_REQUESTED_PROPERTIES} in the options map.
   */
  @SuppressWarnings("unchecked")
  protected Set<String> getRequestedProperties(Map<?, ?> options)
  {
    return (Set<String>)options.get(OPTION_REQUESTED_PROPERTIES);
  }

  /**
   * Returns whether the named property is one requested in the options.
   * @param property the property in question.
   * @param options the options in which to look for the requested property.
   * @return whether the named property is one requested in the options.
   * @see #getRequestedProperties(Map)
   */
  protected boolean isRequestedProperty(String property, Map<?, ?> options)
  {
    if (ContentHandler.VALIDITY_PROPERTY.equals(property) || ContentHandler.CONTENT_TYPE_PROPERTY.equals(property))
    {
      return  true;
    }
    else
    {
      Set<String> requestedProperties = getRequestedProperties(options);
      if (requestedProperties == null)
      {
        return true;
      }
      else
      {
        return requestedProperties.contains(property);
      }
    }
  }

  /**
   * This implementations always return true; clients are generally expected to override this.
   * @param uri the URI in questions.
   * @return true;
   */
  public boolean canHandle(URI uri)
  {
    return true;
  }

  /**
   * This base implementation handles computing the {@link ContentHandler#BYTE_ORDER_MARK_PROPERTY}, 
   * the {@link ContentHandler#CHARSET_PROPERTY character set property},
   * and the {@link ContentHandler#LINE_DELIMITER_PROPERTY line delimiter property}.
   * for each such {@link #isRequestedProperty(String, Map) requested property}.
   */
  public Map<String, Object> contentDescription(URI uri, InputStream inputStream, Map<?, ?> options, Map<Object, Object> context) throws IOException
  {
    Map<String, Object> result = createContentDescription(ContentHandler.Validity.INDETERMINATE);
    if (isRequestedProperty(ContentHandler.BYTE_ORDER_MARK_PROPERTY, options))
    {
      ByteOrderMark byteOrderMark = getByteOrderMark(uri, inputStream, options, context);
      if (byteOrderMark != null)
      {
        result.put(ContentHandler.BYTE_ORDER_MARK_PROPERTY, byteOrderMark);
      }
    }
    if (isRequestedProperty(ContentHandler.CHARSET_PROPERTY, options))
    {
      String charset = getCharset(uri, inputStream, options, context);
      if (charset != null)
      {
        result.put(ContentHandler.CHARSET_PROPERTY, charset);
      }
    }
    if (isRequestedProperty(ContentHandler.LINE_DELIMITER_PROPERTY, options))
    {
      String lineDelimiter = getLineDelimiter(uri, inputStream, options, context);
      if (lineDelimiter != null)
      {
        result.put(ContentHandler.LINE_DELIMITER_PROPERTY, lineDelimiter);
      }
    }
    return result;
  }

  /**
   * Returns the character set of the input stream; this implementation simply returns null.
   * @param uri the URI of the input stream.
   * @param inputStream the input stream.
   * @param options any options that might influence the interpretation of the content.
   * @param context a cache for previously computed information.
   * @return the character set of the input stream.
   * @throws IOException if there is a problem loading the content.
   * @since 2.9
   */
  protected String getCharset(URI uri, InputStream inputStream, Map<?, ?> options, Map<Object, Object> context) throws IOException
  {
    return null;
  }

  /**
   * Returns the line delimiter of the input stream; it's computed from the bytes interpreted using the {@link #getCharset(URI, InputStream, Map, Map) appropriate character set}.
   * @param uri the URI of the input stream.
   * @param inputStream the input stream.
   * @param options any options that might influence the interpretation of the content.
   * @param context a cache for previously computed information.
   * @return the line delimiter of the input stream.
   * @throws IOException if there is a problem loading the content.
   * @since 2.9
   */
  protected String getLineDelimiter(URI uri, InputStream inputStream, Map<?, ?> options, Map<Object, Object> context) throws IOException
  {
    String result = (String)context.get(ContentHandler.LINE_DELIMITER_PROPERTY);
    if (result == null)
    {
      String charset = getCharset(uri, inputStream, options, context);
      if (charset != null)
      {
        result = getLineDelimiter(inputStream, charset);
        if (result != null)
        {
          context.put(ContentHandler.LINE_DELIMITER_PROPERTY, result);
        }
      }
    }
    return result;
  }

  /**
   * Returns the line delimiter of the input stream interpreted using the specified character set.
   * It is the caller's responsibility to close the input stream.
   * @since 2.9
   */
  public static String getLineDelimiter(InputStream inputStream, String charset) throws IOException
  {
    @SuppressWarnings("resource")
    Reader reader = charset == null ? new InputStreamReader(inputStream) : new InputStreamReader(inputStream, charset);
    char [] text = new char [4048];
    char target = 0;
    for (int count = reader.read(text); count > -1; count = reader.read(text))
    {
      for (int i = 0; i < count; ++i)
      {
        char character = text[i];
        if (character == '\n')
        {
          if (target == '\n')
          {
            return "\n";
          }
          else if (target == '\r')
          {
            return "\r\n";
          }
          else
          {
            target = '\n';
          }
        }
        else if (character == '\r')
        {
          if (target == '\n')
          {
            return "\n\r";
          }
          else if (target == '\r')
          {
            return "\r";
          }
          else
          {
            target = '\r';
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns the byte order marker at the start of the input stream.
   * @param uri the URI of the input stream.
   * @param inputStream the input stream to scan.
   * @param options any options to influence the behavior; this base implementation ignores this.
   * @param context the cache for fetching and storing a previous computation of the byte order marker; this base implementation caches {@link ContentHandler#BYTE_ORDER_MARK_PROPERTY}.
   * @return the byte order marker at the start of the input stream.
   * @throws IOException
   */
  protected ByteOrderMark getByteOrderMark(URI uri, InputStream inputStream, Map<?, ?> options, Map<Object, Object> context) throws IOException
  {
    ByteOrderMark result = (ByteOrderMark)context.get(ContentHandler.BYTE_ORDER_MARK_PROPERTY);
    if (result == null)
    {
      result = ByteOrderMark.read(inputStream);
      inputStream.reset();
      context.put(ContentHandler.BYTE_ORDER_MARK_PROPERTY, result);
    }
    return result;
  }
}
