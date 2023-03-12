package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.core.io.BigDecimalParser.parse;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final Pattern NOTIFICATION_TASK_PATTERN = Pattern.compile
            ("([\\d\\\\.:\\s]{16})(\\s)([А-яA-z\\s\\d]+)");


    private final TelegramBot telegramBot;
    private final NotificationTaskService notificationTaskService;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskService notificationTaskService) {
        this.telegramBot = telegramBot;
        this.notificationTaskService = notificationTaskService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        try {
            updates.forEach(update -> {
                logger.info("Processing update: {}", update);
                String text = update.message().text();
                User user = update.message().from();
                if ("/start".equals(text)) {
                    SendMessage sendMessage = new SendMessage(user.id(),
                            "для обработки сообщения пришлите его в формате: \n*дд.мм.гггг ЧЧ:ММ Сделать домашнюю работу*");
                    sendMessage.parseMode(ParseMode.Markdown); //текст сообщения будет жирным шрифтом от * до *
                    telegramBot.execute(sendMessage);
                } else {
                    Matcher matcher = NOTIFICATION_TASK_PATTERN.matcher(text);
                    if (matcher.find()) {
                        LocalDateTime localDateTime = parseLocalDateTime(matcher.group(1));
                        if (!Objects.isNull(localDateTime)) {
                            String message = matcher.group(3);
                            notificationTaskService.addNotificationTask(localDateTime, message, user.id());
                            telegramBot.execute(new SendMessage(user.id(), "Ваша задача запланирована!"));
                        } else {
                            telegramBot.execute(
                                    new SendMessage(user.id(), "Некорректный формат даты и/или времени!"));
                        }
                    } else {
                        telegramBot.execute(new SendMessage(user.id(),
                                "Некорректный формат задачи для планирования! Корректный формат: дд.мм.гггг ЧЧ:ММ Сделать домашнюю работу"));
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Nullable
    private LocalDateTime parseLocalDateTime(String localDateTime) {
        try {
            return LocalDateTime.parse(localDateTime, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
