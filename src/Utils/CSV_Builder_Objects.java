package Utils;

/**
 * A class that holds a series of objects for formatting purposes within the CSV_Builder implementation
 * 	Overriding the toString() method of an object will override how a CSV_Builder will print
 * @author MOREPOWER
 *
 */
public class CSV_Builder_Objects {
	/**
	 * Nested class to help correctly format doubles for CSVs
	 * @author MOREPOWER
	 *
	 */
	public static class CSV_Double extends Object {
		Double value;
		public CSV_Double(Double value) {
			this.value = value;
		}
		public CSV_Double(double value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return HardCode.dcf3.format(value);
		}
	};
	
	public static class CSV_Percent extends Object {
		Double value;
		public CSV_Percent(Double value) {
			this.value = value;
		}
		public CSV_Percent(double value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return HardCode.dcfP.format(value);
		}
	};
}
