package dev.nonvocal.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Shows real-time process output (stdout + stderr) in a dedicated, non-blocking window.
 * Calls the optional {@code onComplete} callback on the EDT when the stream ends.
 */
class ProcessOutputWindow
{
    private ProcessOutputWindow() {}

    static void show(Process process, String title, Runnable onComplete)
    {
        SwingUtilities.invokeLater(() ->
        {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            DefaultListModel<String> model = new DefaultListModel<>();
            JList<String> outputList = new JList<>(model);
            outputList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            frame.add(new JScrollPane(outputList), BorderLayout.CENTER);
            frame.setVisible(true);

            new Thread(() ->
            {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(process.getInputStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        final String out = line;
                        SwingUtilities.invokeLater(() ->
                        {
                            model.addElement(out);
                            outputList.ensureIndexIsVisible(model.getSize() - 1);
                        });
                    }
                }
                catch (IOException ignored) {}

                SwingUtilities.invokeLater(() ->
                {
                    if (onComplete != null)
                    {
                        frame.dispose();
                        onComplete.run();
                    }
                    else
                    {
                        model.addElement("");
                        model.addElement("Process completed.");
                        outputList.ensureIndexIsVisible(model.getSize() - 1);
                    }
                });
            }).start();
        });
    }
}

