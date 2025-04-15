package org.sfa.volunteer.configTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class corsconfigtest {

    @Value("${cors.allowed.origin}")
    private String allowedOrigin;

    @Value("${cors.allowed.headers}")
    private String allowedHeaders;

    @Value("${cors.allowed.methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed.credentials}")
    private boolean allowedCredentials;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCorsConfig() throws Exception{
        mockMvc.perform(options(allowedOrigin)
                .header("Origin", allowedOrigin))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                .andExpect(header().string("Access-Control-Allow-Methods", String.join(",", allowedMethods)))
                .andExpect(header().string("Access-Control-Allow-Headers", allowedHeaders))
                .andExpect(header().string("Access-Control-Allow-Credentials", String.valueOf(allowedCredentials)));
    }
}
