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
    private final XMLMessageProducer producer;
    private final DefaultListModel<AlertEntry> listModel;

    public AlertTopicSubscriber(Consumer<String> onSecurityDetected, DefaultListModel<AlertEntry> model)
            throws JCSMPException {
        this.onSecurityDetected = onSecurityDetected;
        this.session = SolaceConfig.getSession();
        this.session.connect();
        this.producer = session.getMessageProducer(new HandleEvent());
        this.listModel = model; // 주입받은 모델 사용
        subscribeToTopics(List.of("JY/*/*/SECURITY/THEFT/>", "JY/*/*/PAYMENT/FAILURE/>"));
    }

    public void subscribeToTopics(List<String> topics) throws JCSMPException {
        for (String topicStr : topics) {
            session.addSubscription(JCSMPFactory.onlyInstance().createTopic(topicStr));
        }

        XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage) {
                    String payload = ((TextMessage) msg).getText();
                    String topic = msg.getDestination().getName();

                    String[] parts = payload.split("\\|", 2);
                    String ts = parts[0];
                    String content = parts.length > 1 ? parts[1] : payload;

                    SwingUtilities.invokeLater(() -> {
                        listModel.add(0, new AlertEntry(content + " [" + topic + "]", ts));
                    });

                    if (topic.contains("/SECURITY/THEFT")) {
                        sendSoundRequest();
                        onSecurityDetected.accept(topic);
                    }
                }
            }

            public void onException(JCSMPException e) {
                e.printStackTrace();
            }
        });
        consumer.start();
    }

    public void sendSoundRequest() {
        try {
            Queue requestQueue = JCSMPFactory.onlyInstance().createQueue("Q.JAEYOUNG/REQUEST");
            Queue replyQueue = session.createTemporaryQueue();

            TextMessage request = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            request.setText("🔔 SOUND REQUEST");
            request.setDeliveryMode(DeliveryMode.PERSISTENT);
            request.setReplyTo(replyQueue);

            // 응답 수신 설정
            ConsumerFlowProperties replyFlowProps = new ConsumerFlowProperties();
            replyFlowProps.setEndpoint(replyQueue);
            replyFlowProps.setStartState(true);

            session.createFlow(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage replyMsg) {
                    if (replyMsg instanceof TextMessage) {
                        System.out.println("✅ 응답 수신 완료: " + ((TextMessage) replyMsg).getText());
                    }
                }

                @Override
                public void onException(JCSMPException e) {
                    e.printStackTrace();
                }
            }, replyFlowProps).start();

            producer.send(request, requestQueue);
            System.out.println("📨 SOUND 요청 전송: " + requestQueue.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}