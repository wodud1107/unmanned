package com.example.solace.sub;

import com.example.solace.config.SolaceConfig;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageListener;

import com.mongodb.client.*;
import org.bson.Document;

public class LogSubscriber {
    public static void main(String[] args) throws JCSMPException, InterruptedException {
        JCSMPSession session = SolaceConfig.getSession();
        session.connect();

        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("logs");
        MongoCollection<Document> collection = database.getCollection("topic_log");

        final Queue queue = JCSMPFactory.onlyInstance().createQueue("Q.JAEYOUNG4");
        final ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setStartState(true);

        FlowReceiver receiver = session.createFlow(new XMLMessageListener() {
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage) {
                    String topic = msg.getDestination().getName();
                    String content = ((TextMessage) msg).getText();

                    System.out.println("ALERT received: " + ((TextMessage) msg).getText());

                    Document doc = new Document()
                            .append("timestamp", System.currentTimeMillis())
                            .append("topic", topic)
                            .append("content", content);

                    collection.insertOne(doc);
                    System.out.println("MongoDB 로그 저장 완료");
                }
            }

            public void onException(JCSMPException e) {
                System.out.println("Error: " + e);
            }
        }, flowProps);

        receiver.start();

        while (true) {
            Thread.sleep(1000);
        }
    }
}
