package com.example.solace.pub;

import com.example.solace.config.SolaceConfig;
import com.example.solace.util.HandleEvent;
import com.solacesystems.jcsmp.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class AutoPublisher {
    private static XMLMessageProducer producer;
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void main(String[] args) throws JCSMPException {
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

        // 시뮬레이션용 파라미터
        List<String> SHOPS  = List.of("CONVENIENCE","ICECREAM","LAUNDRY");
        List<String> STORES = List.of("SEOUL01","SUWON01","BUSAN01","SEOUL02");
        List<String> EVENTS = List.of(
            "PAYMENT/SUCCESS",
            "PAYMENT/FAILURE",
            "SECURITY/THEFT"
        );

        // 주기적 발행 스케줄러 (10초마다)
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            try {
                int i = ThreadLocalRandom.current().nextInt(SHOPS.size());
                int j = ThreadLocalRandom.current().nextInt(STORES.size());
                int k = ThreadLocalRandom.current().nextInt(EVENTS.size());
                String[] parts = EVENTS.get(k).split("/");

                // 이벤트 발생 시점 타임스탬프
                String ts = LocalDateTime.now().format(TS_FORMATTER);
                String body = parts[0] + "/" + parts[1];
                String payload = ts + "|" + body;

                // 토픽 생성
                String topicStr = String.format(
                    "JY/%s/%s/%s/%s/TEST/USER",
                    SHOPS.get(i), STORES.get(j), parts[0], parts[1]
                );
                Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

                // 메시지 생성 및 전송
                TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                msg.setText(payload);
                msg.setDeliveryMode(DeliveryMode.PERSISTENT);
                producer.send(msg, topic);

                System.out.println("CCTV 시뮬 발행: " + topicStr + " | " + payload);
            } catch (JCSMPException e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
}
