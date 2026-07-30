package org.xhy.infrastructure.llm.http;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.time.Duration;

/** Builds a LangChain4j JDK client that always decodes streaming SSE data as UTF-8. */
public final class Utf8HttpClientBuilder implements HttpClientBuilder {

    private final JdkHttpClientBuilder delegate = JdkHttpClient.builder();

    @Override
    public Duration connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration timeout) {
        delegate.connectTimeout(timeout);
        return this;
    }

    @Override
    public Duration readTimeout() {
        return delegate.readTimeout();
    }

    @Override
    public HttpClientBuilder readTimeout(Duration timeout) {
        delegate.readTimeout(timeout);
        return this;
    }

    @Override
    public HttpClient build() {
        return new Utf8HttpClient(delegate.build());
    }

    private static final class Utf8HttpClient implements HttpClient {

        private final HttpClient delegate;

        private Utf8HttpClient(HttpClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException, RuntimeException {
            return delegate.execute(request);
        }

        @Override
        public void execute(HttpRequest request, ServerSentEventParser ignoredParser,
                ServerSentEventListener listener) {
            delegate.execute(request, Utf8ServerSentEventParser.INSTANCE, listener);
        }
    }
}
