package com.auction.ui;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Equal columns on wide windows, vertical reading order on compact windows. */
public final class ResponsiveColumns extends Pane {
    private double breakpoint = 720;
    private double gap = 20;
    private boolean compactReverse;
    public ResponsiveColumns() { setMinWidth(0); }
    @Override public javafx.geometry.Orientation getContentBias() {
        return javafx.geometry.Orientation.HORIZONTAL;
    }
    public double getBreakpoint() { return breakpoint; }
    public void setBreakpoint(double value) { breakpoint = value; requestLayout(); }
    public double getGap() { return gap; }
    public void setGap(double value) { gap = value; requestLayout(); }
    public boolean isCompactReverse() { return compactReverse; }
    public void setCompactReverse(boolean value) { compactReverse = value; requestLayout(); }
    private List<Node> items(double width) {
        List<Node> result = new ArrayList<>(getManagedChildren());
        if (width < breakpoint && compactReverse) Collections.reverse(result);
        return result;
    }
    @Override protected double computeMinWidth(double height) { return 0; }
    @Override protected double computePrefWidth(double height) { return 900; }
    @Override protected double computeMinHeight(double width) { return computePrefHeight(width); }
    @Override protected double computePrefHeight(double width) {
        double available = width < 0 ? 900 : width;
        List<Node> nodes = items(available);
        if (nodes.isEmpty()) return 0;
        boolean columns = available >= breakpoint;
        double childWidth = columns ? Math.max(0, (available - gap * (nodes.size() - 1)) / nodes.size()) : available;
        double height = columns ? 0 : gap * (nodes.size() - 1);
        for (Node node : nodes) {
            double preferred = node.prefHeight(childWidth);
            height = columns ? Math.max(height, preferred) : height + preferred;
        }
        return height;
    }
    @Override protected void layoutChildren() {
        List<Node> nodes = items(getWidth());
        if (nodes.isEmpty()) return;
        boolean columns = getWidth() >= breakpoint;
        double childWidth = columns ? Math.max(0, (getWidth() - gap * (nodes.size() - 1)) / nodes.size()) : getWidth();
        double offset = 0;
        for (Node node : nodes) {
            double height = node.prefHeight(childWidth);
            node.resizeRelocate(columns ? offset : 0, columns ? 0 : offset, childWidth, height);
            offset += (columns ? childWidth : height) + gap;
        }
    }
}
