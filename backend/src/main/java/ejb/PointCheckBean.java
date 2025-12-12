package ejb;

import dto.CheckResultDTO;
import dto.PointDTO;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import services.AreaCheckService;
import shapes.QuadrantShapeTemplate;
import shapes.factories.QuarterCircleFactory;
import shapes.factories.RectangleFactory;
import shapes.factories.TriangleFactory;

import java.util.Arrays;
import java.util.List;

@Stateless
public class PointCheckBean {
    private AreaCheckService areaCheckService;

    @EJB
    private PointValidationBean validationBean;

    @PostConstruct
    public void init() {
        List<QuadrantShapeTemplate> templates = Arrays.asList(
                new QuadrantShapeTemplate(new RectangleFactory(1.0, 1.0), 2),
                new QuadrantShapeTemplate(new TriangleFactory(1.0, 1.0), 3),
                new QuadrantShapeTemplate(new QuarterCircleFactory(1.0), 4)
        );
        this.areaCheckService = new AreaCheckService(templates);
    }

    public List<CheckResultDTO> checkPointsFromForm(List<PointDTO> points) {
        for (PointDTO point : points) {
            if (!validationBean.validateForm(point)) {
                throw new IllegalArgumentException("Invalid point");
            }
        }
        return areaCheckService.checkPoints(points);
    }

    public List<CheckResultDTO> checkPointsFromGraph(List<PointDTO> points, double graphXMin, double graphXMax, double graphYMin, double graphYMax) {
        for (PointDTO point : points) {
            if (!validationBean.validateGraphClick(point, graphXMin, graphXMax, graphYMin, graphYMax)) {
                throw new IllegalArgumentException("Invalid point");
            }
        }
        return areaCheckService.checkPoints(points);
    }
}
