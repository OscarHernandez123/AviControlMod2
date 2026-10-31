package com.avicontrol.domain.port.out.galpon;

public record GalponPageQuery(int offset, int limit, String sortProperty, SortDirection direction) {

  public enum SortDirection {
    ASC,
    DESC
  }
}
