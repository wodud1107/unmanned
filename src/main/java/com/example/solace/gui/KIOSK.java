package com.example.solace.gui;

import com.example.solace.config.SolaceConfig;
import com.solacesystems.jcsmp.*;

import javax.swing.*;
import java.awt.*;

public class KIOSK extends JFrame {
    private final DefaultListModel<String> model = new DefaultListModel<>();

    public KIOSK() throws JCSMPException {
        super("CheckOut KIOSK");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JList<String> list = new JList<>(model);
        add(new JScrollPane(list), BorderLayout.CENTER);
        setVisible(true);

        JCSMPSession session = SolaceConfig.getSession();
        session.connect();
        XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage tm) {
                    String text = tm.getText();
                    String topic = msg.getDestination().getName(); // 구체적인 토픽 이름
                    String tag = topic.contains("SUCCESS") ? "✅ 결제 성공" :
                                 topic.contains("FAILURE") ? "❌ 결제 실패" : "💬 기타";

                    SwingUtilities.invokeLater(() ->
                        model.add(0, "[" + java.time.LocalTime.now() + "] " + tag + ": " + text)
                    );
                }
            }

            @Override
            public void onException(JCSMPException e) {
                SwingUtilities.invokeLater(() ->
                    model.add(0, "⚠️ Consumer Error: " + e.getMessage())
                );
            }
        });

        // ✅ 두 토픽 모두 구독
        Topic successTopic = JCSMPFactory.onlyInstance().createTopic("JY/*/*/PAYMENT/SUCCESS/>");
        Topic failureTopic = JCSMPFactory.onlyInstance().createTopic("JY/*/*/PAYMENT/FAILURE/>");
        session.addSubscription(successTopic);
        session.addSubscription(failureTopic);

        consumer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new KIOSK();
            } catch (JCSMPException e) {
                e.printStackTrace();
            }
        });
    }
}