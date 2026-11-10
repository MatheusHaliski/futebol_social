package com.footballai.social.controller;

import com.footballai.social.service.DiscoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscoveryControllerTest {
    @Test void multiValueFiltersKeepCommasInsideTheExpression() throws Exception {
        DiscoveryService service=mock(DiscoveryService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new DiscoveryController(service)).setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver()).build();
        mvc.perform(get("/v1/discovery").param("entity","ATLETA")
            .param("filter","athlete.transferSituation:eq:TRANSFER_LIST,LOAN_LIST,SWAP_LIST,FREE_AGENT","stateCode:eq:PR,SP"))
            .andExpect(status().isOk());
        verify(service).search(null,"ATLETA",null,List.of("athlete.transferSituation:eq:TRANSFER_LIST,LOAN_LIST,SWAP_LIST,FREE_AGENT","stateCode:eq:PR,SP"),0,20,"name");
    }
}
