package ru.bazzar.organization.converters;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.bazzar.organization.entities.Logo;

import java.io.IOException;

@Component
public class LogoConverter {
    public Logo toLogoEntity(MultipartFile file) throws IOException {
        Logo logo = new Logo();
        logo.setName(file.getName());
        logo.setOriginalFileName(file.getOriginalFilename());
        logo.setContentType(file.getContentType());
        logo.setSize(file.getSize());
        logo.setBytes(file.getBytes());
        return logo;
    }
}
