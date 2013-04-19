package se.mt.loadbalancerupdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.mt.loadbalancerupdater.util.OrderedMultiMap;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class BalancerPanel extends JPanel implements ApplicationPanel {


    private static final Logger LOGGER = LoggerFactory.getLogger(BalancerPanel.class);

    public String getTitle() {
        return "Balancer Manager Updater";
    }

    private BalancerConfig selectedConfig;
    private java.util.List<JEditorPane> htmlPanes = new ArrayList<JEditorPane>();
    private JPanel currentMainPanel;

    public BalancerPanel(List<BalancerConfig> configs, ActionListener configButtonListener) throws HeadlessException {
        setLayout(new BorderLayout());
        createTopPanel(configs, configButtonListener);
        selectedConfig = configs.iterator().next();
        updateMainPanel();
    }

    /**
     * Creates/updates the main panel containing the actual buttons and displays,
     * to reflect selectedConfig
     */
    private void updateMainPanel() {

        if (currentMainPanel != null) {
            // Remove old panel
            remove(currentMainPanel);
        }

        JPanel mainPanel = new JPanel();
        currentMainPanel = mainPanel;

        add(mainPanel, BorderLayout.CENTER);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));

        Box topBox = new Box(BoxLayout.Y_AXIS);
        Box bottomBox = new Box(BoxLayout.X_AXIS);
        mainPanel.add(topBox);
        mainPanel.add(bottomBox);

        List<String> searchStrings = selectedConfig.getWorkerSearchStrings();
        for (String searchString : searchStrings) {
            Box row = Box.createHorizontalBox();
            //row.setBorder(new EmptyBorder(5, 5, 5, 5));
            JLabel label = new JLabel(searchString);
            row.add(label);
            JButton enableButton = new JButton("Enable");
            JButton disableButton = new JButton("Disable");
            enableButton.addActionListener(new EnableDisableButtonActionListener(searchString, true));
            disableButton.addActionListener(new EnableDisableButtonActionListener(searchString, false));
            row.add(enableButton);
            row.add(disableButton);
            row.add(Box.createHorizontalGlue());
            topBox.add(row);
        }

        List<String> balancerHosts = selectedConfig.getBalancerHosts();
        htmlPanes.clear();

        for (String balancerHost : balancerHosts) {
            Box browserBox = new Box(BoxLayout.Y_AXIS);
            //browserBox.setBorder(new EmptyBorder(5, 5, 5, 5));
            String urlString = BalancerConfig.getBalancerManagerUrl(balancerHost);
            try {
                JEditorPane htmlPane = new JEditorPane(urlString);
                htmlPane.setBorder(new EmptyBorder(5, 5, 5, 5));
                htmlPanes.add(htmlPane);
                htmlPane.setEditable(false);
                browserBox.add(new JScrollPane(htmlPane));
                bottomBox.add(browserBox);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        mainPanel.validate();
        this.validate();

        LOGGER.info("mainPanel size {}", mainPanel.getSize());
    }

    /**
     * Creates the top part of the main display: Combo box for selecting configuration and two buttons.
     * @param configs                   List of configurations
     * @param configButtonListener      Callback for when you press the "Configure" button
     */
    private void createTopPanel(final List<BalancerConfig> configs, ActionListener configButtonListener) {
        JPanel topPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(topPanel, BoxLayout.Y_AXIS);
        topPanel.setLayout(boxLayout);
        topPanel.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));

        Box row1 = Box.createHorizontalBox();
        row1.setBorder(new EmptyBorder(5, 5, 5, 5));
        row1.add(new JLabel("Select configuration"));
        row1.add(Box.createGlue());
        topPanel.add(row1);

        JComboBox configNameCombo = new JComboBox();
        configNameCombo.setMaximumSize(new Dimension(200, 40));

        Box row2 = Box.createHorizontalBox();
        row2.add(configNameCombo);
        row2.add(Box.createGlue());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                reloadHtmlPanes();
            }
        });
        row2.add(refreshButton);
        JButton configButton = new JButton("Configure");
        configButton.addActionListener(configButtonListener);
        configButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        row2.add(configButton);
        topPanel.add(row2);

        List<String> configNames = BalancerConfig.getConfigNames(configs);
        for (String configName : configNames) {
            configNameCombo.addItem(configName);
        }

        configNameCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    switchConfig(itemEvent, configs);
                }
            }
        });

        add(topPanel, BorderLayout.NORTH);

    }

    private void switchConfig(ItemEvent itemEvent, List<BalancerConfig> configs) {
        String name = (String) itemEvent.getItem();
        LOGGER.debug("User selected config {}", name);
        selectedConfig = BalancerConfig.getConfigByName(configs, name);
        updateMainPanel();
    }

    private void reloadHtmlPanes() {
        Iterator<String> it = selectedConfig.getBalancerHosts().iterator();
        for (JEditorPane htmlPane : htmlPanes) {
            Document doc = htmlPane.getDocument();
            doc.putProperty(Document.StreamDescriptionProperty, null);
            try {
                String urlString = BalancerConfig.getBalancerManagerUrl(it.next());
                htmlPane.setPage(urlString);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class EnableDisableButtonActionListener implements ActionListener {

        private String searchString;
        private boolean enable;

        EnableDisableButtonActionListener(String searchString, boolean enable) {
            this.searchString = searchString;
            this.enable = enable;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            updateHost(enable, searchString);
            reloadHtmlPanes();
        }
    }

    private void updateHost(boolean enable, String searchString) {

        try {

            java.util.List<String> hosts = selectedConfig.getBalancerHosts();

            for (String host : hosts) {
                PageGetter getter = new PageGetter(host, selectedConfig.getBalancerNames());

                OrderedMultiMap<String, Worker> workerMap = getter.getWorkers();

                Map<String, Collection<Worker>> map = workerMap.getMap();
                Set<String> balancers = map.keySet();
                for (String balancer : balancers) {
                    Collection<Worker> workers = map.get(balancer);
                    for (Worker worker : workers) {
                        if (worker.getName().contains(searchString)) {
                            Map<String, String> fields = getter.getSubmitFieldsForWorker(worker);
                            fields.put("dw", enable ? "Enable" : "Disable");
                            getter.submit(fields);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (org.htmlparser.util.ParserException e) {
            throw new RuntimeException(e);
        }
    }


}
