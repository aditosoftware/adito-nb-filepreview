package de.adito.aditoweb.nbm.filepreview;

import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;

/**
 * Action to get the preview topcomponent in action
 *
 * @author w.glanzer, 11.10.2022
 */
@ActionID(category = "adito/window", id = "de.adito.aditoweb.nbm.filepreview.TogglePreviewAction")
@ActionRegistration(displayName = "#NAME_TogglePreviewAction")
@ActionReference(path = "Menu/Window", position = 1265)
@NbBundle.Messages("NAME_TogglePreviewAction=File Preview")
public class TogglePreviewAction extends NodeAction
{

  @Override
  protected void performAction(Node[] activatedNodes)
  {
    PreviewTopComponent.INSTANCE.doOpen();
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    return true;
  }

  @Override
  public String getName()
  {
    return Bundle.NAME_TogglePreviewAction();
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

}
