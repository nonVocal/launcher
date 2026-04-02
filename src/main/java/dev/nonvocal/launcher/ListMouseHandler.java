package dev.nonvocal.launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles all mouse interactions on the entry {@link JList}:
 * <ul>
 *   <li>Left-click on an action button → fires the action (ICONS mode) or opens the popup (HAMBURGER mode)</li>
 *   <li>Right-click → shows the context-menu popup (when enabled)</li>
 *   <li>Double-click → launches the entry</li>
 *   <li>Mouse move → changes the cursor to a hand pointer over buttons</li>
 * </ul>
 */
class ListMouseHandler extends MouseAdapter
{
    /** Callback that shows the actions popup at the given list-relative coordinates. */
    @FunctionalInterface
    interface PopupShower { void show(LaunchEntry entry, int x, int y); }

    private final JList<LaunchEntry>         list;
    private final DefaultListModel<LaunchEntry> listModel;
    private final Supplier<String>           buttonStyle;
    private final Supplier<List<String>>     actionOrder;
    private final BooleanSupplier            contextMenuEnabled;
    private final PopupShower                popupShower;
    private final Consumer<LaunchEntry>      launcher;
    private final FolderActions              folderActions;

    ListMouseHandler(JList<LaunchEntry> list,
                     DefaultListModel<LaunchEntry> listModel,
                     Supplier<String> buttonStyle,
                     Supplier<List<String>> actionOrder,
                     BooleanSupplier contextMenuEnabled,
                     PopupShower popupShower,
                     Consumer<LaunchEntry> launcher,
                     FolderActions folderActions)
    {
        this.list              = list;
        this.listModel         = listModel;
        this.buttonStyle       = buttonStyle;
        this.actionOrder       = actionOrder;
        this.contextMenuEnabled = contextMenuEnabled;
        this.popupShower       = popupShower;
        this.launcher          = launcher;
        this.folderActions     = folderActions;
    }

    // ── MouseListener ─────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        int idx = list.locationToIndex(e.getPoint());
        if (idx < 0) return;
        LaunchEntry sel = listModel.getElementAt(idx);

        if (sel.type() != EntryType.SCRIPT)
        {
            if (isHamburger())
            {
                int n = actionOrder.get().isEmpty() ? 0 : 1;
                if (hitActionIcon(e.getPoint(), idx, n) == 0 && e.getClickCount() == 1)
                {
                    popupShower.show(sel, e.getX(), e.getY());
                    return;
                }
            }
            else
            {
                int ai = hitActionIcon(e.getPoint(), idx, actionOrder.get().size());
                if (ai >= 0)
                {
                    if (e.getClickCount() == 1) fireAction(sel, actionOrder.get().get(ai));
                    return;
                }
            }
        }
        if (e.getClickCount() == 2) launcher.accept(sel);
    }

    @Override
    public void mousePressed(MouseEvent e)  { handlePopup(e); }

    @Override
    public void mouseReleased(MouseEvent e) { handlePopup(e); }

    // ── MouseMotionListener ───────────────────────────────────────────────────

    @Override
    public void mouseMoved(MouseEvent e)
    {
        int idx = list.locationToIndex(e.getPoint());
        boolean over = false;
        if (idx >= 0 && listModel.getElementAt(idx).type() != EntryType.SCRIPT)
        {
            int n = isHamburger()
                    ? (actionOrder.get().isEmpty() ? 0 : 1)
                    : actionOrder.get().size();
            over = hitActionIcon(e.getPoint(), idx, n) >= 0;
        }
        list.setCursor(over
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void handlePopup(MouseEvent e)
    {
        if (!e.isPopupTrigger() || !contextMenuEnabled.getAsBoolean()) return;
        int idx = list.locationToIndex(e.getPoint());
        if (idx < 0) return;
        list.setSelectedIndex(idx);
        LaunchEntry sel = listModel.getElementAt(idx);
        if (sel.type() != EntryType.SCRIPT) popupShower.show(sel, e.getX(), e.getY());
    }

    private void fireAction(LaunchEntry sel, String key)
    {
        switch (key)
        {
            case Launcher.EXPLORE_ACTION -> folderActions.openInExplorer(sel.file());
            case Launcher.EDITOR_ACTION  -> folderActions.openInEditor(sel.file());
            case Launcher.COPY_ACTION    -> folderActions.copyWithRobocopy(sel.file());
            case Launcher.DELETE_ACTION  -> folderActions.deleteFolder(sel.file());
        }
    }

    private boolean isHamburger()
    {
        return Launcher.BUTTON_STYLE_HAMBURGER.equals(buttonStyle.get());
    }

    private int hitActionIcon(Point p, int idx, int numActions)
    {
        return Launcher.hitActionIcon(p, list, idx, numActions);
    }
}

