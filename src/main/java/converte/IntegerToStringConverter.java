package converte;

import javafx.util.StringConverter;

class IntegerToStringConverter extends StringConverter<Integer> {
	
	private final String unit;
	private final int factor;
	
	public IntegerToStringConverter(String unit, int factor) {
		super();
		this.unit = unit;
		this.factor = factor;
	}

	@Override
	public String toString(Integer paramT) {
		if(paramT == null) {
			return null;
		}
		return String.format("%d%s", paramT / factor, unit);
	}

	@Override
	public Integer fromString(String paramString) {
		return null;
	}
}