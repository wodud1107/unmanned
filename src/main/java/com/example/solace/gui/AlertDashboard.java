package com.example.solace.gui;

import com.example.solace.config.SolaceConfig;
import com.example.solace.util.AlertEntry;
import com.example.solace.util.HandleEvent;
import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.Queue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AlertDashboard extends JFrame {
    private final DefaultListModel<AlertEntry> listModel = new DefaultListModel<>();
    private XMLMessageProducer producer;
    private String lastSecurityTopic = null;

    public AlertDashboard() throws JCSMPException {
        super("Alert Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);

        JList<AlertEntry> alertList = new JList<>(listModel);
        alertList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel("[" + value.getOriginalTs() + "] " + value.getText());
            lbl.setOpaque(true);
            if (!value.isRead() && value.isBlinkState()) lbl.setBackground(Color.RED);
            else lbl.setBackground(list.getBackground());
            return lbl;
        });
        alertList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int idx = alertList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    AlertEntry entry = listModel.get(idx);
                    entry.setRead(true);
                    alertList.repaint();

                    // SECURITY 메시지 클릭 시 사운드 멈춤 명령 전송
                    if (entry.getText().startsWith("SECURITY") && lastSecurityTopic != null) {
                        try {
                            Topic stopTopic = JCSMPFactory.onlyInstance()
                                .createTopic("JY/SYSTEM/CMD/STOP");
                            TextMessage stopMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                            stopMsg.setText("STOP");
                            stopMsg.setDeliveryMode(DeliveryMode.PERSISTENT);
                            producer.send(stopMsg, stopTopic);
                            System.out.println("🛑 사운드 멈춤 명령 전송: " + stopTopic.getName());
                        } catch (JCSMPException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        add(new JScrollPane(alertList), BorderLayout.CENTER);

        new Timer(500, evt -> {
            for (int i = 0; i < listModel.size(); i++) {
                AlertEntry entry = listModel.get(i);
                if (!entry.isRead()) entry.setBlinkState(!entry.isBlinkState());
            }
            alertList.repaint();
        }).start();

        JCSMPSession session = SolaceConfig.getSession();
        session.connect();
        producer = session.getMessageProducer(new HandleEvent() {
            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                try {
                    Topic errorTopic = JCSMPFactory.onlyInstance().createTopic("JY/SYSTEM/LOG/ERROR");
                    TextMessage errorMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                    errorMsg.setText(
                        "❌ 전송 실패 발생\n" +
                        "메시지 ID: " + messageID + "\n" +
                        "사유: " + e.getMessage() + "\n" +
                        "시간: " + new java.util.Date(timestamp)
                    );
                    errorMsg.setDeliveryMode(DeliveryMode.PERSISTENT);
                    producer.send(errorMsg, errorTopic);
                    System.out.println("📤 실패 로그 전송 완료 → " + errorTopic.getName());
                } catch (JCSMPException sendError) {
                    System.err.println("⚠️ 실패 로그 전송 중 예외 발생: " + sendError.getMessage());
                }
            }
        });

        Queue queue = JCSMPFactory.onlyInstance().createQueue("Q.JAEYOUNG3");
        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setStartState(true);

        FlowReceiver receiver = session.createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage) {
                    String payload = ((TextMessage) msg).getText();
                    String topicName = msg.getDestination().getName();
                    String[] parts = payload.split("\\|", 2);
                    String ts = parts[0];
                    String text = parts.length > 1 ? parts[1] : parts[0];

                    SwingUtilities.invokeLater(() -> {
                        listModel.add(0, new AlertEntry(text, ts));
                        if (listModel.size() > 100) listModel.removeElementAt(100);
                    });

                    if (topicName.contains("/SECURITY/THEFT")) {
                        lastSecurityTopic = topicName;
                        String[] topicParts = topicName.split("/");
                        String shop = topicParts[1];
                        String storeId = topicParts[2];
                        try {
                            Topic cmdTopic = JCSMPFactory.onlyInstance()
                                .createTopic("JY/" + shop + "/" + storeId + "/SECURITY/WARNING");
                            TextMessage cmdMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                            cmdMsg.setText("도난 경보 발생");
                            cmdMsg.setDeliveryMode(DeliveryMode.PERSISTENT);
                            producer.send(cmdMsg, cmdTopic);
                            System.out.println("▶ 경보음 명령 전송: " + cmdTopic.getName());
                        } catch (JCSMPException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                SwingUtilities.invokeLater(() ->
                    listModel.add(0, new AlertEntry("⚠️ Subscriber error: " + e.getMessage(),
                        LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)))
                );
            }
        }, flowProps);
        receiver.start();

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new AlertDashboard();
            } catch (JCSMPException e) {
                JOptionPane.showMessageDialog(null,
                    "Solace 연결 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
