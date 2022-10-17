package de.adito.aditoweb.nbm.filepreview;

import org.jetbrains.annotations.*;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import org.openide.text.DataEditorSupport;

import java.io.IOException;

/**
 * @author w.glanzer, 12.10.2022
 */
public class PreviewEditorSupport extends DataEditorSupport
{
  private PreviewEditorSupport(@NotNull DataObject pDataObject)
  {
    super(pDataObject, new _PreviewEditorSupportEnv(pDataObject));
  }

  /**
   * Creates a new instance of this editor support for the given file
   *
   * @param pFile File to create the support for; a temporary file will be created if null is passed
   * @return the support
   */
  @NotNull
  public static PreviewEditorSupport create(@Nullable FileObject pFile)
  {
    try
    {
      FileObject file = pFile != null ? pFile : FileUtil.createMemoryFileSystem().getRoot();
      return new PreviewEditorSupport(DataObject.find(file));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Environment for the PreviewEditorSupport
   */
  private static class _PreviewEditorSupportEnv extends DataEditorSupport.Env
  {
    public _PreviewEditorSupportEnv(@NotNull DataObject obj)
    {
      super(obj);
    }

    @Override
    protected FileObject getFile()
    {
      return getDataObject().getPrimaryFile();
    }

    @Override
    protected FileLock takeLock() throws IOException
    {
      return ((MultiDataObject) getDataObject()).getPrimaryEntry().takeLock();
    }
  }
}
