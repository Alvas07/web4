package services;

import dto.CheckResultDTO;
import dto.PointDTO;
import shapes.QuadrantShapeTemplate;
import shapes.templates.QuadrantShape;
import shapes.templates.Shape;

import java.util.ArrayList;
import java.util.List;

public class AreaCheckService {
    private final List<QuadrantShapeTemplate> shapeTemplates;

    public AreaCheckService(List<QuadrantShapeTemplate> shapeTemplates) {
        this.shapeTemplates = shapeTemplates;
    }

    public List<CheckResultDTO> checkPoints(List<PointDTO> points) {
        List<CheckResultDTO> results = new ArrayList<>();
        for (PointDTO p : points) {
            results.add(new CheckResultDTO(p, checkSinglePoint(p)));
        }
        return results;
    }

    public boolean checkSinglePoint(PointDTO p) {
        List<Shape> shapesForPoint = new ArrayList<>();
        for (QuadrantShapeTemplate template : shapeTemplates) {
            shapesForPoint.add(new QuadrantShape(template.getFactory().create(p.r()), template.getQuadrant()));
        }
        HitChecker checker = new HitChecker(shapesForPoint);
        return checker.isHit(p);
    }
}
