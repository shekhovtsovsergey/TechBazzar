package ru.bazzar.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.bazzar.core.api.*;
import ru.bazzar.core.configs.GlobalEnum;
import ru.bazzar.core.entities.Product;
import ru.bazzar.core.integrations.OrganizationServiceIntegration;
import ru.bazzar.core.integrations.PictureServiceIntegration;
import ru.bazzar.core.integrations.UserServiceIntegration;
import ru.bazzar.core.repositories.ProductRepository;
import ru.bazzar.core.repositories.specifications.ProductSpecifications;
import ru.bazzar.core.utils.MyQueue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final OrganizationServiceIntegration organizationService;
    private final UserServiceIntegration userService;
    private final MyQueue<Product> productQueue = new MyQueue<>();
    private final String adminEmail = GlobalEnum.ADMIN_EMAIL.getValue();
    private final PictureServiceIntegration pictureService;

    @CacheEvict(cacheNames = {"product"})
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @CacheEvict(cacheNames = {"product"}, allEntries = true)
    public void evictCache() {
    }

    @Cacheable(cacheNames = {"product"}, sync = true, key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Продукт не найден, id: " + id));
    }

    public Page<Product> find(Integer minPrice, Integer maxPrice, String titlePart, String organizationTitle, Integer page, String characteristicPart, int limit) {

        Specification<Product> spec = Specification.where(null);
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.priceGreaterOrEqualsThan(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.priceLessOrEqualsThan(maxPrice));
        }
        if (titlePart != null) {
            spec = spec.and(ProductSpecifications.titleLike(titlePart));
        }
        if (organizationTitle != null) {
            spec = spec.and(ProductSpecifications.titleCompanyLike(organizationTitle));
        }

        if (characteristicPart != null) {
            spec = spec.and(ProductSpecifications.characteristicLike(characteristicPart));
        }
        return productRepository.findAll(spec, PageRequest.of(page - 1, limit));

    }

    public Product notConfirmed() {
        if (productQueue.isEmpty()) {
            List<Product> notConfirmList = productRepository.findAllByIsConfirmed(false);
            if (notConfirmList.isEmpty()) {
                throw new AccessException("Не подтвержденных продуктов больше нет.");
            }
            for (Product product : notConfirmList) {
                productQueue.enqueue(product);
            }
        }
        return productQueue.dequeue();
    }

    public void confirm(String title) {
        Product product = productRepository.findByTitleIgnoreCase(title)
                .orElseThrow(() -> new ResourceNotFoundException("Продукт: " + title + " не найден!"));
        product.setConfirmed(true);
        productRepository.save(product);
        evictCache();//затирает кэш
    }

    public void changeQuantity(Product product) {
        Product productFromDB = productRepository.findById(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Продукт не найден, id: " + product.getId()));
        if (productFromDB.getQuantity() >= product.getQuantity()) {
            productFromDB.setQuantity(productFromDB.getQuantity() - product.getQuantity());
            productRepository.save(productFromDB);
        } else {
            throw new AccessException("Недостаточное количество продукта, id: " + product.getId());
        }
    }

    public ProductDto setProductPicture(ProductDto productDto, MultipartFile multipartFile) {
        Long picId = 1L;
        PictureDto pictureDto = null;
        try {
            pictureDto = PictureDto.builder()
                    .fileName(multipartFile.getOriginalFilename())
                    .contentType(multipartFile.getContentType())
                    .bytes(multipartFile.getBytes())
                    .build();
            productDto.setPictureId(pictureService.savePictureDtoAndReturnId(pictureDto));
        } catch (IOException e) {
            throw new MultipartBuilderException
                    ("Невозможно прочитать байты и сформировать pictureDto.");
        }
        return productDto;
    }

    public OrganizationDto findAndAccessingOrganization(ProductDto productDto, String username) {
        OrganizationDto organizationDto = organizationService.getOrganizationByTitle(productDto.getOrganizationTitle());
        if (!organizationDto.isActive()) {
            throw new AccessException("Организация не прошла модерацию, попробуйте добавить продукт позже.");
        }
        if (!username.equalsIgnoreCase(organizationDto.getOwner())) {
            throw new AccessException("Только владелец компании может добавлять товары в магазин.");
        }
        if (!userService.getUser(organizationDto.getOwner()).isActive()) {
            throw new AccessException("Владелец организации забанен, обратитесь к администратору: " + adminEmail);
        }
        return organizationDto;
    }

    //сохраняем и возвращаем сущность в кэш
    public Product createProduct(ProductDto productDto, String username) {
        OrganizationDto organizationDto = findAndAccessingOrganization(productDto, username);
        Product product = new Product();

        if (productDto.getPictureId() == null || productDto.getPictureId() < 0) {
            product.setPictureId(1L);
        } else {
            product.setPictureId(productDto.getPictureId());
        }
        product.setTitle(productDto.getTitle());
        product.setDescription(productDto.getDescription());
        product.setOrganizationTitle(organizationDto.getTitle());//OrganizationServiceIntegration
        product.setPrice(productDto.getPrice());
        product.setConfirmed(false);
        product.setQuantity(productDto.getQuantity());
        //так надо для кэша!!!
        Long idToReturn = productRepository.save(product).getId();
        log.info("SAVE: " + findById(idToReturn).toString());
        return findById(idToReturn);
    }

    //обновляем и возвращаем сущность в кэш
    @CachePut(cacheNames = {"product"}, key = "#id")
    public Product updateProduct(ProductDto productDto, Long id) {

        Product productFromBd = productRepository.findById(productDto.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException
                                ("Продукт не найден, id: " + productDto.getId()));
        if (productDto.getTitle() != null) {
            productFromBd.setTitle(productDto.getTitle());
        }
        if (productDto.getDescription() != null) {
            productFromBd.setDescription(productDto.getDescription());
        }
        if (productDto.getPrice() != null) {
            productFromBd.setPrice(productDto.getPrice());
        }
        if (productDto.getQuantity() != 0) {
            productFromBd.setQuantity(productFromBd.getQuantity() + productDto.getQuantity());
        }
        if (productDto.getPictureId() != null) {
            //удаляем старую картинку
            pictureService.deletePictureById(productFromBd.getPictureId());
            //ставим новую картинку
            productFromBd.setPictureId(productDto.getPictureId());
        }
        return productRepository.save(productFromBd);
    }

    public Double calculateAverageRating(Long productId) {
        Double averageRating = productRepository.calculateAverageRating(productId);
        if (averageRating == null) {
            return 0.0;
        }
        return averageRating;
    }

    public Optional<PictureDto> getPictureByProductId(Long id) {
        Long pictureId = productRepository.findPictureIdById(id);
        if (pictureId != null) {
            return Optional.of(pictureService.getPictureDtoById(pictureId));
        }
        return Optional.empty();
    }
}