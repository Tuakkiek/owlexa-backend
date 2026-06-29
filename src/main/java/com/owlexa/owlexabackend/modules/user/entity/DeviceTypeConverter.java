package com.owlexa.owlexabackend.modules.user.entity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DeviceTypeConverter implements AttributeConverter<DeviceType, String> {

    @Override
    public String convertToDatabaseColumn(DeviceType attribute) {
        if (attribute == null) {
            return DeviceType.UNKNOWN.name();
        }
        return attribute.name();
    }

    @Override
    public DeviceType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return DeviceType.UNKNOWN;
        }

        return switch (dbData.trim().toUpperCase()) {
            case "WEB", "DESKTOP" -> DeviceType.DESKTOP;
            case "MOBILE" -> DeviceType.MOBILE;
            case "TABLET" -> DeviceType.TABLET;
            default -> DeviceType.UNKNOWN;
        };
    }
}
