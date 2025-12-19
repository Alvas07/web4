package shapes.factories;

import shapes.templates.Shape;
import shapes.templates.Triangle;

public class TriangleFactory implements ShapeFactory {
    private final double baseRatio;
    private final double heightRatio;

    public TriangleFactory(double baseRatio, double heightRatio) {
        this.baseRatio = baseRatio;
        this.heightRatio = heightRatio;
    }

    @Override
    public Shape create(double r) {
        return new Triangle(baseRatio*r, heightRatio*r);
    }
}
