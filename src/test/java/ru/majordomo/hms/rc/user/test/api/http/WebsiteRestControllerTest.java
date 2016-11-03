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
import java.util.Arrays;
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
import ru.majordomo.hms.rc.user.test.config.rest.ConfigWebsiteRestController;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigWebsiteRestController.class, webEnvironment = RANDOM_PORT)
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
            webSite.setDomainIds(Arrays.asList(domain.getId()));

        }

        repository.save((Iterable) batchOfWebsites);
    }

    @Test
    public void readOne() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + resourceName + "/"
                + batchOfWebsites.get(0).getId()).accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(doc)
                .andDo(doc.document(
                        responseFields(
                                fieldWithPath("id").description("Внутренний ID ресурса"),
                                fieldWithPath("name").description("Комментарий к сайту"),
                                fieldWithPath("switchedOn").description("Флаг того, активен ли сайт"),
                                fieldWithPath("unixAccount").description("Аккаунт на сервере, под чьим UID'ом будет работать вебсервер"),
                                fieldWithPath("serverId").description("ID сервера, на котором расположен сайт"),
                                fieldWithPath("documentRoot").description("Домашняя директория сайта. Указывается часть полного пути, не содрежащая путь к домашней директории"),
                                fieldWithPath("domains").description("Домены, привязанные к сайту"),
                                fieldWithPath("charSet").description("Кодировка сайта"),
                                fieldWithPath("ssiEnabled").description("Включен ли SSI"),
                                fieldWithPath("ssiFileExtensions").description("Список расширений файлов, которые должны обрабатываться SSI обработчиком"),
                                fieldWithPath("cgiEnabled").description("Включена ли поддержка CGI"),
                                fieldWithPath("cgiFileExtensions").description("Список расширений файлов, которые должны обрабатываться через CGI"),
                                fieldWithPath("scriptAlias").description("Алиас для CGI скриптов"),
                                fieldWithPath("ddosProtection").description("Включена ли защита сайта от DDOS"),
                                fieldWithPath("autoSubDomain").description("Включена ли поддержка автоподдоменов"),
                                fieldWithPath("accessByOldHttpVersion").description("Включен ли доступ по HTTP 1.0"),
                                fieldWithPath("staticFileExtensions").description("Список расширений файлов, которые обрабатываются напрямую NGINX'ом"),
                                fieldWithPath("customUserConf").description("Кастомные настройки сайта"),
                                fieldWithPath("indexFileList").description("Список индексных файлов"),
                                fieldWithPath("accessLogEnabled").description("Записывать ли логи доступа"),
                                fieldWithPath("errorLogEnabled").description("Записывать ли error логи")
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

}
