package ru.majordomo.hms.rc.user.common;

public enum ExtendedAction {
    /**
     * загрузить пользовательскую программу из git
     */
    LOAD,
    /**
     * установить зависимости
     */
    INSTALL, SHELL,
    /** выполнить команды из Website.appUpdateCommands */
    SHELLUPDATE,
    /** загрузка из git и команды установки */
    LOAD_INSTALL;
}
