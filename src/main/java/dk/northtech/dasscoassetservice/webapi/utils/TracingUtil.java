//package dk.northtech.dasscoassetservice.webapi.utils;
//
//import io.opentelemetry.api.GlobalOpenTelemetry;
//import io.opentelemetry.api.trace.Span;
//import io.opentelemetry.context.Context;
//import io.opentelemetry.context.propagation.TextMapSetter;
//
//import java.net.http.HttpRequest;
//
//public class TracingUtil {
//    private static final TextMapSetter<HttpRequest.Builder> setter = (builder, key, value) -> {
//        System.out.println("Injecting Header: " + key + " = " + value);
//        builder.header(key, value);
//    };
//
//    public static HttpRequest.Builder injectTracing(HttpRequest.Builder requestBuilder) {
//        Context context = Context.current();
//        Span currentSpan = Span.current();
//        if (currentSpan.getSpanContext().isValid()){
//            System.out.println("Current Span is Valid: " + currentSpan.getSpanContext().getTraceId());
//        } else {
//            System.out.println("No current Span");
//        }
//
//        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, requestBuilder, setter);
//
//        return requestBuilder;
//    }
//}
