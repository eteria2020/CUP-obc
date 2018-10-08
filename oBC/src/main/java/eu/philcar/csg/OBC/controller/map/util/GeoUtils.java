package eu.philcar.csg.OBC.controller.map.util;

import android.location.Location;

public class GeoUtils {

    /**
     * Computes the distance from A to B in meters
     *
     * @param firstPodouble
     * @param secondPodouble
     * @return
     */
    public static final double harvesineDistance(Location firstPodouble, Location secondPodouble) {

        double dLat = Math.toRadians(firstPodouble.getLatitude() - secondPodouble.getLatitude());
        double dLon = Math.toRadians(firstPodouble.getLongitude() - secondPodouble.getLongitude());
        double lat1 = Math.toRadians(secondPodouble.getLatitude());
        double lat2 = Math.toRadians(firstPodouble.getLatitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000 * c;
    }

    //Compute the dot product AB ⋅ BC
    public static double dot(Location A, Location B, Location C) {

        double AB[] = new double[2];
        double BC[] = new double[2];

        AB[0] = B.getLatitude() - A.getLatitude();
        AB[1] = B.getLongitude() - A.getLongitude();
        BC[0] = C.getLatitude() - B.getLatitude();
        BC[1] = C.getLongitude() - B.getLongitude();

        double dot = AB[0] * BC[0] + AB[1] * BC[1];

        return dot;
    }

    //Compute the cross product AB x AC
    public static double cross(Location A, Location B, Location C) {

        double AB[] = new double[2];
        double AC[] = new double[2];

        AB[0] = B.getLatitude() - A.getLatitude();
        AB[1] = B.getLongitude() - A.getLongitude();
        AC[0] = C.getLatitude() - A.getLatitude();
        AC[1] = C.getLongitude() - A.getLongitude();

        double cross = AB[0] * AC[1] - AB[1] * AC[0];

        return cross;
    }

    // Compute the distance from A to B in meters
    public static double distance(Location A, Location B) {

        double d1 = A.getLatitude() - B.getLatitude();
        double d2 = A.getLongitude() - B.getLongitude();

        double angle = Math.sqrt(d1 * d1 + d2 * d2);

        return 27840853 * angle / 365;
    }

    //Compute the distance from AB to C
    //if isSegment is true, AB is a segment, not a line.
    public static double linePodoubleDist(Location A, Location B, Location C, boolean isSegment) {

        double dist = cross(A, B, C) / distance(A, B);

        if (isSegment) {

            double dot1 = dot(A, B, C);

            if (dot1 > 0) return distance(B, C);

            double dot2 = dot(B, A, C);

            if (dot2 > 0) return distance(A, C);
        }

        return Math.abs(dist);
    }
    /*
    public static boolean isPointInPolygon(double pointLat, double pointLng, double[] closedPolygon) { 

    	int crossings = 0;
    	
    	// for each edge
        for (int  i=0; i < closedPolygon.length; i+=2) {
        	
            double segStartLat = closedPolygon[i];
            double segStartLng = closedPolygon[i+1];
            
            int j = i + 2;
            
            if (j >= closedPolygon.length) {
                j = 0;
            }
            
            double segEndLat = closedPolygon[j];
            double segEndLng = closedPolygon[j+1];
            
            if (rayCrossesSegment(pointLat, pointLng, segStartLat, segStartLng, segEndLat, segEndLng)) {
            	crossings++;
            }
        }
        
        return (crossings % 2 == 1);
    }*/

    public static boolean contains(double pointLat, double pointLng, double[] closedPolygon) {
        double[] lastPoint = new double[]{closedPolygon[closedPolygon.length - 2], closedPolygon[closedPolygon.length - 1]};
        boolean isInside = false;

        for (int i = 0; i < closedPolygon.length; i += 2) {

            double x1 = lastPoint[1];
            double x2 = closedPolygon[i + 1];
            double dx = x2 - x1;

            if (Math.abs(dx) > 180.0) {
                // we have, most likely, just jumped the dateline (could do further validation to this effect if needed).  normalise the numbers.
                if (pointLng > 0) {
                    while (x1 < 0)
                        x1 += 360;
                    while (x2 < 0)
                        x2 += 360;
                } else {
                    while (x1 > 0)
                        x1 -= 360;
                    while (x2 > 0)
                        x2 -= 360;
                }
                dx = x2 - x1;
            }

            if ((x1 <= pointLng && x2 > pointLng) || (x1 >= pointLng && x2 < pointLng)) {
                double grad = (closedPolygon[i] - lastPoint[0]) / dx;
                double intersectAtLat = lastPoint[0] + ((pointLng - x1) * grad);

                if (intersectAtLat > pointLat)
                    isInside = !isInside;
            }

            lastPoint[0] = closedPolygon[i];
            lastPoint[1] = closedPolygon[i + 1];
        }

        return isInside;
    }
    
    /*public static boolean rayCrossesSegment(double pLat, double pLng, double segStartLat, double segStartLng, double segEndLat, double segEndLng) {
        
    	double px = pLng;
        double py = pLat;
        double ax = segStartLng;
        double ay = segStartLat;
        double bx = segEndLng;
        double by = segEndLat;
    	
    	if (ay > by) {
            ax = segEndLng;
            ay = segEndLat;
            bx = segStartLng;
            by = segEndLat;
        }
    	
        // alter longitude to cater for 180 degree crossings
        if (px < 0) { px += 360.0; };
        if (ax < 0) { ax += 360.0; };
        if (bx < 0) { bx += 360.0; };

        if (py == ay || py == by) py += 0.00000001;
        if ((py > by || py < ay) || (px > Math.max(ax, bx))) return false;
        if (px < Math.min(ax, bx)) return true;

        double red = (ax != bx) ? ((by - ay) / (bx - ax)) : Double.MAX_VALUE;
        double blue = (ax != px) ? ((py - ay) / (px - ax)) : Double.MAX_VALUE;
        
        return (blue >= red);
    }*/

    public static double[] getClosestPointOnPolygon(double pY, double pX, double[] polygon) {

        double minDist = 0, fTo = 0.0, x = 0.0, y = 0.0, dist;
        int i = 0;

        if (polygon.length > 1) {

            for (int n = 2; n < polygon.length; n += 2) {

                if (polygon[n + 1] != polygon[n - 1]) {
                    double a = (polygon[n] - polygon[n - 2]) / (polygon[n + 1] - polygon[n - 1]);
                    double b = polygon[n] - a * polygon[n + 1];
                    dist = Math.abs(a * pX + b - pY) / Math.sqrt(a * a + 1);
                } else {
                    dist = Math.abs(pX - polygon[n + 1]);
                }

                // length^2 of line segment 
                double rl2 = Math.pow(polygon[n] - polygon[n - 2], 2) + Math.pow(polygon[n + 1] - polygon[n - 1], 2);

                // distance^2 of pt to end line segment
                double ln2 = Math.pow(polygon[n] - pY, 2) + Math.pow(polygon[n + 1] - pX, 2);

                // distance^2 of pt to begin line segment
                double lnm12 = Math.pow(polygon[n - 2] - pY, 2) + Math.pow(polygon[n - 1] - pX, 2);

                // minimum distance^2 of pt to infinite line
                double dist2 = Math.pow(dist, 2);

                // calculated length^2 of line segment
                double calcrl2 = ln2 - dist2 + lnm12 - dist2;

                // redefine minimum distance to line segment (not infinite line) if necessary
                if (calcrl2 > rl2)
                    dist = Math.sqrt(Math.min(ln2, lnm12));

                if ((minDist == 0) || (minDist > dist)) {
                    if (calcrl2 > rl2) {
                        if (lnm12 < ln2) {
                            fTo = 0;//nearer to previous point
                            //fFrom = 1;
                        } else {
                            //fFrom = 0;//nearer to current point
                            fTo = 1;
                        }
                    } else {
                        // perpendicular from point intersects line segment
                        fTo = ((Math.sqrt(lnm12 - dist2)) / Math.sqrt(rl2));
                        //fFrom = ((Math.sqrt(ln2 - dist2))   / Math.sqrt(rl2));
                    }
                    minDist = dist;
                    i = n;
                }
            }

            double dx = polygon[i - 1] - polygon[i + 1];
            double dy = polygon[i - 2] - polygon[i];

            x = polygon[i - 1] - (dx * fTo);
            y = polygon[i - 2] - (dy * fTo);

        }

        return new double[]{x, y};
    }

    public static double bearing(double startLat, double startLng, double endLat, double endLng) {

        double lat1 = Math.toRadians(startLat);
        double lat2 = Math.toRadians(endLat);
        double lng1 = Math.toRadians(startLng);
        double lng2 = Math.toRadians(endLng);

        double y = Math.sin(lng2 - lng1) * Math.cos(lat2);

        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lng2 - lng1);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
