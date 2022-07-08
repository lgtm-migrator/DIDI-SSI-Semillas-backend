package com.atixlabs.semillasmiddleware.security.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Optional;

@Setter
@Getter
@Builder
public class FilterUserDto {

  @Builder.Default private Optional<Long> id = Optional.empty();

  private String search;

  @Builder.Default private Optional<Boolean> enabled = Optional.empty();

  @Builder.Default private Optional<String> role = Optional.empty();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FilterUserDto that = (FilterUserDto) o;
    if(!verifyId(that) || !verifyState(that) || !verifyRole(that)) return false;

    return Objects.equals(search, that.search);
  }

  private boolean verifyId(FilterUserDto that){
    if (that.id.isPresent() && !id.isPresent()) return false;
    if (!that.id.isPresent() && id.isPresent()) return false;
    return (that.id.isPresent() && id.isPresent() && !that.id.get().equals(id.get()));
  }

  private boolean verifyState(FilterUserDto that){
    if (that.enabled.isPresent() && !enabled.isPresent()) return false;
    if (!that.enabled.isPresent() && enabled.isPresent()) return false;
    return (that.enabled.isPresent() && enabled.isPresent() && !that.enabled.get().equals(enabled.get()));
  }

  private boolean verifyRole(FilterUserDto that){
    if (that.role.isPresent() && !role.isPresent()) return false;
    if (!that.role.isPresent() && role.isPresent()) return false;
    return (that.role.isPresent() && role.isPresent() && !that.role.get().equals(role.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, search, enabled, role);
  }
}
