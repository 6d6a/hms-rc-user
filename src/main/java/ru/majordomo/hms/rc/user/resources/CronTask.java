package ru.majordomo.hms.rc.user.resources;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Locale;

public class CronTask {
    private String execTime;
    private String execTimeDescription;
    private String command;
    private Boolean switchedOn = true;

    public String getExecTime() {
        return execTime;
    }

    public void setExecTime(String execTime) {
        CronDescriptor descriptor = CronDescriptor.instance(new Locale("ru"));
        CronDefinition cronDefinition = CronDefinitionBuilder.defineCron()
                .withMinutes().and()
                .withHours().and()
                .withDayOfMonth().and()
                .withMonth().and()
                .withDayOfWeek().withValidRange(0,7).withMondayDoWValue(1).withIntMapping(0,7).and()
                .enforceStrictRanges()
                .instance();
        CronParser cronParser = new CronParser(cronDefinition);
        Cron cron = cronParser.parse(execTime);

        this.execTimeDescription = descriptor.describe(cron);
        this.execTime = cron.asString();
    }

    @JsonIgnore
    public void setRawExecTime(String execTime) {
        this.execTime = execTime;
    }

    public String getExecTimeDescription() {
        return execTimeDescription;
    }

    public void setExecTimeDescription(String execTimeDescription) {
        this.execTimeDescription = execTimeDescription;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Boolean getSwitchedOn() {
        return switchedOn;
    }

    public void setSwitchedOn(Boolean switchedOn) {
        this.switchedOn = switchedOn;
    }

    public void switchResource() {
        switchedOn = !switchedOn;
    }

    @Override
    public String toString() {
        return "CronTask{" +
                "execTime='" + execTime + '\'' +
                ", execTimeDescription='" + execTimeDescription + '\'' +
                ", command='" + command + '\'' +
                '}';
    }
}
