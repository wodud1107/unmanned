package com.example.solace.gui;

import com.example.solace.sub.AlertTopicSubscriber;
import com.example.solace.util.AlertEntry;
import com.example.solace.util.HandleEvent;
import com.solacesystems.jcsmp.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

public class AlertDashboard extends JFrame {
    private final DefaultListModel<AlertEntry> listModel = new DefaultListModel<>();
    private AlertTopicSubscriber subscriber;

    private JCSMPSession session;
    private XMLMessageProducer producer;

    public AlertDashboard() {
        setTitle("Alert Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JList<AlertEntry> alertList = new JList<>(listModel);
        alertList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel("[" + value.getOriginalTs() + "] " + value.getText());
            lbl.setOpaque(true);
            lbl.setBackground(value.isRead() ? list.getBackground() : Color.RED);
            return lbl;
        });

        alertList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = alertList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    AlertEntry entry = listModel.get(idx);
                    entry.setRead(true);
                    alertList.repaint();

                    if (entry.getText().contains("SECURITY")) {
                        sendStopRequest();
                    }
                }
            }
        });

        add(new JScrollPane(alertList), BorderLayout.CENTER);

        try {
            session = com.example.solace.config.SolaceConfig.getSession();
            session.connect();
            producer = session.getMessageProducer(new HandleEvent());

            subscriber = new AlertTopicSubscriber(topic -> {
                SwingUtilities.invokeLater(() ->
                    listModel.add(0, new AlertEntry("🔔 경보 요청 전송됨", LocalDateTime.now()))
                );
            }, listModel);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "초기화 실패: " + e.getMessage());
            System.exit(1);
        }

        setVisible(true);
    }

    private void sendStopRequest() {
        try {
            Queue requestQueue = JCSMPFactory.onlyInstance().createQueue("Q.JAEYOUNG/REQUEST");
            Queue replyQueue = session.createTemporaryQueue();

            TextMessage stopRequest = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            stopRequest.setText("STOP");
            stopRequest.setReplyTo(replyQueue);
            stopRequest.setDeliveryMode(DeliveryMode.PERSISTENT);

            ConsumerFlowProperties props = new ConsumerFlowProperties();
            props.setEndpoint(replyQueue);
            props.setStartState(true);

            session.createFlow(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage msg) {
                    if (msg instanceof TextMessage) {
                        String response = ((TextMessage) msg).getText();
                        System.out.println("✅ 사운드 종료 응답 수신: " + response);
                        SwingUtilities.invokeLater(() ->
                            listModel.add(0, new AlertEntry("🛑 사운드 종료 응답: " + response, LocalDateTime.now()))
                        );
                    }
                }

                @Override
                public void onException(JCSMPException e) {
                    e.printStackTrace();
                }
            }, props).start();

            producer.send(stopRequest, requestQueue);
            System.out.println("📤 사운드 종료 요청 전송 완료");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AlertDashboard::new);
    }
}