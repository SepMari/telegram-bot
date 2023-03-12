package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationTaskTimer {
    private final NotificationTaskService notificationTaskService;
    private final TelegramBot telegramBot;

    public NotificationTaskTimer(NotificationTaskService notificationTaskService, TelegramBot telegramBot) {
        this.notificationTaskService = notificationTaskService;
        this.telegramBot = telegramBot;
    }

    @Scheduled(fixedDelay = 60 * 1_000)
    public void checkNotifications() {
        notificationTaskService.findNotificationsForSend().forEach(notificationTask -> {
            telegramBot.execute(
                    new SendMessage(notificationTask.getUserId(),
                            "Напоминание: " + notificationTask.getMessage())
            );
            notificationTaskService.deleteTask(notificationTask);
        });
    }
}
