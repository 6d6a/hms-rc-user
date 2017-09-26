package ru.majordomo.hms.rc.user.resources.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import ru.majordomo.hms.rc.user.common.PathManager;
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.resources.FTPUser;
import ru.majordomo.hms.rc.user.resources.validation.ValidFTPUser;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FTPUserValidator implements ConstraintValidator<ValidFTPUser, FTPUser> {

    private GovernorOfUnixAccount governorOfUnixAccount;

    @Autowired
    public FTPUserValidator(
            GovernorOfUnixAccount governorOfUnixAccount
    ) {
        this.governorOfUnixAccount = governorOfUnixAccount;
    }
    @Override
    public void initialize(ValidFTPUser validFTPUser) {
    }

    @Override
    public boolean isValid(final FTPUser ftpUser, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;

        if (ftpUser.getUnixAccountId() == null) {
            return false;
        }

        if (ftpUser.getUnixAccount() == null) {
            ftpUser.setUnixAccount(governorOfUnixAccount.build(ftpUser.getUnixAccountId()));
        }

        if (!PathManager.isPathInsideTheDir(ftpUser.getHomeDir(), ftpUser.getUnixAccount().getHomeDir())) {
            isValid = false;
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("{ru.majordomo.hms.rc.user.resources.validation.ValidFTPUserHomeDir.message}")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
