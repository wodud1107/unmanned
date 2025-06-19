package com.example.solace.topic;

// import java.util.Set;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.Topic;

public class TopicBuilder {
    // private static final Set<String> ALLOWED_SHOPS = Set.of("CONVENIENCE", "ICECREAM", "LAUNDRY");
    // private static final Set<String> ALLOWED_EVENT_TYPES = Set.of("ENTRY", "PAYMENT", "SECURITY");
    // private static final Set<String> ALLOWED_EVENT_DETAILS = Set.of("SUCCESS", "FAILURE", "IN", "OUT", "THEFT");
    // private static final Set<String> ALLOWED_ENVS = Set.of("PROD", "TEST");

    public static Topic build(String shop, String storeId, String eventType, String eventDetail, String env, String userId){
        // // 허용되는 매장 및 메시지 검증
        // validate("SHOP", shop, ALLOWED_SHOPS);
        // validate("EVENT_TYPE", eventType, ALLOWED_EVENT_TYPES);
        // validate("EVENT_DETAIL", eventDetail, ALLOWED_EVENT_DETAILS);
        // validate("ENV", env, ALLOWED_ENVS);

        // // 매장 및 이용자 형식 체크
        // if (!storeId.matches("^[A-Z0-9]+$")) {
        //     throw new IllegalArgumentException("❌ 잘못된 StoreID 형식: " + storeId);
        // }
        // if (!userId.matches("^USER[0-9]+$")) {
        //     throw new IllegalArgumentException("❌ 잘못된 UserID 형식: " + userId + " (예: USER1)");
        // }
        String topicStr = String.format("JY/%s/%s/%s/%s/%s/%s", shop, storeId, eventType, eventDetail, env, userId);
        return JCSMPFactory.onlyInstance().createTopic(topicStr);
    }

    // private static void validate(String field, String value, Set<String> allowed) {
    //     if (!allowed.contains(value)) {
    //         throw new IllegalArgumentException(String.format("❌ 허용되지 않는 값: %s\n 허용값: %s", value, allowed));
    //     }
    // }
}
