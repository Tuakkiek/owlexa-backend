package com.owlexa.owlexabackend.modules.class_management.service.validation;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class TimeRangeValidator implements ScheduleValidator {

    @Override
    public void validate(ScheduleValidationContext context) {
        if (context.getStartTime() == null || context.getEndTime() == null) {
            throw new BusinessRuleException("INVALID_TIME_RANGE", "Start time and end time are required.");
        }
        if (!context.getStartTime().isBefore(context.getEndTime())) {
            throw new BusinessRuleException("INVALID_TIME_RANGE", "Start time must be before end time.");
        }
    }
}
