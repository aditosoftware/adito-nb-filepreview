package de.adito.aditoweb.nbm.jspreview;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import org.jetbrains.annotations.*;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.beans.*;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Facade to access all relevant information from the netbeans property sheet
 *
 * @author w.glanzer, 14.10.2022
 */
public class PropertySheetConnector implements Disposable
{

  private final BehaviorSubject<Optional<Object>> currentSelection = BehaviorSubject.createDefault(Optional.empty());
  private final PropertyChangeListener propertySheetHackListener;
  private final ListSelectionListener tableListener;
  private JTable propertySheetTable;

  public PropertySheetConnector()
  {
    // listener on property sheet table
    tableListener = e -> {
      if (propertySheetTable != null)
        currentSelection.onNext(Optional.ofNullable(propertySheetTable.getValueAt(propertySheetTable.getSelectedRow(), 0)));
    };

    // listener on property sheet topComponent, with initial values
    propertySheetHackListener = new _PropertySheetChangeListener();
    TopComponent.getRegistry().getOpened().forEach(pTc -> propertySheetHackListener
        .propertyChange(new PropertyChangeEvent(this, TopComponent.Registry.PROP_TC_OPENED, null, pTc)));
    TopComponent.getRegistry().addPropertyChangeListener(WeakListeners.propertyChange(propertySheetHackListener, TopComponent.getRegistry()));
  }

  @Override
  public void dispose()
  {
    TopComponent.getRegistry().removePropertyChangeListener(propertySheetHackListener);
    _tableChanged(null);
  }

  @Override
  public boolean isDisposed()
  {
    return propertySheetHackListener == null;
  }

  /**
   * @return Observable to listen on the current selection in the property sheet
   */
  @NotNull
  public Observable<Optional<Object>> observeSelection()
  {
    return currentSelection
        .distinctUntilChanged();
  }

  /**
   * Gets triggered if the underlying table changed
   *
   * @param pTable new table
   */
  private void _tableChanged(@Nullable JTable pTable)
  {
    if (propertySheetTable != null)
      propertySheetTable.getSelectionModel().removeListSelectionListener(tableListener);

    propertySheetTable = pTable;

    if (propertySheetTable != null)
    {
      propertySheetTable.getSelectionModel().removeListSelectionListener(tableListener);
      propertySheetTable.getSelectionModel().addListSelectionListener(tableListener);
    }
  }

  /**
   * PropertyChangeListener-Impl
   */
  private class _PropertySheetChangeListener implements PropertyChangeListener
  {
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
      try
      {
        Object openedTC = evt.getNewValue();
        if (openedTC != null && openedTC.getClass().getName().equals("org.netbeans.core.windows.view.ui.NbSheet") &&
            TopComponent.Registry.PROP_TC_OPENED.equals(evt.getPropertyName()))
        {
          Field propertySheetField = openedTC.getClass().getDeclaredField("propertySheet");
          propertySheetField.setAccessible(true);
          Object propertySheet = propertySheetField.get(openedTC);

          Field tableField = propertySheet.getClass().getDeclaredField("table");
          tableField.setAccessible(true);
          Container table = (Container) tableField.get(propertySheet);

          if (table instanceof JTable)
            _tableChanged((JTable) table);
          else
            _tableChanged(null);
        }
      }
      catch (Exception e)
      {
        // ignore
      }
    }
  }

}
