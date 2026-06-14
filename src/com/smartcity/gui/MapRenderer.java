package com.smartcity.gui;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import com.smartcity.traffic.road.Junction;
import com.smartcity.traffic.road.Road;


public class MapRenderer {

	  private double zoomFactor;

	  public MapRenderer() {
	  this.zoomFactor = 1.0;
	  }

	  public void setZoomFactor(double zoomFactor) {
	  this.zoomFactor = zoomFactor;
	  }

public void render(Graphics g,List<Road> roads,List<Junction> junctions) {

		    Graphics2D g2 = (Graphics2D) g;

		    drawRoads(g2, roads);

		    drawJunctions(g2, junctions);
		    }
//Đoạn này dùng vễ road đang dễ 1 màu+cần chỉnh các điểm cần render
private void drawRoads(Graphics2D g, //render road
List<Road> roads) {

  g.setColor(Color.DARK_GRAY);

  for (Road road : roads) {

  
   int x1 = scale(road.getStartX());
   int y1 = scale(road.getStartY());

   int x2 = scale(road.getEndX());
   int y2 = scale(road.getEndY());

   g.setStroke(new BasicStroke(40));

   g.drawLine(x1, y1, x2, y2);
  


  }
  }
//Như vẽ road cần chỉnh các điểm đầu cuối để render 
private void drawJunctions(		  Graphics2D g,
		  List<Junction> junctions) {

		    g.setColor(Color.GRAY);

		    for (Junction junctions1
		    : junctions) {

		    //cần chỉnh đầu cuối render đoạn nầy
		     int x = scale(junctions1.getX());
		     int y = scale(junctions1.getY());

		     g.fillOval(
		             x - 25,
		             y - 25,
		             50,
		             50);
		    


		    }
		    }


}