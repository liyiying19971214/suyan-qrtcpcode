package com.example.suyanqrtcpcode.core.decoder;

import com.example.suyanqrtcpcode.bean.CustomProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 使用自定义解析器
 */
public class HeartbeatDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //givecode string 二个字符内容
        if (in.readableBytes() < 4) {
            // 数据不足，等待下次读取
            return;
        }
        byte[] bytes = new byte[in.readableBytes()] ;
        in.readBytes(bytes) ;
        String content = new String(bytes) ;
        CustomProtocol customProtocol = new CustomProtocol() ;
        //定义一个自定义的获取longid 的方法
        String ctxId = ctx.channel().id().asLongText();
        customProtocol.setId(ctxId);
        customProtocol.setContent(content) ;
        out.add(customProtocol) ;
    }


}