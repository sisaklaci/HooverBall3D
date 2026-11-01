package com.sisak.hooverball.graphics;

import com.badlogic.gdx.math.Rectangle;

/** A simple rectangular hit-area with a label, shared by all screens' menus/overlays. */
public class UiButton {
    public final Rectangle rect = new Rectangle();
    public String label;
    public boolean selected;

    public UiButton(String label) {
        this.label = label;
    }

    public boolean hit(float x, float y) {
        return rect.contains(x, y);
    }
}
