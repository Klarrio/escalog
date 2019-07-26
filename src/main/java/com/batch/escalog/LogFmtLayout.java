package com.batch.escalog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import org.slf4j.Marker;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.batch.escalog.LogFmtLayout.NativeKey.*;

/**
 * Logback Layout that format logs with logfmt format (ie. level="debug" ... key1="value1" key2="value2" ...)
 * @author Guillaume PERRUDIN
 */
public class LogFmtLayout extends LayoutBase<ILoggingEvent>
{
    private static final String LOGFMT_CLASS = com.batch.escalog.LogFmt.class.getName();
    private static final String LOGFMTBUILDER_CLASS = com.batch.escalog.LogFmtBuilder.class.getName();

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";


// ----------------------------------->
// logback.xml parameters

    /**
     * All the log lines will be prepended by this prefix (if present)
     */
    private String prefix = null;

    /**
     * The app name (adds the key app_name=[appName])
     */
    private String appName = null;

    /**
     * The time format
     */
    private String timeFormat;

// ----------------------------------->

    /**
     * Appenders registry by key
     */
    private final Map<String, KeyValueAppender> appenders = new HashMap<>();

    /**
     * List of appenders used if there's no field config
     */
    private final List<KeyValueAppender> defaultAppenders;

    /**
     * List of appenders used if there's a field config
     */
    private List<KeyValueAppender> customAppenders;

