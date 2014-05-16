package nl.tudelft.sps.app.localization;

import com.google.common.collect.ImmutableList;

import java.util.Collection;

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
    ;

    private final int id;
    private Collection<Room> adjacent;

    // Unfortunately you cannot do this in the constructors. See http://stackoverflow.com/a/5678375/572635
    static {
        C1_AISLE1.adjacent = ImmutableList.of(C2_AISLE2, C11_CONF_BTM, C12_CONF_TOP);
        C2_AISLE2.adjacent = ImmutableList.of(C1_AISLE1, C3_AISLE3, C13_COFFEE, C14_PLEA);
        C3_AISLE3.adjacent = ImmutableList.of(C2_AISLE2, C4_AISLE4, C14_PLEA);
        C4_AISLE4.adjacent = ImmutableList.of(C3_AISLE3, C5_AISLE5);
        C5_AISLE5.adjacent = ImmutableList.of(C4_AISLE4, C6_AISLE6);
        C6_AISLE6.adjacent = ImmutableList.of(C5_AISLE5, C7_AISLE7);
        C7_AISLE7.adjacent = ImmutableList.of(C6_AISLE6, C8_AISLE8);
        C8_AISLE8.adjacent = ImmutableList.of(C7_AISLE7, C9_AISLE9, C16_FLEX_BTM);
        C9_AISLE9.adjacent = ImmutableList.of(C8_AISLE8, C10_AISLE10, C16_FLEX_BTM, C17_FLEX_TOP);
        C10_AISLE10.adjacent = ImmutableList.of(C9_AISLE9);
        C11_CONF_BTM.adjacent = ImmutableList.of(C1_AISLE1, C12_CONF_TOP);
        C12_CONF_TOP.adjacent = ImmutableList.of(C1_AISLE1, C11_CONF_BTM);
        C13_COFFEE.adjacent = ImmutableList.of(C2_AISLE2, C3_AISLE3);
        C14_PLEA.adjacent = ImmutableList.of(C2_AISLE2);
        C15_MTG.adjacent = ImmutableList.of(C3_AISLE3);
        C16_FLEX_BTM.adjacent = ImmutableList.of(C8_AISLE8, C9_AISLE9);
        C17_FLEX_TOP.adjacent = ImmutableList.of(C9_AISLE9);
    }

    private Room (final int id) {
        this.id = id;
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

    public Collection<Room> getAdjacentRooms() {
        return adjacent;
    }

    public int getIdentifier() {
        if (id == 0) {
            throw new RuntimeException("You're not supposed to call this method for enum " + String.valueOf(this));
        }
        return id;
    }
}
