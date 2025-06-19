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

        Queue requestQueue = JCSMPFactory.onlyInstance().createQueue("Q.JAEYOUNG3");
        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(requestQueue);
        flowProps.setStartState(true);

        producer = session.getMessageProducer(new HandleEvent() {
            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                try {
                    Topic errorTopic = JCSMPFactory.onlyInstance().createTopic("JY/SYSTEM/LOG/ERROR");
                    TextMessage errorMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                    errorMsg.setText(
                        "❌ SOUND 요청 실패\n" +
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

        FlowReceiver receiver = session.createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                Destination replyTo = msg.getReplyTo();
                if (replyTo == null) {
                    System.err.println("⚠️ ReplyTo 큐가 지정되지 않았습니다. 응답 생략됨.");
                    return;
                }

                System.out.println("📥 경보 요청 수신 → 비프음 실행");
                startBeeping();

                try {
                    TextMessage reply = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                    reply.setText("✅ SOUND_ACK");
                    reply.setDeliveryMode(DeliveryMode.PERSISTENT);
                    producer.send(reply, replyTo);
                    System.out.println("📤 응답 전송 완료 → " + replyTo.getName());
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

        while (true) Thread.sleep(1000);
    }

    private static void startBeeping() {
        if (beeper != null && !beeper.isShutdown()) return;
        System.out.println("🚨 비프음 시작");
        beeper = Executors.newSingleThreadScheduledExecutor();
        beeper.scheduleAtFixedRate(() ->
            Toolkit.getDefaultToolkit().beep(),
            0, 2, TimeUnit.SECONDS
        );
    }
}