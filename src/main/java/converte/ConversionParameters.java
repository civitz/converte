package converte;

public class ConversionParameters {
	private final String name;
	private final int sampleRate;
	private final int bitRate;
	private final int channels = 2;

	public ConversionParameters(String name, int sampleRate, int bitRate) {
		super();
		this.name = name;
		this.sampleRate = sampleRate;
		this.bitRate = bitRate;
	}

	public String getName() {
		return name;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getBitRate() {
		return bitRate;
	}

	public int getChannels() {
		return channels;
	}

	@Override
	public String toString() {
		return "ConversionConfig [name=" + name + ", sampleRate=" + sampleRate + ", bitRate=" + bitRate + ", channels="
				+ channels + "]";
	}
}
