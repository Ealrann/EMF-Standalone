/**
 * Copyright (c) 2002-2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.common.notify.impl;


import java.util.Collection;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;


/**
 * An extensible notifier implementation.
 */
public class BasicNotifierImpl implements Notifier
{
  /**
   * Creates a blank new instance.
   */
  public BasicNotifierImpl()
  {
    super();
  }

  /**
   * An interface implemented by {@link BasicNotifierImpl#eAdapters() adapter lists}
   * that allows {@link BasicNotifierImpl.EObservableAdapterList.Listener listeners} to observe {@link Adapter adapters} being added and removed.
   * @since 2.6
   */
  public interface EObservableAdapterList
  {
    /**
     * An interface implemented by clients wishing to observe {@link Adapter adapters} being added and removed from an {@link BasicNotifierImpl#eAdapters() adapter lists}.
     * @since 2.6
     */
    interface Listener
    {
      /**
       * Called when the given adapter is added to the given notifier.
       * @param notifier the notifier being adapted.
       * @param adapter the adapter being added to the notifier.
       */
      void added(Notifier notifier, Adapter adapter);

      /**
       * Called when the given adapter is remove from the given notifier.
       * @param notifier the notifier that was being adapted.
       * @param adapter the adapter being removed from the notifier.
       */
      void removed(Notifier notifier, Adapter adapter);
    }

    /**
     * Adds another listener from the adapter list.
     * @param listener the listener being added.
     */
    void addListener(Listener listener);

    /**
     * Remove the listener from the adapter list.
     * @param listener the listener being removed.
     */
    void removeListener(Listener listener);
  }

  /**
   * An interface implemented by an adapter list that supports direct adapter lookup based on type,
   * i.e., that directly and very efficiently implements the equivalent of the follow logic.
   * <pre>
   * public static Adapter getAdapter(List&lt;Adapter> adapters, Object type)
   * {
   *   for (int i = 0, size = adapters.size(); i < size; ++i)
   *   {
   *     Adapter adapter = adapters.get(i);
   *     if (adapter.isAdapterForType(type))
   *     {
   *       return adapter;
   *     }
   *    }
   *    return null;
   *  }
   * </pre>
   * @since 2.15
   */
  public interface EScannableAdapterList
  {
    /**
     * Returns that first adapter in the list for which {@link Adapter#isAdapterForType(Object) <tt>isAdapterForType(type)</tt>} return {@code true}.
     * @param type the type of adapter to get.
     * @return the first adapter in the list for which {@code isAdapterForType(Object)} return {@code true}, or {@code null} if there isn't one.
     */
    Adapter getAdapterForType(Object type);
  }

  public static class EAdapterList<E extends Object & Adapter> extends BasicEList<E> implements EObservableAdapterList, EScannableAdapterList
  {
    private static final long serialVersionUID = 1L;

    protected Notifier notifier;

    protected Listener [] listeners;

    public EAdapterList(Notifier notifier)
    {
      this.notifier = notifier;
    }

    protected boolean safe;

    @Override
    protected boolean canContainNull()
    {
      return false;
    }

    @Override
    protected boolean useEquals()
    {
      return false;
    }

    @Override
    protected Object [] newData(int capacity)
    {
      return new Adapter [capacity];
    }

    @Override
    protected void didAdd(int index, E newObject)
    {
      if (listeners != null)
      {
        for (Listener listener : listeners)
        {
          listener.added(notifier, newObject);
        }
      }
      newObject.setTarget(notifier);
    }

    @Override
    protected void didRemove(int index, E oldObject)
    {
      if (listeners != null)
      {
        for (Listener listener : listeners)
        {
          listener.removed(notifier, oldObject);
        }
      }
      E adapter = oldObject;
      if (notifier.eDeliver())
      {
        Notification notification = 
          new NotificationImpl(Notification.REMOVING_ADAPTER, oldObject, null, index)
          {
            @Override
            public Object getNotifier()
            {
              return notifier;
            }
          };
        adapter.notifyChanged(notification);
      }
      if (adapter instanceof Adapter.Internal)
      {
        ((Adapter.Internal)adapter).unsetTarget(notifier);
      }
      else if (adapter.getTarget() == notifier) 
      {
        adapter.setTarget(null);
      }
    }

    @Override
    public Object [] data()
    {
      if (data != null && data.length != size)
      {
        if (size == 0)
        {
          data = null;
        }
        else
        {
          Object [] oldData = data;
          Object [] newData = newData(size);
          System.arraycopy(oldData, 0, newData, 0, size);
          data = newData;
        }
      }
      safe = true;
      return data;
    }

    protected void ensureSafety()
    {
      if (safe && data != null)
      {
        Object [] oldData = data;
        Object [] newData = newData(oldData.length);
        System.arraycopy(oldData, 0, newData, 0, size);
        data = newData;
        safe = false;
      }
    }

