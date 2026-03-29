import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
//сервис для отправки сообщений в телеграмм боте
public class AutoResponder {

    private final List<IAutoResponder> responders;
    private final RedisCacheService redisCacheService;

    public void executeRespond(MenuContext menuContext, IMenuStater newState) {
        boolean currentHasMedia = UpdateUtils.existMedia(menuContext.getUpdate());
        boolean newHasMedia = newState.getFileId(menuContext) != null;

        calculateRespond(currentHasMedia, newHasMedia).respond(menuContext, newState, null);
    }

    //для сохранения первого месседжа или ансейва последующего(в целом для сценариев чтобы не оставались ненужные сообщ ввода)
    public void executeRespond(MenuContext menuContext, IMenuStater newState, boolean saveMessage) {
        BotMessage botMessage = getBotMessage(menuContext);
        boolean currentHasMedia = botMessage != null
                ? botMessage.getMediaType() != MediaType.NONE
                : UpdateUtils.existMedia(menuContext.getUpdate());
        boolean newHasMedia = newState.getFileId(menuContext) != null;

        if (botMessage == null) {
            botMessage = new BotMessage(null, null, saveMessage);
        }
        else {
            botMessage.setSaveMessage(saveMessage);
        }

        calculateRespond(currentHasMedia, newHasMedia).respond(menuContext, newState, botMessage);
    }

    private IAutoResponder calculateRespond(boolean currentHasMedia, boolean newHasMedia) {
        IAutoResponder responder = responders.stream()
            .filter(r -> r.equals(currentHasMedia, newHasMedia))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No responder found for state"));

        return responder;
    }

    private BotMessage getBotMessage(MenuContext menuContext) {
        return redisCacheService.getHashObjectField(
                RedisHashData.BOT_MESSAGE.getKey() + UpdateUtils.getUserId(menuContext.getUpdate()),
                RedisHashData.BOT_MESSAGE.getFieldName(),
                BotMessage.class
        );
    }
}
