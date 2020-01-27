package com.cscd488_490.team5.i_nav;

import java.util.List;

import i_nav.Edge;
import i_nav_model.Location;
import i_nav_model.LocationObject;

public class LocationMapCacheItem {

    Location location;
    List<LocationObject> objects;
    List<Edge> edges;
    String canvas_image;

    @Override
    public String toString() {
        String str = "CACHE ITEM: ";
        if (location != null) {
            str += "location: " + location.getJSONString() + " ";
        } else {
            str += "location NULL ";
        }
        str += "image: " + canvas_image + " ";
        str += "objects: ";
        if (objects != null) {
            for (LocationObject o : objects) {
                str += o.getJSONString() + " ";
            }
        } else {
            str += "objects NULL ";
        }
        if (edges != null) {
            for (Edge e : edges) {
                str += e.getJson() + " ";
            }
        } else {
            str += "edges NULL ";
        }

        return str;
    }
}
