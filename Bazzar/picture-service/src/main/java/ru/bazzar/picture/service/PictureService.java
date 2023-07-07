package ru.bazzar.picture.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bazzar.picture.api.ResourceNotFoundException;
import ru.bazzar.picture.converters.PictureConverter;
import ru.bazzar.picture.entities.Picture;
import ru.bazzar.picture.repositories.PictureRepository;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PictureService {

    private final PictureRepository pictureRepository;
    private final PictureConverter pictureConverter;

    public Picture save(Picture picture){
        return pictureRepository.save(picture);
    }

    @Cacheable(cacheNames = {"pic"}, sync = true, key = "#id")
    public Picture findById(Long id){
        return pictureRepository.findById(id).orElseThrow(
                ()-> new ResourceNotFoundException("Картинка с id: " + id + " - не найдена!")
        );
    }

    @CacheEvict(cacheNames = {"pic"})
    public void deleteById(Long id){
            pictureRepository.deleteById(id);
            log.warn("deleteById "+ id + ".");
    }

    @CacheEvict(cacheNames = {"pic"}, allEntries = true)
    public void cacheEvict(){};



}