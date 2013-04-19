package se.mt.loadbalancerupdater;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.mt.loadbalancerupdater.util.OrderedMultiMap;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PageGetter {
    private String balanceManagerHost;
    private List<String> balancerNames;

    private static final Logger LOGGER = LoggerFactory.getLogger(PageGetter.class);

    public PageGetter(String balanceManagerHost, List<String> balancerNames) {
        LOGGER.info("Creating PageGetter: balanceManagerHost = {}, balancerNames = {}", balanceManagerHost, balancerNames);
        this.balanceManagerHost = balanceManagerHost;
        this.balancerNames = balancerNames;
    }

    public OrderedMultiMap<String, Worker> getWorkers() throws IOException, ParserException {

        OrderedMultiMap<String, Worker> balancerToWorkerMap = new OrderedMultiMap<String, Worker>();

        String urlString = BalancerConfig.getBalancerManagerUrl(balanceManagerHost);
        NodeList balancers = getConfiguredBalancers(urlString, balancerNames);

        SimpleNodeIterator elements = balancers.elements();
        while (elements.hasMoreNodes()) {
            Node node = elements.nextNode();
            String balancerText = node.getFirstChild().getText();

            int numTablesSeen = 0;
            while (true) {
                if (node instanceof TableTag) {
                    if (++numTablesSeen == 2) {
                        break;
                    }
                }
                node = node.getNextSibling();
            }

            NodeList linksInTable = node.getChildren().extractAllNodesThatMatch(new NodeFilter() {
                @Override
                public boolean accept(Node node) {
                    return node instanceof LinkTag;
                }
            }, true);


            SimpleNodeIterator linkIter = linksInTable.elements();

            while (linkIter.hasMoreNodes()) {
                LinkTag linkNode = (LinkTag) linkIter.nextNode();
                balancerToWorkerMap.put(balancerText, new Worker(linkNode.getLinkText(), linkNode.getLink()));
            }
        }

        return balancerToWorkerMap;
    }

    public Map<String, String> getSubmitFieldsForWorker(Worker worker) throws IOException, ParserException {
        URL url = new URL(worker.getUrl());
        org.htmlparser.Parser parser = new org.htmlparser.Parser(url.openConnection());

        NodeList inputTags = parser.extractAllNodesThatMatch(new NodeFilter() {
            @Override
            public boolean accept(Node node) {
                return node instanceof InputTag;
            }
        });

        Map<String, String> fields = new LinkedHashMap<String, String>();

        SimpleNodeIterator elements = inputTags.elements();
        while (elements.hasMoreNodes()) {
            InputTag node = (InputTag) elements.nextNode();
            if (!node.getAttribute("type").equalsIgnoreCase("submit")) {
                fields.put(node.getAttribute("name"), node.getAttribute("value"));
            }
        }
        return fields;
    }


    public void submit(Map<String, String> fields) throws IOException {
        StringBuilder urlString = new StringBuilder();
        urlString.append(BalancerConfig.getBalancerManagerUrl(balanceManagerHost)).append('?');
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            urlString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        // Remove the last &
        urlString.delete(urlString.length() - 1, urlString.length());

        LOGGER.info("About to submit: {}", urlString);

        URL url = new URL(urlString.toString());
        url.getContent();
    }


    private NodeList getConfiguredBalancers(String urlString,
                                            final List<String> balancerNames) throws IOException, ParserException {
        URL url = new URL(urlString);

        org.htmlparser.Parser parser = new org.htmlparser.Parser(url.openConnection());

        return parser.extractAllNodesThatMatch(new NodeFilter() {
            @Override
            public boolean accept(Node node) {
                if (!node.getText().equals("h3")) {
                    return false;
                }
                Node firstChild = node.getFirstChild();
                if (firstChild == null) {
                    return false;
                }
                boolean found = false;
                if (firstChild.getText().startsWith("LoadBalancer Status for")) {
                    for (String balancerName : balancerNames) {
                        if (firstChild.getText().endsWith("://" + balancerName)) {
                            found = true;
                            break;
                        }
                    }
                }
                return found;
            }
        });
    }

}
