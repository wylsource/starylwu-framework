/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.filter.reactive;

import java.net.URI;
import java.time.Duration;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;

/**
 * @author Arjen Poutsma
 */
public class ForwardedHeaderFilterTests {

	private static final String BASE_URL = "http://example.com/path";


	private final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

	private final TestWebFilterChain filterChain = new TestWebFilterChain();


	@Test
	public void removeOnly() {
		ServerWebExchange exchange = MockServerWebExchange.from(get(BASE_URL)
				.header("Forwarded", "for=192.0.2.60;proto=http;by=203.0.113.43")
				.header("X-Forwarded-Host", "example.com")
				.header("X-Forwarded-Port", "8080")
				.header("X-Forwarded-Proto", "http")
				.header("X-Forwarded-Prefix", "prefix")
				.header("X-Forwarded-Ssl", "on"));

		this.filter.setRemoveOnly(true);
		this.filter.filter(exchange, this.filterChain).block(Duration.ZERO);

		HttpHeaders result = this.filterChain.getHeaders();
		assertNotNull(result);
		assertFalse(result.containsKey("Forwarded"));
		assertFalse(result.containsKey("X-Forwarded-Host"));
		assertFalse(result.containsKey("X-Forwarded-Port"));
		assertFalse(result.containsKey("X-Forwarded-Proto"));
		assertFalse(result.containsKey("X-Forwarded-Prefix"));
		assertFalse(result.containsKey("X-Forwarded-Ssl"));
	}

	@Test
	public void xForwardedRequest() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(get(BASE_URL)
				.header("X-Forwarded-Host", "84.198.58.199")
				.header("X-Forwarded-Port", "443")
				.header("X-Forwarded-Proto", "https"));

		assertEquals(new URI("https://84.198.58.199/path"), filterAndGetUri(exchange));
	}

	@Test
	public void forwardedRequest() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(get(BASE_URL)
				.header("Forwarded", "host=84.198.58.199;proto=https"));

		assertEquals(new URI("https://84.198.58.199/path"), filterAndGetUri(exchange));
	}

	@Test
	public void requestUriWithForwardedPrefix() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(get(BASE_URL)
				.header("X-Forwarded-Prefix", "/prefix"));

		assertEquals(new URI("http://example.com/prefix/path"), filterAndGetUri(exchange));
	}

	@Test
	public void requestUriWithForwardedPrefixTrailingSlash() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(get(BASE_URL)
				.header("X-Forwarded-Prefix", "/prefix/"));

		assertEquals(new URI("http://example.com/prefix/path"), filterAndGetUri(exchange));
	}

	@Nullable
	private URI filterAndGetUri(ServerWebExchange exchange) {
		this.filter.filter(exchange, this.filterChain).block(Duration.ZERO);
		return this.filterChain.uri;
	}


	private static class TestWebFilterChain implements WebFilterChain {

		@Nullable
		private HttpHeaders headers;

		@Nullable
		private URI uri;


		@Nullable
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Nullable
		public URI getUri() {
			return this.uri;
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange) {
			this.headers = exchange.getRequest().getHeaders();
			this.uri = exchange.getRequest().getURI();
			return Mono.empty();
		}
	}

}