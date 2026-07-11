package com.viescloud.llc.venzora.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** ISO-8601 timestamp range, echoed back on every report response. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportPeriod {

    private String from;
    private String to;
}
