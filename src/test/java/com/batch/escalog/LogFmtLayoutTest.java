package com.batch.escalog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.junit.Test;
import org.slf4j.Marker;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

import static com.batch.escalog.LogFmtLayout.escapeValue;
import static com.batch.escalog.LogFmtMarker.with;
import static org.junit.Assert.assertEquals;

/**
 * Tests LogFmtLayout
 * @author Guillaume PERRUDIN
 */
public class LogFmtLayoutTest
{

    @Test
    public void escapeTest()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        System.out.println(sdf.format(new Date()));
        assertEquals("the \\\"message\\\"", escapeValue("the \"message\"").toString());
        assertEquals("the \\n carriage \\n return", escapeValue("the \n carriage \n return").toString());
        assertEquals("the \\t tab \\t", escapeValue("the \t tab \t").toString());
        assertEquals("the \\\\ backslash \\\\", escapeValue("the \\ backslash \\").toString());
    }

    @Test
    public void logFmtLayoutTest()
    {

        String appName = "escalog";
        String prefix = "prefix=\"prefix\"";

        LogFmtLayout logFmtLayout = new LogFmtLayout();

        logFmtLayout.setAppName(appName);
        logFmtLayout.setPrefix(prefix);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(2017, Calendar.NOVEMBER, 30, 15, 10, 25);
        calendar.set(Calendar.MILLISECOND, 123);
        ILoggingEvent loggingEvent = createLoggingEvent("thread0", Level.DEBUG, calendar.getTime(),
            with("key1", "value1").and("key2", "val ue2"), "message with \"double quotes\"", null);


        assertEquals(
            "prefix=\"prefix\" pname=escalog time=\"2017-11-30T15:10:25.123Z\" level=debug tname=thread0 logger=loggerName msg=\"message with \\\"double quotes\\\"\" key1=value1 key2=\"val ue2\"\n",
            logFmtLayout.doLayout(loggingEvent)
        );

    }

    @Test
    public void timeZoneTest()
    {

        String appName = "escalog";
        String prefix = "prefix=\"prefix\"";

        LogFmtLayout logFmtLayout = new LogFmtLayout();

        logFmtLayout.setAppName(appName);
        logFmtLayout.setPrefix(prefix);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+4"));
        calendar.set(2017, Calendar.NOVEMBER, 30, 15, 10, 25);
        calendar.set(Calendar.MILLISECOND, 123);
        ILoggingEvent loggingEvent = createLoggingEvent("thread0", Level.DEBUG, calendar.getTime(),
                with("key1", "value1").and("key2", "val ue2"), "message with \"double quotes\"", null);


        assertEquals(
                "prefix=\"prefix\" pname=escalog time=\"2017-11-30T11:10:25.123Z\" level=debug tname=thread0 logger=loggerName msg=\"message with \\\"double quotes\\\"\" key1=value1 key2=\"val ue2\"\n",
                logFmtLayout.doLayout(loggingEvent)
        );

    }

    @Test
    public void fieldsConfigTest()
    {
        LogFmtLayout logFmtLayout = new LogFmtLayout();
        logFmtLayout.setFields("time, mdc, level, msg, custom");
        logFmtLayout.setTimeFormat("YYYY");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.NOVEMBER, 30, 15, 10, 25);

        Map<String, String> mdc = new HashMap<>();
        mdc.put("mdckey", "mdc value");

        ILoggingEvent loggingEvent = createLoggingEvent("thread0", Level.DEBUG, calendar.getTime(),
            with("key1", "value1").and("key2", "val ue2"), "message with \"double quotes\"", mdc);

        assertEquals(
            "time=2017 level=debug msg=\"message with \\\"double quotes\\\"\" key1=value1 key2=\"val ue2\"\n",
            logFmtLayout.doLayout(loggingEvent)
        );
    }

    ILoggingEvent createLoggingEvent(String threadName, Level logLevel, Date date, Marker marker, String msg, Map<String, String> mdc)
    {
        return new ILoggingEvent()
        {
            @Override
            public String getThreadName()
            {
                return threadName;
            }

            @Override
            public Level getLevel()
            {
                return logLevel;
            }

            @Override
            public String getMessage()
            {
                return msg;
            }

            @Override
            public Object[] getArgumentArray()
            {
                return new Object[ 0 ];
            }

            @Override
            public String getFormattedMessage()
            {
                return msg;
            }

            @Override
            public String getLoggerName()
            {
                return "loggerName";
            }

            @Override
            public LoggerContextVO getLoggerContextVO()
            {
                return null;
            }

            @Override
            public IThrowableProxy getThrowableProxy()
            {
                return null;
            }

            @Override
            public StackTraceElement[] getCallerData()
            {
                return null;
            }

            @Override
            public boolean hasCallerData()
            {
                return false;
            }

            @Override
            public Marker getMarker()
            {
                return marker;
            }

            @Override
            public Map<String, String> getMDCPropertyMap()
            {
                return mdc;
            }

            @Override
            public Map<String, String> getMdc()
            {
                return null;
            }

            @Override
            public long getTimeStamp()
            {
                return date.getTime();
            }

            @Override
            public void prepareForDeferredProcessing()
            {

            }
        };

    }

}
