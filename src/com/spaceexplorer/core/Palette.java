package com.spaceexplorer.core;

import java.awt.Color;

/**
 * Central colour definitions so the whole ship keeps a consistent sci-fi look.
 * These are the *fully lit* colours: the darkness overlay in {@link Lighting}
 * dims them down while the ship has no power.
 */
public final class Palette {

    private Palette() {
        // static only
    }

    /* Structure */
    public static final Color VOID = new Color(0x0A0F1A);
    public static final Color FLOOR_A = new Color(0x3B4659);
    public static final Color FLOOR_B = new Color(0x445065);
    public static final Color FLOOR_LINE = new Color(0x2E374A);
    public static final Color WALL_BODY = new Color(0x55617A);
    public static final Color WALL_TOP = new Color(0x7C8AA6);
    public static final Color WALL_EDGE = new Color(0x232B3A);
    public static final Color DOOR_BODY = new Color(0x6B7791);

    /* Props / metal */
    public static final Color METAL_DARK = new Color(0x39435A);
    public static final Color METAL = new Color(0x5A6880);
    public static final Color METAL_LIGHT = new Color(0x8E9BB5);

    /* Screens + lights */
    public static final Color SCREEN_OFF = new Color(0x4A1418);
    public static final Color SCREEN_ON = new Color(0x0F3A28);
    public static final Color SCREEN_TEXT_OFF = new Color(0xFF9B7E);
    public static final Color SCREEN_TEXT_ON = new Color(0x9BFFD2);
    public static final Color LED_OFF = new Color(0xB32B2B);
    public static final Color LED_EMERGENCY = new Color(0xFF6A3D);
    public static final Color LED_ON = new Color(0x5CFFB0);

    /* Hazards */
    public static final Color GAS = new Color(0x8FE8B4);

    /* UI */
    public static final Color TEXT_PRIMARY = new Color(0xE6EEF9);
    public static final Color TEXT_DIM = new Color(0x9DAAC2);
    public static final Color ACCENT_AMBER = new Color(0xFFC46B);
    public static final Color ACCENT_BLUE = new Color(0x7FD6FF);
    public static final Color HAZARD = new Color(0xF2C14E);

    /* Player */
    public static final Color PLAYER_BODY = new Color(0x2F89A8);
    public static final Color PLAYER_LIGHT = new Color(0x8FD9E8);
    public static final Color PLAYER_DARK = new Color(0x1B4A5C);
}
