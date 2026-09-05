package com.chat;

import com.chat.model.ChatMessage;
import com.chat.model.User;
import com.chat.repository.MessageRepository;
import com.chat.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
public class DatabaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void testDatabaseUserOperations() {
        String testUser = "test_user_" + System.currentTimeMillis();
        String passHash = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
        String now = "2026-09-05 12:00:00";

        // 1. Tạo user
        boolean created = userRepository.createUser(testUser, passHash, now);
        Assertions.assertTrue(created, "Phải tạo được user mới vào SQLite");

        // 2. Kiểm tra tồn tại
        boolean exists = userRepository.existsByUsername(testUser);
        Assertions.assertTrue(exists, "User vừa tạo phải tồn tại");

        // 3. Đọc user
        Optional<User> opt = userRepository.findByUsername(testUser);
        Assertions.assertTrue(opt.isPresent(), "Phải đọc được user vừa tạo");
        Assertions.assertEquals(testUser, opt.get().getUsername());
        Assertions.assertEquals(passHash, opt.get().getPasswordHash());

        // 4. Cập nhật mật khẩu
        String newPassHash = "88d4266fd4e6338d13b845fcf289579d209c897823b9217da3e1619363c18812";
        boolean updated = userRepository.updatePassword(testUser, newPassHash);
        Assertions.assertTrue(updated, "Phải cập nhật được mật khẩu");

        Optional<User> updatedUser = userRepository.findByUsername(testUser);
        Assertions.assertEquals(newPassHash, updatedUser.get().getPasswordHash());
    }

    @Test
    public void testDatabaseMessageOperations() {
        String alice = "alice_" + System.currentTimeMillis();
        String bob = "bob_" + System.currentTimeMillis();
        String charlie = "charlie_" + System.currentTimeMillis();

        // 1. Lưu tin nhắn công khai
        boolean pubSaved = messageRepository.savePublicMessage(alice, "Hello public room!", "2026-09-05 12:01:00");
        Assertions.assertTrue(pubSaved, "Phải lưu được tin nhắn public");

        List<ChatMessage> publicHistory = messageRepository.getPublicHistory(10);
        Assertions.assertFalse(publicHistory.isEmpty(), "Lịch sử public không được rỗng");
        boolean foundPublic = publicHistory.stream().anyMatch(m -> "Hello public room!".equals(m.getMessage()));
        Assertions.assertTrue(foundPublic, "Phải tìm thấy tin nhắn public vừa gửi");

        // 2. Lưu tin nhắn riêng giữa Alice và Bob
        boolean priv1 = messageRepository.savePrivateMessage(alice, bob, "Hi Bob, private message 1", "2026-09-05 12:02:00");
        boolean priv2 = messageRepository.savePrivateMessage(bob, alice, "Hi Alice, private reply 2", "2026-09-05 12:03:00");
        Assertions.assertTrue(priv1 && priv2, "Phải lưu được tin nhắn riêng");

        // 3. Đọc lịch sử riêng giữa Alice và Bob (theo cả 2 chiều)
        List<ChatMessage> privateHistory = messageRepository.getPrivateHistory(alice, bob, 10);
        Assertions.assertTrue(privateHistory.size() >= 2, "Lịch sử riêng phải có ít nhất 2 tin");

        // 4. Kiểm tra tin nhắn riêng của Alice & Bob KHÔNG lộ sang Charlie
        List<ChatMessage> charlieHistory = messageRepository.getPrivateHistory(alice, charlie, 10);
        boolean leaked = charlieHistory.stream().anyMatch(m -> m.getMessage().contains("Bob"));
        Assertions.assertFalse(leaked, "Tin nhắn riêng của Bob không được lọt sang Charlie");
    }
}
