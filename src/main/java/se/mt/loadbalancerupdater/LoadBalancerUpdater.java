package se.mt.loadbalancerupdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.mt.loadbalancerupdater.exception.BadConfigurationException;
import sun.tools.jstat.ParserException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadBalancerUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerUpdater.class);

    private ConfigPanel configPanel;
    private BalancerPanel balancerPanel;
    private MainWindow mainWindow;
    private List<BalancerConfig> configs;

    private ActionListener configButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            configPanel = new ConfigPanel(configs, configDoneActionListener);
            mainWindow.setContent(configPanel);
        }
    };

    private ActionListener configDoneActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            balancerPanel = new BalancerPanel(configs, configButtonListener);
            mainWindow.setContent(balancerPanel);
        }
    };


    public LoadBalancerUpdater(List<BalancerConfig> configs) {

        if (configs == null) {
            configs = new ArrayList<BalancerConfig>();
            configs.add(new BalancerConfig("my_system", Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList()));
        }

        this.configs = configs;
        balancerPanel = new BalancerPanel(configs, configButtonListener);
        mainWindow = new MainWindow();
        configPanel = new ConfigPanel(configs, configDoneActionListener);

    }

    private void doIt(boolean showConfig) {
        if (showConfig) {
            mainWindow.setContent(configPanel);
        } else {
            mainWindow.setContent(balancerPanel);
        }
        mainWindow.setVisible(true);
    }

    public static void main(String[] args) throws IOException, ParserException {

        if (args.length > 1) {
            System.err.println("Usage: java -jar load-balancer-updater.jar <optional config_file_path>");
            return;
        }

        String filename;

        if (args.length == 1) {
            filename = args[0];
        } else {
            filename = BalancerConfig.getDefaultConfigPath();
        }

        List<BalancerConfig> configs;
        try {
            configs = BalancerConfig.readFromFile(filename);
            if (configs == null) {
                // File not found
                if (args.length == 1) {
                    // They specified the filename, that's an error
                    System.err.println("Config file " + filename + " not found");
                    return;
                }
                // Otherwise just show the config dialog

            }
        } catch (BadConfigurationException e) {
            System.err.println("Config file " + filename + " contains bad data: " + e.getMessage());
            return;
        }

        LOGGER.info("Loaded configs: \n\n{}", configs);

        LoadBalancerUpdater updater = new LoadBalancerUpdater(configs);
        updater.doIt(configs == null);

    }

}
