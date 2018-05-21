package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.rc.user.common.PathManager;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Redirect;
import ru.majordomo.hms.rc.user.resources.WebSite;

import ru.majordomo.hms.rc.user.resources.validation.ValidWebSite;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class WebSiteValidator implements ConstraintValidator<ValidWebSite, WebSite> {
    private final MongoOperations operations;

    @Autowired
    public WebSiteValidator(
            MongoOperations operations
    ) {
        this.operations = operations;
    }

    @Override
    public void initialize(ValidWebSite validWebSite) {
    }

    @Override
    public boolean isValid(final WebSite webSite, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid;
        try {
            Query query;

            if (webSite.getId() != null) {
                query = new Query(where("_id").nin(webSite.getId()).and("domainIds").in(webSite.getDomainIds()));
            } else {
                query = new Query(where("domainIds").in(webSite.getDomainIds()));
            }

            isValid = !operations.exists(query, WebSite.class);

//            if (isValid) {
//                List<Redirect> redirects = operations.find(new Query(where("domainId").in(webSite.getDomainIds())), Redirect.class);
//                if (!redirects.isEmpty()) {
//                    isValid = false;
////
//                    Set<String> domainIds = redirects.stream().map(Redirect::getDomainId).collect(Collectors.toSet());
//                    List<Domain> domains = operations.find(new Query(where("_id").in(domainIds)), Domain.class);
//                    constraintValidatorContext.disableDefaultConstraintViolation();
//                    constraintValidatorContext
//                            .buildConstraintViolationWithTemplate(
//                                    "Для добавления следующих доменов на сайт необходимо удалить использующие их редиректы "
//                                            + String.join(", ", domains.stream().map(Domain::getName).collect(Collectors.toList())))
//                            .addConstraintViolation();
//                }
//            }
            if (isValid) {
                isValid = !operations.exists(new Query(where("domainId").in(webSite.getDomainIds())), Redirect.class);
                if (!isValid) {
                    constraintValidatorContext.disableDefaultConstraintViolation();
                    constraintValidatorContext.buildConstraintViolationWithTemplate(
                            "{ru.majordomo.hms.rc.user.resources.validation.ConcurrentWebSiteAndRedirect.message}"
                    ).addConstraintViolation();
                }
            }
        } catch (RuntimeException e) {
            return false;
        }

        if (isValid) {
            if (!PathManager.isPathInsideTheDir(webSite.getDocumentRoot(), webSite.getUnixAccount().getHomeDir())) {
                isValid = false;
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("{ru.majordomo.hms.rc.user.resources.validation.ValidWebSiteDocumentRoot.message}")
                        .addConstraintViolation();
            }
        }

        return isValid;
    }
}
