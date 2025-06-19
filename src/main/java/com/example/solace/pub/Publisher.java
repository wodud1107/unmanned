package com.example.solace.pub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.example.solace.config.SolaceConfig;
import com.example.solace.topic.TopicBuilder;
import com.example.solace.util.HandleEvent;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class Publisher {
    private static XMLMessageProducer producer;
    public static void main(String[] args) throws JCSMPException, IOException {
        JCSMPSession session = SolaceConfig.getSession();
        producer = session.getMessageProducer(new HandleEvent() {
            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                try {
                    Topic errorTopic = JCSMPFactory.onlyInstance().createTopic("JY/SYSTEM/LOG/ERROR");

                    TextMessage errorMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                    errorMsg.setText("❌ 전송 실패 에러 발생\n"
                            + "메시지 ID: " + messageID + "\n"
                            + "사유: " + e.getMessage() + "\n"
                            + "시간: " + new java.util.Date(timestamp));

                    errorMsg.setDeliveryMode(DeliveryMode.PERSISTENT);

                    producer.send(errorMsg, errorTopic);
                    System.out.println("📤 실패 로그 전송 완료 → " + errorTopic.getName());

                } catch (JCSMPException sendError) {
                    System.err.println("⚠️ 실패 로그 전송 중 예외 발생: " + sendError.getMessage());
                }
            }
        });
        session.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("👀 매장 감시를 시작합니다.");

            try {
                System.out.print("매장 정보 입력 (EX|CONVENIENCE, ICECREAM) : ");
                String shop = br.readLine().toUpperCase();

                System.out.print("매장 위치 입력 (EX|SEOUL01, SUWON01) : ");
                String storeId = br.readLine().toUpperCase();

                System.out.print("알람 정보 입력 (EX|ENTRY, PAYMENT) : ");
                String eventType = br.readLine().toUpperCase();

                System.out.print("이벤트 정보 입력 (EX|SUCCESS, IN) : ");
                String eventDetail = br.readLine().toUpperCase();

                System.out.print("배포 환경 입력 (EX|PROD, TEST) : ");
                String env = br.readLine().toUpperCase();

                System.out.print("유저 정보 입력 (EX|USER1, USER2) : ");
                String userId = br.readLine().toUpperCase();

                Topic topic = TopicBuilder.build(shop, storeId, eventType, eventDetail, env, userId);

                TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                msg.setText("✉️" + eventType + "/" + eventDetail + "이벤트 발생");
                msg.setDeliveryMode(DeliveryMode.PERSISTENT);

                producer.send(msg, topic);

                Thread.sleep(500);
            } catch (Exception e) {
                System.err.println("⚠️ 전송 중 오류 발생 : " + e.getMessage());
            }
        }

    }
}