package Utils;

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
		@Override
		public String toString() {
			return HardCode.dcfP.format(value);
		}
	};
}
