package com.example.solace.gui;

import com.example.solace.config.SolaceConfig;
import com.solacesystems.jcsmp.*;
import javax.swing.*;
import java.awt.*;

public class CheckoutApp extends JFrame {
    private DefaultListModel<String> model = new DefaultListModel<>();

    public CheckoutApp() throws JCSMPException {
        super("Checkout App");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JList<String> list = new JList<>(model);
        add(new JScrollPane(list), BorderLayout.CENTER);
        setVisible(true);

        // Solace 연결
        JCSMPSession session = SolaceConfig.getSession();
        session.connect();

        // Checkout 전용 큐(또는 토픽 바로)
        Topic topic = JCSMPFactory.onlyInstance()
            .createTopic("JY/SHOP/>/PAYMENT/SUCCESS"); // 와일드카드 사용

        // Topic 구독 후 Direct Subscriber
        XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage tm) {
                    String text = tm.getText();
                    SwingUtilities.invokeLater(() ->
                        model.add(0, "[" + java.time.LocalTime.now() + "] 결제 성공: " + text)
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
        session.addSubscription(topic);
        consumer.start(); 
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { new CheckoutApp(); }
            catch (JCSMPException e) { e.printStackTrace(); }
        });
    }
}