package home.inventory.shopping.common;

import home.inventory.shopping.common.dto.AuditDto;

public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditDto toDto(AuditedEntity<?> entity) {
        return new AuditDto(entity.createdDate(), entity.lastModifiedDate(), entity.version(), entity.deleted());
    }
}
