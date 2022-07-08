package com.atixlabs.semillasmiddleware.security.service;

import com.atixlabs.semillasmiddleware.security.exceptions.ExistUserException;
import com.atixlabs.semillasmiddleware.security.exceptions.InexistentUserException;
import com.atixlabs.semillasmiddleware.security.exceptions.PasswordNotMatchException;
import com.atixlabs.semillasmiddleware.security.repository.RoleRepository;
import com.atixlabs.semillasmiddleware.security.repository.UserRepository;
import com.atixlabs.semillasmiddleware.security.dto.FilterUserDto;
import com.atixlabs.semillasmiddleware.security.dto.UserEditRequest;
import com.atixlabs.semillasmiddleware.security.model.Role;
import com.atixlabs.semillasmiddleware.security.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;
import java.util.*;

import static com.atixlabs.semillasmiddleware.security.util.ObjectMapper.mergeObjects;

@Service
@Slf4j
public class UserService {
    @Autowired
    UserRepository repository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    public Collection<User> findAll() {
        return Lists.newArrayList(repository.findAll());
    }

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id : " + id));
    }

    public User disable(Long id) {
        return repository.findById(id).map(user -> {
            user.setActive(false);
            return repository.save(user);
        }).orElseThrow(() -> new EntityNotFoundException("User not exist."));
    }

    public User enable(Long id) {
        return repository.findById(id).map(user -> {
            user.setActive(true);
            return repository.save(user);
        }).orElseThrow(() -> new EntityNotFoundException("User not exist."));
    }

    public User createOrEdit(UserEditRequest userResponse) throws InexistentUserException, ExistUserException, PasswordNotMatchException {
        User user;
        if (userResponse.getId() != null) {
            user = repository.findById(userResponse.getId()).orElseThrow(RuntimeException::new);
            if (isNullOrEmpty(userResponse.getPassword())
                    || !passwordEncoder.matches(userResponse.getPassword(), user.getPassword())) {
                throw new InexistentUserException();
            }
        }

        Optional<User> opUser = repository.findByUsername(userResponse.getUsername());
        if (opUser.isPresent()) {
            throw new ExistUserException(opUser.get());
        } else {
            user = new User();
        }

        if (isNullOrEmpty(userResponse.getNewPassword())
                || isNullOrEmpty(userResponse.getConfirmNewPassword())
                || !userResponse.getNewPassword().equals(userResponse.getConfirmNewPassword())) {
            throw new PasswordNotMatchException("Password not found or not same.");
        }

        Role role = roleRepository.findByCode(userResponse.getRole());
        user.setName(userResponse.getName());
        user.setLastName(userResponse.getLastName());
        user.setEmail(userResponse.getEmail());
        user.setPhone(userResponse.getPhone());
        user.setPassword(userResponse.getNewPassword());
        user.setRole(role);
        user.setUsername(userResponse.getUsername());
        return repository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    public Optional<User> updateUser(Map<String, Object> updates, Long id) throws Exception {

        User existentUser;
        Optional<String> password = Optional.empty();
        Role role = null;

        if (id == null || id < 1) return Optional.empty();
        existentUser = repository.findById(id).orElseThrow(InexistentUserException::new);

        if (updates.containsKey(Constants.ROLE_CODE_FRONTEND)) {
            role = roleRepository.findByCode(updates.get(Constants.ROLE_CODE_FRONTEND).toString());
        }

        if (updates.containsKey("password")
                && !passwordEncoder.matches(
                updates.get("password").toString(), existentUser.getPassword())) {
            log.warn(
                    String.format(
                            "Error changed password. Password not match for user %s",
                            existentUser.getUsername()));
            throw new PasswordNotMatchException("Password not found or not same.");
        }
        if (updates.containsKey(Constants.NEW_PASSWORD) && updates.containsKey(Constants.CONFIRM_PASSWORD)
                && updates.get(Constants.NEW_PASSWORD).equals(updates.get(Constants.CONFIRM_PASSWORD))) {
                password = Optional.of(passwordEncoder.encode(updates.get(Constants.NEW_PASSWORD).toString()));
        }

        Optional<User> tempUser = repository.findByUsername(updates.get(Constants.USERNAME).toString());
        if (updates.containsKey(Constants.USERNAME)
                && tempUser.isPresent()
                && updates.get(Constants.USERNAME).equals(existentUser.getUsername())
                && !tempUser.get().getId().equals(id)) {
            log.warn("Username already exists");
            return Optional.empty();
        }

        User updateUser;
        updateUser = new ObjectMapper().convertValue(parseUpdatesForUser(updates), User.class);
        if (role != null) updateUser.setRole(role);
        password.ifPresent(updateUser::setPassword);

        User third;
        try {
            third = mergeObjects(updateUser, existentUser);
            repository.save(third);
            return Optional.of(third);
        } catch (Exception ex) {
            log.error(Arrays.toString(ex.getStackTrace()));
            return Optional.empty();
        }
    }

    private Map<String, Object> parseUpdatesForUser(Map<String, Object> updates) {
        updates.remove(Constants.TYPE_CODE_FRONTEND);
        updates.remove(Constants.COUNTRY_OR_ZONE_CODE_FRONTEND);
        updates.remove(Constants.ROLE_CODE_FRONTEND);
        updates.remove(Constants.NEW_PASSWORD);
        updates.remove(Constants.CONFIRM_PASSWORD);
        return updates;
    }

    public Collection<User> findAll(FilterUserDto filter) {
        return repository.findAll(getUserSpecification(filter));
    }

    private Specification<User> getUserSpecification(FilterUserDto filter) {
        return (Specification<User>)
                (root, query, cb) -> {
                    List<Predicate> predicates = Lists.newLinkedList();
                    if (!isNullOrEmpty(filter.getSearch())) {
                        String search = "%".concat(filter.getSearch()).concat("%").toUpperCase();
                        predicates.add(
                                cb.or(
                                        cb.like(cb.upper(root.get("name")), search),
                                        cb.like(cb.upper(root.get("lastName")), search),
                                        cb.like(cb.upper(root.get(Constants.USERNAME)), search)));
                    }
                    if (filter.getId().isPresent()) {
                        predicates.add(cb.notEqual(root.get("id"), filter.getId().get()));
                    }
                    if (filter.getEnabled().isPresent()) {
                        predicates.add(cb.equal(root.get("active"), filter.getEnabled().get()));
                    }
                    if (!isNullOrEmpty(filter.getRole().orElse(null))) {
                        String search = "%".concat(filter.getRole().get()).concat("%").toUpperCase();
                        predicates.add(cb.like(cb.upper(root.get("role").get("description")), search));
                    }
                    return cb.and(predicates.toArray(new Predicate[predicates.size()]));
                };
    }

    public Page<User> findUsersFilteredAndPaginated(FilterUserDto filter, Pageable page) {
        return repository.findAll(getUserSpecification(filter), page);
    }

    private boolean isNullOrEmpty(String word){
        return (word == null || StringUtils.isEmpty(word));
    }


    public List<User> getAll(){
        return repository.findAll();
    }

    public User findOne(Long id) {
        return repository.findById(id).orElseThrow(RuntimeException::new);
    }

    private static class Constants{
        public static final String ROLE_CODE_FRONTEND = "role";

        public static final String TYPE_CODE_FRONTEND = "type";

        public static final String COUNTRY_OR_ZONE_CODE_FRONTEND = "selectedKey";
        public static final String NEW_PASSWORD = "newPassword";
        public static final String CONFIRM_PASSWORD = "confirmNewPassword";
        public static final String USERNAME = "username";
    }
}
