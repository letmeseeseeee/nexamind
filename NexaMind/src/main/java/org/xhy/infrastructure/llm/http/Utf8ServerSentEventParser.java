package org.xhy.infrastructure.llm.http;

import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Parses SSE responses with the UTF-8 encoding required by the SSE specification. */
final class Utf8ServerSentEventParser implements ServerSentEventParser {

    static final Utf8ServerSentEventParser INSTANCE = new Utf8ServerSentEventParser();

    private Utf8ServerSentEventParser() {
    }

    @Override
    public void parse(InputStream inputStream, ServerSentEventListener listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String event = null;
            StringBuilder data = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    event = dispatch(listener, event, data);
                } else if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).trim());
                }
            }

            if (data.length() > 0) {
                listener.onEvent(new ServerSentEvent(event, data.toString()));
            }
        } catch (IOException exception) {
            listener.onError(exception);
        }
    }

    private String dispatch(ServerSentEventListener listener, String event, StringBuilder data) {
        if (data.length() > 0) {
            listener.onEvent(new ServerSentEvent(event, data.toString()));
            data.setLength(0);
        }
        return null;
    }
}
