package com.example.namegeneratorservice;
 
import io.opentracing.Span;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.io.IOException;
 
import static strman.Strman.toKebabCase;
import io.opentracing.Tracer;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;

import io.jaegertracing.internal.samplers.ConstSampler;
import org.springframework.context.annotation.Bean;
import javax.annotation.Resource;

@SpringBootApplication
public class NameGeneratorService {
 
    public static void main(String[] args) {
        SpringApplication.run(NameGeneratorService.class, args);
    }
    /* Configure sender host and port details */
    private static final int JAEGER_PORT = 6831;
    private static final String JAEGER_HOST = "127.0.0.1";
    /* End */
    @Bean
    public Tracer tracer() {
        SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        SenderConfiguration senderConfig = Configuration.SenderConfiguration.fromEnv()
                .withAgentHost(JAEGER_HOST)
                .withAgentPort(JAEGER_PORT);

        ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv()
                .withLogSpans(true)
                .withSender(senderConfig);
     
        Configuration config = new Configuration("name-generator-service")
                .withSampler(samplerConfig)
                .withReporter(reporterConfig);
     
        return config.getTracer();
    }
}
 


@RestController
@RequestMapping("/api/v1/names")
class NameResource {
 
    OkHttpClient client = new OkHttpClient();
    @Resource
    private Tracer tracer;

    @GetMapping(path = "/random")
    public String name() throws Exception {
        Span span = tracer.buildSpan("generate-name").start();
 
        Span scientistSpan = tracer.buildSpan("scientist-name-service").asChildOf(span).start();
        String scientist = makeRequest("http://localhost:8090/api/v1/scientists/random");
        scientistSpan.finish();
     
        Span animalSpan = tracer.buildSpan("animal-name-service").asChildOf(span).start();
        String animal = makeRequest("http://localhost:9000/api/v1/animals/random");
        animalSpan.finish();
     
        String name = toKebabCase(scientist) + "-" + toKebabCase(animal);
        span.finish();
        return name;
    }
 
    private String makeRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
 
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
 
}