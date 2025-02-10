//package com.example.suyanqrtcpcode.log;
//
//
//public class DataLogLayout extends Layout
//{
//
//    public DataLogLayout()
//    {
//        sbuf = new StringBuffer(128);
//    }
//
//    public void activateOptions()
//    {
//    }
//
//    public String format(LoggingEvent event)
//    {
//        sbuf.setLength(0);
//        sbuf.append(event.getRenderedMessage());
//        sbuf.append(LINE_SEP);
//        return sbuf.toString();
//    }
//
//    public boolean ignoresThrowable()
//    {
//        return true;
//    }
//
//    StringBuffer sbuf;
//}
