package com.owlexa.owlexabackend.modules.class_management.service.validation;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import org.springframework.stereotype.Component;

@Component
public class ClassLifecycleValidator implements ScheduleValidator {

    @Override
    public void validate(ScheduleValidationContext context) {
        if (context.getClazz().getStatus() == ClassStatus.FINISHED) {
            throw new BusinessRuleException(
                    "CLASS_FINISHED",
                    "Chỉ có thể thêm hoặc chỉnh sửa lịch khi lớp đang ở trạng thái dự kiến hoặc đang học."
            );
        }
    }
}
