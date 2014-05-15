package nl.tudelft.sps.app.localization;

import nl.tudelft.sps.app.R;

public enum Room {
    C1_AISLE1 (R.id.c1_a1),
    C2_AISLE2 (R.id.c2_a2),
    C3_AISLE3 (R.id.c3_a3),
    C4_AISLE4 (R.id.c4_a4),
    C5_AISLE5 (R.id.c5_a5),
    C6_AISLE6 (R.id.c6_a6),
    C7_AISLE7 (R.id.c7_a7),
    C8_AISLE8 (R.id.c8_a8),
    C9_AISLE9 (R.id.c9_a9),
    C10_AISLE10 (R.id.c10_a10),
    C11_CONF_BTM (R.id.c11_conf_btm),
    C12_CONF_TOP (R.id.c12_conf_top),
    C13_COFFEE (R.id.c13_coffee),
    C14_PLEA (R.id.c14_plea),
    C15_MTG (R.id.c15_mtg),
    C16_FLEX_BTM (R.id.c16_flex_btm),
    C17_FLEX_TOP (R.id.c17_flex_top),
    Unknown (0);

    private final int id;

    private Room (final int id) {
        this.id = id;
    }

    public int getIdentifier() {
        if (id == 0) {
            throw new RuntimeException("You're not supposed to call this method for enum " + String.valueOf(this));
        }
        return id;
    }

    public static Room getEnum(final int id) {
        if (id == 0) {
            throw new RuntimeException("You're not supposed to call this method for id " + String.valueOf(id));
        }
        for (Room room : Room.values()) {
            if (room.getIdentifier() == id) {
                return room;
            }
        }
        throw new RuntimeException("Invalid Room id");
    }
}
