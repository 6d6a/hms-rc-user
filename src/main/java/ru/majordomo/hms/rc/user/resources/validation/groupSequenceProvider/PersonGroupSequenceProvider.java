package ru.majordomo.hms.rc.user.resources.validation.groupSequenceProvider;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

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

        sequence.add(PersonChecks.class);


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

        return sequence;
    }
}
