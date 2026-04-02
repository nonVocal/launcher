package dev.nonvocal.launcher;

import java.io.File;

/**
 * An entry in the launcher list – a script, application folder, or plain folder.
 *
 * @param file     The file or directory this entry represents.
 * @param type     The classification of this entry.
 * @param iconFile For APP_FOLDER: the .lnk or fallback exe used as icon source; null otherwise.
 * @param appType  The matched / assigned {@link AppType}, or {@code null} for built-in detection.
 */
record LaunchEntry(File file, EntryType type, File iconFile, AppType appType)
{
    /** Convenience constructor – no icon source and no explicit app type. */
    LaunchEntry(File file, EntryType type)
    {
        this(file, type, null, null);
    }

    /** Convenience constructor – icon source provided, no explicit app type. */
    LaunchEntry(File file, EntryType type, File iconFile)
    {
        this(file, type, iconFile, null);
    }

    @Override
    public String toString()
    {
        return file.getName();
    }
}
