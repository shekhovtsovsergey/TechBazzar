package ru.bazzar.auth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bazzar.auth.api.ResourceNotFoundException;
import ru.bazzar.auth.entities.Role;
import ru.bazzar.auth.repositories.RoleRepository;

@Component
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public Role findRoleByName(String name) {
        return roleRepository.findRoleByName(name).orElseThrow(() -> new ResourceNotFoundException("Роль с id: " + name + " не найдена!"));
    }

}
