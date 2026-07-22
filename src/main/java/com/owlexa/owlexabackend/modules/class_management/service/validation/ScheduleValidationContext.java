package com.owlexa.owlexabackend.modules.class_management.service.validation;

import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.room.entity.Room;
import com.owlexa.owlexabackend.modules.user.entity.User;
import lombok.Builder;
import lombok.Value;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Value
@Builder
public class ScheduleValidationContext {
    Long scheduleId;
    Class clazz;
    Room room;
    User teacher;
    DayOfWeek dayOfWeek;
    LocalTime startTime;
    LocalTime endTime;
    Long centerId;
}