    /**
     * Formats the time field
     */
    private SimpleDateFormat newDf(String strformat) {
        SimpleDateFormat sdf = new SimpleDateFormat(strformat);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    private ThreadLocal<SimpleDateFormat> simpleDateFormat = ThreadLocal.withInitial(() -> newDf(timeFormat != null ? timeFormat : DATE_FORMAT));



    public LogFmtLayout()
    {
        appenders.put(TIME.toString(),      this::timeAppender);
        appenders.put(LEVEL.toString(),     this::levelAppender);
        appenders.put(MESSAGE.toString(),   this::msgAppender);
        appenders.put(TNAME.toString(),     this::threadAppender);
        appenders.put(EXCEPTION.toString(), this::errorAppender);
        appenders.put(LOGGER.toString(),    this::classAppender);

        this.defaultAppenders = new ArrayList<>(Arrays.asList(
            this::timeAppender, this::levelAppender,this::threadAppender, this::classAppender, this::msgAppender, this::errorAppender
        ));
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public void setTimeFormat(String timeFormat)
    {
        try
        {
            new SimpleDateFormat(timeFormat);
            this.timeFormat = timeFormat;
        }
        catch ( Exception e ) {}
    }


    public void setFields(String fields)
    {
        customAppenders = new ArrayList<>();
        for ( String field : fields.split(",") )
        {
            KeyValueAppender appender = appenders.get(field.trim());
            if( appender != null )
            {
                customAppenders.add(appender);
            }
        }
    }

// ----------------------------------->


    public String doLayout(ILoggingEvent iLoggingEvent)
    {
        StringBuilder sb = new StringBuilder();

        // prefix
        if ( prefix != null )
        {
            sb.append(prefix).append(' ');
        }

        // app_name
        if ( appName != null )
        {
            appendKeyValueAndEscape(sb, PNAME.toString(), appName);
        }

        for ( KeyValueAppender keyValueAppender : customAppenders != null ? customAppenders : defaultAppenders )
        {
            keyValueAppender.append(sb, iLoggingEvent);
        }

        // removes the last space char and adds a carriage return
        sb.setCharAt(sb.length() - 1, '\n');

        return sb.toString();
    }


    private void levelAppender(StringBuilder sb, ILoggingEvent iLoggingEvent)
    {
        appendKeyValueAndEscape(sb, LEVEL.toString(), formatLogLevel(iLoggingEvent.getLevel()));
    }

    private void timeAppender(StringBuilder sb, ILoggingEvent iLoggingEvent)
    {
        appendKeyValueAndEscape(sb, TIME.toString(), simpleDateFormat.get().format(new Date(iLoggingEvent.getTimeStamp())));
    }

    private void threadAppender(StringBuilder sb, ILoggingEvent iLoggingEvent)
    {
        appendKeyValueAndEscape(sb, TNAME.toString(), iLoggingEvent.getThreadName());
    }

    private void msgAppender(StringBuilder sb, ILoggingEvent iLoggingEvent)
    {
        appendKeyValueAndEscape(sb, MESSAGE.toString(), iLoggingEvent.getFormattedMessage());
    }

    private void errorAppender(StringBuilder sb, ILoggingEvent iLoggingEvent)
    {
        if ( iLoggingEvent.getThrowableProxy() != null )
        {
            appendKeyValueAndEscape(sb, EXCEPTION.toString(), ThrowableProxyUtil.asString(iLoggingEvent.getThrowableProxy()));
        }
    }

    private void classAppender(StringBuilder sb, ILoggingEvent iLoggingEvent)
    {
        String className = getLastClassName(iLoggingEvent.getCallerData());
        if ( className != null )
        {
            appendKeyValueAndEscape(sb, LOGGER.toString(), className);
        }

    }

    private String getLastClassName(StackTraceElement[] callerData)
    {
        String className = null;
        if ( callerData != null && callerData.length > 0 )
        {
            className = callerData[ 0 ].getClassName();

            // FIXME this is dirty. Find a way to remove last callerData when log from LogFmt
            if ( className.equals(LOGFMT_CLASS) || className.equals(LOGFMTBUILDER_CLASS) )
            {
                className = null;
                if ( callerData.length > 1 )
                {
                    className = callerData[ 1 ].getClassName();
                }

            }
        }
        return className;
    }

    @FunctionalInterface
    interface KeyValueAppender
    {
        void append(StringBuilder sb, ILoggingEvent iLoggingEvent);
    }


    /**
     * Appends the given key and value (escaped with escapeJava(String string)) to the given StringBuilder (appends key="value")
     */
    private static StringBuilder appendKeyValueAndEscape(StringBuilder sb, String key, Object value)
    {
        if ( key == null )
        {
            return sb;
        }

        if ( value == null )
        {
            value = "null";
        }

        sb.append(key).append('=');

        String valueStr = value.toString();

        if ( needsQuoting(valueStr) )
        {
            sb.append('"').append(escapeValue(valueStr)).append('"');
        }
        else
        {
            sb.append(valueStr);
        }

        sb.append(' ');
        return sb;
    }

    private static boolean needsQuoting(String value)
    {
        for ( int i = 0; i < value.length(); i++ )
        {
            char c = value.charAt(i);

            if ( !((c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                c == '-' || c == '.' || c == '_' || c == '/' || c == '@' || c == '^' || c == '+') )
            {
                return true;
            }
        }
        return false;
    }

// ----------------------------------->

    /**
     * Returns a StringBuilder representing the given string with characters escaped (see list of escaped characters here : https://docs.oracle.com/javase/tutorial/java/data/characters.html)
     */
    public static StringBuilder escapeValue(String string)
    {
        StringBuilder sb = new StringBuilder();

        for ( int i = 0; i < string.length(); i++ )
        {
            char c = string.charAt(i);
            switch ( c )
            {
                case '\t': sb.append("\\t");    break;
                case '\b': sb.append("\\b");    break;
                case '\n': sb.append("\\n");    break;
                case '\r': sb.append("\\r");    break;
                case '\f': sb.append("\\f");    break;
                case '\"': sb.append("\\\"");   break;
                case '\\': sb.append("\\\\");   break;
                default:   sb.append(c);
            }
        }

        return sb;
    }

    private static String formatLogLevel(Level level)
    {
        return level.toString().toLowerCase();
    }

    /**
     * Native keys that are automatically added by the Layout.
     * Cannot be used with Markers and MDC
     */
    enum NativeKey
    {
        TIME("time"),
        LEVEL("level"),
        MESSAGE("msg"),
        PNAME("pname"),
        TNAME("tname"),
        LOGGER("logger"),
        PID("pid"),
        TID("tid"),
        EXCEPTION("exception");

    // ----------------------------------->

        final String text;

    // ----------------------------------->

        NativeKey(final String text)
        {
            this.text = text;
        }

    // ----------------------------------->

        @Override
        public String toString()
        {
            return text;
        }

        public static boolean isNativeKey(String key)
        {
            NativeKey[] keys = values();
            for ( int i = 0; i < keys.length; i++ )
            {
                if ( keys[ i ].text.equals(key) )
                {
                    return true;
                }
            }

            return false;
        }
    }
}
