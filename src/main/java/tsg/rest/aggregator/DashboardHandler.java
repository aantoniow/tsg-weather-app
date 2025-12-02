package tsg.rest.aggregator;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;

public class DashboardHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger log = LoggerFactory.getLogger(DashboardHandler.class);
    public final AggregateService aggregateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardHandler(AggregateService aggregateService) {
        this.aggregateService = aggregateService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        if (!request.uri().equals(WebPath.ENDPOINT) || !request.method().equals(HttpMethod.GET)) {
            sendResponse(context, HttpResponseStatus.NOT_FOUND, "Not Found");
            return;
        }

        // Launch async aggregation without blocking
//        aggregator.aggregateData()
//                .whenComplete((result, throwable) -> {
//                    context.executor().execute(() -> {
//                        if (throwable != null) {
//                            sendResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
//                                    "{\"error\":\"" + throwable.getMessage() + "\"}");
//                        } else {
//                            sendResponse(context, HttpResponseStatus.OK, result);
//                        }
//                    });
//                });

        aggregateService.getAggregatedData()
                .thenAccept(aggregate -> {
                    try {
                        String json = objectMapper.writeValueAsString(aggregate);
                        sendResponse(context, HttpResponseStatus.OK, json);
                    } catch (Exception e) {
                        log.error("JSON serialization failed", e);
                        sendResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Aggregation failed", ex);
                    sendResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
                    return null;
                });
    }

    private void sendResponse(ChannelHandlerContext context, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set("Content-Type", "application/json; charset=UTF-8");
        response.headers().set("Content-Length", response.content().readableBytes());

        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}