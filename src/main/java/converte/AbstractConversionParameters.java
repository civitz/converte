package converte;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

@Value.Immutable
@Value.Style(allMandatoryParameters = true, typeAbstract = "Abstract*", typeImmutable = "*")
public interface AbstractConversionParameters {

	public String getName();

	public int getSampleRate();

	public int getBitRate();

	@Default
	default Channels getChannels() {
		return Channels.STEREO;
	}
	
	public static enum Channels {
		MONO,STEREO;
	}
}
