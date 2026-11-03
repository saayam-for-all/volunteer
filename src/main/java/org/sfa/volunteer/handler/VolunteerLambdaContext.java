package org.sfa.volunteer.handler;

import org.sfa.volunteer.VolunteerApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

/** Lazily boots the service beans for direct Lambda entry points. */
final class VolunteerLambdaContext {
    private VolunteerLambdaContext() {}

    static <T> T bean(Class<T> type) {
        return Holder.CONTEXT.getBean(type);
    }

    private static final class Holder {
        private static final ApplicationContext CONTEXT = new SpringApplicationBuilder(VolunteerApplication.class)
                .web(WebApplicationType.NONE)
                // The AWS servlet bridge expects a web server context and is unnecessary for direct handlers.
                .properties("spring.autoconfigure.exclude=org.springframework.cloud.function.serverless.web.ServerlessAutoConfiguration")
                .run();
    }
}
