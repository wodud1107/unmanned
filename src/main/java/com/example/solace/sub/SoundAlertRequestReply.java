package com.example.solace.sub;

import com.example.solace.config.SolaceConfig;
import com.example.solace.util.HandleEvent;
import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.Queue;

import java.awt.*;
import java.util.concurrent.*;

public class SoundAlertRequestReply {
    private static XMLMessageProducer producer;
    private static ScheduledExecutorService beeper;

    public static void main(String[] args) throws Exception {
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
                                    "시간: " + new java.util.Date(timestamp));
                    errorMsg.setDeliveryMode(DeliveryMode.PERSISTENT);
                    producer.send(errorMsg, errorTopic);
                    System.out.println("📤 실패 로그 전송 완료 → " + errorTopic.getName());
                } catch (JCSMPException sendError) {
                    System.err.println("⚠️ 실패 로그 전송 중 예외 발생: " + sendError.getMessage());
                }
            }
        });

        Queue requestQueue = JCSMPFactory.onlyInstance().createQueue("Q.JAEYOUNG/REQUEST");
        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(requestQueue);
        flowProps.setStartState(true);

        FlowReceiver receiver = session.createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                Destination replyTo = msg.getReplyTo();
                String content = ((TextMessage) msg).getText();

                try {
                    TextMessage reply = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                    reply.setDeliveryMode(DeliveryMode.PERSISTENT);

                    if ("STOP".equalsIgnoreCase(content)) {
                        stopBeeping();
                        System.out.println("🛑 사운드 종료 요청 처리");
                        reply.setText("✅ SOUND_STOP_ACK");
                    } else {
                        startBeeping();
                        System.out.println("🚨 사운드 재생 요청 처리");
                        reply.setText("✅ SOUND_START_ACK");
                    }

                    if (replyTo != null) {
                        producer.send(reply, replyTo);
                        System.out.println("📨 응답 전송: " + reply.getText());
                    } else {
                        System.err.println("⚠️ ReplyTo 큐 없음. 응답 생략됨");
                    }
                } catch (JCSMPException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onException(JCSMPException e) {
                e.printStackTrace();
            }
        }, flowProps);

        receiver.start();
        System.out.println("🔔 경보 큐 대기 시작");
        while (true)
            Thread.sleep(1000);
    }

    private static void startBeeping() {
        if (beeper != null && !beeper.isShutdown())
            return;
        System.out.println("🚨 비프음 시작");
        beeper = Executors.newSingleThreadScheduledExecutor();
        beeper.scheduleAtFixedRate(() -> Toolkit.getDefaultToolkit().beep(), 0, 2, TimeUnit.SECONDS);
    }

    private static void stopBeeping() {
        if (beeper != null && !beeper.isShutdown()) {
            System.out.println("🛑 비프음 종료");
            beeper.shutdownNow(); // 즉시 중지
            beeper = null;
        } else {
            System.out.println("ℹ️ 비프음이 실행 중이 아님");
        }
    }
}