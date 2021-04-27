package converte;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(typeAbstract = "Abstract*", typeImmutable = "*", jdkOnly = true)
abstract class AbstractFfmpegParameters {
	public abstract String ffmpegPath();

	public abstract String ffprobePath();
}
