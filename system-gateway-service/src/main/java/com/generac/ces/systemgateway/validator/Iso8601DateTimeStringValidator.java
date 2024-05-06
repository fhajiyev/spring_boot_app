package com.generac.ces.systemgateway.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.format.ISODateTimeFormat;

public class Iso8601DateTimeStringValidator
        implements ConstraintValidator<ValidIso8601DateTimeString, String> {
    @Override
    public boolean isValid(String iso8601DateTimeString, ConstraintValidatorContext context) {
        return isValidISO8601ToDateTime(iso8601DateTimeString) || iso8601DateTimeString.isEmpty();
    }

    public static boolean isValidISO8601ToDateTime(String isoDateString) {
        try {
            ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(isoDateString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
