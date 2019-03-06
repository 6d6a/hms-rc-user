package ru.majordomo.hms.rc.user.resourceProcessor.support;

@FunctionalInterface
public interface ResourceByUrlBuilder<T> {
    T get(String url);
}
