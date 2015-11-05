package org.ihtsdo.rvf.messaging;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.plugin.StatisticsBroker;

public class JMSBrokerManager {

	public JMSBrokerManager(String url) throws Exception {
		BrokerService broker = new BrokerService();
		broker.addConnector(url);
		BrokerPlugin plugin = new BrokerPlugin() {
			
			@Override
			public Broker installPlugin(Broker broker) throws Exception {
				return new StatisticsBroker(broker);
			}
		};
		BrokerPlugin[] plugins = new BrokerPlugin[1];
		plugins[0] = plugin;
		broker.setPlugins(plugins);
		broker.start();
	}
}
