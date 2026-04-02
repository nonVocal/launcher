package dev.nonvocal.launcher;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Handles drag-and-drop row reordering for the entry {@link JList}.
 * Reordering is only allowed when no search filter is active.
 * After a successful drop, {@code onReorder} is invoked to persist the new order.
 */
class EntryListTransferHandler extends TransferHandler
{
    private static final long serialVersionUID = 1L;

    private final DataFlavor                  rowFlavor = new DataFlavor(Integer.class, "Row Index");
    private final JList<LaunchEntry>          list;
    private final DefaultListModel<LaunchEntry> listModel;
    private final List<LaunchEntry>           allEntries;
    private final BooleanSupplier             filterActive;
    private final Runnable                    onReorder;

    private int dragIndex = -1;

    EntryListTransferHandler(JList<LaunchEntry> list,
                             DefaultListModel<LaunchEntry> listModel,
                             List<LaunchEntry> allEntries,
                             BooleanSupplier filterActive,
                             Runnable onReorder)
    {
        this.list         = list;
        this.listModel    = listModel;
        this.allEntries   = allEntries;
        this.filterActive = filterActive;
        this.onReorder    = onReorder;
    }

    @Override
    public int getSourceActions(JComponent c) { return MOVE; }

    @Override
    protected Transferable createTransferable(JComponent c)
    {
        dragIndex = list.getSelectedIndex();
        final int idx = dragIndex;
        return new Transferable()
        {
            @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{rowFlavor}; }
            @Override public boolean isDataFlavorSupported(DataFlavor f) { return rowFlavor.equals(f); }
            @Override public Object getTransferData(DataFlavor f) { return idx; }
        };
    }

    @Override
    public boolean canImport(TransferSupport s)
    {
        return s.isDrop() && !filterActive.getAsBoolean() && s.isDataFlavorSupported(rowFlavor);
    }

    @Override
    public boolean importData(TransferSupport support)
    {
        if (!canImport(support)) return false;
        try
        {
            int drop = ((JList.DropLocation) support.getDropLocation()).getIndex();
            int src  = dragIndex;
            if (src < 0 || src >= listModel.getSize()) return false;

            LaunchEntry moved = listModel.getElementAt(src);
            listModel.remove(src);

            int target = Math.max(0, Math.min((src < drop) ? drop - 1 : drop, listModel.getSize()));
            listModel.add(target, moved);

            allEntries.clear();
            for (int i = 0; i < listModel.getSize(); i++) allEntries.add(listModel.getElementAt(i));

            onReorder.run();
            list.setSelectedIndex(target);
            return true;
        }
        catch (Exception ex) { return false; }
    }

    @Override
    protected void exportDone(JComponent s, Transferable d, int a) { dragIndex = -1; }
}

