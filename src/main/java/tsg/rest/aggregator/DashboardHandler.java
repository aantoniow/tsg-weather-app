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

public class DashboardHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public final ApiAggregator aggregator;

    public DashboardHandler(ApiAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        if (!request.uri().equals("/api/dashboard") || !request.method().equals(HttpMethod.GET)) {
            sendResponse(context, HttpResponseStatus.NOT_FOUND, "Not Found");
            return;
        }

        // Launch async aggregation without blocking
        aggregator.aggregateData()
                .whenComplete((result, throwable) -> {
                    context.executor().execute(() -> {
                        if (throwable != null) {
                            sendResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                    "{\"error\":\"" + throwable.getMessage() + "\"}");
                        } else {
                            sendResponse(context, HttpResponseStatus.OK, result);
                        }
                    });
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

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        cause.printStackTrace();
        context.close();
    }

}