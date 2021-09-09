/*
  Copyright 2021 The Cyber Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package io.github.aomsweet.cyber.http.interceptor;

import io.github.aomsweet.cyber.HandlerNames;
import io.github.aomsweet.cyber.http.ChannelContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;

/**
 * @author aomsweet
 */
public abstract class FullHttpResponseInterceptor extends FullHttpMessageInterceptor<FullHttpResponseInterceptor>
    implements HttpResponseInterceptor {

    public FullHttpResponseInterceptor() {
        super();
    }

    public FullHttpResponseInterceptor(int maxContentLength) {
        super(maxContentLength);
    }

    @Override
    public boolean preHandle(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext context) throws Exception {
        ChannelPipeline pipeline = context.getServerChannel().pipeline();
        if (httpResponse instanceof FullHttpResponse) {
            pipeline.remove(HandlerNames.DECOMPRESS);
            pipeline.remove(HandlerNames.AGGREGATOR);
            return preHandle(httpRequest, (FullHttpResponse) httpResponse, context);
        } else {
            pipeline
                .addAfter(HandlerNames.DECODER, HandlerNames.DECOMPRESS, new HttpContentDecompressor())
                .addAfter(HandlerNames.DECOMPRESS, HandlerNames.AGGREGATOR, new HttpObjectAggregator(maxContentLength))
                .fireChannelRead(httpResponse);
            return false;
        }
    }

    public abstract boolean preHandle(HttpRequest httpRequest, FullHttpResponse httpResponse, ChannelContext context) throws Exception;
}
