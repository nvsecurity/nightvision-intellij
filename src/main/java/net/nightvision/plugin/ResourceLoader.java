package net.nightvision.plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Loads a complete resource snapshot off the EDT and rejects stale results. */
public final class ResourceLoader<T> {
    private final JComponent owner;
    private final JPanel statusPanel;
    private final Consumer<Boolean> loading;
    private final Supplier<T> fetch;
    private final Consumer<T> accept;
    private SwingWorker<T, Void> worker;

    public ResourceLoader(JComponent owner, JPanel statusPanel, Consumer<Boolean> loading,
                          Supplier<T> fetch, Consumer<T> accept) {
        this.owner = owner;
        this.statusPanel = statusPanel;
        this.loading = loading;
        this.fetch = fetch;
        this.accept = accept;
        statusPanel.setLayout(new BorderLayout(0, 4));
        owner.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0 && owner.getParent() == null) {
                cancel();
            }
        });
    }

    public void cancel() {
        if (worker != null) worker.cancel(true);
    }

    public void load() {
        cancel();
        loading.accept(true);
        statusPanel.removeAll();
        statusPanel.add(new JLabel("Loading..."), BorderLayout.CENTER);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> cancel());
        statusPanel.add(cancel, BorderLayout.EAST);
        statusPanel.revalidate();
        statusPanel.repaint();
        worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() {
                return fetch.get();
            }

            @Override
            protected void done() {
                if (worker != this || owner.getParent() == null) return;
                statusPanel.removeAll();
                try {
                    T result = get();
                    loading.accept(false);
                    accept.accept(result);
                } catch (CancellationException ex) {
                    showFailure("Loading cancelled.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showFailure("Loading interrupted.");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showFailure(cause.getMessage() == null ? "Could not load resources." : cause.getMessage());
                }
                statusPanel.revalidate();
                statusPanel.repaint();
            }
        };
        worker.execute();
    }

    private void showFailure(String message) {
        JTextArea error = new JTextArea(message);
        error.setEditable(false);
        error.setLineWrap(true);
        error.setWrapStyleWord(true);
        error.setOpaque(false);
        statusPanel.add(error, BorderLayout.CENTER);
        JButton retry = new JButton("Retry");
        retry.addActionListener(event -> load());
        statusPanel.add(retry, BorderLayout.SOUTH);
    }
}
