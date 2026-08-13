package net.friendly_bets.models.enums;

/**
 * Диапазоны кэфа. Нижняя граница строго больше, верхняя включительно.
 * Первое ведро включает 1.00, иначе такие кэфы не попали бы ни в один диапазон.
 *
 * SUPER_LOW:    ≤ 1.50
 * LOW:          (1.50, 1.80]
 * MEDIUM:       (1.80, 2.20]
 * HIGH:         (2.20, 2.50]
 * VERY_HIGH:    (2.50, 4.00]
 * UNLIKELY:     (4.00, 7.00]
 * COSMIC:       (7.00, 15.00]
 * UNREALISTIC:  > 15.00
 */
public enum BetValueRange {
    SUPER_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH,
    UNLIKELY,
    COSMIC,
    UNREALISTIC;

    public static BetValueRange fromOdds(double odds) {
        if (odds <= 1.5) {
            return SUPER_LOW;
        }
        if (odds <= 1.8) {
            return LOW;
        }
        if (odds <= 2.2) {
            return MEDIUM;
        }
        if (odds <= 2.5) {
            return HIGH;
        }
        if (odds <= 4.0) {
            return VERY_HIGH;
        }
        if (odds <= 7.0) {
            return UNLIKELY;
        }
        if (odds <= 15.0) {
            return COSMIC;
        }
        return UNREALISTIC;
    }
}
