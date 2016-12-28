package ru.majordomo.hms.rc.user.test.api.http;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.rest.ConfigDomainRestController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ConfigDomainRestController.class, ConfigDomainRegistrarClient.class, ConfigStaffResourceControllerClient.class}, webEnvironment = RANDOM_PORT)
public class DomainRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "domain";
    private List<Domain> batchOfDomains = new ArrayList<>();

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private DomainRepository repository;
    @Autowired
    private PersonRepository personRepository;

    @Before
    public void setUp() {
        this.doc = document("domain/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfDomains = ResourceGenerator.generateBatchOfDomains();
        for (Domain domain: batchOfDomains) {
            personRepository.save(domain.getPerson());
            domain.setPersonId(domain.getPerson().getId());
            repository.save(domain);
        }
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfDomains.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("name").description("FQDN"),
                                fieldWithPath("switchedOn").description("Флаг того, активен ли домен"),
                                fieldWithPath("person").description("Персона, на которую зарегистрирован домен"),
                                fieldWithPath("regSpec").description("Регистрационная информация"),
                                fieldWithPath("dnsResourceRecords").description("Список записей в зоне домена (DNS resource records)"),
                                fieldWithPath("sslCertificate").description("Объект SSL сертификата")
                        )
                ));
    }

    @Test
    public void readAll() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc);
    }

    @Test
    public void readAllByAccountId() throws Exception {
        String accountId = batchOfDomains.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfDomains.get(0).getName()))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfDomains.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].person").isMap())
                .andExpect(jsonPath("$[0].person.id").value(batchOfDomains.get(0).getPerson().getId()))
                .andExpect(jsonPath("$[0].regSpec").isMap())
                .andExpect(jsonPath("$[0].dnsResourceRecords").isArray());
    }

    @Test
    public void readByIdAndByAccountId() throws Exception {
        String accountId = batchOfDomains.get(0).getAccountId();
        String domainId = batchOfDomains.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + domainId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfDomains.get(0).getName()))
                .andExpect(jsonPath("switchedOn").value(batchOfDomains.get(0).getSwitchedOn()))
                .andExpect(jsonPath("person").isMap())
                .andExpect(jsonPath("person.id").value(batchOfDomains.get(0).getPerson().getId()))
                .andExpect(jsonPath("regSpec").isMap())
                .andExpect(jsonPath("dnsResourceRecords").isArray());
    }
}
