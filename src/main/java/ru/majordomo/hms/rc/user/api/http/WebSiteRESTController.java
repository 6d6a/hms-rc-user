package ru.majordomo.hms.rc.user.api.http;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resources.WebSite;

@RestController
public class WebSiteRESTController {

    private GovernorOfWebSite governor;

    private SimpleFilterProvider getWebSiteFilter(WebSite webSite) {
        String[] allowedFields = webSite.getFilteredFieldsAsSequence().toArray(new String[0]);

        return new SimpleFilterProvider()
                .addFilter("websiteFilter",
                        SimpleBeanPropertyFilter.filterOutAllExcept(allowedFields)
                );
    }

    private MappingJacksonValue getWrapperFromWebSite(WebSite webSite) {
        MappingJacksonValue wrapper = new MappingJacksonValue(webSite);
        wrapper.setFilters(getWebSiteFilter(webSite));

        return wrapper;
    }

    private ResponseEntity getResponseFromBunchOfWebsites(Collection<WebSite> webSites) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            JsonGenerator jsonGen = new JsonFactory().createGenerator(stream, JsonEncoding.UTF8);

            jsonGen.writeStartArray();
            for (WebSite item : webSites) {
                jsonGen.setCodec(new ObjectMapper().setFilterProvider(getWebSiteFilter(item)));
                jsonGen.writeObject(item);
            }
            jsonGen.writeEndArray();
            jsonGen.close();

            return ResponseEntity.ok(new String(stream.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @Autowired
    public void setGovernor(GovernorOfWebSite governor) {
        this.governor = governor;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/website/{websiteId}")
    public ResponseEntity readOne(@PathVariable String websiteId) {
        return ResponseEntity.ok(getWrapperFromWebSite(governor.build(websiteId)));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/website/{websiteId}")
    public ResponseEntity readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("websiteId") String websiteId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", websiteId);
        keyValue.put("accountId", accountId);
        return ResponseEntity.ok(getWrapperFromWebSite(governor.build(keyValue)));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping(value = "/website", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity readAll() {
        return getResponseFromBunchOfWebsites(governor.buildAll());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping(value = "/{accountId}/website", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity readAllByAccountId(@PathVariable String accountId, @RequestParam(required = false) boolean withoutBuiltIn) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        if (withoutBuiltIn) {
            keyValue.put("withoutBuiltIn", Boolean.toString(true));
        }
        return getResponseFromBunchOfWebsites(governor.buildAll(keyValue));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/website/count")
    public Count countByAccountId(@PathVariable String accountId) {
        return governor.countByAccountId(accountId);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/website/find")
    public ResponseEntity readOneWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return ResponseEntity.ok(getWrapperFromWebSite(governor.build(requestParams)));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/website/find")
    public ResponseEntity readOneWithParams(@RequestParam Map<String, String> requestParams) {
        return ResponseEntity.ok(getWrapperFromWebSite(governor.build(requestParams)));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping(value = "/{accountId}/website/filter", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity filterByAccountId(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return getResponseFromBunchOfWebsites(governor.buildAll(requestParams));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping(value = "/website/filter", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity filter(@RequestParam Map<String, String> requestParams) {
        return getResponseFromBunchOfWebsites(governor.buildAll(requestParams));
    }
}
