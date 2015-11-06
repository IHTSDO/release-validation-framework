package org.ihtsdo.rvf.messaging;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.plugin.StatisticsBroker;
import org.apache.log4j.Logger;
public class JMSBrokerManager {
	
	private Logger logger = Logger.getLogger(JMSBrokerManager.class);

	public JMSBrokerManager(String url, Boolean isAutoScalingEnabled) throws Exception {
		BrokerService broker = new BrokerService();
		String brokerUrl = "tcp://localhost:61617";
		if (isAutoScalingEnabled) {
			brokerUrl = url;
		}
		logger.info("Auto scaling is enabled:" + isAutoScalingEnabled + " and the queue broker url is:" + brokerUrl);
		broker.addConnector(brokerUrl);
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
