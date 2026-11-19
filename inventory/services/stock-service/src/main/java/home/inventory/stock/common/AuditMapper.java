package home.inventory.stock.common;

import home.inventory.stock.common.dto.AuditDto;

public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditDto toDto(AuditedEntity<?> entity) {
        return new AuditDto(entity.createdDate(), entity.lastModifiedDate(), entity.version(), entity.deleted());
    }
}
