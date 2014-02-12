package Unimplemented;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.util.ShapeUtilities;

/**
 * Can place in the data and produce a graph to display it
 * @author MOREPOWER
 *
 */
public class Plot extends ApplicationFrame {
	
	private static final long serialVersionUID = 1L;
	static DecimalFormat dcf = new DecimalFormat("0.00");	
	private ArrayList<ArrayList<Double>> datas = new ArrayList<ArrayList<Double>>();
	
	public Plot(String title, ArrayList<ArrayList<Double>> datapoints) {
		super(title);
		JPanel jpanel = createDemoPanel(datapoints);
		jpanel.setPreferredSize(new Dimension(1000, 800));
		setContentPane(jpanel);
	}
	
	public Plot(String title) {
		super(title);
	}
	
	public JPanel createDemoPanel(ArrayList<ArrayList<Double>> datapoints) {
		JFreeChart jfreechart = ChartFactory.createScatterPlot(
				"Scatter Plot", "time", "fps", getXYDataset(datapoints),
				PlotOrientation.VERTICAL, true, true, false);
		Shape cross = ShapeUtilities.createDiagonalCross(2, 1);
		XYPlot xyplot = (XYPlot) jfreechart.getPlot();
		xyplot.setDomainCrosshairVisible(true);
		xyplot.setRangeCrosshairVisible(true);
		xyplot.setBackgroundPaint(Color.white);
		xyplot.setDomainGridlinePaint(Color.white);
		xyplot.setRangeGridlinePaint(Color.white);
		XYItemRenderer renderer = xyplot.getRenderer();
		renderer.setSeriesShape(0, cross);
		renderer.setSeriesPaint(0, Color.red);
		return new ChartPanel(jfreechart);
		
	}
	
	public XYDataset getXYDataset(ArrayList<ArrayList<Double>> datapoints) {
		String datasize = Integer.toString(datapoints.size());
		XYSeries series = new XYSeries("data size: " + datasize);
		int j = 0;
		for(int i = 0; i < datapoints.size(); i++) {
			ArrayList<Double> da = new ArrayList<Double>();
			double id = datapoints.get(i).get(0);
			da.add(id);
			double x = datapoints.get(i).get(1); // true value
			da.add(x);
			double y = datapoints.get(i).get(2); // sampled value
			da.add(y);
			series.add(x, y);
			datas.add(da);
			j = i;
		}
		System.out.println(j); 
		XYSeriesCollection xySeriesCollection = new XYSeriesCollection(series);
		return xySeriesCollection;
	}
}
