package com.example.solace.util;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;

public class HandleEvent implements JCSMPStreamingPublishEventHandler {
    @Override
    public void responseReceived(String messageID) {
        System.out.println("✅ 메시지 전송 성공: ID = " + messageID);
    }

    @Override
    public void handleError(String messageID, JCSMPException e, long timestamp) {
        System.out.println("❌ 메시지 전송 실패: ID = " + messageID + ", 오류: " + e.getMessage());
    }
}
