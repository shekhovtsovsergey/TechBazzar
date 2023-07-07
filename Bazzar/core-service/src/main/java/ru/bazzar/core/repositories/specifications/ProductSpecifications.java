package ru.bazzar.core.repositories.specifications;


import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import ru.bazzar.core.entities.Characteristic;
import ru.bazzar.core.entities.Product;


public class ProductSpecifications {
    public static Specification<Product> priceGreaterOrEqualsThan(Integer minPrice) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessOrEqualsThan(Integer maxPrice) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> titleLike(String titlePart) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.upper(root.get("title")), String.format("%%%s%%", titlePart.toUpperCase()));
    }

    public static Specification<Product> titleCompanyLike(String organizationTitle) {
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("organizationTitle"), organizationTitle);
    }

    public static Specification<Product> characteristicLike(String characteristicPart) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Join<Product, Characteristic> characteristicJoin = root.join("characteristics");
            return criteriaBuilder.like(
                    criteriaBuilder.lower(characteristicJoin.get("name")),
                    String.format("%%%s%%", characteristicPart.toLowerCase())
            );
        };
    }
}
