package ru.majordomo.hms.rc.user.common;

public enum ResourceAction {
    CREATE("." + Constants.Exchanges.Command.CREATE, "Создание"),
    DELETE("." + Constants.Exchanges.Command.DELETE, "Удаление"),
    UPDATE("." + Constants.Exchanges.Command.UPDATE, "Обновление");

    private final String exchangeSuffix;
    private final String actionName;

    ResourceAction(String exchangeSuffix, String actionName) {
        this.exchangeSuffix = exchangeSuffix;
        this.actionName = actionName;
    }

    public String getExchangeSuffix() {
        return exchangeSuffix;
    }

    public String getActionName() {
        return actionName;
    }
}