package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import ru.majordomo.hms.rc.user.resources.Redirect;
import ru.majordomo.hms.rc.user.resources.RedirectItem;
import ru.majordomo.hms.rc.user.resources.WebSite;
import ru.majordomo.hms.rc.user.resources.validation.ValidRedirect;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class RedirectValidator implements ConstraintValidator<ValidRedirect, Redirect> {
    private final MongoOperations operations;

    @Autowired
    public RedirectValidator(
            MongoOperations operations
    ) {
        this.operations = operations;
    }

    @Override
    public void initialize(ValidRedirect validRedirect) {
    }

    @Override
    public boolean isValid(final Redirect redirect, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid;
        try {
            Query query;
            if (redirect.getId() != null) {
                query = new Query(where
                        ("_id").nin(redirect.getId())
                        .and("domainId").is(redirect.getDomainId())
                );
            } else {
                query = new Query(where
                        ("domainId").is(redirect.getDomainId())
                );
            }

            isValid = !operations.exists(query, Redirect.class);

            if (isValid) {
                Set<String> sourcePaths = new HashSet<>();
                for (RedirectItem item : redirect.getRedirectItems()) {
                    try {
                        URL sourceURL = new URL("http", "localhost", item.getSourcePath());
                        item.setSourcePath(sourceURL.getPath());

                        if (!sourcePaths.add(item.getSourcePath())) {
                            constraintValidatorContext.disableDefaultConstraintViolation();
                            constraintValidatorContext
                                    .buildConstraintViolationWithTemplate(
                                            "Исходный адрес " + item.getSourcePath() + " используется более одного раза"
                                    ).addConstraintViolation();
                            return false;
                        }
                    } catch (MalformedURLException e) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext
                                .buildConstraintViolationWithTemplate(
                                        "Некорректный исходный адрес " + item.getSourcePath()
                                ).addConstraintViolation();
                        return false;
                    }
                    try {
                        URL url = new URL(item.getTargetUrl());
                        if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                            constraintValidatorContext.disableDefaultConstraintViolation();
                            constraintValidatorContext
                                    .buildConstraintViolationWithTemplate(
                                            "Протокол адреса назначения должен быть 'http' или 'https'" + item.getTargetUrl()
                                    ).addConstraintViolation();
                            return false;
                        }
                    } catch (MalformedURLException e) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext
                                .buildConstraintViolationWithTemplate(
                                        "Некорректный адрес назначения" + item.getTargetUrl()
                                ).addConstraintViolation();
                        return false;
                    }
                }
            }

            if (isValid) {
                isValid = !operations.exists(new Query(where("domainIds").in(redirect.getDomainId())), WebSite.class);
                if (!isValid) {
                    constraintValidatorContext.disableDefaultConstraintViolation();
                    constraintValidatorContext
                            .buildConstraintViolationWithTemplate(
                                    "{ru.majordomo.hms.rc.user.resources.validation.ConcurrentWebSiteAndRedirect.message}"
                            ).addConstraintViolation();
                }
            }
        } catch (RuntimeException e) {
            return false;
        }

        return isValid;
    }
}
