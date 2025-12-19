package shapes.factories;

import shapes.templates.Rectangle;
import shapes.templates.Shape;

public class RectangleFactory implements ShapeFactory {
    private final double widthRatio;
    private final double heightRatio;

    public RectangleFactory(double widthRatio, double heightRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }

    @Override
    public Shape create(double r) {
        return new Rectangle(widthRatio*r, heightRatio*r);
    }
}
