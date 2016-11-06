package ru.majordomo.hms.rc.user.resources;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import java.util.Locale;

import static com.cronutils.model.CronType.UNIX;

public class CronTask extends Resource {
    private String execTime;
    private String execTimeDescription;
    private String command;

    public String getExecTime() {
        return execTime;
    }

    public void setExecTime(String execTime) {
        CronDescriptor descriptor = CronDescriptor.instance(new Locale("ru"));
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(UNIX);
        CronParser cronParser = new CronParser(cronDefinition);
        Cron cron = cronParser.parse(execTime);

        this.execTimeDescription = descriptor.describe(cron);
        this.execTime = cron.asString();
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

    @Override
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
