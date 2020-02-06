package ru.majordomo.hms.rc.user.test.api.http;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.PersonRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;
import ru.majordomo.hms.rc.user.test.common.ResourceGenerator;
import ru.majordomo.hms.rc.user.test.config.DatabaseConfig;
import ru.majordomo.hms.rc.user.test.config.FongoConfig;
import ru.majordomo.hms.rc.user.test.config.RedisConfig;
import ru.majordomo.hms.rc.user.test.config.amqp.AMQPBrokerConfig;
import ru.majordomo.hms.rc.user.test.config.common.ConfigDomainRegistrarClient;
import ru.majordomo.hms.rc.user.test.config.common.ConfigStaffResourceControllerClient;
import ru.majordomo.hms.rc.user.test.config.governors.ConfigGovernors;
import ru.majordomo.hms.rc.user.test.config.rest.ConfigRestControllers;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                ConfigStaffResourceControllerClient.class,
                ConfigDomainRegistrarClient.class,

                FongoConfig.class,
                RedisConfig.class,
                DatabaseConfig.class,

                ConfigRestControllers.class,

                ConfigGovernors.class,
                AMQPBrokerConfig.class
        },
        webEnvironment = RANDOM_PORT
)
public class WebsiteRestControllerTest {

    private MockMvc mockMvc;
    private String resourceName = "website";
    private List<WebSite> batchOfWebsites = new ArrayList<>();

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");
    private RestDocumentationResultHandler doc;

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private WebSiteRepository repository;
    @Autowired
    private UnixAccountRepository unixAccountRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private DomainRepository domainRepository;

    @Before
    public void setUp() throws Exception{
        this.doc = document("website/{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        batchOfWebsites = ResourceGenerator.generateBatchOfWebsites();
        for (WebSite webSite: batchOfWebsites) {
            UnixAccount unixAccount = webSite.getUnixAccount();

            Domain domain = webSite.getDomains().get(0);
            Person person = domain.getPerson();
            personRepository.save(person);
            domain.setPersonId(person.getId());
            domainRepository.save(domain);
            unixAccountRepository.save(unixAccount);
            webSite.setUnixAccountId(unixAccount.getId());
            webSite.setDomainIds(Collections.singletonList(domain.getId()));

        }

        repository.saveAll((Iterable) batchOfWebsites);
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfWebsites.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfWebsites.get(0).getName()))
                .andExpect(jsonPath("accountId").value(batchOfWebsites.get(0).getAccountId()))
                .andExpect(jsonPath("switchedOn").value(batchOfWebsites.get(0).getSwitchedOn()))
                .andExpect(jsonPath("unixAccount").isMap())
                .andExpect(jsonPath("unixAccount.id").value(batchOfWebsites.get(0).getUnixAccount().getId()))
                .andExpect(jsonPath("serviceId").value(batchOfWebsites.get(0).getServiceId()))
                .andExpect(jsonPath("documentRoot").value(batchOfWebsites.get(0).getDocumentRoot()))
                .andExpect(jsonPath("domains").isArray())
                .andExpect(jsonPath("domains.[0].id").value(batchOfWebsites.get(0).getDomains().get(0).getId()))
//                .andExpect(jsonPath("charSet").value(batchOfWebsites.get(0).getCharSet().toString()))
//                .andExpect(jsonPath("ssiEnabled").value(batchOfWebsites.get(0).getSsiEnabled()))
//                .andExpect(jsonPath("ssiFileExtensions").isArray())
//                .andExpect(jsonPath("cgiEnabled").value(batchOfWebsites.get(0).getCgiEnabled()))
//                .andExpect(jsonPath("cgiFileExtensions").isArray())
//                .andExpect(jsonPath("scriptAlias").value(batchOfWebsites.get(0).getScriptAlias()))
//                .andExpect(jsonPath("ddosProtection").value(batchOfWebsites.get(0).getDdosProtection()))
//                .andExpect(jsonPath("autoSubDomain").value(batchOfWebsites.get(0).getAutoSubDomain()))
//                .andExpect(jsonPath("accessByOldHttpVersion").value(batchOfWebsites.get(0).getAccessByOldHttpVersion()))
//                .andExpect(jsonPath("staticFileExtensions").isArray())
//                .andExpect(jsonPath("autoSubDomain").value(batchOfWebsites.get(0).getAutoSubDomain()))
//                .andExpect(jsonPath("customUserConf").value(batchOfWebsites.get(0).getCustomUserConf()))
//                .andExpect(jsonPath("expiresForTypes").isMap())
//                .andExpect(jsonPath("indexFileList").isArray())
//                .andExpect(jsonPath("accessLogEnabled").value(batchOfWebsites.get(0).getAccessLogEnabled()))
//                .andExpect(jsonPath("errorLogEnabled").value(batchOfWebsites.get(0).getErrorLogEnabled()))
                .andExpect(jsonPath("serviceId").value(batchOfWebsites.get(0).getServiceId()));
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
        String accountId = batchOfWebsites.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name").value(batchOfWebsites.get(0).getName()))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[0].switchedOn").value(batchOfWebsites.get(0).getSwitchedOn()))
                .andExpect(jsonPath("$[0].unixAccount").isMap())
                .andExpect(jsonPath("$[0].unixAccount.id").value(batchOfWebsites.get(0).getUnixAccount().getId()))
                .andExpect(jsonPath("$[0].serviceId").value(batchOfWebsites.get(0).getServiceId()))
                .andExpect(jsonPath("$[0].documentRoot").value(batchOfWebsites.get(0).getDocumentRoot()))
