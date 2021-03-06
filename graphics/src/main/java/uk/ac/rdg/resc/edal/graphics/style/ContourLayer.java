/*******************************************************************************
 * Copyright (c) 2013 The University of Reading
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package uk.ac.rdg.resc.edal.graphics.style;

import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.ContourLineAttribute;
import gov.noaa.pmel.sgt.DefaultContourLineAttribute;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SimpleGrid;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Range2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.util.Array2D;
import uk.ac.rdg.resc.edal.util.Extents;

public class ContourLayer extends GriddedImageLayer {
    public enum ContourLineStyle {
        SOLID {
            @Override
            public int getLineStyleInteger() {
                return ContourLineAttribute.SOLID;
            }
        },

        DASHED {
            @Override
            public int getLineStyleInteger() {
                return ContourLineAttribute.DASHED;
            }
        },
        
        HEAVY {
            @Override
            public int getLineStyleInteger() {
                return ContourLineAttribute.HEAVY;
            }
        },
        
        HIGHLIGHT {
            @Override
            public int getLineStyleInteger() {
                return ContourLineAttribute.HIGHLIGHT;
            }
        },
        
        MARK {
            @Override
            public int getLineStyleInteger() {
                return ContourLineAttribute.MARK;
            }
        },
        
        MARK_LINE {
            @Override
            public int getLineStyleInteger() {
                return ContourLineAttribute.MARK_LINE;
            }
        },
        
        STROKE {
            @Override
            public int getLineStyleInteger() {
                return ContourLineAttribute.STROKE;
            }
        };
        
        public abstract int getLineStyleInteger();
    }

    private String dataFieldName;
    private ScaleRange scale;
    
    private Boolean autoscaleEnabled = true;
    private Double numberOfContours = 10.0;
    private Color contourLineColour = Color.BLACK;
    private Integer contourLineWidth = 1;
    private ContourLineStyle contourLineStyle = ContourLineStyle.DASHED;
    private Boolean labelEnabled = true;
    
    public ContourLayer(String dataFieldName, ScaleRange scale, boolean autoscaleEnabled, 
    		double numberOfContours, Color contourLineColour, int contourLineWidth, ContourLineStyle contourLineStyle, boolean labelEnabled) {
    	this.dataFieldName = dataFieldName;
    	this.scale = scale;
    	this.autoscaleEnabled = autoscaleEnabled;
    	this.numberOfContours = numberOfContours;
    	this.contourLineColour = contourLineColour;
    	this.contourLineWidth = contourLineWidth;
    	this.contourLineStyle = contourLineStyle;
    	this.labelEnabled = labelEnabled;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    public ScaleRange getScale() {
		return scale;
	}
	
	public boolean isAutoscaleEnabled() {
		return autoscaleEnabled;
	}

	public double getNumberOfContours() {
		return numberOfContours;
	}
	
	public Color getContourLineColour() {
		return contourLineColour;
	}
	
	public int getContourLineWidth() {
		return contourLineWidth;
	}

	public ContourLineStyle getContourLineStyle() {
		return contourLineStyle;
	}

	public boolean isLabelEnabled() {
		return labelEnabled;
	}

	@Override
	protected void drawIntoImage(BufferedImage image, MapFeatureDataReader dataReader) throws EdalException  {
		int width = image.getWidth();
		int height = image.getHeight();
		double[] values = new double[width * height];
        double[] xAxis = new double[width];
        double[] yAxis = new double[height];

        int count = 0;
        for (int i = 0; i < width; i++) {
            xAxis[i] = i;
            for (int j = 0; j < height; j++) {
                yAxis[j] = height - j - 1;
                values[count] = Double.NaN;
                count++;
            }
        }
        
        Float scaleMin = null;
        Float scaleMax = null;
        if (autoscaleEnabled) {
        	scaleMin = Float.MAX_VALUE;
        	scaleMax = -Float.MAX_VALUE;
        } else {
            scaleMin = scale.getScaleMin();
            scaleMax = scale.getScaleMax();
        }
        
        Array2D<Number> dataValues = dataReader.getDataForLayerName(dataFieldName);
        for(int j=0; j<height; j++) {
            for(int i=0; i< width;i++){
                Number value = dataValues.get(j,i);
                float val;
                if(value == null) {
                    val = Float.NaN;
                } else {
                    val = value.floatValue();
                }
                /*
                 * SGT goes against the grain somewhat by specifying that the y-axis
                 * values vary fastest.
                 */
                values[j + i*height] = val;
                if (autoscaleEnabled) {
                    if (val < scaleMin) scaleMin = val;
                    if (val > scaleMax) scaleMax = val;
                }
            }
        }
		
		SGTGrid sgtGrid = new SimpleGrid(values, xAxis, yAxis, null);

        CartesianGraph cg = getCartesianGraph(sgtGrid, width, height);

        double contourSpacing = (scaleMax - scaleMin) / numberOfContours;

        Range2D contourValues = new Range2D(scaleMin, scaleMax, contourSpacing);

        ContourLevels clevels = ContourLevels.getDefault(contourValues);

        DefaultContourLineAttribute defAttr = new DefaultContourLineAttribute();

        defAttr.setColor(contourLineColour);
        if(contourLineStyle != null) {
            defAttr.setStyle(contourLineStyle.getLineStyleInteger());
        }
        defAttr.setWidth(contourLineWidth);
        defAttr.setLabelEnabled(labelEnabled);
        clevels.setDefaultContourLineAttribute(defAttr);

        GridAttribute attr = new GridAttribute(clevels);
        attr.setStyle(GridAttribute.CONTOUR);

        CartesianRenderer renderer = CartesianRenderer.getRenderer(cg, sgtGrid, attr);

        Graphics g = image.getGraphics();
        renderer.draw(g);
	}

    private static CartesianGraph getCartesianGraph(SGTData data, int width, int height) {
        /*
         * To get fixed size labels we need to set a physical size much smaller
         * than the pixel size (since pixels can't represent physical size).
         * Since the SGT code is so heavily tied into the display mechanism, and
         * a factor of around 100 seems to produce decent results, it's almost
         * certainly measured in inches (96dpi being a fairly reasonable monitor
         * resolution).
         * 
         * Anyway, setting the physical size as a constant factor of the pixel
         * size gives good results.
         * 
         * Font size seems to be ignored.
         */
        double factor = 96;
        double physWidth = width / factor;
        double physHeight = height / factor;

        Layer layer = new Layer("", new Dimension2D(physWidth, physHeight));
        JPane pane = new JPane("id", new Dimension(width, height));
        layer.setPane(pane);
        layer.setBounds(0, 0, width, height);

        CartesianGraph graph = new CartesianGraph();
        // Create Ranges representing the size of the image
        Range2D physXRange = new Range2D(0, physWidth);
        Range2D physYRange = new Range2D(0, physHeight);
        // These transforms convert x and y coordinates to pixel indices
        LinearTransform xt = new LinearTransform(physXRange, data.getXRange());
        LinearTransform yt = new LinearTransform(physYRange, data.getYRange());
        graph.setXTransform(xt);
        graph.setYTransform(yt);
        layer.setGraph(graph);
        return graph;
    }
    
    @Override
    public Set<NameAndRange> getFieldsWithScales() {
        Set<NameAndRange> ret = new HashSet<Drawable.NameAndRange>();
        ret.add(new NameAndRange(dataFieldName, Extents.newExtent(scale.getScaleMin(), scale.getScaleMax())));
        return ret;
    }
}
