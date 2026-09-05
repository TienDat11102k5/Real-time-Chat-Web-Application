package com.chat.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequestCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(RequestCleanupTask.class);

    private final ConnectionManager connectionManager;

    public RequestCleanupTask(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Scheduled(fixedRate = 5000)
    public void cleanupExpiredPrivateRequests() {
        List<PrivateRequest> expired = connectionManager.cleanupExpiredRequests();
        for (PrivateRequest req : expired) {
            String sender = req.getSender();
            String receiver = req.getReceiver();

            logger.info("[YÊU CẦU HẾT HẠN] {} -> {} ({}s)", sender, receiver, ConnectionManager.REQUEST_TIMEOUT_SECONDS);

            Map<String, Object> notifySender = new HashMap<>();
            notifySender.put("type", "system");
            notifySender.put("message", "Yêu cầu chat với " + receiver + " đã hết hạn (" + ConnectionManager.REQUEST_TIMEOUT_SECONDS + "s)");
            connectionManager.sendMessage(sender, notifySender);

            Map<String, Object> notifyReceiver = new HashMap<>();
            notifyReceiver.put("type", "system");
            notifyReceiver.put("message", "Yêu cầu chat từ " + sender + " đã hết hạn (" + ConnectionManager.REQUEST_TIMEOUT_SECONDS + "s)");
            connectionManager.sendMessage(receiver, notifyReceiver);
        }
    }
}
