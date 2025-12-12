package shapes.templates;

import dto.PointDTO;

public class QuadrantShape implements Shape {
    private final Shape shape;
    private final int quadrant;

    public QuadrantShape(Shape shape, int quadrant) {
        this.shape = shape;
        this.quadrant = quadrant;
    }

    @Override
    public boolean contains(PointDTO p) {
        double x = p.x();
        double y = p.y();
        switch (quadrant) {
            case 2 -> x = -x;
            case 3 -> { x = -x; y = -y; }
            case 4 -> y = -y;
        }
        return shape.contains(new PointDTO(x, y, p.r()));
    }
}
