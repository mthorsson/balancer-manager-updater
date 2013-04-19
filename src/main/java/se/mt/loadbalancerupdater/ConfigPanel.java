package se.mt.loadbalancerupdater;

import se.mt.loadbalancerupdater.exception.BadConfigurationException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

public class ConfigPanel extends JPanel implements ApplicationPanel {

    private static final String HOSTS_INFO = "This is a list of host names or IP addresses that the load balancers reside on. Typically two but could be more. " +
            "The same operations are performed on <b>ALL</b> of these and they are displayed side-by-side in the main view.";
    private static final String NAME_INFO = "A list of names used when locating \"balancers\": The program searches for strings like \"balancer://<name>\"";
    private static final String SEARCH_INFO = "These strings are used when searching for worker URLs within the balancers. Each search " +
            "string represents one demon (e.g. one Tomcat instances)";

    private JPanel currentMainPanel;

    public String getTitle() {
        return "Configuration";
    }

    private Map<BalancerConfig, MainConfPanel> panels = new HashMap<BalancerConfig, MainConfPanel>();


    public ConfigPanel(final List<BalancerConfig> configs,
                       final ActionListener configDoneActionListener) throws HeadlessException {

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEADING));

        add(topPanel, BorderLayout.NORTH);

        JComboBox configNameCombo = new JComboBox();
        List<String> configNames = BalancerConfig.getConfigNames(configs);
        for (String configName : configNames) {
            configNameCombo.addItem(configName);
        }
        topPanel.add(configNameCombo);

        configNameCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    String name = (String) itemEvent.getItem();
                    BalancerConfig newConfig = BalancerConfig.getConfigByName(configs, name);
                    switchMainPanel(newConfig);
                }
            }
        });


        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

        Box bottomRow = Box.createHorizontalBox();
        bottomRow.add(Box.createHorizontalGlue());

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                for (MainConfPanel panel : panels.values()) {
                    String error = panel.getErrorString();
                    if (error != null) {
                        JOptionPane.showMessageDialog(ConfigPanel.this, "Bad config: " + error);
                        return;
                    }
                }
                for (MainConfPanel panel : panels.values()) {
                    panel.sync();
                }
                BalancerConfig.saveToFile(panels.keySet());
                configDoneActionListener.actionPerformed(actionEvent);
            }
        });

        bottomRow.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                configDoneActionListener.actionPerformed(actionEvent);
            }
        });
        bottomRow.add(cancelButton);
        southPanel.add(bottomRow);
        add(southPanel, BorderLayout.SOUTH);


        for (BalancerConfig config : configs) {
            panels.put(config, new MainConfPanel(config));
        }

        switchMainPanel(configs.iterator().next());
    }

    private void switchMainPanel(BalancerConfig config) {

        if (currentMainPanel != null) {
            remove(currentMainPanel);
        }

        JPanel newPanel = panels.get(config);

        // Trick to get the layout to update
        // Swing has some problem with removing and re-adding components
        JPanel tempPanel = new JPanel();
        newPanel.add(tempPanel);

        currentMainPanel = newPanel;
        add(newPanel, BorderLayout.CENTER);
        newPanel.validate();
        validate();

        remove(tempPanel);
    }


    private void createLabelRow(Box verticalBoxToAddTo, String text) {
        Box row = Box.createHorizontalBox();
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", 0, 14));
        label.setAlignmentY(BOTTOM_ALIGNMENT);
        row.add(label);
        row.add(Box.createHorizontalGlue());
        verticalBoxToAddTo.add(row);
    }

    private JTextArea getTextArea(List<String> rows) {
        JTextArea area = new JTextArea(10, 10);

        StringBuilder sb = new StringBuilder(128);
        for (String row : rows) {
            sb.append(row).append("\n");
        }
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        area.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(0, 5, 5, 5)));
        area.setAlignmentY(TOP_ALIGNMENT);
        area.setText(sb.toString());

        return area;
    }


    private void createInfoLabel(Box box, String text) {
        String html = "<html>" + text.replaceAll("\n", "<br>") + "</html>";
        JLabel area = new JLabel(html);
        area.setFont(new Font("Arial", Font.ITALIC, 12));
        area.setVerticalAlignment(SwingConstants.TOP);
        area.setAlignmentY(TOP_ALIGNMENT);

        area.setVerticalTextPosition(SwingConstants.TOP);
        area.setHorizontalTextPosition(SwingConstants.LEFT);

        Box b = Box.createHorizontalBox();
        b.setAlignmentY(TOP_ALIGNMENT);
        b.add(area);
        box.add(b);
    }

    private class MainConfPanel extends JPanel {
        private BalancerConfig config;

        private MainConfPanel(BalancerConfig config) {
            this.config = config;
            setup(config);
        }

        private JTextArea hostsArea;
        private JTextArea nameArea;
        private JTextArea searchArea;

        public void sync() {
            config.setWorkerSearchStrings(getStringList(searchArea.getText()));
            config.setBalancerNames(getStringList(nameArea.getText()));
            config.setBalancerHosts(getStringList(hostsArea.getText()));
        }

        public String getErrorString() {
            // Make a temporary config so that we can validate
            BalancerConfig temp = new BalancerConfig("temp",
                    getStringList(hostsArea.getText()), getStringList(searchArea.getText()), getStringList(nameArea.getText()));
            // Will throw an exception if broken
            try {
                temp.validate();
                return null;
            } catch (BadConfigurationException e) {
                return e.getMessage();
            }
        }

        private void setup(BalancerConfig config) {
            BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
            setLayout(boxLayout);
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            Box verticalBox = Box.createVerticalBox();
            add(verticalBox);

            Box row1 = Box.createHorizontalBox();
            JLabel headline = new JLabel("Configuring " + config.getConfigName());
            headline.setFont(new Font("Arial", 0, 18));
            row1.add(headline);
            verticalBox.add(row1);

            createLabelRow(verticalBox, "Hosts");
            createInfoLabel(verticalBox, HOSTS_INFO);

            Box row2 = Box.createHorizontalBox();
            hostsArea = getTextArea(config.getBalancerHosts());
            row2.add(hostsArea);
            verticalBox.add(row2);

            createLabelRow(verticalBox, "Balancer names");
            createInfoLabel(verticalBox, NAME_INFO);
            Box row3 = Box.createHorizontalBox();
            nameArea = getTextArea(config.getBalancerNames());
            row3.add(nameArea);
            verticalBox.add(row3);

            createLabelRow(verticalBox, "Search strings");
            createInfoLabel(verticalBox, SEARCH_INFO);
            Box row4 = Box.createHorizontalBox();
            searchArea = getTextArea(config.getWorkerSearchStrings());
            row4.add(searchArea);
            verticalBox.add(row4);

            verticalBox.add(Box.createVerticalGlue());
        }

        public BalancerConfig getConfig() {
            return config;
        }

        public JTextArea getHostsArea() {
            return hostsArea;
        }

        public JTextArea getNameArea() {
            return nameArea;
        }

        public JTextArea getSearchArea() {
            return searchArea;
        }
    }

    private List<String> getStringList(String text) {
        String[] tokens = text.split("\n|\r");
        List<String> retVal = new ArrayList<String>();
        for (String token : tokens) {
            if (token.trim().length() != 0) {
                retVal.add(token.trim());
            }
        }
        return retVal;
    }
}














