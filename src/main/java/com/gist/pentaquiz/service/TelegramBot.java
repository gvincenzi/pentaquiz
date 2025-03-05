package com.gist.pentaquiz.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
@Profile({"gist"})
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private static final String ITALIAN = "italian";

	@Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Autowired
    private MistralAIChatService mistralAIChatService;
    
    private final TelegramClient telegramClient;

    public TelegramBot(@Value("${telegram.bot.token}") String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }
    
	@Override
	public void consume(Update update) {
		if(update.hasMessage() && update.getMessage().hasPhoto()) {
			try {
				PhotoSize photoSize = update.getMessage().getPhoto().get(update.getMessage().getPhoto().size()-1);
				InputStream inputStream = getFile(photoSize.getFileId());
				String lang = update.getMessage().getCaption() != null ? update.getMessage().getCaption() : ITALIAN;
				SendMessage message = prepareMessage(Long.toString(update.getMessage().getChatId()), mistralAIChatService.bookQuiz(inputStream, lang ));
				telegramClient.execute(message);
			} catch (TelegramApiException | IOException e) {
				log.error(e.getMessage());
			}
		}
	}

	@Override
	public LongPollingUpdateConsumer getUpdatesConsumer() {
		return this;
	}
	
	public InputStream getFile(String fileId) throws TelegramApiException, IOException {
    	//GetFile getFile = new GetFile(document.getFileId());
		GetFile getFile = new GetFile(fileId);
    	String filePath = telegramClient.execute(getFile).getFilePath();
        return telegramClient.downloadFileAsStream(filePath);
    }
	
	public SendMessage prepareMessage(String chat_id, String text) {
        SendMessage sendMessage = new SendMessage(chat_id,text);
        return sendMessage;
    }
}
