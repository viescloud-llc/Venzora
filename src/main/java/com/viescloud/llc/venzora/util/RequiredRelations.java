package com.viescloud.llc.venzora.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

/**
 * Pre-flight check for the {@code @ManyToOne} / {@code @OneToOne} relations a
 * client is expected to supply, so a missing or dangling reference comes back as
 * a {@code 400} naming the field instead of a {@code 500} from Hibernate.
 *
 * <p><b>Why this exists.</b> The framework's own not-null validator
 * ({@code ViesService.validateNotNullField}) is built from
 * {@code JpaReflectionUtils.getColumnAnnotationValueMapFrom}, which only reads
 * {@code @Column} annotations. Required <em>relations</em> are declared with
 * {@code @JoinColumn(nullable = false)} / {@code @ManyToOne(optional = false)},
 * so they are invisible to it. A product posted with {@code category:{id:""}}
 * therefore sailed past validation and blew up at flush time as a transient-
 * instance error, surfacing as
 * {@code 500 "Transaction silently rolled back because it has been marked as rollback-only"}.
 *
 * <p><b>What is skipped, and why.</b> Relations annotated {@code @JsonIgnore}
 * are parent back-references ({@code ProductVariant.product},
 * {@code CartItem.cart}, {@code OrderFulfillmentItem.orderFulfillment}). They
 * are unreachable from incoming JSON by design and are stamped server-side by
 * the owning entity's {@code @PrePersist} hook — which runs <em>after</em> this
 * check. Validating them here would reject every legitimate nested write, so
 * they are deliberately left alone.
 *
 * <p>Only the fields declared directly on the entity are inspected; nested
 * children are not walked. Each child is validated by its own service when it is
 * written on its own.
 */
public final class RequiredRelations {

    private RequiredRelations() {}

    /**
     * Validates every client-supplied required relation on {@code entity}.
     *
     * @param entity        the entity about to be written; a null entity is a no-op
     * @param entityManager used to confirm the referenced row exists. When null
     *                      (the framework autowires it optionally) the existence
     *                      check is skipped and only presence is enforced.
     * @throws ResponseStatusException {@code 400} naming the offending field
     */
    public static void validate(Object entity, EntityManager entityManager) {
        if (entity == null) {
            return;
        }

        for (Field field : declaredFieldsOf(entity.getClass())) {
            if (!isRequiredRelation(field) || field.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }

            Object reference = read(field, entity);
            if (reference == null) {
                throw badRequest(field.getName() + " is required");
            }

            Field idField = idFieldOf(field.getType());
            if (idField == null) {
                // No @Id on the referenced type — nothing further we can check.
                continue;
            }

            Object id = read(idField, reference);
            if (id == null || (id instanceof CharSequence cs && cs.length() == 0)) {
                throw badRequest(field.getName() + "." + idField.getName() + " is required");
            }

            if (entityManager != null && entityManager.find(field.getType(), id) == null) {
                throw badRequest(field.getName() + " not found: " + id);
            }
        }
    }

    /**
     * A relation is required when the join column is explicitly non-nullable or
     * the association itself is declared non-optional. Both spellings appear in
     * this codebase, sometimes on the same field.
     */
    private static boolean isRequiredRelation(Field field) {
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (manyToOne == null && oneToOne == null) {
            return false;
        }

        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null && !joinColumn.nullable()) {
            return true;
        }
        return (manyToOne != null && !manyToOne.optional())
                || (oneToOne != null && !oneToOne.optional());
    }

    private static Field idFieldOf(Class<?> type) {
        for (Field field : declaredFieldsOf(type)) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        return null;
    }

    /** Every declared field from {@code type} up to (but excluding) Object. */
    private static List<Field> declaredFieldsOf(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static Object read(Field field, Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // An unreadable field cannot be validated; let the write proceed and
            // fail downstream rather than rejecting a legitimate request here.
            return null;
        }
    }

    /**
     * Thrown as a plain {@link ResponseStatusException} rather than via
     * {@code HttpResponseThrowers} — the framework's builder-style throwers drop
     * the reason into the internal-log slot, so the client sees a body with
     * {@code reason: null}. See {@code document/vies-spring-utils-fix-checklist.md}.
     */
    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
