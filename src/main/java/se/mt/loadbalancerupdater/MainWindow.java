package se.mt.loadbalancerupdater;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow extends JFrame {

    public MainWindow() throws HeadlessException {
        super("Load balancer updater");
        setSize(1024, 768);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
    }

    public void setContent(ApplicationPanel component) {
        if (!(component instanceof JComponent)) {
            throw new IllegalArgumentException("Please pass in a subclass of JComponent");
        }
        Container contentPane = getContentPane();
        contentPane.removeAll();
        contentPane.add((Component) component);
        this.setTitle(component.getTitle());
        contentPane.validate();
    }

}
