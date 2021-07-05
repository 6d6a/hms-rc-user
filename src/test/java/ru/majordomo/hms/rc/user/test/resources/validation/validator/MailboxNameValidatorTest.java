package ru.majordomo.hms.rc.user.test.resources.validation.validator;

import org.junit.Assert;
import org.junit.Test;
import ru.majordomo.hms.rc.user.resources.validation.validator.MailboxNameValidator;

public class MailboxNameValidatorTest {
    private final MailboxNameValidator mailboxNameValidator = new MailboxNameValidator();
    @Test
    public void testRightName() {
        Assert.assertTrue(mailboxNameValidator.isValid("postmaster"));
        Assert.assertTrue(mailboxNameValidator.isValid("p111"));
        Assert.assertTrue(mailboxNameValidator.isValid("1111"));
        Assert.assertTrue(mailboxNameValidator.isValid("absdifghijklmnopqastuvwxyz01234567890absdifghijklmnopqastuvwxyz"));

        Assert.assertTrue(mailboxNameValidator.isValid("p.p.p"));
        Assert.assertTrue(mailboxNameValidator.isValid("p_p"));
        Assert.assertTrue(mailboxNameValidator.isValid("p-p"));

        // from wiki
//        Assert.assertTrue(mailboxNameValidator.isValid("disposable.style.email.with+symbol"));
        Assert.assertTrue(mailboxNameValidator.isValid("other.email-with-hyphen"));
        Assert.assertTrue(mailboxNameValidator.isValid("fully-qualified-domain"));
//        Assert.assertTrue(mailboxNameValidator.isValid("user.name+tag+sorting"));
        Assert.assertTrue(mailboxNameValidator.isValid("x"));
        Assert.assertTrue(mailboxNameValidator.isValid("example-indeed"));
//        Assert.assertTrue(mailboxNameValidator.isValid("test/test"));
//        Assert.assertTrue(mailboxNameValidator.isValid("\"john..doe\""));
//        Assert.assertTrue(mailboxNameValidator.isValid("mailhost!username"));
//        Assert.assertTrue(mailboxNameValidator.isValid("user%example.com"));
        Assert.assertTrue(mailboxNameValidator.isValid("user-"));
    }

    @Test
    public void testWrongName() {
        Assert.assertFalse(mailboxNameValidator.isValid(null));
        Assert.assertFalse(mailboxNameValidator.isValid(""));

        Assert.assertFalse(mailboxNameValidator.isValid(".word"));
        Assert.assertFalse(mailboxNameValidator.isValid("word."));
        Assert.assertFalse(mailboxNameValidator.isValid("word..word"));

        Assert.assertFalse(mailboxNameValidator.isValid("UPPER"));
        Assert.assertFalse(mailboxNameValidator.isValid("upPer"));
        Assert.assertFalse(mailboxNameValidator.isValid("wрусскийw"));
        Assert.assertFalse(mailboxNameValidator.isValid("русский"));

        Assert.assertFalse(mailboxNameValidator.isValid("*"));

        // without special symbols because they don't work
        Assert.assertFalse(mailboxNameValidator.isValid("p&p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p#p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p&p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p$p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p#p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p?p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p|p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p{p"));
        Assert.assertFalse(mailboxNameValidator.isValid("p*p"));

        Assert.assertFalse(mailboxNameValidator.isValid("egorvasil'ev"));

        // from wiki
        Assert.assertFalse(mailboxNameValidator.isValid("a@b@c"));
        Assert.assertFalse(mailboxNameValidator.isValid("a\"b(c)d,e:f;g<h>i[j\\k]l"));
        Assert.assertFalse(mailboxNameValidator.isValid("just\"not\"right"));
        Assert.assertFalse(mailboxNameValidator.isValid("this is\"not\\allowed"));
        Assert.assertFalse(mailboxNameValidator.isValid("this\\ still\\\"not\\\\allowed"));
        Assert.assertFalse(mailboxNameValidator.isValid("1234567890123456789012345678901234567890123456789012345678901234+x"));
        Assert.assertFalse(mailboxNameValidator.isValid("QA[icon]CHOCOLATE[icon]".toLowerCase()));
    }
}
