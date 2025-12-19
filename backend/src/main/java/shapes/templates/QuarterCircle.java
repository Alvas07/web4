package shapes.templates;

import dto.PointDTO;

public class QuarterCircle implements Shape {
    private final double radius;

    public QuarterCircle(double radius) {
        this.radius = radius;
    }

    @Override
    public boolean contains(PointDTO p) {
        return p.x() >= 0 && p.y() >= 0 && (p.x() * p.x() + p.y() * p.y() <= radius * radius);
    }
}
