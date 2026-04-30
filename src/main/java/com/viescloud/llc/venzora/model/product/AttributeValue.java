package com.viescloud.llc.venzora.model.product;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.viescloud.eco.viesspringutils.config.jpa.BooleanConverter;
import com.viescloud.eco.viesspringutils.config.jpa.DateConverter;
import com.viescloud.eco.viesspringutils.config.jpa.DateTimeConverter;
import com.viescloud.eco.viesspringutils.config.jpa.TimeConverter;
import com.viescloud.eco.viesspringutils.util.Date;
import com.viescloud.eco.viesspringutils.util.DateTime;
import com.viescloud.eco.viesspringutils.util.Time;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttributeValue {
    @Column(columnDefinition = "TEXT")
    private String textValue;

    @Column
    private BigDecimal numberValue;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = BooleanConverter.class)
    private Boolean booleanValue;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = DateConverter.class)
    private Date dateValue;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = TimeConverter.class)
    private Time timeValue;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = DateTimeConverter.class)
    private DateTime dateTimeValue;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    private AttributeOption selectValue;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REFRESH, CascadeType.DETACH})
    private List<AttributeOption> multiSelectValues;
}
