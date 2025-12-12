package shapes.templates;

import dto.PointDTO;

public class Rectangle implements Shape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean contains(PointDTO p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() <= width && p.y() <= height;
    }
}
