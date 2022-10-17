package de.adito.aditoweb.nbm.filepreview;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.jetbrains.annotations.*;
import org.openide.filesystems.*;
import org.openide.loaders.DataObject;
import org.openide.modules.Modules;
import org.openide.nodes.Node;
import org.openide.text.*;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.windows.*;

import javax.swing.*;
import java.lang.reflect.*;
import java.util.concurrent.TimeUnit;

/**
 * Preview TopComponent
 *
 * @author w.glanzer, 11.10.2022
 */
@TopComponent.Description(preferredID = PreviewTopComponent.TC_ID, persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(mode = "output", openAtStartup = false, position = 1000)
public class PreviewTopComponent extends CloneableEditor
{

  public static final String TC_ID = "ADITOPreview"; //NOI18N
  protected static final PreviewTopComponent INSTANCE = new PreviewTopComponent();
  private static final String _NAME = "File Preview";
  private final CompositeDisposable disposable = new CompositeDisposable();

  public PreviewTopComponent()
  {
    super(create(null), false);

    // Remove Node from Lookup to prevent navigator change
    associateLookup(Lookups.proxy(() -> {
      Lookup lookup = _getLookupOfEditorSupport();
      return lookup == null ? Lookup.EMPTY : Lookups.exclude(lookup, Node.class);
    }));
  }

  @Override
  protected void componentOpened()
  {
    super.componentOpened();

    PropertySheetConnector connector = new PropertySheetConnector();
    disposable.add(connector);
    disposable.add(connector.observeSelection()
                       .debounce(200, TimeUnit.MILLISECONDS)

                       // extract file
                       .map(pSelectionOpt -> pSelectionOpt
                           .filter(Node.Property.class::isInstance)
                           .map(pSelection -> _findFileObjectInProperty((Node.Property<?>) pSelection)))
                       .distinctUntilChanged()

                       // change on ui
                       .subscribe(pSelectedFile -> _changeFile(pSelectedFile.orElse(null))));
  }

  @Override
  protected void componentClosed()
  {
    disposable.clear();
    super.componentClosed();
  }

  @Override
  public void setName(String name)
  {
    super.setName(_NAME);
  }

  @Override
  public void setDisplayName(String displayName)
  {
    super.setDisplayName(_NAME);
  }

  @Override
  public void setHtmlDisplayName(String htmlDisplayName)
  {
    super.setHtmlDisplayName(_NAME);
  }

  /**
   * Opens this TopComponent and activates it
   */
  public void doOpen()
  {
    SwingUtilities.invokeLater(() -> {
      if (!INSTANCE.isOpened())
      {
        INSTANCE.open();
        WindowManager.getDefault().findMode("output").dockInto(INSTANCE);
        INSTANCE.requestActive();
      }
    });
  }

  /**
   * Returns the fileObject that the given property represents
   *
   * @param pProperty Property to get the fileobject for
   * @return the fo or null, if no fo was found
   */
  @Nullable
  private FileObject _findFileObjectInProperty(@NotNull Node.Property<?> pProperty)
  {
    try
    {
      ClassLoader classloader = Modules.getDefault().findCodeNameBase("de.adito.designer.netbeans.DesignerCommonInterface").getClassLoader();
      Class<?> linkCookie = Class.forName("de.adito.aditoweb.nbm.designer.commoninterface.cookies.ILinkCookie", false, classloader);
      Method getLookup = pProperty.getClass().getMethod("getLookup");
      getLookup.setAccessible(true);
      Lookup lookup = (Lookup) getLookup.invoke(pProperty);
      Object linkCookieObj = lookup.lookup(linkCookie);
      if (linkCookieObj != null)
      {
        Method getLinked = linkCookieObj.getClass().getMethod("getLinked");
        getLinked.setAccessible(true);
        DataObject dataObject = (DataObject) getLinked.invoke(linkCookieObj);
        if (dataObject != null)
          return dataObject.getPrimaryFile();
      }
    }
    catch (Exception e)
    {
      // ignore
    }

    return null;
  }

  /**
   * @return the current used lookup in the current editor support
   */
  @Nullable
  private Lookup _getLookupOfEditorSupport()
  {
    CloneableEditorSupport ces = cloneableEditorSupport();

    try
    {
      Field lookup = CloneableEditorSupport.class.getDeclaredField("lookup");
      lookup.setAccessible(true);
      return (Lookup) lookup.get(ces);
    }
    catch (Throwable e)
    {
      return null;
    }
  }

  /**
   * Changes the underlying file of this top component
   *
   * @param pFile file to change to
   */
  private void _changeFile(@Nullable FileObject pFile)
  {
    if (pFile == null)
      return;

    try
    {
      CloneableEditorSupport ces = create(pFile);
      if (ces != null)
      {
        //Preserve activated nodes to prevent property sheet changes
        setActivatedNodes(TopComponent.getRegistry().getActivatedNodes());

        pane = null;
        removeAll();

        Field support = CloneableEditor.class.getDeclaredField("support");
        support.setAccessible(true);
        support.set(this, ces);

        updateName();

        super.componentOpened();
        super.componentShowing();
      }
    }
    catch (Exception pE)
    {
      throw new RuntimeException(pE);
    }
  }

  /**
   * Creates a new editor support instance for the underlying file
   *
   * @param pFile file to get the support for
   * @return the support or null if no support could be created
   */
  @Nullable
  private static CloneableEditorSupport create(@Nullable FileObject pFile)
  {
    try
    {
      FileObject file = pFile != null ? pFile : FileUtil.createMemoryFileSystem().getRoot().createData("dummy.js");
      DataObject dataObject = DataObject.find(file);
      return dataObject.getLookup().lookup(CloneableEditorSupport.class);
    }
    catch (Throwable e)
    {
      // ignore
    }

    return null;
  }
}
