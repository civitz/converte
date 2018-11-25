package converte;

import javafx.util.StringConverter;

final class ConversionParametersStringConverter extends StringConverter<ConversionParameters> {
	@Override
	public String toString(ConversionParameters config) {
		return String.format("%s: %dk %dKHz", config.getName(), config.getBitRate()/1024, config.getSampleRate()/1000);
	}

	@Override
	public ConversionParameters fromString(String paramString) {
		return null;
	}
}