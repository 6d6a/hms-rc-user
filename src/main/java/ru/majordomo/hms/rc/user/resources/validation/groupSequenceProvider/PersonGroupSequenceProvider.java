package ru.majordomo.hms.rc.user.resources.validation.groupSequenceProvider;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

import javax.validation.groups.Default;

import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonCompanyForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonEntrepreneurForeignChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.PersonIndividualForeignChecks;

public class PersonGroupSequenceProvider implements DefaultGroupSequenceProvider<Person> {
    @Override
    public List<Class<?>> getValidationGroups(Person person) {
        List<Class<?>> sequence = new ArrayList<>();

        sequence.add(Person.class);

        sequence.add(PersonChecks.class);

        addCustomGroupsToSecuence(sequence, person);

        return sequence;
    }

    public List<Class<?>> getValidationGroupsCustom(Person person) {
        List<Class<?>> sequence = new ArrayList<>();

        sequence.add(Default.class);

        sequence.add(PersonChecks.class);

        addCustomGroupsToSecuence(sequence, person);

        return sequence;
    }

    private void addCustomGroupsToSecuence(List<Class<?>> sequence, Person person) {
        if(person != null && person.getType() != null){
            switch (person.getType()) {
                case INDIVIDUAL:
                    sequence.add(PersonIndividualChecks.class);
                    break;
                case INDIVIDUAL_FOREIGN:
                    sequence.add(PersonIndividualForeignChecks.class);
                    break;
                case COMPANY:
                    sequence.add(PersonCompanyChecks.class);
                    break;
                case COMPANY_FOREIGN:
                    sequence.add(PersonCompanyForeignChecks.class);
                    break;
                case ENTREPRENEUR:
                    sequence.add(PersonEntrepreneurChecks.class);
                    break;
                case ENTREPRENEUR_FOREIGN:
                    sequence.add(PersonEntrepreneurForeignChecks.class);
                    break;
            }
        }
    }
}
