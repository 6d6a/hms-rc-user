package ru.majordomo.hms.rc.user.test.cleaner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Cleaner.class})
public class CleanerTest {
    @Autowired
    private Cleaner cleaner;

    @Test
    public void cleanString() throws Exception {
        String result = cleaner.cleanString("  nksdj?sr3#4vn/\\f349><>340~4\t\t");
        assertThat(result, is("nksdj?sr3#4vn/\\f349><>340~4"));
    }

    @Test
    public void cleanStringNull() throws Exception {
        String result = cleaner.cleanString(null);
        assertThat(result, is(""));
    }

    @Test
    public void cleanListWithStrings() throws Exception {
        List<String> input = new ArrayList<>();
        input.add("  nksdj?sr3#4vn/\\f349><>340~4\t\t");
        input.add("\t alksfury*4j;&nfr? fjkserh~\\~<<<>afer~?@$/a3) \t");
        List<String> result = cleaner.cleanListWithStrings(input);
        assertThat(result, is(Arrays.asList("nksdj?sr3#4vn/\\f349><>340~4", "alksfury*4j;&nfr? fjkserh~\\~<<<>afer~?@$/a3)")));
    }

    @Test
    public void cleanListWithNull() throws Exception {
        List<String> result = cleaner.cleanListWithStrings(null);
        assertThat(result, is(Collections.EMPTY_LIST));
    }
}