//                .andExpect(jsonPath("$[0].domains").isArray())
//                .andExpect(jsonPath("$[0].domains.[0].id").value(batchOfWebsites.get(0).getDomains().get(0).getId()))
//                .andExpect(jsonPath("$[0].charSet").value(batchOfWebsites.get(0).getCharSet().toString()))
//                .andExpect(jsonPath("$[0].ssiEnabled").value(batchOfWebsites.get(0).getSsiEnabled()))
//                .andExpect(jsonPath("$[0].ssiFileExtensions").isArray())
//                .andExpect(jsonPath("$[0].cgiEnabled").value(batchOfWebsites.get(0).getCgiEnabled()))
//                .andExpect(jsonPath("$[0].cgiFileExtensions").isArray())
//                .andExpect(jsonPath("$[0].scriptAlias").value(batchOfWebsites.get(0).getScriptAlias()))
//                .andExpect(jsonPath("$[0].ddosProtection").value(batchOfWebsites.get(0).getDdosProtection()))
//                .andExpect(jsonPath("$[0].autoSubDomain").value(batchOfWebsites.get(0).getAutoSubDomain()))
//                .andExpect(jsonPath("$[0].accessByOldHttpVersion").value(batchOfWebsites.get(0).getAccessByOldHttpVersion()))
//                .andExpect(jsonPath("$[0].staticFileExtensions").isArray())
//                .andExpect(jsonPath("$[0].autoSubDomain").value(batchOfWebsites.get(0).getAutoSubDomain()))
//                .andExpect(jsonPath("$[0].customUserConf").value(batchOfWebsites.get(0).getCustomUserConf()))
//                .andExpect(jsonPath("$[0].expiresForTypes").value(batchOfWebsites.get(0).getExpiresForTypes()))
//                .andExpect(jsonPath("$[0].indexFileList").isArray())
//                .andExpect(jsonPath("$[0].accessLogEnabled").value(batchOfWebsites.get(0).getAccessLogEnabled()))
//                .andExpect(jsonPath("$[0].errorLogEnabled").value(batchOfWebsites.get(0).getErrorLogEnabled()))
                .andExpect(jsonPath("$[0].serviceId").value(batchOfWebsites.get(0).getServiceId()))
        ;
    }

    @Test
    public void readByIdAndByAccountId() throws Exception {
        String accountId = batchOfWebsites.get(0).getAccountId();
        String websiteId = batchOfWebsites.get(0).getId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/" + websiteId).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(batchOfWebsites.get(0).getName()))
                .andExpect(jsonPath("accountId").value(accountId))
                .andExpect(jsonPath("switchedOn").value(batchOfWebsites.get(0).getSwitchedOn()))
                .andExpect(jsonPath("unixAccount").isMap())
                .andExpect(jsonPath("unixAccount.id").value(batchOfWebsites.get(0).getUnixAccount().getId()))
                .andExpect(jsonPath("serviceId").value(batchOfWebsites.get(0).getServiceId()))
                .andExpect(jsonPath("documentRoot").value(batchOfWebsites.get(0).getDocumentRoot()))
                .andExpect(jsonPath("domains").isArray())
                .andExpect(jsonPath("domains.[0].id").value(batchOfWebsites.get(0).getDomains().get(0).getId()))
//                .andExpect(jsonPath("charSet").value(batchOfWebsites.get(0).getCharSet().toString()))
//                .andExpect(jsonPath("ssiEnabled").value(batchOfWebsites.get(0).getSsiEnabled()))
//                .andExpect(jsonPath("ssiFileExtensions").isArray())
//                .andExpect(jsonPath("cgiEnabled").value(batchOfWebsites.get(0).getCgiEnabled()))
//                .andExpect(jsonPath("cgiFileExtensions").isArray())
//                .andExpect(jsonPath("scriptAlias").value(batchOfWebsites.get(0).getScriptAlias()))
//                .andExpect(jsonPath("ddosProtection").value(batchOfWebsites.get(0).getDdosProtection()))
//                .andExpect(jsonPath("autoSubDomain").value(batchOfWebsites.get(0).getAutoSubDomain()))
//                .andExpect(jsonPath("accessByOldHttpVersion").value(batchOfWebsites.get(0).getAccessByOldHttpVersion()))
//                .andExpect(jsonPath("staticFileExtensions").isArray())
//                .andExpect(jsonPath("autoSubDomain").value(batchOfWebsites.get(0).getAutoSubDomain()))
//                .andExpect(jsonPath("customUserConf").value(batchOfWebsites.get(0).getCustomUserConf()))
//                .andExpect(jsonPath("expiresForTypes").isMap())
//                .andExpect(jsonPath("indexFileList").isArray())
//                .andExpect(jsonPath("accessLogEnabled").value(batchOfWebsites.get(0).getAccessLogEnabled()))
//                .andExpect(jsonPath("errorLogEnabled").value(batchOfWebsites.get(0).getErrorLogEnabled()))
                .andExpect(jsonPath("serviceId").value(batchOfWebsites.get(0).getServiceId()))
        ;
    }

    @Test
    public void countByAccountId() throws Exception {
        String accountId = batchOfWebsites.get(0).getAccountId();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + accountId + "/" + resourceName + "/count").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("count").value(1))
                .andDo(doc)
                .andDo(doc.document(responseFields(fieldWithPath("count").description("Количество ресурсов WebSite для указанного accountId"))));
    }
}
