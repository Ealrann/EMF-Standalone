/**
 * Copyright (c) 2002-2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.common.command;


import java.util.EventObject;


/**
 * A listener to a {@link org.eclipse.emf.common.command.CommandStack}.
 */ 
public interface CommandStackListener
{
  /**
   * Called when the {@link org.eclipse.emf.common.command.CommandStack}'s state has changed.
   * @param event the event.
   */
  void commandStackChanged(EventObject event);
}
