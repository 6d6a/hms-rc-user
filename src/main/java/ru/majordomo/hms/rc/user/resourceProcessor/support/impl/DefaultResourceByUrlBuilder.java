package ru.majordomo.hms.rc.user.resourceProcessor.support.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.LordOfResources;
import ru.majordomo.hms.rc.user.resourceProcessor.support.ResourceByUrlBuilder;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@AllArgsConstructor
public class DefaultResourceByUrlBuilder<T extends Resource> implements ResourceByUrlBuilder<T> {
    private final LordOfResources<T> governor;

    @Override
    public T get(String url) {
        T resource = null;
        try {
            URL processingUrl = new URL(url);
            String path = processingUrl.getPath();
            String[] pathParts = path.split("/");
            String resourceId = pathParts[2];
            resource = governor.build(resourceId);
        } catch (MalformedURLException e) {
            log.warn("Ошибка при обработке URL:" + url);
            e.printStackTrace();
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found");
        }

        return resource;
    }
}
