package org.ccci.framework.sblio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Field;

import org.ccci.framework.sblio.annotations.SiebelFieldValue;
import org.testng.annotations.Test;

public class DssDataBeanTest
{

    public enum Gender
    {
        Male("M"),
        Female("F");

        Gender(String code)
        {
            this.code = code;
        }

        @SiebelFieldValue
        String code;
    }

    public static class Person
    {
        String name;
        
        Gender gender;
    }
    
    SiebelHelper helper = new SiebelHelper();

    @Test
    public void testConvertEnumToString() throws SecurityException, NoSuchFieldException
    {
        Field field = Person.class.getDeclaredField("gender");
        String code = helper.convertEnumToSiebelCode(Gender.Male, field);
        assertThat(code, is("M"));
    }
    
    @Test
    public void testConvertStringToEnum() throws SecurityException, NoSuchFieldException
    {
        Enum<?> gender = helper.convertSiebelCodeToEnum(Gender.class, "M");
        assertThat(gender, is(instanceOf(Gender.class)));
        assertThat((Gender) gender, is(Gender.Male));
    }
    
}
