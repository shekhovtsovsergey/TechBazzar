package ru.bazzar.organization.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.bazzar.organization.api.LogoSaverException;
import ru.bazzar.organization.api.OrganizationDto;
import ru.bazzar.organization.api.ResourceNotFoundException;
import ru.bazzar.organization.converters.OrganizationConverter;
import ru.bazzar.organization.entities.Logo;
import ru.bazzar.organization.entities.Organization;
import ru.bazzar.organization.repositories.OrganizationRepository;
import ru.bazzar.organization.utils.MyQueue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository repository;
    private final LogoService logoService;
    private final OrganizationConverter organizationConverter;
    private final MyQueue<Organization> myQueue = new MyQueue<>();

    public void save(OrganizationDto organizationDto, String username, MultipartFile file){
        try{
            Organization organization = organizationConverter.dtoToEntity(organizationDto);
            organization.setOwner(username);
            organization.setLogo(logoService.save(file));
            log.info("Добавлена новая организация {}, её собственник {}", organization.getTitle(), organization.getOwner());
            myQueue.enqueue(organization);
            repository.save(organization);
        }catch (IOException e){
            throw new LogoSaverException("Невозможно сохранить лого компании (MultipartFile - IOException) в сервисе логотипов");
        }

    }

    public Organization notConfirmed(){
        if (myQueue.isEmpty()) {
            List<Organization> notActiveList = repository.findAllByIsActive(false);
            if (notActiveList.isEmpty()) {
                throw new ResourceNotFoundException("Не подтвержденных организаций больше нет.");
            }
            for (Organization organization : notActiveList) {
                myQueue.enqueue(organization);
            }
        }
        return myQueue.dequeue();
    }

    public Organization findByTitleIgnoreCase(String title) {
        return repository.findByTitleIgnoreCase(title)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с названием: " + title + " не найдена."));
    }

    public List<OrganizationDto> findAllByOwner(String owner) {
        return repository.findAllByOwner(owner)
                .stream()
                .map(organizationConverter::entityToDto)
                .collect(Collectors.toList());
    }

    public Organization findByTitle(String title) throws ResourceNotFoundException {
        return repository.findByTitleIgnoreCase(title)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с названием: " + title + " не найдена."));
    }

    public Logo findLogoByTitleOrganization(String title) {
        return findByTitle(title).getLogo();
    }

    public void confirm(String title) {
        Organization organization = repository.findByTitleIgnoreCase(title).orElseThrow(
                () -> new ResourceNotFoundException("Организация с названием: " + title + " не найдена.")
        );
        organization.setActive(true);
        repository.save(organization);
    }

    public List<OrganizationDto> findAll() {
        return repository.findAll()
                .stream()
                .map(organizationConverter::entityToDto)
                .collect(Collectors.toList());
    }

    public void orgBun(Long id) {
        Organization organization = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с id: " + id + " не найдена!"));
        organization.setActive(!organization.isActive());
        repository.save(organization);
    }

    public boolean isOrgActive(String title) {
        Organization organization = repository.findByTitleIgnoreCase(title)
                .orElseThrow(() -> new ResourceNotFoundException("Организация с названием: " + title + " не найдена."));
        return organization.isActive();
    }

    public List<String> getOrganizationTitles() {
        return repository.getOrganizationTitles();
    }
}
