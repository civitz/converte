package converte;

import converte.AbstractConversionParameters.Channels;
import javafx.util.StringConverter;

final class ConversionParametersStringConverter extends StringConverter<ConversionParameters> {
	@Override
	public String toString(ConversionParameters config) {
		return String.format("%s: %dk %dKHz %s", config.getName(), config.getBitRate() / 1024,
				config.getSampleRate() / 1000, config.getChannels() == Channels.MONO ? "mono" : "stereo");
	}

	@Override
	public ConversionParameters fromString(String paramString) {
		throw new UnsupportedOperationException();
	}
}