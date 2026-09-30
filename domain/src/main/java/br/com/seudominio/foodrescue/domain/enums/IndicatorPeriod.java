package br.com.seudominio.foodrescue.domain.enums;

/**
 * Predefined periods for calculating consolidated indicators (UC12).
 *
 * @author Clovis Luan
 * @since 1.0.0
 */
public enum IndicatorPeriod {

    /** Current day (from 00:00:00 to 23:59:59). */
    DAY,

    /** Last 7 days including today. */
    WEEK,

    /** Current month (from 1st day of month to today). */
    MONTH,

    /** Explicit custom date range informed by the client. */
    CUSTOM
}
