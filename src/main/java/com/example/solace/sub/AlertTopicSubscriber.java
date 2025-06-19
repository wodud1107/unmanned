package com.example.solace.sub;

import com.example.solace.config.SolaceConfig;
import com.example.solace.util.AlertEntry;
import com.example.solace.util.HandleEvent;
import com.solacesystems.jcsmp.*;

import javax.swing.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class AlertTopicSubscriber {
    private final Consumer<String> onSecurityDetected;
    private final JCSMPSession session;
    private final DefaultListModel<AlertEntry> listModel;
    private final XMLMessageProducer producer;

    public AlertTopicSubscriber(Consumer<String> onSecurityDetected) throws JCSMPException {
        this.onSecurityDetected = onSecurityDetected;
        this.session = SolaceConfig.getSession();
        this.session.connect();
        this.producer = this.session.getMessageProducer(new HandleEvent());
        this.listModel = new DefaultListModel<>();
        subscribeToTopics(List.of("JY/*/*/SECURITY/THEFT", "JY/*/*/PAYMENT/FAILURE"));
    }

    public void subscribeToTopics(List<String> topics) throws JCSMPException {
        for (String topicStr : topics) {
            Topic topicFilter = JCSMPFactory.onlyInstance().createTopic(topicStr);
            session.addSubscription(topicFilter);
        }

        XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage) {
                    String text = ((TextMessage) msg).getText();
                    String topic = msg.getDestination().getName();
                    String ts = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);

                    SwingUtilities.invokeLater(() -> {
                        listModel.add(0, new AlertEntry(text, ts));
                        if (listModel.size() > 100)
                            listModel.removeElementAt(100);
                    });

                    if (topic.contains("/SECURITY/THEFT")) {
                        onSecurityDetected.accept(topic);
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                SwingUtilities.invokeLater(() -> listModel.add(0,
                        new AlertEntry("⚠️ Subscriber error: " + e.getMessage(),
                                LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME))));
            }
        });

        consumer.start();
    }

    public void sendStartCommand() {
        try {
            Topic topic = JCSMPFactory.onlyInstance().createTopic("JY/SECURITY/ALERT");
            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText("START");
            msg.setDeliveryMode(DeliveryMode.DIRECT);
            producer.send(msg, topic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendStopCommand() {
        try {
            Topic topic = JCSMPFactory.onlyInstance().createTopic("JY/SECURITY/ALERT");
            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText("STOP");
            msg.setDeliveryMode(DeliveryMode.DIRECT);
            producer.send(msg, topic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
