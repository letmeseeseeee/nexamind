package org.xhy.infrastructure.llm.http;

import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Utf8ServerSentEventParserTest {

    @Test
    void preservesChineseCharactersWhenJvmDefaultCharsetIsNotUtf8() {
        String stream = "data: {\"content\":\"DeepSeek \u8fde\u63a5\u6210\u529f\"}\n\n" + "event: done\n"
                + "data: [DONE]\n\n";
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();

        Utf8ServerSentEventParser.INSTANCE.parse(new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8)),
                new ServerSentEventListener() {
                    @Override
                    public void onEvent(ServerSentEvent event) {
                        events.add(event);
                    }

                    @Override
                    public void onError(Throwable error) {
                        errors.add(error);
                    }
                });

        assertEquals(2, events.size());
        assertEquals("{\"content\":\"DeepSeek \u8fde\u63a5\u6210\u529f\"}", events.get(0).data());
        assertNull(events.get(0).event());
        assertEquals("done", events.get(1).event());
        assertEquals("[DONE]", events.get(1).data());
        assertEquals(0, errors.size());
    }
}
