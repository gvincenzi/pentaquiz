package com.gist.pentaquiz.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
@Profile({"gist"})
public class MistralAIChatService {
	@Autowired
	MistralAiChatModel chatModel;
	
	@Value("classpath:/prompts/book.st")
	private Resource bookResource;
	
	@Value("${telegram.bot.assistant.title}")
	private String assistantTitle;
	
	public String bookQuiz(InputStream photo, String language) {
		log.info(String.format("%s -> %s", MistralAIChatService.class.getSimpleName(),"bookQuiz"));
		Resource resource = new InputStreamResource(photo);
		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.bookResource);
		Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", assistantTitle, "language", language));
		UserMessage userMessage = new UserMessage(systemMessage.getText(),
				List.of(Media.builder()
					.mimeType(MimeTypeUtils.IMAGE_PNG)
					.data(resource)
					.build()));
		Prompt prompt = new Prompt(List.of(userMessage, systemMessage),ChatOptions.builder().model(MistralAiApi.ChatModel.PIXTRAL.getValue()).build());
		log.info(String.format("Calling MistralAI"));
		ChatResponse response = this.chatModel.call(prompt);
		log.info(String.format("MistralAI FinishReason : %s",response.getResult().getMetadata().getFinishReason()));
		return response.getResult().getOutput().getText();
	}
}
