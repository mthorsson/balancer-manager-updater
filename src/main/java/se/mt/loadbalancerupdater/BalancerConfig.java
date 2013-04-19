package se.mt.loadbalancerupdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.mt.loadbalancerupdater.exception.BadConfigurationException;

import java.io.*;
import java.util.*;

import static java.util.Arrays.*;

public class BalancerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalancerConfig.class);

    private static final String URL_TEMPLATE = "http://%s/balancer-manager";

    // The name of this configuration, such as "Viktklubb PROD"
    private String configName;

    // The host names or IP addresses of the load balancers
    private List<String> balancerHosts;

    private List<String> workerSearchStrings;

    private List<String> balancerNames;

    private static String lastLoadPath;

    private static final String FILE_SEP = System.getProperty("file.separator");
    private static final String DEFAULT_CONFIG_FILENAME = "loadbalancerupdater.properties";

    public BalancerConfig(String configName, List<String> balancerHosts,
                          List<String> workerSearchStrings, List<String> balancerNames) {
        this.configName = configName;
        this.balancerHosts = balancerHosts;
        this.workerSearchStrings = workerSearchStrings;
        this.balancerNames = balancerNames;
    }


    public void validate() throws BadConfigurationException {

        if (configName == null || configName.trim().length() == 0) {
            throw new BadConfigurationException("configName should not be empty");
        }

        if (balancerHosts == null || balancerHosts.isEmpty()) {
            throw new BadConfigurationException("No balancer hosts configured for config " + configName);
        }

        if (workerSearchStrings == null || workerSearchStrings.isEmpty()) {
            throw new BadConfigurationException("No worker search string configured for config " + configName);
        }

        if (balancerNames == null || balancerNames.isEmpty()) {
            throw new BadConfigurationException("No balancer names configured for config " + configName);
        }

    }

    public List<String> getBalancerHosts() {
        return balancerHosts;
    }

    public List<String> getWorkerSearchStrings() {
        return workerSearchStrings;
    }

    public List<String> getBalancerNames() {
        return balancerNames;
    }

    public String getConfigName() {
        return configName;
    }

    public void setBalancerHosts(List<String> balancerHosts) {
        this.balancerHosts = balancerHosts;
    }

    public void setWorkerSearchStrings(List<String> workerSearchStrings) {
        this.workerSearchStrings = workerSearchStrings;
    }

    public void setBalancerNames(List<String> balancerNames) {
        this.balancerNames = balancerNames;
    }

    public static List<BalancerConfig> readFromFile(String path) throws BadConfigurationException {

        LOGGER.info("Loading properties from file {}", path);

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(path));
        } catch (IOException e) {
            LOGGER.warn("Could not find config file " + path);
            return null;
        }

        Set<String> configNames = getConfigNames(path, props);

        LOGGER.debug("configNames = {}", configNames);

        List<BalancerConfig> configs = new ArrayList<BalancerConfig>();

        for (String configName : configNames) {

            List<String> hostList = null;
            List<String> searchStringList = null;
            List<String> balancerNameList = null;

            String hostString = props.getProperty(configName + ".balancer.hosts");
            if (isSet(hostString)) {
                hostList = asList(hostString.split(" *, *"));
            }

            String searchStringString = props.getProperty(configName + ".worker.search.strings");
            if (isSet(searchStringString)) {
                searchStringList = asList(searchStringString.split(" *, *"));
            }

            String balancerNameString = props.getProperty(configName + ".balancer.names");
            if (isSet(balancerNameString)) {
                balancerNameList = asList(balancerNameString.split(" *, *"));
            }

            BalancerConfig balancerConfig = new BalancerConfig(
                    configName,
                    hostList,
                    searchStringList,
                    balancerNameList);

            balancerConfig.validate();

            configs.add(balancerConfig);
        }

        lastLoadPath = path;

        return configs;

    }

    public static String getDefaultConfigPath() {
        String userHome = System.getProperty("user.home");
        return userHome + FILE_SEP + DEFAULT_CONFIG_FILENAME;
    }

    public static void saveToFile(Collection<BalancerConfig> configs) {
        Properties newProperties = new Properties();

        for (BalancerConfig config : configs) {
            newProperties.setProperty(config.getConfigName() + ".worker.search.strings", createListString(config.getWorkerSearchStrings()));
            newProperties.setProperty(config.getConfigName() + ".balancer.names", createListString(config.getBalancerNames()));
            newProperties.setProperty(config.getConfigName() + ".balancer.hosts", createListString(config.getBalancerHosts()));
        }

        try {
            if (lastLoadPath == null) {
                lastLoadPath = getDefaultConfigPath();
            }
            newProperties.store(new FileWriter(lastLoadPath), "Properties for Balancer Manager Updater");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String createListString(Collection<String> items) {
        StringBuilder sb = new StringBuilder(128);
        for (String item : items) {
            sb.append(item.trim()).append(",");
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    public static List<String> getConfigNames(List<BalancerConfig> configs) {
        List<String> names = new ArrayList<String>(configs.size());
        for (BalancerConfig config : configs) {
            names.add(config.configName);
        }
        return names;
    }

    public static BalancerConfig getConfigByName(List<BalancerConfig> configs, String name) {
        for (BalancerConfig config : configs) {
            if (config.configName.equals(name)) {
                return config;
            }
        }
        return null;
    }

    public static String getBalancerManagerUrl(String host) {
        String url = String.format(URL_TEMPLATE, host);
        LOGGER.debug("Created balancer manager URL {}", url);
        return url;
    }

    private static Set<String> getConfigNames(String path, Properties props) {
        Set<String> configNames = new HashSet<String>();

        Enumeration enumeration = props.propertyNames();
        while (enumeration.hasMoreElements()) {
            String keyName = (String) enumeration.nextElement();
            int dotPos = keyName.indexOf(".");
            if (dotPos <= 0) {
                throw new IllegalArgumentException("Bad property name in config file " + path + ": " + keyName);
            }
            configNames.add(keyName.substring(0, dotPos));
        }
        return configNames;
    }

    private static boolean isSet(String value) {
        return value != null && value.trim().length() != 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BalancerConfig");
        sb.append("{configName='").append(configName).append('\'');
        sb.append(", balancerHosts=").append(balancerHosts);
        sb.append(", workerSearchStrings=").append(workerSearchStrings);
        sb.append(", balancerNames=").append(balancerNames);
        sb.append('}');
        return sb.toString();
    }
}
