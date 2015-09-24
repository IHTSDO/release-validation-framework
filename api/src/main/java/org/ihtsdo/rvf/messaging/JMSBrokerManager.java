package org.ihtsdo.rvf.messaging;

import org.apache.activemq.broker.BrokerService;

public class JMSBrokerManager {

	public JMSBrokerManager() throws Exception {
		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://localhost:61617");
		broker.start();
	}
}
