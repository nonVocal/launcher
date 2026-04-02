package dev.nonvocal.launcher;

import java.io.File;

/**
 * An entry in the launcher list – a script, application folder, or plain folder.
 *
 * @param file     The file or directory this entry represents.
 * @param type     The classification of this entry.
 * @param iconFile For APP_FOLDER: the .lnk or fallback exe used as icon source; null otherwise.
 */
record LaunchEntry(File file, EntryType type, File iconFile)
{
    /** Convenience constructor for entries without a dedicated icon source. */
    LaunchEntry(File file, EntryType type)
    {
        this(file, type, null);
    }

    @Override
    public String toString()
    {
        return file.getName();
    }
}