    @Override
    public boolean add(E object)
    {
      ensureSafety();
      return super.add(object);
    }

    @Override
    public void add(int index, E object)
    {
      ensureSafety();
      super.add(index, object);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection)
    {
      ensureSafety();
      return super.addAll(collection);
    }

    @Override
    public boolean remove(Object object)
    {
      ensureSafety();
      return super.remove(object);
    }

    @Override
    public E remove(int index)
    {
      ensureSafety();
      return super.remove(index);
    }

    @Override
    public boolean removeAll(Collection<?> collection)
    {
      ensureSafety();
      return super.removeAll(collection);
    }

    @Override
    public void clear()
    {
      ensureSafety();
      super.clear();
    }

    @Override
    public boolean retainAll(Collection<?> collection)
    {
      ensureSafety();
      return super.retainAll(collection);
    }

    @Override
    public E set(int index, E object)
    {
      ensureSafety();
      return super.set(index, object);
    }

    @Override
    public void move(int newPosition, E object)
    {
      ensureSafety();
      super.move(newPosition, object);
    }

    @Override
    public E move(int newPosition, int oldPosition)
    {
      ensureSafety();
      return super.move(newPosition, oldPosition);
    }

    public void addListener(Listener listener)
    {
      if (listeners == null)
      {
        listeners = new Listener [] { listener };
      }
      else
      {
        Listener[] newListeners = new Listener[listeners.length + 1];
        System.arraycopy(listeners, 0, newListeners, 0, listeners.length);
        newListeners[listeners.length] = listener;
        listeners = newListeners;
      }
    }

    public void removeListener(Listener listener)
    {
      if (listeners != null)
      {
        for (int i = 0; i < listeners.length; ++i)
        {
          if (listeners[i] == listener)
          {
            if (listeners.length == 1)
            {
              listeners = null;
            }
            else
            {
              Listener[] newListeners = new Listener[listeners.length - 1];
              System.arraycopy(listeners, 0, newListeners, 0, i);
              if (i != newListeners.length)
              {
                System.arraycopy(listeners, i + 1, newListeners, i, newListeners.length - i);
              } 
              listeners = newListeners;
            }
            break;
          }
        }
      }
    }

    /**
     * @since 2.15
     */
    public Adapter getAdapterForType(Object type)
    {
      Adapter[] adapters = (Adapter[])data();
      if (adapters != null)
      {
        for (Adapter adapter : adapters)
        {
          if (adapter.isAdapterForType(type))
          {
            return adapter;
          }
        }
      }
      return null;
     }
  }

  public EList<Adapter> eAdapters()
  {
    return ECollections.emptyEList();
  }

  /**
   * Returns the adapter list, even if it is <code>null</code>.
   * @return the adapter list, even if it is <code>null</code>.
   */
  protected BasicEList<Adapter> eBasicAdapters()
  {
    return null;
  }

  /**
   * Returns the underlying array of adapters.
   * The length of this array reflects exactly the number of adapters
   * where <code>null</code> represents the lack of any adapters.
   * This array may not be modified by the caller 
   * and must be guaranteed not to be modified even if the {@link #eAdapters() list of adapters} is modified.
   * @return the underlying array of adapters.
   */
  protected Adapter[] eBasicAdapterArray()
  {
    BasicEList<Adapter> eBasicAdapters = eBasicAdapters();
    return eBasicAdapters == null ? null : (Adapter[])eBasicAdapters.data();
  }

  /**
   * Returns whether there are any adapters.
   * @return whether there are any adapters.
   */
  protected boolean eBasicHasAdapters()
  {
    BasicEList<Adapter> eBasicAdapters = eBasicAdapters();
    return eBasicAdapters != null && !eBasicAdapters.isEmpty();
  }

  /*
   * Javadoc copied from interface.
   */
  public boolean eDeliver()
  {
    return false;
  }

  /*
   * Javadoc copied from interface.
   */
  public void eSetDeliver(boolean deliver)
  {
    throw new UnsupportedOperationException();
  }

  /*
   * Javadoc copied from interface.
   */
  public void eNotify(Notification notification)
  {
    Adapter[] eAdapters = eBasicAdapterArray();
    if (eAdapters != null && eDeliver())
    {
      for (int i = 0, size = eAdapters.length; i < size; ++i)
      {
        eAdapters[i].notifyChanged(notification);
      }
    }
  }

  /**
   * Returns whether {@link #eNotify eNotify} needs to be called.
   * This may return <code>true</code> even when {@link #eDeliver eDeliver} is <code>false</code>
   * or when {@link #eAdapters eAdapters} is empty.
   * @return whether {@link #eNotify eNotify} needs to be called.
   */
  public boolean eNotificationRequired()
  {
    return eBasicHasAdapters() && eDeliver();
  }
}
